package com.nakudin.videotoaudio.data.repository

import android.content.Context
import com.nakudin.videotoaudio.data.local.ConversionHistoryDatabase
import com.nakudin.videotoaudio.data.local.entity.ConversionHistoryEntity
import com.nakudin.videotoaudio.domain.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

/** Persists and reads conversion history via Room. */
class HistoryRepository(context: Context) {

    private val dao = ConversionHistoryDatabase.getDatabase(context).dao()

    fun observeAll(): Flow<List<HistoryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun insert(item: HistoryItem): Long = dao.insert(item.toEntity())

    suspend fun delete(item: HistoryItem) = dao.delete(item.toEntity())

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    /** Removes every history record and the associated generated audio files. */
    suspend fun clearAll() {
        dao.observeAll().first().forEach { entity ->
            runCatching { File(entity.outputPath).delete() }
        }
        dao.deleteAll()
    }
}

private fun ConversionHistoryEntity.toDomain() = HistoryItem(
    id = id,
    originalFilename = originalFilename,
    originalUri = originalUri,
    outputFilename = outputFilename,
    outputPath = outputPath,
    outputFormat = outputFormat,
    fileSize = fileSize,
    durationMs = durationMs,
    conversionDate = conversionDate,
    status = status
)

private fun HistoryItem.toEntity() = ConversionHistoryEntity(
    id = id,
    originalFilename = originalFilename,
    originalUri = originalUri,
    outputFilename = outputFilename,
    outputPath = outputPath,
    outputFormat = outputFormat,
    fileSize = fileSize,
    durationMs = durationMs,
    conversionDate = conversionDate,
    status = status
)
