package com.nakudin.videotoaudio.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.nakudin.videotoaudio.domain.VideoSelectionUseCase
import com.nakudin.videotoaudio.domain.model.VideoFile

/** Resolves metadata for a selected video using [MediaMetadataRetriever]. */
class VideoSelectionRepository : VideoSelectionUseCase {

    override fun buildVideoFile(context: Context, uri: Uri): VideoFile {
        var durationMs = 0L
        var width: Int? = null
        var height: Int? = null
        val mimeType = context.contentResolver.getType(uri)
        var format: String? = null
        var resolution: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            durationMs =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
            format = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (width != null && height != null) resolution = "${width}x${height}"
        } catch (e: Exception) {
            // Gracefully fall back to defaults for corrupt/unsupported videos.
        } finally {
            runCatching { retriever.release() }
        }

        val meta = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val n = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    s to (n ?: uri.lastPathSegment ?: "video")
                } else {
                    0L to (uri.lastPathSegment ?: "video")
                }
            } ?: (0L to (uri.lastPathSegment ?: "video"))
        }.getOrDefault(0L to (uri.lastPathSegment ?: "video"))
        val size = meta.first
        val displayName = meta.second

        return VideoFile(
            uri = uri,
            displayName = displayName,
            mimeType = mimeType ?: format,
            size = size,
            durationMs = durationMs,
            resolution = resolution,
            format = format,
            width = width,
            height = height
        )
    }
}
