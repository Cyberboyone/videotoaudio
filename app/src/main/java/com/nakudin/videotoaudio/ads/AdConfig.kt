package com.nakudin.videotoaudio.ads

import com.nakudin.videotoaudio.BuildConfig

/**
 * Centralized AdMob configuration. All ad unit IDs are sourced from BuildConfig,
 * which is populated at build time from `admob.properties` (production) or the
 * official Google test IDs (development). No ad IDs are hard-coded across the UI.
 */
object AdConfig {

    val appId: String = BuildConfig.ADMOB_APP_ID
    val homeBannerId: String = BuildConfig.ADMOB_BANNER_HOME
    val historyBannerId: String = BuildConfig.ADMOB_BANNER_HISTORY
    val interstitialId: String = BuildConfig.ADMOB_INTERSTITIAL

    /**
     * Minimum time between two interstitial impressions, in milliseconds.
     * Enforces "reasonable frequency controls" so users are not interrupted
     * on every single conversion.
     */
    const val INTERSTITIAL_MIN_INTERVAL_MS: Long = 120_000L

    /** True when the currently configured IDs are Google's test IDs. */
    val isUsingTestIds: Boolean
        get() = appId.startsWith("ca-app-pub-3940256099942544")
}
