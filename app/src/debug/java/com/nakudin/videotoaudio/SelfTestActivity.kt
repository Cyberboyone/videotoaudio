package com.nakudin.videotoaudio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.nakudin.videotoaudio.domain.Bitrate
import com.nakudin.videotoaudio.domain.Channels
import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.SampleRate
import com.nakudin.videotoaudio.model.ConversionRequest
import com.nakudin.videotoaudio.ui.viewmodel.ConversionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Debug-only self-test hook. Never shipped in the release build.
 * A foreground Service / manifest receiver is blocked by Android's
 * background-execution limits, so this is a Translucent Activity launched
 * via `am start` that runs a full conversion THROUGH THE REAL
 * ConversionViewModel (the exact app path), logging every state transition
 * under the SELF_TEST tag.
 *
 * Run it with:
 *   adb shell am start -n com.nakudin.videotoaudio/.SelfTestActivity \
 *       --es uri "/sdcard/Android/data/com.nakudin.videotoaudio/files/selftest_input.mp4" \
 *       --es format M4A
 */
class SelfTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("SELF_TEST", "activity created; launching conversion via ConversionViewModel")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = intent.getStringExtra("uri")?.let { Uri.parse(it) }
                    ?: run {
                        Log.e("SELF_TEST", "NO_URI: pass --es uri")
                        return@launch
                    }
                Log.i("SELF_TEST", "videoUri=$uri")
                val fmtName = intent.getStringExtra("format")?.uppercase() ?: "M4A"
                val fmt = OutputFormat.values().firstOrNull { it.name == fmtName }
                    ?: OutputFormat.M4A
                Log.i("SELF_TEST", "format=$fmt")

                val vm = ConversionViewModel(application)
                vm.state
                    .onEach { s -> Log.i("SELF_TEST", "VM state=$s") }
                    .launchIn(this)

                val req = ConversionRequest(
                    inputUri = uri.toString(),
                    outputFormat = fmt,
                    bitrate = Bitrate.KBPS_128,
                    sampleRate = SampleRate.Hz_44100,
                    channels = Channels.STEREO,
                    trimStartSeconds = 0.0,
                    trimEndSeconds = 0.0,
                    filename = "selftest"
                )
                vm.start(req)
                Log.i("SELF_TEST", "vm.start() returned (conversion running in ViewModel scope)")
            } catch (t: Throwable) {
                Log.e("SELF_TEST", "EXCEPTION", t)
            } finally {
                runOnUiThread { finish() }
            }
        }
    }
}
