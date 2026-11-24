package com.invatech.pockit.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A validated internet connection does not imply that Firebase or Play is available. */
object NetworkMonitor {
    private val mutable = MutableStateFlow(false)
    val online = mutable.asStateFlow()
    private var manager: ConnectivityManager? = null
    fun initialize(context: Context) {
        if (manager != null) return
        val service = context.getSystemService(ConnectivityManager::class.java)
        manager = service
        fun update() { mutable.value = service.getNetworkCapabilities(service.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true }
        service.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = update()
            override fun onLost(network: Network) = update()
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = update()
        })
        update()
    }
    fun requireOnline() { check(online.value) { "You're offline. Connect to the internet to use this feature." } }
}
