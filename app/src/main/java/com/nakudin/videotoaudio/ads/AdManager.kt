package com.nakudin.videotoaudio.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError

/**
 * Manages interstitial ad lifecycle with built-in frequency capping.
 *
 * IMPORTANT placement rules (per app requirements):
 *  - Only show AFTER a successful conversion.
 *  - NEVER show while selecting a video, during conversion, during audio
 *    playback, immediately on app open, or over critical error dialogs.
 *  - Call [showInterstitialIfEligible] exclusively from the success state of
 *    the conversion result screen.
 */
object AdManager {

    private var lastShownMs: Long = 0L

    private fun canShow(): Boolean =
        System.currentTimeMillis() - lastShownMs >= AdConfig.INTERSTITIAL_MIN_INTERVAL_MS

    fun showInterstitialIfEligible(
        activity: Activity,
        adUnitId: String = AdConfig.interstitialId
    ) {
        if (!canShow()) return

        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            lastShownMs = System.currentTimeMillis()
                        }

                        override fun onAdFailedToShowFullScreenContent(
                            ad: InterstitialAd,
                            error: AdError
                        ) {
                            lastShownMs = System.currentTimeMillis()
                        }
                    }
                    ad.show(activity)
                    lastShownMs = System.currentTimeMillis()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    // No ad shown; keep lastShownMs unchanged so a later
                    // successful conversion can retry without violating the cap.
                }
            }
        )
    }
}
