package com.nakudin.videotoaudio.domain

/**
 * Output audio format supported by the conversion engine.
 * Note: OGG/Vorbis is intentionally not offered because Android ships no
 * Vorbis encoder; MP3 is provided but falls back to a clear error on devices
 * without an MP3 encoder.
 */
enum class OutputFormat {
    MP3,
    M4A,
    WAV
}

/** Audio bitrate options (in kbps). */
enum class Bitrate(val value: Int) {
    KBPS_64(64),
    KBPS_96(96),
    KBPS_128(128),
    KBPS_192(192),
    KBPS_256(256),
    KBPS_320(320);

    companion object {
        /** Bitrates applicable for the given output [format]. */
        fun applicableFor(format: OutputFormat): List<Bitrate> = when (format) {
            OutputFormat.MP3, OutputFormat.M4A -> entries.toList()
            OutputFormat.WAV -> listOf(KBPS_128, KBPS_192, KBPS_320)
        }
    }
}

/** Sample rate options (in Hz). */
enum class SampleRate(val value: Int) {
    Hz_44100(44100),
    Hz_48000(48000);

    companion object {
        /** Sample rates applicable for the given output [format]. */
        fun applicableFor(format: OutputFormat): List<SampleRate> = when (format) {
            OutputFormat.MP3, OutputFormat.M4A, OutputFormat.WAV -> entries.toList()
        }
    }
}

/** Channel mode options. */
enum class Channels {
    MONO,
    STEREO;

    companion object {
        /** Channels applicable for the given output [format]. */
        fun applicableFor(format: OutputFormat): List<Channels> = entries.toList()
    }
}

/** All conversion settings selected by the user. */
data class ConversionSettings(
    /** Selected output audio format. */
    val outputFormat: OutputFormat = OutputFormat.MP3,

    /** Selected audio bitrate (kbps). Null when not applicable. */
    val bitrate: Bitrate? = Bitrate.KBPS_128,

    /** Selected sample rate (Hz). Null when not applicable. */
    val sampleRate: SampleRate? = SampleRate.Hz_44100,

    /** Selected channels. */
    val channels: Channels = Channels.STEREO,

    /** User-provided output filename (without extension). */
    val filename: String = "audio",

    /** Trim start time in seconds. */
    val trimStartSeconds: Double = 0.0,

    /** Trim end time in seconds. */
    val trimEndSeconds: Double = 0.0
)
