package com.invatech.pockit.data.auth

/*
 * Maps credential/sign-in exceptions into actionable UI messages.
 * Keeps platform error interpretation outside the sign-in layout.
 */

import androidx.credentials.exceptions.*

/** Never mistake a missing provider or OAuth configuration issue for lost connectivity. */
fun googleSignInErrorMessage(error: Exception): String {
    val reason = error.message.orEmpty().lowercase()
    return when {
        reason.contains("28444") || reason.contains("developer") || reason.contains("oauth") || reason.contains("client is not configured") ->
            "Google sign-in isn't configured for this app build yet. Please contact support."
        error is GetCredentialProviderConfigurationException || error is GetCredentialUnsupportedException ->
            "Google sign-in services aren't available. Enable or update Google Play services, then try again."
        error is NoCredentialException ->
            "No Google account is available to sign in. Add or reauthenticate an account on this device, then try again."
        error is GetCredentialInterruptedException ->
            "Google sign-in was interrupted. Please try again."
        reason.contains("network") || reason.contains("connection") || reason.contains("timeout") ->
            "Google couldn't connect. Check your internet connection and try again."
        else -> "Google couldn't complete sign-in. Try again or use another account."
    }
}
