package com.nakudin.videotoaudio.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nakudin.videotoaudio.data.repository.VideoSelectionRepository
import com.nakudin.videotoaudio.domain.model.VideoFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoSelectionViewModel : ViewModel() {

    private val repository = VideoSelectionRepository()

    private val _selectedVideo = MutableStateFlow<VideoFile?>(null)
    val selectedVideo: StateFlow<VideoFile?> = _selectedVideo.asStateFlow()

    fun setSelectedUri(context: Context, uri: Uri) {
        _selectedVideo.value = repository.buildVideoFile(context, uri)
    }

    fun clear() {
        _selectedVideo.value = null
    }
}
