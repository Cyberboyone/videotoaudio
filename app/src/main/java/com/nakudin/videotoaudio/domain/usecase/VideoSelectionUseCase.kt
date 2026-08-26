package com.nakudin.videotoaudio.domain

import android.content.Context
import android.net.Uri
import com.nakudin.videotoaudio.domain.model.VideoFile

/** Abstraction for resolving metadata of a user-selected video. */
interface VideoSelectionUseCase {

    /**
     * Build a [VideoFile] model from a content/data URI, extracting metadata
     * such as duration, resolution and size where possible.
     */
    fun buildVideoFile(context: Context, uri: Uri): VideoFile
}
