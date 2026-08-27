package com.nakudin.videotoaudio

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.os.Environment
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nakudin.videotoaudio.domain.Bitrate
import com.nakudin.videotoaudio.domain.Channels
import com.nakudin.videotoaudio.domain.ConversionSettings
import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.SampleRate
import com.nakudin.videotoaudio.domain.model.VideoFile
import com.nakudin.videotoaudio.domain.model.HistoryItem
import com.nakudin.videotoaudio.model.ConversionRequest
import com.nakudin.videotoaudio.ui.screen.AudioPlayerScreen
import com.nakudin.videotoaudio.ui.viewmodel.ConversionViewModel
import com.nakudin.videotoaudio.ui.viewmodel.HistoryViewModel
import com.nakudin.videotoaudio.ui.viewmodel.VideoSelectionViewModel
import com.nakudin.videotoaudio.data.repository.ThemeMode
import com.nakudin.videotoaudio.ui.viewmodel.SettingsViewModel
import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.nakudin.videotoaudio.ads.AdConfig
import com.nakudin.videotoaudio.ads.AdManager
import com.nakudin.videotoaudio.ui.component.BannerAd
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.RowScope

class MainActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val selectionVm: VideoSelectionViewModel = viewModel()
            val conversionVm: ConversionViewModel = viewModel()
            val themeMode by settingsViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                AppNavigation(selectionVm, conversionVm)
            }
        }
    }
}

@Composable
fun AppNavigation(selectionVm: VideoSelectionViewModel, conversionVm: ConversionViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController, selectionVm) }
        composable("videodetails") { VideoDetailsScreen(navController, selectionVm) }
        composable("conversionsettings") { ConversionSettingsScreen(navController, selectionVm, conversionVm) }
        composable("conversionprogress") { ConversionProgressScreen(navController, conversionVm) }
        composable(
            "conversionresult/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStack ->
            val raw = backStack.arguments?.getString("path").orEmpty()
            ConversionResultScreen(navController, Uri.decode(raw))
        }
        composable(
            "audioplayer/{path}",
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStack ->
            val raw = backStack.arguments?.getString("path").orEmpty()
            AudioPlayerScreen(navController, Uri.decode(raw))
        }
        composable("history") { HistoryScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("about") { AboutScreen(navController) }
        composable("privacy") { PrivacyScreen(navController) }
    }
}

@Composable
fun HomeScreen(navController: NavController, selectionVm: VideoSelectionViewModel) {
    val context = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectionVm.setSelectedUri(context, uri)
            navController.navigate("videodetails")
        }
    }

    Scaffold(
        topBar = { AppTopBar("Video to Audio") },
        bottomBar = { BannerAd(AdConfig.homeBannerId, Modifier.fillMaxWidth()) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { launcher.launch(arrayOf("video/*")) }) {
                IconLabel(Icons.Filled.FolderOpen, "Select Video")
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { navController.navigate("history") }) { IconLabel(Icons.Filled.History, "History") }
                OutlinedButton(onClick = { navController.navigate("settings") }) { IconLabel(Icons.Filled.Settings, "Settings") }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { navController.navigate("about") }) { IconLabel(Icons.Filled.Info, "About") }
                OutlinedButton(onClick = { navController.navigate("privacy") }) { IconLabel(Icons.Filled.PrivacyTip, "Privacy") }
            }
        }
    }
}

