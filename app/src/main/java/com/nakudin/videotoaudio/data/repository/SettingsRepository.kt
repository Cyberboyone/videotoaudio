package com.nakudin.videotoaudio.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nakudin.videotoaudio.domain.Bitrate
import com.nakudin.videotoaudio.domain.Channels
import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.SampleRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Persists non-sensitive user preferences (theme, default conversion settings)
 * using Jetpack DataStore Preferences. No account or private data is stored here.
 */
class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    val defaultFormat: Flow<OutputFormat> = dataStore.data.map { prefs ->
        runCatching { OutputFormat.valueOf(prefs[KEY_FORMAT] ?: OutputFormat.MP3.name) }
            .getOrDefault(OutputFormat.MP3)
    }

    val defaultBitrate: Flow<Bitrate?> = dataStore.data.map { prefs ->
        prefs[KEY_BITRATE]?.let { runCatching { Bitrate.valueOf(it) }.getOrNull() }
    }

    val defaultSampleRate: Flow<SampleRate?> = dataStore.data.map { prefs ->
        prefs[KEY_SAMPLE_RATE]?.let { runCatching { SampleRate.valueOf(it) }.getOrNull() }
    }

    val defaultChannels: Flow<Channels> = dataStore.data.map { prefs ->
        runCatching { Channels.valueOf(prefs[KEY_CHANNELS] ?: Channels.STEREO.name) }
            .getOrDefault(Channels.STEREO)
    }

    suspend fun setTheme(mode: ThemeMode) = dataStore.edit { it[KEY_THEME] = mode.name }
    suspend fun setDefaultFormat(format: OutputFormat) =
        dataStore.edit { it[KEY_FORMAT] = format.name }

    suspend fun setDefaultBitrate(bitrate: Bitrate?) =
        dataStore.edit { if (bitrate == null) it.remove(KEY_BITRATE) else it[KEY_BITRATE] = bitrate.name }

    suspend fun setDefaultSampleRate(sampleRate: SampleRate?) =
        dataStore.edit { if (sampleRate == null) it.remove(KEY_SAMPLE_RATE) else it[KEY_SAMPLE_RATE] = sampleRate.name }

    suspend fun setDefaultChannels(channels: Channels) =
        dataStore.edit { it[KEY_CHANNELS] = channels.name }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_FORMAT = stringPreferencesKey("default_format")
        private val KEY_BITRATE = stringPreferencesKey("default_bitrate")
        private val KEY_SAMPLE_RATE = stringPreferencesKey("default_sample_rate")
        private val KEY_CHANNELS = stringPreferencesKey("default_channels")
    }
}
