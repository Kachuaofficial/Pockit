package com.invatech.pockit.glance

/*
 * Requests Glance widget refreshes after a ledger change. Keeps widget update work out of screen code.
 */

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manages pushing reactive updates to the Pockit Home Screen Glance Widget.
 */
object PockitWidgetManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Triggers asynchronous refresh of all active Pockit Home Screen widgets.
     */
    fun updateWidget(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                PockitHomeWidget().updateAll(appContext)
            }
        }
    }
}