@Composable
fun VideoDetailsScreen(navController: NavController, selectionVm: VideoSelectionViewModel) {
    val video by selectionVm.selectedVideo.collectAsState()

    Scaffold(topBar = { AppTopBar("Video Details", onBack = { navController.popBackStack() }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
        ) {
            if (video == null) {
                Text("No video selected.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Back") }
            } else {
                DetailRow("Name", video!!.displayName)
                DetailRow("Size", formatFileSize(video!!.size))
                DetailRow("Duration", formatDuration(video!!.durationMs))
                DetailRow("Resolution", video!!.resolution ?: "Unknown")
                DetailRow("Format", video!!.format ?: "Unknown")
            Spacer(Modifier.height(24.dp))
            Button(
                    onClick = { navController.navigate("conversionsettings") },
                    modifier = Modifier.fillMaxWidth()
                ) { IconLabel(Icons.Filled.PlayArrow, "Continue") }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversionSettingsScreen(navController: NavController, selectionVm: VideoSelectionViewModel, conversionVm: ConversionViewModel) {
    val settingsVm: SettingsViewModel = viewModel()
    val video by selectionVm.selectedVideo.collectAsState()

    val defFormat by settingsVm.defaultFormat.collectAsState()
    val defBitrate by settingsVm.defaultBitrate.collectAsState()
    val defSampleRate by settingsVm.defaultSampleRate.collectAsState()
    val defChannels by settingsVm.defaultChannels.collectAsState()

    var settings by remember(defFormat, defBitrate, defSampleRate, defChannels) {
        mutableStateOf(
            ConversionSettings(
                outputFormat = defFormat,
                bitrate = defBitrate ?: Bitrate.KBPS_128,
                sampleRate = defSampleRate ?: SampleRate.Hz_44100,
                channels = defChannels
            )
        )
    }
    var filename by remember { mutableStateOf("audio") }

    var trimEnabled by remember { mutableStateOf(false) }
    var trimStart by remember { mutableStateOf(0.0) }
    var trimEnd by remember { mutableStateOf(0.0) }

    val durationSec = (video?.durationMs ?: 0L) / 1000.0
    val trimValid = !trimEnabled || (trimStart >= 0.0 && trimEnd <= durationSec && trimStart < trimEnd)
    val canConvert = video != null && trimValid

    Scaffold(topBar = { AppTopBar("Conversion Settings", onBack = { navController.popBackStack() }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Format", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutputFormat.entries.forEach { fmt ->
                    FilterChip(
                        selected = settings.outputFormat == fmt,
                        onClick = {
                            settings = settings.copy(outputFormat = fmt)
                            // Keep bitrate/sample rate valid for the new format.
                            val applicableBitrate = Bitrate.applicableFor(fmt)
                            val b = settings.bitrate
                            if (b == null || b !in applicableBitrate) {
                                settings = settings.copy(bitrate = applicableBitrate.first())
                            }
                            val applicableSampleRate = SampleRate.applicableFor(fmt)
                            val s = settings.sampleRate
                            if (s == null || s !in applicableSampleRate) {
                                settings = settings.copy(sampleRate = applicableSampleRate.first())
                            }
                        },
                        label = { Text(fmt.name) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Bitrate", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Bitrate.applicableFor(settings.outputFormat).forEach { b ->
                    FilterChip(
                        selected = settings.bitrate == b,
                        onClick = { settings = settings.copy(bitrate = b) },
                        label = { Text("${b.value}") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Sample Rate", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleRate.applicableFor(settings.outputFormat).forEach { s ->
                    FilterChip(
                        selected = settings.sampleRate == s,
                        onClick = { settings = settings.copy(sampleRate = s) },
                        label = { Text("${s.value}") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Channels", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Channels.entries.forEach { c ->
                    FilterChip(
                        selected = settings.channels == c,
                        onClick = { settings = settings.copy(channels = c) },
                        label = { Text(c.name) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = filename,
                onValueChange = { filename = it },
                label = { Text("Output filename") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // ---- Trim controls ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trim video", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Switch(
                    checked = trimEnabled,
                    onCheckedChange = {
                        trimEnabled = it
                        if (it) {
                            if (trimEnd == 0.0) trimEnd = durationSec
                        } else {
                            trimStart = 0.0
                            trimEnd = 0.0
                        }
                    }
                )
            }

            if (trimEnabled && durationSec > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Start: ${formatDuration((trimStart * 1000).toLong())}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = trimStart.toFloat(),
                    valueRange = 0f..durationSec.toFloat(),
                    onValueChange = {
                        trimStart = it.toDouble().coerceAtMost((trimEnd - 0.5).coerceAtLeast(0.0))
                    }
                )
                TimeField("Start time", trimStart, durationSec) { trimStart = it }

                Spacer(Modifier.height(8.dp))
                Text(
                    "End: ${formatDuration((trimEnd * 1000).toLong())}",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = trimEnd.toFloat(),
                    valueRange = 0f..durationSec.toFloat(),
                    onValueChange = {
                        trimEnd = it.toDouble().coerceAtLeast(trimStart + 0.5)
                    }
                )
                TimeField("End time", trimEnd, durationSec) { trimEnd = it }

                Spacer(Modifier.height(8.dp))
                val selected = (trimEnd - trimStart).coerceAtLeast(0.0)
                Text(
                    "Selected duration: ${formatDuration((selected * 1000).toLong())}",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (trimStart < 0) {
                    Text(
                        "Start must be 0 or greater.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (trimEnd > durationSec) {
                    Text(
                        "End must not exceed the video duration.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (trimStart >= trimEnd) {
                    Text(
                        "Start must be before the end.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val request = ConversionRequest(
                        inputUri = video!!.uri.toString(),
                        outputFormat = settings.outputFormat,
                        bitrate = settings.bitrate ?: Bitrate.KBPS_128,
                        sampleRate = settings.sampleRate ?: SampleRate.Hz_44100,
                        channels = settings.channels,
                        trimStartSeconds = if (trimEnabled) trimStart else 0.0,
                        trimEndSeconds = if (trimEnabled) trimEnd else 0.0,
                        filename = filename.ifBlank { "audio" },
                        originalDisplayName = video!!.displayName,
                        originalUri = video!!.uri.toString()
                    )
                    conversionVm.start(request)
                    navController.navigate("conversionprogress")
                },
                enabled = canConvert,
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.PlayArrow, "Convert") }

            if (!canConvert) {
                Spacer(Modifier.height(8.dp))
                Text("Select a video first.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ConversionProgressScreen(navController: NavController, conversionVm: ConversionViewModel) {
    val state by conversionVm.state.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            is ConversionViewModel.State.Completed -> {
                val path = (state as ConversionViewModel.State.Completed).outputPath
                navController.navigate("conversionresult/${Uri.encode(path)}") {
                    popUpTo("conversionprogress") { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    Scaffold(topBar = {
        AppTopBar("Converting", onBack = {
            conversionVm.cancel()
            navController.popBackStack()
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val s = state) {
                is ConversionViewModel.State.Preparing -> {
                    Text("Preparing conversion…")
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator()
                }
                is ConversionViewModel.State.Converting -> {
                    Text("Converting… ${s.progress}%")
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(progress = { s.progress / 100f })
                }
                is ConversionViewModel.State.Cancelled -> {
                    Text("Conversion cancelled.")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("conversionsettings") }) {
                        IconLabel(Icons.Filled.Settings, "Back to Settings")
                    }
                }
                is ConversionViewModel.State.Failed -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Error: ${s.message}", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("conversionsettings") }) {
                        IconLabel(Icons.Filled.Settings, "Back to Settings")
                    }
                }
                else -> {
                    Text("Starting…")
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator()
                }
            }
            if (state is ConversionViewModel.State.Preparing ||
                state is ConversionViewModel.State.Converting
            ) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = conversionVm::cancel) { IconLabel(Icons.Filled.Cancel, "Cancel") }
            }
        }
    }
}

@Composable
fun ConversionResultScreen(navController: NavController, filePath: String) {
    val context = LocalContext.current
    val file = File(filePath)

    Scaffold(topBar = { AppTopBar("Conversion Result", onBack = { navController.popBackStack() }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)
        ) {
            if (!file.exists()) {
                Text(
                    "The converted file could not be found. It may have been deleted or moved.",
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.navigate("home") }) { Text("Back to Home") }
                return@Scaffold
            }

            val activity = context as? Activity
            LaunchedEffect(filePath) {
                delay(800)
                runCatching { activity?.let { AdManager.showInterstitialIfEligible(it) } }
            }

            Text("Success!", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            DetailRow("Output", file.name)
            DetailRow("Format", file.extension.uppercase())
            DetailRow("Size", formatFileSize(file.length()))

            val audioDurationMs = remember(filePath) {
                runCatching {
                    val retriever = android.media.MediaMetadataRetriever()
                    retriever.setDataSource(filePath)
                    retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: 0L
                }.getOrDefault(0L)
            }
            if (audioDurationMs > 0) {
                DetailRow("Duration", formatDuration(audioDurationMs))
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate("audioplayer/${Uri.encode(filePath)}") },
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.PlayArrow, "Play") }

            OutlinedButton(
                onClick = { openAudioFile(context, file) },
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.OpenInNew, "Open") }

            OutlinedButton(
                onClick = {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.nakudin.videotoaudio.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share audio"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.Share, "Share") }

            OutlinedButton(
                onClick = {
                    if (file.delete()) {
                        Toast.makeText(context, "File deleted", Toast.LENGTH_SHORT).show()
                    }
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { IconLabel(Icons.Filled.Delete, "Delete") }

            OutlinedButton(
                onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.Refresh, "Convert Another Video") }
        }
    }
}

@Composable
fun HistoryScreen(navController: NavController) {
    val viewModel: HistoryViewModel = viewModel()
    val historyItems by viewModel.items.collectAsState()

    Scaffold(
        topBar = { AppTopBar("History", onBack = { navController.popBackStack() }) },
        bottomBar = { BannerAd(AdConfig.historyBannerId, Modifier.fillMaxWidth()) }
    ) { padding ->
        if (historyItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No conversions yet.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Converted audio files will appear here.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(8.dp)
            ) {
                items(historyItems, key = { it.id }) { item ->
                    HistoryItemRow(item, navController, viewModel)
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    item: HistoryItem,
    navController: NavController,
    viewModel: HistoryViewModel
) {
    val context = LocalContext.current
    val fileExists = remember(item.outputPath) { File(item.outputPath).exists() }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                item.outputFilename,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${item.outputFormat}  •  ${formatDate(item.conversionDate)}  •  ${formatFileSize(item.fileSize)}",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!fileExists) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "File missing (deleted outside the app).",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = fileExists,
                    onClick = { navController.navigate("audioplayer/${Uri.encode(item.outputPath)}") }
                ) { IconLabel(Icons.Filled.PlayArrow, "Play") }

                OutlinedButton(
                    enabled = fileExists,
                    onClick = { openAudioFile(context, File(item.outputPath)) }
                ) { IconLabel(Icons.Filled.OpenInNew, "Open") }

                OutlinedButton(
                    enabled = fileExists,
                    onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "com.nakudin.videotoaudio.fileprovider",
                            File(item.outputPath)
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "audio/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share audio"))
                    }
                ) { IconLabel(Icons.Filled.Share, "Share") }

                OutlinedButton(
                    onClick = { viewModel.delete(item) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { IconLabel(Icons.Filled.Delete, "Delete") }

                OutlinedButton(
                    onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } }
                ) { IconLabel(Icons.Filled.Refresh, "Convert Again") }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
}

/** Consistent top app bar; shows a back button when [onBack] is provided. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        navigationIcon = if (onBack != null) {
            {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        } else {
            { }
        }
    )
}

/** Icon + text content for a button, labelled by the text for accessibility. */
@Composable
private fun RowScope.IconLabel(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}

@Composable
fun SettingsScreen(navController: NavController) {
    val vm: SettingsViewModel = viewModel()
    val themeMode by vm.themeMode.collectAsState()
    val defaultFormat by vm.defaultFormat.collectAsState()
    val defaultBitrate by vm.defaultBitrate.collectAsState()
    val defaultSampleRate by vm.defaultSampleRate.collectAsState()
    val defaultChannels by vm.defaultChannels.collectAsState()
    val context = LocalContext.current
    val outputDir = remember {
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath
            ?: context.filesDir.absolutePath
    }
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar("Settings", onBack = { navController.popBackStack() }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Appearance")
            Text("Theme", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                ThemeMode.values().forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { vm.setTheme(mode) },
                        label = {
                            Text(
                                mode.name.lowercase()
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            SectionTitle("Default conversion settings")
            Text("Output format", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                OutputFormat.values().forEach { format ->
                    FilterChip(
                        selected = defaultFormat == format,
                        onClick = { vm.setDefaultFormat(format) },
                        label = { Text(format.name) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text("Bitrate (kbps)", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Bitrate.values().forEach { bitrate ->
                    FilterChip(
                        selected = defaultBitrate == bitrate,
                        onClick = { vm.setDefaultBitrate(bitrate) },
                        label = { Text(bitrate.value.toString()) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text("Sample rate (Hz)", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                SampleRate.values().forEach { sampleRate ->
                    FilterChip(
                        selected = defaultSampleRate == sampleRate,
                        onClick = { vm.setDefaultSampleRate(sampleRate) },
                        label = { Text(sampleRate.value.toString()) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Text("Channels", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                Channels.values().forEach { channels ->
                    FilterChip(
                        selected = defaultChannels == channels,
                        onClick = { vm.setDefaultChannels(channels) },
                        label = {
                            Text(
                                channels.name.lowercase()
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            SectionTitle("Storage")
            Text("Output location", style = MaterialTheme.typography.titleSmall)
            Text(text = outputDir, style = MaterialTheme.typography.bodySmall)
            Text(
                "Generated audio is stored in the app's private folder and is removed when the app is uninstalled.",
                style = MaterialTheme.typography.bodySmall
            )

            SectionTitle("Data")
            Button(
                onClick = { showClearDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.Delete, "Clear conversion history") }

            SectionTitle("About")
            OutlinedButton(
                onClick = { navController.navigate("about") },
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.Info, "About") }
            OutlinedButton(
                onClick = {
                    val privacyUrl = context.getString(R.string.privacy_policy_url)
                    if (privacyUrl.isNotBlank()) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                        )
                    } else {
                        navController.navigate("privacy")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { IconLabel(Icons.Filled.PrivacyTip, "Privacy Policy") }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear history?") },
            text = {
                Text("This permanently deletes all conversion history and the generated audio files.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.clearHistory()
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(topBar = { AppTopBar("About", onBack = { navController.popBackStack() }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Video to Audio Converter", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Convert videos to audio entirely on your device. Your media is processed locally and is " +
                    "never uploaded. Advertising is provided by Google AdMob; see the Privacy Policy for details.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PolicyBlock(title: String, body: String) {
    Spacer(Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun PrivacyScreen(navController: NavController) {
    Scaffold(topBar = { AppTopBar("Privacy Policy", onBack = { navController.popBackStack() }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Last updated: this policy reflects the app as built.",
                style = MaterialTheme.typography.bodySmall
            )

            PolicyBlock(
                "Local media processing",
                "All video-to-audio conversion is performed entirely on your device using Android's built-in " +
                    "media engine (MediaCodec / MediaExtractor / MediaMuxer). " +
                    "Your selected videos and the audio produced from them are never uploaded to any server or " +
                    "transmitted over the network. The only network traffic initiated by the app is for advertising " +
                    "(see below)."
            )

            PolicyBlock(
                "Conversion history",
                "The app keeps a local history of your conversions on your device. This stores only metadata: " +
                    "the original file name, output file name, audio format, file size, duration, and the date. " +
                    "This data stays on your device and is never sent anywhere. You can delete individual entries " +
                    "or clear the entire history from Settings."
            )

            PolicyBlock(
                "App settings",
                "Your preferences (theme, and default output format, bitrate, sample rate and channels) are stored " +
                    "locally on your device and are not transmitted."
            )

            PolicyBlock(
                "Advertising (Google AdMob)",
                "The app displays advertisements through Google AdMob. To deliver, measure, and improve ads, " +
                    "AdMob and its partners may collect and process data such as: the Advertising ID (if available " +
                    "on your device), device information (model, manufacturer, OS version), approximate location " +
                    "(such as a general region derived from your IP address), your IP address, and your ad " +
                    "interactions (views and clicks). This data is collected and processed by Google and its " +
                    "advertising partners, not by us, and may involve cookies or similar technologies. " +
                    "We do not control and are not responsible for the data practices of AdMob; please review " +
                    "Google's privacy policy for details."
            )

            PolicyBlock(
                "Network access",
                "The app uses the internet solely to load and display ads. No other network requests are made: " +
                    "no analytics, no crash reporting, and no upload of your media or history."
            )

            PolicyBlock(
                "Children",
                "This app is not directed to children under 13, and we do not knowingly collect personal data from " +
                    "children. Ads may nonetheless be shown by AdMob."
            )

            PolicyBlock(
                "Your choices",
                "You can clear the conversion history at any time in Settings. You can opt out of personalized " +
                    "advertising through your device settings (Reset Advertising ID / Opt out of Ads " +
                    "Personalization) or via Google's Ads Settings. Uninstalling the app removes all data stored " +
                    "locally on your device."
            )

            PolicyBlock(
                "Data retention",
                "History and settings remain on your device until you delete them or uninstall the app. Advertising " +
                    "data is handled by Google in accordance with their own policies."
            )

            PolicyBlock(
                "Third-party software",
                "Audio processing uses Android's built-in media engine and is performed locally on your device; " +
                    "it does not transmit data. Google Mobile Ads provides advertising as described above. " +
                    "Standard AndroidX libraries are used for the user interface and local storage."
            )

            PolicyBlock(
                "Contact",
                "For privacy questions, contact the developer through the Google Play store listing."
            )

            Spacer(Modifier.height(16.dp))
            Button(onClick = { navController.popBackStack() }) { Text("Back") }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

/** Formats a byte count as KB / MB / GB, handling very large files gracefully. */
private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    return when {
        kb < 1024 -> "%.1f KB".format(kb)
        kb < 1024 * 1024 -> "%.1f MB".format(kb / 1024)
        else -> "%.2f GB".format(kb / (1024 * 1024))
    }
}

private fun formatDate(ts: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}

/** Map an audio file extension to its MIME type for share/open intents. */
private fun audioMimeType(path: String): String {
    return when (path.substringAfterLast('.').lowercase()) {
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "wav" -> "audio/wav"
        else -> "audio/*"
    }
}

/**
 * Open a generated audio file in an external app via a FileProvider content URI.
 * Never exposes a raw file:// URI. The receiving app is granted temporary
 * read access via FLAG_GRANT_READ_URI_PERMISSION.
 */
private fun openAudioFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "com.nakudin.videotoaudio.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, audioMimeType(file.absolutePath))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open with"))
}

/**
 * Text field for entering a time as either seconds ("90") or mm:ss ("1:30").
 * Keeps its display in sync with [valueSec] (e.g. when changed via the slider).
 */
@Composable
fun TimeField(label: String, valueSec: Double, maxSec: Double, onValueChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(formatDuration((valueSec * 1000).toLong())) }
    LaunchedEffect(valueSec) { text = formatDuration((valueSec * 1000).toLong()) }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            text = new
            parseTime(new)?.let { onValueChange(it.coerceIn(0.0, maxSec)) }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

/** Parse a time string in "m:ss" or plain seconds form. Returns null if invalid. */
private fun parseTime(text: String): Double? {
    val t = text.trim()
    if (t.isEmpty()) return null
    return if (t.contains(":")) {
        val parts = t.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toDoubleOrNull() ?: return null
            val seconds = parts[1].toDoubleOrNull() ?: return null
            (minutes * 60 + seconds).coerceAtLeast(0.0)
        } else null
    } else {
        t.toDoubleOrNull()?.coerceAtLeast(0.0)
    }
}
