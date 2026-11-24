package com.invatech.pockit.data.notification

/*
 * Optional detected-payment inbox and deduplication coordinator.
 * Stores a pending FinanceRecord; user approval in FinanceViewModel is the path that writes the wallet.
 */

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** On-device review inbox. No detected payment writes directly to the wallet. */
object UpiNotificationManager {
    private const val PREFS_NAME = "pockit_upi_prefs"
    private const val KEY_AUTO_UPI_ENABLED = "auto_upi_tracking_enabled"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _isAutoTrackingEnabledFlow = MutableStateFlow(false)
    val isAutoTrackingEnabledFlow: StateFlow<Boolean> = _isAutoTrackingEnabledFlow.asStateFlow()
    fun init(context: Context) { _isAutoTrackingEnabledFlow.value = isAutoTrackingEnabled(context) }

    fun isAppEnabled(context: Context, packageName: String): Boolean = context.getSharedPreferences(PREFS_NAME, 0).getBoolean("app_$packageName", true)
    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) { com.invatech.pockit.data.repository.AccountPreferences.write("upiApps", mapOf(packageName to enabled)); context.getSharedPreferences(PREFS_NAME, 0).edit().putBoolean("app_$packageName", enabled).apply() }

    fun isAutoTrackingEnabled(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_UPI_ENABLED, false)
    }

    fun setAutoTrackingEnabled(context: Context, enabled: Boolean) {
        com.invatech.pockit.data.repository.AccountPreferences.write("upiReview", enabled)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_UPI_ENABLED, enabled).apply()
        _isAutoTrackingEnabledFlow.value = enabled
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
                    val componentName = ComponentName(context, UpiNotificationListenerService::class.java)
                    putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    /**
     * Creates a pending owner-scoped review item only after opt-in. A reference or notification
     * identity is hashed into a stable ID so duplicate delivery cannot reopen an approved/rejected
     * item. The parser result is not automatically posted to the wallet.
     */
    fun processAndLogTransaction(context: Context, tx: ParsedUpiTransaction, eventKey: String = "", postedAt: Long = System.currentTimeMillis()) {
        if (!isAutoTrackingEnabled(context)) return
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch {
            try {
            // A stable notification identity survives process death and repeat delivery.
            // Never deduplicate on amount alone: two identical purchases can both be genuine.
            val reference = Regex("(?i)(?:utr|upi ref(?:erence)?|transaction id)[: #]*(\\w{8,})")
                .find(tx.rawText)?.groupValues?.get(1)
            val identity = if (reference != null) "$uid:$reference" else "$uid:$eventKey:$postedAt"
            val id = java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val dao = com.invatech.pockit.data.local.database.FinanceDatabase.get(context).records()
            val result = dao.insert(com.invatech.pockit.data.local.database.FinanceRecord(
                id = id, owner = uid, kind = "UPI", name = tx.merchantOrSender,
                amountPaise = tx.amountPaise, direction = tx.type.name, category = tx.category,
                note = tx.appName, occurredAt = postedAt
            ))
            val record = dao.list(uid, "UPI").firstOrNull { it.id == id }
            if (record != null) {
                val cloud = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val ref = cloud.collection("users").document(uid).collection("financeRecords").document(id)
                // Create only: repeated notifications must never reopen approved/rejected reviews.
                cloud.runTransaction { transaction -> if (!transaction.get(ref).exists()) transaction.set(ref, record) }
            }
            if (result != -1L) NotificationHelper.showAndStoreNotification(
                context = context, title = "Payment ready to review",
                body = "Check the amount and category before adding it to your wallet.",
                type = "expense",
                extraData = mapOf("navigate_to" to "upi_review"),
                stableId = 4201
            )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.w("UpiReview", "Unable to prepare payment review")
            }
        }
    }
}
