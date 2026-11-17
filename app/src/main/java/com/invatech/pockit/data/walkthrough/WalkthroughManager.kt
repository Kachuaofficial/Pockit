package com.invatech.pockit.data.walkthrough

import android.content.Context
import android.content.SharedPreferences

class WalkthroughManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pockit_walkthrough_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WALKTHROUGH_COMPLETED = "walkthrough_completed"
        private const val KEY_ANALYTICS_UNLOCKED_UNTIL = "analytics_unlocked_until"
        private const val KEY_DEVELOPER_UPDATES_DISMISSED = "developer_updates_dismissed_v1_8"
        private const val UNLOCK_DURATION_MS = 24 * 60 * 60 * 1000L // 24 Hours
    }

    fun isWalkthroughCompleted(): Boolean {
        return prefs.getBoolean(KEY_WALKTHROUGH_COMPLETED, false)
    }

    fun setWalkthroughCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_WALKTHROUGH_COMPLETED, completed).apply()
    }

    fun isDeveloperUpdatesDismissed(): Boolean {
        return prefs.getBoolean(KEY_DEVELOPER_UPDATES_DISMISSED, false)
    }

    fun setDeveloperUpdatesDismissed(dismissed: Boolean = true) {
        prefs.edit().putBoolean(KEY_DEVELOPER_UPDATES_DISMISSED, dismissed).apply()
    }

    fun isAdvancedAnalyticsUnlocked(): Boolean {
        val unlockUntil = prefs.getLong(KEY_ANALYTICS_UNLOCKED_UNTIL, 0L)
        return System.currentTimeMillis() < unlockUntil
    }

    fun unlockAdvancedAnalytics24Hours() {
        val unlockUntil = System.currentTimeMillis() + UNLOCK_DURATION_MS
        prefs.edit().putLong(KEY_ANALYTICS_UNLOCKED_UNTIL, unlockUntil).apply()
    }

    fun getRemainingUnlockHours(): Int {
        val unlockUntil = prefs.getLong(KEY_ANALYTICS_UNLOCKED_UNTIL, 0L)
        val remainingMs = unlockUntil - System.currentTimeMillis()
        return if (remainingMs > 0) (remainingMs / (1000 * 60 * 60)).toInt().coerceAtLeast(1) else 0
    }
}
