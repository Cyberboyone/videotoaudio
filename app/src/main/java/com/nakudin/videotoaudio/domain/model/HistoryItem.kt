package com.nakudin.videotoaudio.domain.model

/**
 * Domain representation of a stored conversion history record.
 * Holds only metadata and a reference to the output file — never the binary.
 */
data class HistoryItem(
    val id: Long,
    val originalFilename: String,
    val originalUri: String?,
    val outputFilename: String,
    val outputPath: String,
    val outputFormat: String,
    val fileSize: Long,
    val durationMs: Long,
    val conversionDate: Long,
    val status: String
)
