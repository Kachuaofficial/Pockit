package com.invatech.pockit

/*
 * Process-level initialization shared by activities, services and workers.
 * Initializes preferences and reminders; database access resolves the current account instead of caching one account globally.
 */

import android.app.Application
import com.invatech.pockit.data.local.database.PockitDatabase

class PockitApplication : Application() {

    val database: PockitDatabase get() = PockitDatabase.getDatabase(this)

    /**
     * Initializes process-wide managers once and records which signed-in account may adopt the old
     * unscoped cache. Activities and background components then share these initialized services.
     */
    override fun onCreate() {
        super.onCreate()
        instance = this
        com.invatech.pockit.data.security.AppAttestation.install()
        com.invatech.pockit.data.network.NetworkMonitor.initialize(this)
        // Only adopt unscoped legacy data for the account already signed in at upgrade.
        val ownerPrefs = getSharedPreferences("pockit_database_owner", 0)
        if (!ownerPrefs.contains("legacy_owner")) {
            val existingUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ownerPrefs.edit().putString("legacy_owner", existingUid ?: "unassigned_legacy").commit()
        }
        com.invatech.pockit.data.offline.OfflineSyncManager.initialize()
        com.invatech.pockit.ui.common.motion.MotionSettings.refresh(this)
        com.invatech.pockit.data.notification.UpiNotificationManager.init(this)
        com.invatech.pockit.data.notification.PockitEngagementNotificationManager.init(this)
        com.invatech.pockit.data.notification.SubscriptionReminderWorker.schedule(this)
    }

    companion object {
        lateinit var instance: PockitApplication
            private set
    }
}
