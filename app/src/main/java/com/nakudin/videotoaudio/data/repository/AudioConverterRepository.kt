package com.nakudin.videotoaudio.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.ReturnCode
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.Statistics
import com.nakudin.videotoaudio.domain.Channels
import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.usecase.AudioConverter
import com.nakudin.videotoaudio.model.ConversionRequest
import com.nakudin.videotoaudio.model.ConversionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/** FFmpeg-based implementation of [AudioConverter]. */
class AudioConverterRepository : AudioConverter {

    @Volatile
    private var currentSession: FFmpegSession? = null

    override suspend fun convert(
        context: Context,
        request: ConversionRequest,
        onProgress: (Int) -> Unit
    ): ConversionResult = withContext(Dispatchers.IO) {
        try {
            val inputPath = resolveInputPath(context, request.inputUri)

            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            outputDir.mkdirs()

            // Guard against obviously insufficient storage before starting a
            // potentially long conversion that would fail partway through.
            if (runCatching { StatFs(outputDir.absolutePath).availableBytes < 5_000_000L }
                    .getOrDefault(false)
            ) {
                cleanupTempInput(inputPath)
                return@withContext ConversionResult.Error(
                    "Not enough free storage space to save the audio file."
                )
            }

            val outputFile = resolveUniqueOutputFile(outputDir, request)

            // Reject videos that have no audio track up front with a clear message.
            if (!hasAudioStream(inputPath)) {
                cleanupTempInput(inputPath)
                return@withContext ConversionResult.Error(
                    "The selected video contains no audio track to extract."
                )
            }

            val command = buildCommand(inputPath, outputFile.absolutePath, request)
            val durationMs = if (request.trimEndSeconds > request.trimStartSeconds) {
                ((request.trimEndSeconds - request.trimStartSeconds) * 1000).toLong().coerceAtLeast(1)
            } else {
                getDurationMs(inputPath)
            }

            val result = suspendCancellableCoroutine { cont ->
                val session = FFmpegKit.executeAsync(
                    command,
                    { completed ->
                        if (!cont.isActive) return@executeAsync
                        val rc = completed.returnCode
                        val finalResult = when {
                            ReturnCode.isSuccess(rc) -> {
                                if (outputFile.exists() && outputFile.length() > 0) {
                                    ConversionResult.Success(
                                        outputFile.absolutePath,
                                        estimateDuration(outputFile.absolutePath)
                                    )
                                } else {
                                    ConversionResult.Error("Output file was not created.")
                                }
                            }
                            ReturnCode.isCancel(rc) -> ConversionResult.Cancelled
                            else -> ConversionResult.Error(
                                "Conversion failed (code $rc)."
                            )
                        }
                        cleanupTempInput(inputPath)
                        cont.resume(finalResult)
                    },
                    null,
                    { stats ->
                        // FFmpegKit reports statistics time in milliseconds.
                        if (durationMs > 0 && stats.time > 0) {
                            val pct = (stats.time / durationMs.toDouble() * 100)
                                .toInt().coerceIn(0, 100)
                            onProgress(pct)
                        }
                    }
                )
                currentSession = session
                cont.invokeOnCancellation { runCatching { session.cancel() } }
            }
            result
        } catch (e: Exception) {
            ConversionResult.Error("Conversion failed: ${e.message}", e)
        }
    }

    override fun cancel() {
        currentSession?.cancel() ?: FFmpegKit.cancel()
    }

    private fun resolveInputPath(context: Context, inputUri: String): String {
        // Accept plain filesystem paths (also handle a possible file:// prefix)
        // and content URIs. Content URIs cannot be read directly by FFmpeg, so we
        // copy them to a temporary cache file first.
        val uriString = inputUri.removePrefix("file://")
        if (!uriString.startsWith("content://")) return uriString

        val uri = android.net.Uri.parse(uriString)
        val temp = File(context.cacheDir, "v2a_input_${System.currentTimeMillis()}.tmp")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException(
                "Could not open the selected video. It may have been moved or " +
                    "the access permission was revoked."
            )
        inputStream.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        return temp.absolutePath
    }

