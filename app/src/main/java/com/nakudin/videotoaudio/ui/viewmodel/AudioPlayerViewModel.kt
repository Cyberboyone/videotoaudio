package com.nakudin.videotoaudio.ui.viewmodel

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI state for the in-app audio player.
 *
 * @param filePath        Absolute path of the audio file being played.
 * @param displayName     Human friendly file name.
 * @param isLoading       True while the player is preparing the data source.
 * @param isPrepared      True once the player is ready to play.
 * @param isPlaying       True while actively playing.
 * @param currentPositionMs Current playback position in milliseconds.
 * @param durationMs      Total duration in milliseconds (0 until known).
 * @param isCompleted     True when playback reached the end.
 * @param fileMissing     True when the underlying file no longer exists.
 * @param error           Human readable error, or null when healthy.
 */
data class AudioPlayerUiState(
    val filePath: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val isPrepared: Boolean = false,
    val isPlaying: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val isCompleted: Boolean = false,
    val fileMissing: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel that owns a single [MediaPlayer] instance and exposes playback state.
 *
 * The player is released in [onCleared], which is triggered when the destination
 * using this ViewModel is removed from the back stack. This guarantees media
 * resources are never kept alive after leaving the player screen.
 */
class AudioPlayerViewModel(private val filePath: String) : ViewModel() {

    private val _uiState = MutableStateFlow(AudioPlayerUiState(filePath = filePath))
    val uiState: StateFlow<AudioPlayerUiState> = _uiState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var positionJob: Job? = null

    init {
        val file = File(filePath)
        _uiState.value = _uiState.value.copy(displayName = if (filePath.isNotBlank()) file.name else "")
        when {
            filePath.isBlank() -> _uiState.value = _uiState.value.copy(
                error = "No audio file was provided."
            )
            !file.exists() -> _uiState.value = _uiState.value.copy(
                fileMissing = true,
                error = "The audio file could not be found. It may have been deleted or moved."
            )
            else -> prepare()
        }
    }

    private fun prepare() {
        _uiState.value = _uiState.value.copy(
            isLoading = true, error = null, fileMissing = false, isPrepared = false
        )
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(filePath)
                setOnPreparedListener { mp ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPrepared = true,
                        durationMs = mp.duration.coerceAtLeast(0),
                        currentPositionMs = 0,
                        isCompleted = false,
                        error = null
                    )
                }
                setOnCompletionListener {
                    _uiState.value = _uiState.value.copy(isPlaying = false, isCompleted = true)
                    stopPositionPolling()
                }
                setOnErrorListener { _, what, extra ->
                    val message = when (what) {
                        MediaPlayer.MEDIA_ERROR_UNSUPPORTED ->
                            "This audio format is not supported on this device."
                        MediaPlayer.MEDIA_ERROR_IO ->
                            "Could not read the audio file."
                        else -> "Playback failed (error $what/$extra)."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPlaying = false,
                        isPrepared = false,
                        error = message
                    )
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            val message = when (e) {
                is IllegalArgumentException -> "Unsupported or invalid audio format."
                is SecurityException -> "Access to the audio file was denied."
                else -> "Could not open the audio file: ${e.message}"
            }
            _uiState.value = _uiState.value.copy(isLoading = false, error = message)
        }
    }

    fun play() {
        val mp = mediaPlayer ?: return
        if (!_uiState.value.isPrepared) return
        try {
            mp.start()
            _uiState.value = _uiState.value.copy(isPlaying = true, isCompleted = false, error = null)
            startPositionPolling()
        } catch (e: IllegalStateException) {
            _uiState.value = _uiState.value.copy(error = "Playback could not be started.")
        }
    }

    fun pause() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) {
                mp.pause()
                _uiState.value = _uiState.value.copy(isPlaying = false)
                stopPositionPolling()
            }
        } catch (e: IllegalStateException) {
            // Ignore; player already in an invalid state.
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Int) {
        val mp = mediaPlayer ?: return
        if (!_uiState.value.isPrepared) return
        try {
            val clamped = positionMs.coerceIn(0, _uiState.value.durationMs)
            mp.seekTo(clamped)
            _uiState.value = _uiState.value.copy(currentPositionMs = clamped)
        } catch (e: IllegalStateException) {
            // Ignore seek while in an invalid state.
        }
    }

    fun restart() {
        val mp = mediaPlayer ?: return
        if (!_uiState.value.isPrepared) return
        try {
            mp.seekTo(0)
            mp.start()
            _uiState.value = _uiState.value.copy(
                isPlaying = true, isCompleted = false, currentPositionMs = 0
            )
            startPositionPolling()
        } catch (e: IllegalStateException) {
            _uiState.value = _uiState.value.copy(error = "Could not restart playback.")
        }
    }

    fun stop() {
        val mp = mediaPlayer ?: return
        try {
            if (mp.isPlaying) mp.stop()
        } catch (e: IllegalStateException) {
            // Already stopped.
        }
        stopPositionPolling()
        _uiState.value = _uiState.value.copy(
            isPlaying = false, isCompleted = false, currentPositionMs = 0
        )
        // Re-prepare so the track can be played again from the start.
        prepare()
    }

    private fun startPositionPolling() {
        stopPositionPolling()
        positionJob = viewModelScope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && _uiState.value.isPlaying) {
                    try {
                        _uiState.value = _uiState.value.copy(currentPositionMs = mp.currentPosition)
                    } catch (e: IllegalStateException) {
                        stopPositionPolling()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionPolling()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    class Factory(private val filePath: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AudioPlayerViewModel(filePath) as T
        }
    }
}
