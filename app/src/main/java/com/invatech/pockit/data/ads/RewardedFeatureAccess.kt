package com.invatech.pockit.data.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.rewarded.OnUserEarnedRewardListener
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.invatech.pockit.PockitApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RewardedFeature(val title: String, val description: String) {
    RECEIPT_SCAN("Smart receipt scan", "Scan receipts and fill in the amount, shop and category automatically."),
    ADVANCED_REPORTS("Advanced reports", "Explore 3, 6 and 12-month reports and export your records as CSV.")
}

data class RewardedUnlockState(
    val loading: Boolean = false,
    val ready: Boolean = false,
    val showing: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/** Local, ad-only feature access. A completed rewarded ad unlocks one feature for 24 hours. */
object RewardedFeatureAccess {
    const val UNLOCK_DURATION_MS = 24L * 60L * 60L * 1000L
    private val changes = MutableStateFlow(0)
    val updates = changes.asStateFlow()
    private val prefs get() = PockitApplication.instance.getSharedPreferences("rewarded_feature_access", 0)

    fun expiresAt(feature: RewardedFeature): Long = prefs.getLong(feature.name, 0L)
    fun isUnlocked(feature: RewardedFeature, now: Long = System.currentTimeMillis()): Boolean = expiresAt(feature) > now

    fun unlock(feature: RewardedFeature) {
        val now = System.currentTimeMillis()
        val start = maxOf(now, expiresAt(feature))
        prefs.edit().putLong(feature.name, start + UNLOCK_DURATION_MS).apply()
        changes.value += 1
    }
}

class RewardedFeatureController {
    private val mutable = MutableStateFlow(RewardedUnlockState())
    val state = mutable.asStateFlow()
    private val main = Handler(Looper.getMainLooper())
    private var ad: RewardedInterstitialAd? = null
    private var loadedAt = 0L
    private var closed = false

    fun load() {
        if (closed || mutable.value.loading || mutable.value.showing || ad != null ||
            !AdManager.adsReady.value || !com.invatech.pockit.data.network.NetworkMonitor.online.value) return
        mutable.value = RewardedUnlockState(loading = true)
        RewardedInterstitialAd.load(
            AdRequest.Builder(AdConfig.REWARDED_AD_UNIT_ID).build(),
            object : AdLoadCallback<RewardedInterstitialAd> {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    main.post {
                        if (closed) ad.destroy() else {
                            this@RewardedFeatureController.ad = ad
                            loadedAt = android.os.SystemClock.elapsedRealtime()
                            mutable.value = RewardedUnlockState(ready = true)
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: com.google.android.libraries.ads.mobile.sdk.common.LoadAdError) {
                    main.post {
                        AdManager.reportLoadFailure("rewarded feature", adError)
                        if (!closed) mutable.value = RewardedUnlockState(error = "No rewarded ad is available right now. Please try again later.")
                    }
                }
            }
        )
    }

    /** Must be called from a clearly labelled user action that names the feature and 24-hour reward. */
    fun show(activity: Activity, feature: RewardedFeature) {
        val value = ad ?: run { load(); return }
        if (closed || mutable.value.showing || !AdManager.adsReady.value) return
        if (android.os.SystemClock.elapsedRealtime() - loadedAt >= 45L * 60L * 1000L) {
            value.destroy(); ad = null; load(); return
        }
        mutable.value = RewardedUnlockState(showing = true)
        value.adEventCallback = object : RewardedInterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                main.post {
                    value.destroy(); ad = null
                    mutable.value = RewardedUnlockState(message = mutable.value.message)
                    load()
                }
            }

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                main.post {
                    value.destroy(); ad = null
                    mutable.value = RewardedUnlockState(error = "The video could not open. The feature was not unlocked.")
                    load()
                }
            }
        }
        ad = null
        value.show(activity, OnUserEarnedRewardListener {
            main.post {
                RewardedFeatureAccess.unlock(feature)
                mutable.value = mutable.value.copy(message = "${feature.title} is active for 24 hours.")
            }
        })
    }

    fun close() {
        closed = true
        ad?.destroy()
        ad = null
    }
}
