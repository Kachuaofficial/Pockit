package com.invatech.pockit

import android.app.Application
import com.invatech.pockit.data.local.database.PockitDatabase

class PockitApplication : Application() {

    val database: PockitDatabase get() = PockitDatabase.getDatabase(this)

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Only adopt unscoped legacy data for the account already signed in at upgrade.
        val ownerPrefs = getSharedPreferences("pockit_database_owner", 0)
        if (!ownerPrefs.contains("legacy_owner")) {
            val existingUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ownerPrefs.edit().putString("legacy_owner", existingUid ?: "unassigned_legacy").commit()
        }
        com.invatech.pockit.ui.theme.ThemeManager.init(this)
        com.invatech.pockit.data.rewards.UserRewardManager.init(this)
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
