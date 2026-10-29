package com.invatech.pockit.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Global ad manager for Pockit.
 * In compliance with user preference, only non-intrusive banner ads are enabled.
 * All interstitial, popup, and rewarded ads have been completely removed.
 */
object AdManager {

    private const val TAG = "AdManager"
    private val isMobileAdsInitialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (isMobileAdsInitialized.getAndSet(true)) return

        MobileAds.initialize(context) { status ->
            Log.d(TAG, "MobileAds initialized for banner ads: $status")
        }
    }

    /**
     * No-op: Full-screen interstitial ads have been disabled.
     * Immediately proceeds with user action without showing any ad.
     */
    fun showInterstitialIfEligible(activity: Activity, onAdDismissed: () -> Unit) {
        onAdDismissed()
    }

    /**
     * No-op: Rewarded ads have been disabled.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewardEarned: (RewardItem) -> Unit,
        onAdClosed: () -> Unit
    ) {
        onAdClosed()
    }
}
