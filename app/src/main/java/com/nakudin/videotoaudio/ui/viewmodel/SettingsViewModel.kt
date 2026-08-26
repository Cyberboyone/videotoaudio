package com.nakudin.videotoaudio.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nakudin.videotoaudio.data.repository.HistoryRepository
import com.nakudin.videotoaudio.data.repository.SettingsRepository
import com.nakudin.videotoaudio.data.repository.ThemeMode
import com.nakudin.videotoaudio.domain.Bitrate
import com.nakudin.videotoaudio.domain.Channels
import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.SampleRate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val historyRepository = HistoryRepository(application)

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM
    )
    val defaultFormat: StateFlow<OutputFormat> = settingsRepository.defaultFormat.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), OutputFormat.MP3
    )
    val defaultBitrate: StateFlow<Bitrate?> = settingsRepository.defaultBitrate.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null
    )
    val defaultSampleRate: StateFlow<SampleRate?> = settingsRepository.defaultSampleRate.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null
    )
    val defaultChannels: StateFlow<Channels> = settingsRepository.defaultChannels.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), Channels.STEREO
    )

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setTheme(mode) }
    fun setDefaultFormat(format: OutputFormat) = viewModelScope.launch { settingsRepository.setDefaultFormat(format) }
    fun setDefaultBitrate(bitrate: Bitrate?) = viewModelScope.launch { settingsRepository.setDefaultBitrate(bitrate) }
    fun setDefaultSampleRate(sampleRate: SampleRate?) = viewModelScope.launch { settingsRepository.setDefaultSampleRate(sampleRate) }
    fun setDefaultChannels(channels: Channels) = viewModelScope.launch { settingsRepository.setDefaultChannels(channels) }

    fun clearHistory() = viewModelScope.launch { historyRepository.clearAll() }
}
