package com.invatech.pockit.data.auth

/*
 * Unit tests for translating credential exceptions into user-facing sign-in messages.
 */

import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import org.junit.Assert.*
import org.junit.Test

class GoogleSignInErrorsTest {
    @Test fun missingAccountDoesNotClaimNetworkFailure() {
        val message = googleSignInErrorMessage(NoCredentialException("No credentials"))
        assertTrue(message.contains("account")); assertFalse(message.contains("connection"))
    }
    @Test fun providerAndDeveloperErrorsGiveDifferentActions() {
        assertTrue(googleSignInErrorMessage(GetCredentialProviderConfigurationException("Provider missing")).contains("Play services"))
        assertTrue(googleSignInErrorMessage(IllegalStateException("[28444] Developer console configuration")).contains("configured"))
    }
    @Test fun actualConnectionFailureMentionsNetwork() {
        assertTrue(googleSignInErrorMessage(IllegalStateException("Network connection timeout")).contains("internet connection"))
    }
}
