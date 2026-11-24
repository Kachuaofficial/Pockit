package com.invatech.pockit.ui.common

/*
 * Connectivity-aware presentation helpers shared by screens and finance error notices.
 * Network validation drives display state; filtering an error message neither retries nor completes a failed request.
 */

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Observes Android validated-network capability and unregisters its callback when composition
 * leaves. This describes network availability, not successful access to a particular backend.
 */
@Composable
fun rememberOnline(): Boolean {
    val online by com.invatech.pockit.data.network.NetworkMonitor.online.collectAsState()
    return online
}

/**
 * Suppresses messages containing backend/network markers while offline but preserves other
 * validation text. This is display filtering only; the operation remains failed and requires the
 * appropriate retry.
 */
fun visibleError(message: String?, online: Boolean): String? {
    if (online || message == null) return message
    val serverMarkers = listOf("cloud", "server", "network", "offline", "unavailable", "firestore", "firebase",
        "unable to resolve host", "failed to connect", "connection", "deadline", "timeout", "timed out", "sync failed")
    return message.takeUnless { text -> serverMarkers.any { text.contains(it, ignoreCase = true) } }
}
