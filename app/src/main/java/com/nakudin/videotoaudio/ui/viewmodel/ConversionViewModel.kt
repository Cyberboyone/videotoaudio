package com.nakudin.videotoaudio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakudin.videotoaudio.data.repository.AudioConverterRepository
import com.nakudin.videotoaudio.data.repository.HistoryRepository
import com.nakudin.videotoaudio.domain.model.HistoryItem
import com.nakudin.videotoaudio.domain.usecase.AudioConverter
import com.nakudin.videotoaudio.model.ConversionRequest
import com.nakudin.videotoaudio.model.ConversionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ConversionViewModel(application: Application) : AndroidViewModel(application) {

    private val converter: AudioConverter = AudioConverterRepository()
    private val historyRepository = HistoryRepository(application)

    sealed interface State {
        data object Idle : State
        data object Preparing : State
        data class Converting(val progress: Int) : State
        data class Completed(val outputPath: String) : State
        data object Cancelled : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun start(request: ConversionRequest) {
        _state.value = State.Preparing
        viewModelScope.launch {
            val result = converter.convert(getApplication(), request) { progress ->
                _state.value = State.Converting(progress)
            }
            _state.value = when (result) {
                is ConversionResult.Success -> {
                    recordHistory(request, result.outputFile, result.durationMs)
                    State.Completed(result.outputFile)
                }
                is ConversionResult.Cancelled -> State.Cancelled
                is ConversionResult.Error -> State.Failed(result.message)
            }
        }
    }

    private fun recordHistory(request: ConversionRequest, outputPath: String, durationMs: Long) {
        val file = File(outputPath)
        val item = HistoryItem(
            id = 0,
            originalFilename = request.originalDisplayName,
            originalUri = request.originalUri.ifBlank { null },
            outputFilename = file.name,
            outputPath = outputPath,
            outputFormat = request.outputFormat.name,
            fileSize = file.length(),
            durationMs = durationMs,
            conversionDate = System.currentTimeMillis(),
            status = "SUCCESS"
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { historyRepository.insert(item) }
        }
    }

    fun cancel() {
        converter.cancel()
    }

    fun reset() {
        _state.value = State.Idle
    }
}
