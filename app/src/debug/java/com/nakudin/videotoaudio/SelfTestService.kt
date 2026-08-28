package com.nakudin.videotoaudio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
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
 * A manifest BroadcastReceiver is blocked by Android's background-execution
 * limits, so this is a foreground Service that runs a full conversion and
 * logs the result (and any stage failure) under the SELF_TEST tag.
 *
 * Run it with:
 *   adb shell am start-service -n com.nakudin.videotoaudio/.SelfTestService \
 *       --es uri "/sdcard/Android/data/com.nakudin.videotoaudio/files/selftest_input.mp4"
 */
class SelfTestService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        val chan = NotificationChannel(
            "selftest", "Self-test", NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(chan)
        val notif = Notification.Builder(this, "selftest")
            .setContentTitle("Self-test running")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
        startForeground(1, notif)
        Log.i("SELF_TEST", "service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("SELF_TEST", "startCommand; launching conversion")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uri = intent?.getStringExtra("uri")?.let { Uri.parse(it) }
                    ?: pickFirstVideo(this@SelfTestService)
                if (uri == null) {
                    Log.e("SELF_TEST", "NO_VIDEO: no video supplied or found")
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
                val result = repo.convert(applicationContext, req) { p ->
                    Log.i("SELF_TEST", "progress=$p")
                }
                Log.i("SELF_TEST", "RESULT=$result")
            } catch (t: Throwable) {
                Log.e("SELF_TEST", "EXCEPTION", t)
            } finally {
                stopSelf()
            }
        }
        return START_NOT_STICKY
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
