package com.nakudin.videotoaudio.domain.model

/** Model representing a video file selected from the device, with extractable metadata. */
class VideoFile(
    /** Android content URI representing the video's location. */
    val uri: android.net.Uri,

    /** Safe display filename (last path segment of the URI). */
    val displayName: String,

    /** MIME type of the video (e.g., "video/mp4"), where available. */
    val mimeType: String?,

    /** File size in bytes, if available from the URI. */
    val size: Long,

    /** Duration in milliseconds, if available from media metadata. */
    val durationMs: Long,

    /** Optional Java File wrapper if URI is file-based. */
    val file: java.io.File? = null,

    /** Video resolution as "WIDTHxHEIGHT", where available. */
    val resolution: String? = null,

    /** Video format/container name (e.g., "mp4", "mkv", "mov"), where available. */
    val format: String? = null,

    /** Video width in pixels, where available. */
    val width: Int? = null,

    /** Video height in pixels, where available. */
    val height: Int? = null
)