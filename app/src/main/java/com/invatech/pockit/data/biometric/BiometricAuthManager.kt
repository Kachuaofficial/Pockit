package com.invatech.pockit.data.biometric

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pockit_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
    }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric(
        activity: FragmentActivity,
        title: String = "Unlock Pockit",
        subtitle: String = "Confirm fingerprint or face to continue",
        negativeButtonText: String? = null,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        val authenticators = if (negativeButtonText != null) {
            promptInfoBuilder.setNegativeButtonText(negativeButtonText)
            BIOMETRIC_STRONG or BIOMETRIC_WEAK
        } else {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

        val promptInfo = promptInfoBuilder.build()

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
