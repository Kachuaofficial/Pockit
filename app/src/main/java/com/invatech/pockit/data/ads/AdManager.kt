package com.invatech.pockit.data.ads

/*
 * Consent-aware SDK readiness and ad failure reporting shared across screens.
 * Readiness gates ad loading; it is separate from wallet and reward balances.
 */

import android.content.Context
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/** Consent-gated initialization; reward interstitials are shown only after an explicit offer. */
object AdManager {
    private val ready = MutableStateFlow(false)
    val adsReady = ready.asStateFlow()
    private val started = AtomicBoolean(false)
    private val initialized = AtomicBoolean(false)
    private val privacyChoicesOpen = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(context: Context) {
        val app = context.applicationContext
        val allowed = !privacyChoicesOpen.get() && UserMessagingPlatform.getConsentInformation(app).canRequestAds()
        ready.value = allowed && initialized.get()
        if (!allowed || started.getAndSet(true)) return
        // Never overwrite publisher privacy/age settings or SDK-managed test-device configuration.
        scope.launch {
            try {
                MobileAds.initialize(app, InitializationConfig.Builder(AdConfig.APP_ID).build()) { status ->
                    scope.launch(Dispatchers.Main.immediate) {
                        initialized.set(true)
                        ready.value = !privacyChoicesOpen.get() && UserMessagingPlatform.getConsentInformation(app).canRequestAds()
                        status.adapterStatusMap.forEach { (adapter, state) ->
                            Log.d("AdManager", "$adapter: ${state.initializationState}; ${state.description}")
                        }
                    }
                }
            } catch (error: Exception) {
                started.set(false)
                Log.e("AdManager", "Mobile Ads initialization failed", error)
            }
        }
    }

    fun beginPrivacyChoices() {
        privacyChoicesOpen.set(true)
        ready.value = false
    }

    fun finishPrivacyChoices(context: Context) {
        privacyChoicesOpen.set(false)
        initialize(context)
    }

    fun reportLoadFailure(format: String, error: LoadAdError) {
        Log.w("PockitAds", "$format load failed: code=${error.code}, message=${error.message}, " +
            "responseId=${error.responseInfo?.responseId}")
    }
}
