package com.invatech.pockit.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.invatech.pockit.data.notification.*
import com.invatech.pockit.ui.common.motion.MotionSettings

/** Sync user choices, never OS permissions or device authentication enrollment. */
object AccountPreferences {
    private var applying = false
    val error = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun write(key: String, value: Any) {
        if (applying) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("settings").document("preferences")
            .set(mapOf(key to value), SetOptions.merge()).addOnFailureListener { error.value = "Preferences couldn't sync: ${it.message}" }
    }
    fun bind(context: Context, uid: String): com.google.firebase.firestore.ListenerRegistration {
        val ref = FirebaseFirestore.getInstance().collection("users").document(uid).collection("settings").document("preferences")
        return ref.addSnapshotListener { snapshot, failure ->
            if (failure != null) { error.value = "Preferences couldn't sync: ${failure.message}"; return@addSnapshotListener }
            if (FirebaseAuth.getInstance().currentUser?.uid != uid) return@addSnapshotListener
            val data = snapshot?.data
            if (data == null) {
                ref.set(mapOf(
                    "animations" to context.getSharedPreferences("pockit_accessibility", 0).getBoolean("animations", true),
                    "reminders" to PockitEngagementNotificationManager.isSmartRemindersEnabled(context),
                    "frequency" to PockitEngagementNotificationManager.frequency(context),
                    "upiReview" to UpiNotificationManager.isAutoTrackingEnabled(context),
                    "upiApps" to listOf("com.google.android.apps.nbu.paisa.user", "com.phonepe.app", "net.one97.paytm", "com.dreamplug.androidapp", "in.org.npci.upiapp", "com.naviapp").associateWith { UpiNotificationManager.isAppEnabled(context, it) }
                )).addOnFailureListener { error.value = "Preferences couldn't sync: ${it.message}" }
                return@addSnapshotListener
            }
            applying = true
            try {
                (data["animations"] as? Boolean)?.let { MotionSettings.setEnabled(context, it) }
                (data["reminders"] as? Boolean)?.let { PockitEngagementNotificationManager.setSmartRemindersEnabled(context, it) }
                (data["frequency"] as? Number)?.let { PockitEngagementNotificationManager.setFrequency(context, it.toInt()) }
                (data["upiReview"] as? Boolean)?.let { UpiNotificationManager.setAutoTrackingEnabled(context, it) }
                (data["upiApps"] as? Map<*, *>)?.forEach { (pkg, enabled) -> if (pkg is String && enabled is Boolean) UpiNotificationManager.setAppEnabled(context, pkg, enabled) }
                error.value = null
            } finally { applying = false }
        }
    }
}
