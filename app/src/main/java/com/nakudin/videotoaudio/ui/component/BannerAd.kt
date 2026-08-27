package com.nakudin.videotoaudio.ui.component

import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Hosts a standard AdMob banner via an AndroidView bridge.
 * The ad unit ID is passed in (sourced from [com.nakudin.videotoaudio.ads.AdConfig])
 * so no identifiers are scattered through the UI.
 */
@Composable
fun BannerAd(adUnitId: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }.also { it.loadAd(AdRequest.Builder().build()) }
        }
    )
}
