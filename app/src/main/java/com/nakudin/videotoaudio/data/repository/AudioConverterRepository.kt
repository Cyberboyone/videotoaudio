package com.nakudin.videotoaudio.data.repository

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.nakudin.videotoaudio.domain.Channels
import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.usecase.AudioConverter
import com.nakudin.videotoaudio.model.ConversionRequest
import com.nakudin.videotoaudio.model.ConversionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * On-device audio conversion engine built on Android's MediaCodec /
 * MediaExtractor / MediaMuxer. No external native dependency is required.
 *
 * Pipeline: extract the audio track -> decode to 16-bit PCM -> (optionally)
 * resample / re-channel the PCM -> mux/encode to the requested container
 * (WAV = raw PCM, M4A = AAC in MP4, MP3 = raw MP3 frames).
 *
 * The decoded PCM is written to a temporary file on disk and streamed into
 * the encoder in fixed-size chunks. The previously used in-memory
 * [java.io.ByteArrayOutputStream] approach caused OutOfMemoryError on any
 * but tiny inputs, because the full decoded audio (often 100+ MB) was held
 * in the heap at once.
 *
 * OGG/Vorbis is intentionally not supported because Android ships no
 * Vorbis encoder; MP3 is attempted and falls back to a clear error if the
 * device exposes no MP3 encoder.
 */
class AudioConverterRepository : AudioConverter {

    @Volatile
    private var cancelled = false

    override suspend fun convert(
        context: Context,
        request: ConversionRequest,
        onProgress: (Int) -> Unit
    ): ConversionResult = withContext(Dispatchers.IO) {
        cancelled = false
        var stage = "init"
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var fos: FileOutputStream? = null
        var pcmOut: FileOutputStream? = null
        val outputFile: File
        val pcmFile: File
        try {
            val inputPath = resolveInputPath(context, request.inputUri)

            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: context.filesDir
            outputDir.mkdirs()

            outputFile = resolveUniqueOutputFile(outputDir, request)
            pcmFile = File(context.cacheDir, "v2a_pcm_${System.currentTimeMillis()}.pcm")

            if (runCatching { StatFs(outputDir.absolutePath).availableBytes < 5_000_000L }
                    .getOrDefault(false)
            ) {
                cleanupTempInput(inputPath)
                cleanupPcm(pcmFile)
                return@withContext ConversionResult.Error(
                    "Not enough free storage space to save the audio file."
                )
            }

            extractor = MediaExtractor()
            extractor.setDataSource(inputPath)

            var audioIdx = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioIdx = i
                    break
                }
            }
            if (audioIdx < 0) {
                cleanupTempInput(inputPath)
                cleanupPcm(pcmFile)
                return@withContext ConversionResult.Error(
                    "The selected video contains no audio track to extract."
                )
            }

