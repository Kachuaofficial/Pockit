package com.invatech.pockit.data.ads

object AdConfig {
    /**
     * Replace these with your live AdMob Ad Unit IDs before publishing to the Play Store.
     * Google's official sample/test IDs are used here to prevent account invalid traffic bans during testing.
     */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-5858375161867194/6572551621"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Strict non-intrusive policy: minimum interval of 10 minutes between full-screen ads.
     * Ads will NEVER interrupt user typing, calculations, or critical user flows.
     */
    const val MIN_FULLSCREEN_AD_INTERVAL_MS = 10 * 60 * 1000L // 10 minutes
}
