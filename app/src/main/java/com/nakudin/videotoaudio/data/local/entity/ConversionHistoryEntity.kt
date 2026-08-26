package com.nakudin.videotoaudio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted record of a completed conversion.
 *
 * Only metadata and file references are stored here — never the video or audio
 * binary data itself.
 */
@Entity(tableName = "conversion_history")
data class ConversionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Display name of the source video. */
    val originalFilename: String,
    /** Source content/data URI, when available. May be null. */
    val originalUri: String?,
    /** Generated audio file name (without directory). */
    val outputFilename: String,
    /** Absolute path of the generated audio file (app-specific storage). */
    val outputPath: String,
    /** Output container format, stored as a string (e.g. "MP3"). */
    val outputFormat: String,
    /** Size of the generated audio file in bytes. */
    val fileSize: Long,
    /** Duration of the generated audio in milliseconds. */
    val durationMs: Long,
    /** Epoch millis when the conversion finished. */
    val conversionDate: Long,
    /** Status of the conversion (e.g. "SUCCESS", "FAILED", "CANCELLED"). */
    val status: String
)