            val trackFormat = extractor.getTrackFormat(audioIdx)
            val srcMime = trackFormat.getString(MediaFormat.KEY_MIME)
                ?: return@withContext ConversionResult.Error("Unsupported audio track.")
            val srcSampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val srcChannelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val fullDurationUs = if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                trackFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                0L
            }

            val startUs = (request.trimStartSeconds * 1_000_000).toLong()
            val trimming = request.trimEndSeconds > request.trimStartSeconds
            val endUs = if (trimming) {
                (request.trimEndSeconds * 1_000_000).toLong()
            } else {
                Long.MAX_VALUE
            }
            val segDurationUs = if (trimming) {
                (endUs - startUs).coerceAtLeast(1_000_000)
            } else {
                fullDurationUs.coerceAtLeast(1_000_000)
            }

            if (request.trimStartSeconds > 0) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            }
            extractor.selectTrack(audioIdx)

            val targetSampleRate = request.sampleRate?.value ?: srcSampleRate
            val targetChannels = when (request.channels) {
                Channels.MONO -> 1
                Channels.STEREO -> 2
            }
            val needResample = (targetSampleRate != srcSampleRate) ||
                (targetChannels != srcChannelCount)

            // ---- Stage A: decode to PCM, streamed to a temp file ----
            stage = "decode"
            Log.d(
                "AudioConverter",
                "stage=decode mime=$srcMime rate=$srcSampleRate ch=$srcChannelCount " +
                    "targetRate=$targetSampleRate targetCh=$targetChannels"
            )
            decoder = MediaCodec.createDecoderByType(srcMime)
            decoder.configure(trackFormat, null, null, 0)
            decoder.start()

            pcmOut = FileOutputStream(pcmFile)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var lastPts = 0L
            var decodedBytes = 0L
            var pcmSampleRate = srcSampleRate
            var pcmChannels = srcChannelCount

            while (!outputDone && !cancelled) {
                if (!inputDone) {
                    val inIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val buf = decoder.getInputBuffer(inIdx)!!
                        if (trimming && lastPts >= endUs) {
                            decoder.queueInputBuffer(
                                inIdx, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                        } else {
                            val size = extractor.readSampleData(buf, 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(
                                    inIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                            } else {
                                lastPts = extractor.sampleTime
                                decoder.queueInputBuffer(inIdx, 0, size, lastPts, 0)
                                if (segDurationUs > 0 && lastPts >= startUs) {
                                    val pct = (((lastPts - startUs).toDouble() / segDurationUs) * 100)
                                        .toInt().coerceIn(0, 99)
                                    onProgress(pct)
                                }
                                extractor.advance()
                            }
                        }
                    }
                }

                val outIdx = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = decoder.outputFormat
                        pcmSampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        pcmChannels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    outIdx >= 0 -> {
                        val buf = decoder.getOutputBuffer(outIdx)!!
                        val bytes = ByteArray(info.size)
                        buf.get(bytes)
                        buf.clear()
                        val toWrite = if (needResample) {
                            convertPcm(
                                bytes, pcmSampleRate, pcmChannels,
                                targetSampleRate, targetChannels
                            )
                        } else {
                            bytes
                        }
                        pcmOut.write(toWrite)
                        decodedBytes += toWrite.size
                        decoder.releaseOutputBuffer(outIdx, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                        }
                    }
                }
            }
            decoder.stop()
            decoder.release()
            decoder = null
            pcmOut.close()
            pcmOut = null
            extractor.release()
            extractor = null

            if (cancelled) {
                cleanupTempInput(inputPath)
                cleanupPcm(pcmFile)
                return@withContext ConversionResult.Cancelled
            }

            if (decodedBytes == 0L) {
                cleanupTempInput(inputPath)
                cleanupPcm(pcmFile)
                return@withContext ConversionResult.Error(
                    "No audio data could be decoded from this video."
                )
            }

            stage = "decoded"
            Log.d("AudioConverter", "stage=decoded pcmBytes=$decodedBytes")

            // ---- Stage B: encode / write output, streaming from the PCM file ----
            val pcmBytesTotal = pcmFile.length()
            when (request.outputFormat) {
                OutputFormat.WAV -> {
                    stage = "wav"
                    writeWav(outputFile, pcmFile, targetSampleRate, targetChannels)
                }
                OutputFormat.M4A -> {
                    stage = "m4a-encode"
                    muxer = MediaMuxer(
                        outputFile.absolutePath,
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    )
                    encodeAndMux(
                        encoder = {
                            encoder = MediaCodec.createEncoderByType(
                                MediaFormat.MIMETYPE_AUDIO_AAC
                            ).also { enc ->
                                val f = MediaFormat.createAudioFormat(
                                    MediaFormat.MIMETYPE_AUDIO_AAC,
                                    targetSampleRate, targetChannels
                                )
                                f.setInteger(
                                    MediaFormat.KEY_BIT_RATE,
                                    (request.bitrate?.value ?: 128) * 1000
                                )
                                f.setInteger(
                                    MediaFormat.KEY_AAC_PROFILE,
                                    MediaCodecInfoProfileAacLC
                                )
                                f.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
                                enc.configure(
                                    f, null, null,
                                    MediaCodec.CONFIGURE_FLAG_ENCODE
                                )
                            }
                        },
                        getEncoder = { encoder!! },
                        releaseEncoder = {
                            encoder?.stop(); encoder?.release(); encoder = null
                        },
                        pcmFile = pcmFile,
                        pcmBytesTotal = pcmBytesTotal,
                        sampleRate = targetSampleRate,
                        channels = targetChannels,
                        muxer = muxer,
                        isMp3 = false,
                        outFile = outputFile,
                        getFos = { fos },
                        setFos = { fos = it },
                        onProgress = onProgress
                    )
                    Log.d("AudioConverter", "stage=m4a-encode-done")
                }
                OutputFormat.MP3 -> {
                    stage = "mp3-encode"
                    try {
                        encoder = MediaCodec.createEncoderByType(
                            MediaFormat.MIMETYPE_AUDIO_MPEG
                        )
                    } catch (e: Exception) {
                        cleanupTempInput(inputPath)
                        cleanupPcm(pcmFile)
                        return@withContext ConversionResult.Error(
                            "This device does not provide an MP3 encoder. " +
                                "Please choose M4A (AAC) instead.",
                            e
                        )
                    }
                    val f = MediaFormat.createAudioFormat(
                        MediaFormat.MIMETYPE_AUDIO_MPEG,
                        targetSampleRate, targetChannels
                    )
                    f.setInteger(
                        MediaFormat.KEY_BIT_RATE,
                        (request.bitrate?.value ?: 128) * 1000
                    )
                    f.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192)
                    encoder!!.configure(
                        f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE
                    )
                    fos = FileOutputStream(outputFile)
                    encodeAndMux(
                        encoder = { /* already configured */ },
                        getEncoder = { encoder!! },
                        releaseEncoder = {
                            encoder?.stop(); encoder?.release(); encoder = null
                        },
                        pcmFile = pcmFile,
                        pcmBytesTotal = pcmBytesTotal,
                        sampleRate = targetSampleRate,
                        channels = targetChannels,
                        muxer = null,
                        isMp3 = true,
                        outFile = outputFile,
                        getFos = { fos },
                        setFos = { fos = it },
                        onProgress = onProgress
                    )
                }
            }

            muxer?.stop()
            Log.d("AudioConverter", "stage=muxer-stopped")
            muxer?.release()
            muxer = null
            fos?.close()
            fos = null
            stage = "finished"
            Log.d("AudioConverter", "stage=finished")
            cleanupPcm(pcmFile)
            cleanupTempInput(inputPath)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                return@withContext ConversionResult.Error(
                    "Output file was not created."
                )
            }

            val durationMs = (segDurationUs / 1000).coerceAtLeast(1)
            onProgress(100)
            ConversionResult.Success(outputFile.absolutePath, durationMs)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("AudioConverter", "Conversion failed at stage=$stage", e)
            runCatching {
                decoder?.stop(); decoder?.release()
                encoder?.stop(); encoder?.release()
                muxer?.release()
                fos?.close()
                pcmOut?.close()
            }
            ConversionResult.Error("Conversion failed: ${e.message}", e)
        }
    }

    override fun cancel() {
        cancelled = true
    }

    // ------------------------------------------------------------------
    // Encoding helper. Drives a MediaCodec encoder over the supplied PCM
    // file (read in fixed-size chunks) and either muxes into [muxer]
    // (AAC) or writes raw frames to [outFile] (MP3).
    // ------------------------------------------------------------------
    private fun encodeAndMux(
        encoder: () -> Unit,
        getEncoder: () -> MediaCodec,
        releaseEncoder: () -> Unit,
        pcmFile: File,
        pcmBytesTotal: Long,
        sampleRate: Int,
        channels: Int,
        muxer: MediaMuxer?,
        isMp3: Boolean,
        outFile: File,
        getFos: () -> FileOutputStream?,
        setFos: (FileOutputStream?) -> Unit,
        onProgress: (Int) -> Unit
    ) {
        encoder()
        val enc = getEncoder()
        enc.start()
        val frameSize = channels * 2
        val info = MediaCodec.BufferInfo()
        var offset = 0L
        var inputDone = false
        var outputDone = false
        var muxerStarted = false
        var trackIdx = -1
        var eosQueuedAt = 0L
        val chunk = ByteArray(64 * 1024)
        pcmFile.inputStream().use { fis ->
            while (!outputDone && !cancelled) {
                // Safety guard: once the EOS input has been queued, allow the
                // encoder a bounded window to flush. Some device encoders never
                // surface the EOS output buffer, which would otherwise leave
                // this loop spinning forever (progress stuck at 100%).
                if (inputDone && eosQueuedAt > 0 &&
                    System.currentTimeMillis() - eosQueuedAt > 5000L
                ) {
                    Log.w("AudioConverter", "encode loop: no EOS from encoder after 5s, finalizing")
                    break
                }
                if (!inputDone) {
                    val inIdx = enc.dequeueInputBuffer(TIMEOUT_US)
                    if (inIdx >= 0) {
                        val inBuf = enc.getInputBuffer(inIdx)!!
                        val remaining = pcmBytesTotal - offset
                        if (remaining <= 0) {
                            enc.queueInputBuffer(
                                inIdx, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputDone = true
                            eosQueuedAt = System.currentTimeMillis()
                        } else {
                            val toRead = minOf(remaining, inBuf.remaining().toLong()).toInt()
                            val read = fis.read(chunk, 0, toRead)
                            if (read <= 0) {
                                enc.queueInputBuffer(
                                    inIdx, 0, 0, 0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                inputDone = true
                                eosQueuedAt = System.currentTimeMillis()
                            } else {
                                inBuf.put(chunk, 0, read)
                                val ptsUs = (offset / frameSize.toDouble() / sampleRate * 1_000_000)
                                    .toLong().coerceAtLeast(0)
                                enc.queueInputBuffer(inIdx, 0, read, ptsUs, 0)
                                offset += read
                                if (pcmBytesTotal > 0) {
                                    val pct = ((offset.toDouble() / pcmBytesTotal) * 100)
                                        .toInt().coerceIn(0, 100)
                                    onProgress(pct)
                                }
                                if (offset >= pcmBytesTotal) inputDone = true
                            }
                        }
                    }
                }

                val outIdx = enc.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (muxer != null) {
                            trackIdx = muxer.addTrack(enc.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outIdx >= 0 -> {
                        val buf = enc.getOutputBuffer(outIdx)!!
                        if (isMp3) {
                            val bytes = ByteArray(info.size)
                            buf.get(bytes)
                            buf.clear()
                            getFos()?.write(bytes)
                        } else if (muxer != null && muxerStarted &&
                            (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                        ) {
                            muxer.writeSampleData(trackIdx, buf, info)
                        }
                        enc.releaseOutputBuffer(outIdx, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            outputDone = true
                            Log.d("AudioConverter", "encode loop outputDone=true")
                        }
                    }
                }
            }
        }
        releaseEncoder()
        if (isMp3) {
            getFos()?.close()
            setFos(null)
        }
    }

    // ------------------------------------------------------------------
    // PCM utilities
    // ------------------------------------------------------------------

    /**
     * Resample (linear interpolation) and re-channel [pcm] (16-bit interleaved)
     * from [srcRate]/[srcCh] to [dstRate]/[dstCh]. Returns the input unchanged
     * when rates and channels already match. Safe to call on independent
     * contiguous chunks of the stream.
     */
    private fun convertPcm(
        pcm: ByteArray,
        srcRate: Int,
        srcCh: Int,
        dstRate: Int,
        dstCh: Int
    ): ByteArray {
        if (srcRate == dstRate && srcCh == dstCh) return pcm
        val srcSamples = pcm.size / (2 * srcCh)
        val dstSamples = (srcSamples.toDouble() * dstRate / srcRate).toInt()
            .coerceAtLeast(0)
        val out = ByteArray(dstSamples * 2 * dstCh)
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        for (i in 0 until dstSamples) {
            val pos = i * ratio
            val i0 = pos.toInt().coerceIn(0, srcSamples - 1)
            val i1 = (i0 + 1).coerceIn(0, srcSamples - 1)
            val frac = (pos - i0).coerceIn(0.0, 1.0)
            for (c in 0 until dstCh) {
                val s0 = sampleAt(pcm, srcCh, i0, c, dstCh)
                val s1 = sampleAt(pcm, srcCh, i1, c, dstCh)
                val v = (s0 + (s1 - s0) * frac).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                val idx = (i * dstCh + c) * 2
                out[idx] = (v and 0xFF).toByte()
                out[idx + 1] = ((v shr 8) and 0xFF).toByte()
            }
        }
        return out
    }

    /** Read a 16-bit sample for destination channel [dstC] (handles up/down-mix). */
    private fun sampleAt(
        pcm: ByteArray,
        srcCh: Int,
        i: Int,
        dstC: Int,
        dstCh: Int
    ): Int {
        return when {
            srcCh == dstCh -> getChannel(pcm, srcCh, i, dstC)
            dstCh == 1 -> { // down-mix to mono: average source channels
                var sum = 0
                for (c in 0 until srcCh) sum += getChannel(pcm, srcCh, i, c)
                sum / srcCh
            }
            srcCh == 1 -> getChannel(pcm, 1, i, 0) // up-mix mono to all dst channels
            else -> getChannel(pcm, srcCh, i, dstC.coerceIn(0, srcCh - 1))
        }
    }

    private fun getChannel(pcm: ByteArray, ch: Int, i: Int, c: Int): Int {
        val idx = (i * ch + c) * 2
        if (idx + 1 >= pcm.size) return 0
        val lo = pcm[idx].toInt() and 0xFF
        val hi = pcm[idx + 1].toInt()
        val v = lo or (hi shl 8)
        return if (v >= 0x8000) v - 0x10000 else v
    }

    /** Write a 16-bit PCM WAV file with a standard 44-byte RIFF header,
     *  streaming the PCM payload from [pcmFile] so memory stays bounded. */
    private fun writeWav(file: File, pcmFile: File, sampleRate: Int, channels: Int) {
        val pcmSize = pcmFile.length()
        FileOutputStream(file).use { out ->
            val byteRate = sampleRate * channels * 2
            val blockAlign = channels * 2
            out.write("RIFF".toByteArray(Charsets.US_ASCII))
            out.write(intLe((36 + pcmSize).toInt()))
            out.write("WAVE".toByteArray(Charsets.US_ASCII))
            out.write("fmt ".toByteArray(Charsets.US_ASCII))
            out.write(intLe(16))
            out.write(shortLe(1)) // PCM
            out.write(shortLe(channels))
            out.write(intLe(sampleRate))
            out.write(intLe(byteRate))
            out.write(shortLe(blockAlign))
            out.write(shortLe(16)) // bits per sample
            out.write("data".toByteArray(Charsets.US_ASCII))
            out.write(intLe(pcmSize.toInt()))
            pcmFile.inputStream().use { `in` -> `in`.copyTo(out) }
        }
    }

    private fun intLe(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte(),
        ((v shr 16) and 0xFF).toByte(),
        ((v shr 24) and 0xFF).toByte()
    )

    private fun shortLe(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v shr 8) and 0xFF).toByte()
    )

    // ------------------------------------------------------------------
    // Path / file helpers
    // ------------------------------------------------------------------

    private fun resolveInputPath(context: Context, inputUri: String): String {
        val uriString = inputUri.removePrefix("file://")
        if (!uriString.startsWith("content://")) return uriString
        val uri = android.net.Uri.parse(uriString)
        val temp = File(context.cacheDir, "v2a_input_${System.currentTimeMillis()}.tmp")
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException(
                "Could not open the selected video. It may have been moved or " +
                    "the access permission was revoked."
            )
        inputStream.use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
        return temp.absolutePath
    }

    private fun cleanupTempInput(path: String) {
        if (path.contains("v2a_input_") && path.contains("cache")) {
            runCatching { File(path).delete() }
        }
    }

    private fun cleanupPcm(file: File) {
        runCatching { file.delete() }
    }

    /**
     * Filesystem-safe base name (no path, no extension) keeping Unicode and
     * spaces. Only characters that are illegal in paths or would break shell
     * argument parsing are replaced with an underscore.
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
     * Resolve a unique output [File] inside [dir] for the request. If a file
     * with the chosen name already exists, append an incrementing suffix.
     */
    private fun resolveUniqueOutputFile(dir: File, request: ConversionRequest): File {
        val base = safeFileName(request.filename)
        val ext = when (request.outputFormat) {
            OutputFormat.MP3 -> "mp3"
            OutputFormat.M4A -> "m4a"
            OutputFormat.WAV -> "wav"
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

    companion object {
        private const val TIMEOUT_US = 10_000L
        // MediaCodecInfo.CodecProfileLevel.AACObjectLC
        private const val MediaCodecInfoProfileAacLC = 2
    }
}
