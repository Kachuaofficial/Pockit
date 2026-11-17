package com.invatech.pockit.data.notification

import android.content.Context
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.database.FinanceDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Uses only Pockit activity. No external app-usage permission or financial-text analytics. */
object PockitEngagementNotificationManager {
    private const val WORK = "pockit_smart_engagement_work"
    private fun prefs(context: Context) = context.getSharedPreferences("pockit_reminders_${FirebaseAuth.getInstance().currentUser?.uid ?: "guest"}", 0)
    fun init(context: Context) { if (isSmartRemindersEnabled(context)) schedule(context) }
    fun isSmartRemindersEnabled(context: Context) = prefs(context).getBoolean("enabled", true)
    fun setSmartRemindersEnabled(context: Context, enabled: Boolean) {
        com.invatech.pockit.data.repository.AccountPreferences.write("reminders", enabled)
        prefs(context).edit().putBoolean("enabled", enabled).apply()
        if (enabled) schedule(context) else WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
    fun frequency(context: Context) = prefs(context).getInt("frequency", 2)
    fun setFrequency(context: Context, value: Int) { com.invatech.pockit.data.repository.AccountPreferences.write("frequency", value.coerceIn(1, 3)); prefs(context).edit().putInt("frequency", value.coerceIn(1, 3)).apply() }
    fun recordAppUse(context: Context) {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        val last = p.getLong("last_open", 0)
        val date = dayKey()
        val count = if (p.getString("open_day", "") == date) p.getInt("opens", 0) else 0
        p.edit().putLong("last_open", now).putString("open_day", date)
            .putInt("opens", count + if (now - last > 30 * 60_000) 1 else 0).apply()
        if (isSmartRemindersEnabled(context)) schedule(context)
    }
    private fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<PockitEngagementWorker>(3, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build()).build())
    }
    private fun dayKey(): String = Calendar.getInstance().let { "${it.get(Calendar.YEAR)}-${it.get(Calendar.DAY_OF_YEAR)}" }
    suspend fun evaluateAndTriggerNotification(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!isSmartRemindersEnabled(context) || !androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val now = System.currentTimeMillis()
        val c = Calendar.getInstance()
        val minute = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
        if (minute < 510 || minute >= 1320) return
        val p = prefs(context)
        val sinceOpen = now - p.getLong("last_open", 0)
        if (sinceOpen < 2 * 60 * 60_000 || sinceOpen > 7 * 86400_000L) return
        val today = dayKey()
        val sent = if (p.getString("sent_day", "") == today) p.getInt("sent_count", 0) else 0
        val active = p.getString("open_day", "") == today && p.getInt("opens", 0) >= 3
        val cap = if (active) frequency(context) else 1
        val cooldown = if (active) 4 * 60 * 60_000L else 24 * 60 * 60_000L
        if (sent >= cap || now - p.getLong("last_sent", 0) < cooldown) return
        val pending = FinanceDatabase.get(context).records().list(uid, "UPI").count { it.status == "PENDING" }
        val latest = PockitApplication.instance.database.walletDao().getTransactions(1).firstOrNull()
        val title: String
        val body: String
        val route: String
        if (pending > 0) {
            title = "Payments waiting for your review"
            body = "$pending detected payments are ready. Approve only the entries you want to track."
            route = "upi_review"
        } else if (minute >= 1080 && (latest == null || now - latest.createdAtMillis > 12 * 60 * 60_000L)) {
            title = "A quick money check-in"
            body = "Any spending to record today? Keep your budget up to date in a few taps."
            route = "home"
        } else return
        NotificationHelper.showNotification(context, title, body, mapOf("navigate_to" to route))
        p.edit().putString("sent_day", today).putInt("sent_count", sent + 1).putLong("last_sent", now).apply()
    }
}
