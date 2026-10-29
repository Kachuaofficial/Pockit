package com.invatech.pockit.data.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Handles Google User Messaging Platform (UMP) consent flow.
 * Required by Google Play & AdMob policies for GDPR / European Economic Area (EEA) & UK compliance.
 */
class ConsentManager(private val activity: Activity) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    fun requestConsentAndInit(onConsentComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    // Initialize MobileAds once consent flow completes or falls back
                    onConsentComplete()
                }
            },
            { requestConsentError ->
                Log.w(TAG, "Consent info update failed: ${requestConsentError.message}")
                // Fallback: Always allow AdMob to initialize, especially for test ads and non-EEA environments
                onConsentComplete()
            }
        )

        // Check if ads can already be requested from previously saved consent
        if (consentInformation.canRequestAds()) {
            onConsentComplete()
        }
    }

    companion object {
        private const val TAG = "ConsentManager"
    }
}
