package com.invatech.pockit.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.invatech.pockit.R

class GoogleAuthClient(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): String {
        val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<Activity>().firstOrNull()
            ?: error("Google sign-in needs a foreground activity.")
        val clientId = context.getString(R.string.default_web_client_id)
        check(clientId.endsWith(".apps.googleusercontent.com")) { "Google sign-in client is not configured." }
        // Explicit button flow also supports accounts that need reauthentication and
        // devices where the bottom-sheet credential request returns no credentials.
        val option = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = credentialManager.getCredential(context = MutableContextWrapper(activity), request = request)
        val credential = result.credential
        check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google returned an unsupported sign-in response."
        }
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
