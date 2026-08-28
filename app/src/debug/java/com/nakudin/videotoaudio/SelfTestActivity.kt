package com.nakudin.videotoaudio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.nakudin.videotoaudio.data.repository.AudioConverterRepository
import com.nakudin.videotoaudio.domain.Bitrate
import com.nakudin.videotoaudio.domain.Channels
import com.nakudin.videotoaudio.domain.OutputFormat
import com.nakudin.videotoaudio.domain.SampleRate
import com.nakudin.videotoaudio.model.ConversionRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only self-test hook. Never shipped in the release build.
 * A foreground Service / manifest receiver is blocked by Android's
 * background-execution limits, so this is a NoDisplay Activity that is
 * launched via `am start` and runs a full conversion, logging the result
 * (and any stage failure) under the SELF_TEST tag.
 *
 * Run it with:
 *   adb shell am start -n com.nakudin.videotoaudio/.SelfTestActivity \
 *       --es uri "/sdcard/Android/data/com.nakudin.videotoaudio/files/selftest_input.mp4"
 */
class SelfTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("SELF_TEST", "activity created; launching conversion")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = intent.getStringExtra("uri")?.let { Uri.parse(it) }
                    ?: pickFirstVideo(this@SelfTestActivity)
                if (uri == null) {
                    Log.e("SELF_TEST", "NO_VIDEO: no video supplied or found")
                    return@launch
                }
                Log.i("SELF_TEST", "videoUri=$uri")
                val fmtName = intent.getStringExtra("format")?.uppercase() ?: "M4A"
                val fmt = OutputFormat.values().firstOrNull { it.name == fmtName }
                    ?: OutputFormat.M4A
                Log.i("SELF_TEST", "format=$fmt")
                val repo = AudioConverterRepository()
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
                val result = repo.convert(applicationContext, req) { p ->
                    Log.i("SELF_TEST", "progress=$p")
                }
                Log.i("SELF_TEST", "RESULT=$result")
            } catch (t: Throwable) {
                Log.e("SELF_TEST", "EXCEPTION", t)
            } finally {
                runOnUiThread { finish() }
            }
        }
    }

    private fun pickFirstVideo(context: android.content.Context): Uri? {
        val proj = arrayOf(MediaStore.Video.Media._ID)
        val q = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            proj,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )
        q?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                return Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
            }
        }
        return null
    }
}
