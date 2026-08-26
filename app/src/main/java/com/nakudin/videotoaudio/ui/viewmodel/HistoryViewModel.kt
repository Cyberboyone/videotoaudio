package com.nakudin.videotoaudio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakudin.videotoaudio.data.repository.HistoryRepository
import com.nakudin.videotoaudio.domain.model.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HistoryRepository(application)

    /** Reactive list of history items, newest first. */
    val items: StateFlow<List<HistoryItem>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Delete both the history record and the underlying audio file (if present). */
    fun delete(item: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(item.outputPath).delete() }
            repository.delete(item)
        }
    }
}
