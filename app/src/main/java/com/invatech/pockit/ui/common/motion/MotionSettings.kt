package com.invatech.pockit.ui.common.motion

/*
 * Observable animation preference combined with the system animator duration scale.
 * The activity refreshes the system value on resume and the UI observes the resulting state.
 */

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object MotionSettings {
    private val state = MutableStateFlow(true)
    val enabled = state.asStateFlow()
    val durationScale = object : androidx.compose.ui.MotionDurationScale {
        override val scaleFactor: Float get() = if (state.value) systemScale else 0f
    }
    private var systemScale = 1f
    fun refresh(context: Context) {
        val preferred = context.getSharedPreferences("pockit_accessibility", 0).getBoolean("animations", true)
        systemScale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        val system = systemScale > 0f
        state.value = preferred && system
    }
    fun setEnabled(context: Context, enabled: Boolean) {
        com.invatech.pockit.data.repository.AccountPreferences.write("animations", enabled)
        context.getSharedPreferences("pockit_accessibility", 0).edit().putBoolean("animations", enabled).apply()
        refresh(context)
    }
}
