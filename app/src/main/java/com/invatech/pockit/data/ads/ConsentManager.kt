package com.invatech.pockit.data.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/** Refresh each launch, retry failures on foreground with a bounded cooldown. Never infer consent. */
class ConsentManager(private val activity: Activity) {
    private val information = UserMessagingPlatform.getConsentInformation(activity)
    private var updating = false
    private var completed = false
    private var lastAttempt = -60_000L

    fun requestConsentAndInit(onConsentComplete: () -> Unit) {
        // Also propagates revoked consent to mounted ad slots.
        onConsentComplete()
        if (activity.isFinishing || activity.isDestroyed || updating || completed ||
            SystemClock.elapsedRealtime() - lastAttempt < 60_000L) return
        updating = true
        lastAttempt = SystemClock.elapsedRealtime()
        information.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (activity.isFinishing || activity.isDestroyed) { updating = false; return@requestConsentInfoUpdate }
            onConsentComplete()
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                updating = false
                completed = error == null
                error?.let { Log.w("ConsentManager", "Consent form: code=${it.errorCode}; ${it.message}") }
                onConsentComplete()
            }
        }, { error ->
            updating = false
            Log.w("ConsentManager", "Consent update: code=${error.errorCode}; ${error.message}; canRequestAds=${information.canRequestAds()}")
            onConsentComplete()
        })
    }
}