    private fun cleanupTempInput(path: String) {
        if (path.contains("v2a_input_") && path.contains("cache")) {
            runCatching { File(path).delete() }
        }
    }

    /**
     * Produce a filesystem-safe base name (no path, no extension) while keeping
     * Unicode characters and spaces, which are valid in Android filenames and
     * friendlier to users. Only characters that are illegal in paths or would
     * break shell argument parsing are replaced with an underscore.
     */
    private fun safeFileName(name: String): String {
        val cleaned = name
            .trim()
            .replace(Regex("""[\\/:*?"<>|`\$;&()\n\r\t]"""), "_")
            .replace(Regex("""\s{2,}"""), " ")
            .trim('.', ' ')
        return cleaned.ifBlank { "audio" }.take(120)
    }

    /**
     * Resolve a unique output [File] inside [dir] for the request.
     * If a file with the chosen name already exists, append an incrementing
     * suffix (e.g. "audio(1).mp3") instead of overwriting the existing one.
     */
    private fun resolveUniqueOutputFile(dir: File, request: ConversionRequest): File {
        val base = safeFileName(request.filename)
        val ext = when (request.outputFormat) {
            OutputFormat.MP3 -> "mp3"
            OutputFormat.M4A -> "m4a"
            OutputFormat.WAV -> "wav"
            OutputFormat.OGG -> "ogg"
        }
        val primary = File(dir, "$base.$ext")
        if (!primary.exists()) return primary
        var n = 1
        while (true) {
            val candidate = File(dir, "$base($n).$ext")
            if (!candidate.exists()) return candidate
            n++
        }
    }

    private fun buildCommand(input: String, output: String, request: ConversionRequest): String {
        val formatArgs = when (request.outputFormat) {
            OutputFormat.MP3 -> "-vn -c:a libmp3lame"
            OutputFormat.M4A -> "-vn -c:a aac"
            OutputFormat.WAV -> "-vn -c:a pcm_s16le"
            OutputFormat.OGG -> "-vn -c:a libvorbis"
        }
        // WAV (PCM) is uncompressed, so a bitrate is not applicable.
        val bitrate = if (request.outputFormat == OutputFormat.WAV) {
            ""
        } else {
            request.bitrate?.let { "-b:a ${it.value}k" } ?: ""
        }
        val sampleRate = request.sampleRate?.let { "-ar ${it.value}" } ?: ""
        val channels = when (request.channels) {
            Channels.MONO -> "-ac 1"
            Channels.STEREO -> "-ac 2"
        }
        // Fast + accurate trimming: input seeking (-ss) with an explicit
        // output duration (-t). The original video is never modified.
        val trimming = request.trimEndSeconds > request.trimStartSeconds
        val seek = if (trimming) "-ss ${fmt(request.trimStartSeconds)}" else ""
        val durationOpt = if (trimming) {
            "-t ${fmt(request.trimEndSeconds - request.trimStartSeconds)}"
        } else ""
        return listOf(
            "-y",
            seek,
            "-i", quote(input),
            durationOpt,
            formatArgs,
            bitrate,
            sampleRate,
            channels,
            quote(output)
        ).filter { it.isNotBlank() }.joinToString(" ")
    }

    /** Format a double with a dot decimal separator regardless of device locale. */
    private fun fmt(value: Double): String = String.format(java.util.Locale.US, "%.3f", value)

    private fun quote(arg: String): String = "\"" + arg.replace("\"", "\\\"") + "\""

    private fun hasAudioStream(path: String): Boolean {
        return runCatching {
            val info = FFprobeKit.getMediaInformation(path)
            info?.streams?.any { stream ->
                stream.type.equals("audio", ignoreCase = true)
            } ?: true
        }.getOrDefault(true)
    }

    private fun getDurationMs(path: String): Long {
        return runCatching {
            val info = FFprobeKit.getMediaInformation(path)
            info?.duration?.toDoubleOrNull()?.times(1000)?.toLong() ?: 0L
        }.getOrDefault(0L)
    }

    private fun estimateDuration(path: String): Long = getDurationMs(path)
}
