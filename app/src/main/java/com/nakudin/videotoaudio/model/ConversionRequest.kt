package com.nakudin.videotoaudio.model

import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.Bitrate
import com.nakudin.videotoaudio.domain.SampleRate
import com.nakudin.videotoaudio.domain.Channels

/** Request model for audio conversion. */
data class ConversionRequest(
    /** Input video URI or file path. */
    val inputUri: String,

    /** Output audio format. */
    val outputFormat: OutputFormat,

    /** Audio bitrate in kbps (applicable for format). */
    val bitrate: Bitrate,

    /** Sample rate in Hz (applicable for format). */
    val sampleRate: SampleRate,

    /** Channel configuration (applicable for format). */
    val channels: Channels,

    /** Trim start time in seconds. */
    val trimStartSeconds: Double,

    /** Trim end time in seconds. */
    val trimEndSeconds: Double,

    /** User-provided output filename (without extension). */
    val filename: String,

    /** Display name of the source video (for history). */
    val originalDisplayName: String = "",

    /** Source content/data URI (for history), when available. */
    val originalUri: String = ""
)

/** Result model for audio conversion. */
sealed class ConversionResult {
    /** Conversion completed successfully. */
    data class Success(
        /** Path to the generated audio file. */
        val outputFile: String,

        /** Duration of the generated audio. */
        val durationMs: Long
    ) : ConversionResult()

    /** Conversion was cancelled by the user. */
    object Cancelled : ConversionResult()

    /** Conversion failed with an error. */
    data class Error(
        /** User-friendly error message. */
        val message: String,

        /** Original error cause, if available. */
        val cause: Throwable? = null
    ) : ConversionResult()
}