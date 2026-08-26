package com.nakudin.videotoaudio.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nakudin.videotoaudio.ui.viewmodel.AudioPlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    navController: NavController,
    filePath: String
) {
    // Scope the ViewModel to this destination so media resources are released
    // when the player screen is popped off the back stack.
    val navBackStackEntry = requireNotNull(navController.currentBackStackEntry)
    val viewModel: AudioPlayerViewModel = viewModel(
        navBackStackEntry,
        factory = AudioPlayerViewModel.Factory(filePath)
    )
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Player") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                state.fileMissing || state.error != null -> {
                    Text(
                        text = state.error
                            ?: "The audio file is no longer available.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Text("Go Back")
                    }
                }

                state.isLoading && !state.isPrepared -> {
                    Text("Loading audio…", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator()
                }

                else -> {
                    Text(
                        text = state.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(24.dp))

                    PlayerSeekBar(
                        currentMs = state.currentPositionMs,
                        durationMs = state.durationMs,
                        onSeek = { viewModel.seekTo(it) }
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = viewModel::restart) {
                            Icon(
                                imageVector = Icons.Filled.Replay,
                                contentDescription = "Restart"
                            )
                        }
                        IconButton(
                            onClick = viewModel::togglePlayPause,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        IconButton(onClick = viewModel::stop) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Stop"
                            )
                        }
                    }

                    if (state.isCompleted) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Playback finished.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSeekBar(
    currentMs: Int,
    durationMs: Int,
    onSeek: (Int) -> Unit
) {
    val safeDuration = durationMs.coerceAtLeast(1)
    Column(Modifier.fillMaxWidth()) {
        androidx.compose.material3.Slider(
            value = currentMs.toFloat().coerceIn(0f, safeDuration.toFloat()),
            onValueChange = { onSeek(it.toInt()) },
            valueRange = 0f..safeDuration.toFloat()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentMs), style = MaterialTheme.typography.bodySmall)
            Text(formatTime(durationMs), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatTime(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
