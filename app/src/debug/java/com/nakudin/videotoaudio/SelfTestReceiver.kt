package com.nakudin.videotoaudio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
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
 * Fire it with: adb shell am broadcast -a com.nakudin.videotoaudio.SELF_TEST
 * It picks the most recent video on the device and runs a full M4A
 * conversion, logging the result (and any stage failure) to logcat under
 * the SELF_TEST tag.
 */
class SelfTestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("SELF_TEST", "received broadcast; launching conversion")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = intent.getStringExtra("uri")?.let { Uri.parse(it) } ?: pickFirstVideo(context)
                if (uri == null) {
                    Log.e("SELF_TEST", "NO_VIDEO: no video found in MediaStore")
                    return@launch
                }
                Log.i("SELF_TEST", "videoUri=$uri")
                val repo = AudioConverterRepository()
                val req = ConversionRequest(
                    inputUri = uri.toString(),
                    outputFormat = OutputFormat.M4A,
                    bitrate = Bitrate.KBPS_128,
                    sampleRate = SampleRate.Hz_44100,
                    channels = Channels.STEREO,
                    trimStartSeconds = 0.0,
                    trimEndSeconds = 0.0,
                    filename = "selftest"
                )
                val result = repo.convert(context.applicationContext, req) { p ->
                    Log.i("SELF_TEST", "progress=$p")
                }
                Log.i("SELF_TEST", "RESULT=$result")
            } catch (t: Throwable) {
                Log.e("SELF_TEST", "EXCEPTION", t)
            }
        }
    }

    private fun pickFirstVideo(context: Context): Uri? {
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
