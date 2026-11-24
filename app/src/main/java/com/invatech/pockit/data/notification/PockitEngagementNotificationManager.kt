package com.invatech.pockit.data.notification

/*
 * Schedules and evaluates optional engagement reminders using account-related activity/preferences.
 * Frequency controls notification prompts, not financial calculations.
 */

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
    fun frequency(context: Context) = prefs(context).getInt("frequency", 6).coerceIn(1, 6)
    fun setFrequency(context: Context, value: Int) {
        val safeValue = value.coerceIn(1, 6)
        com.invatech.pockit.data.repository.AccountPreferences.write("frequency", safeValue)
        prefs(context).edit().putInt("frequency", safeValue).apply()
    }
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
            PeriodicWorkRequestBuilder<PockitEngagementWorker>(150, TimeUnit.MINUTES)
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
        val today = dayKey()
        val sent = if (p.getString("sent_day", "") == today) p.getInt("sent_count", 0) else 0
        val cap = frequency(context)
        val cooldown = 2 * 60 * 60_000L
        if (sent >= cap || now - p.getLong("last_sent", 0) < cooldown) return
        val pending = FinanceDatabase.get(context).records().list(uid, "UPI").count { it.status == "PENDING" }
        val prompts = listOf(
            Triple("Record today's spending", "A ten-second check-in keeps your wallet and monthly totals accurate.", "home"),
            Triple("See where your money went", "Open Insights for a quick category and spending-trend check.", "insights"),
            Triple("Stay close to your budget", "Review this month's limits before the next purchase.", "budgets"),
            Triple("Keep udhar clear", "Check Friends for pending balances, repayments, and shared expenses.", "friends"),
            Triple("Plan the next money move", "Use Planner to prepare for upcoming expenses and goals.", "planner"),
            Triple("Review your wallet", "Open Pockit and make sure today's income and expenses are complete.", "all_transactions")
        )
        val (title, body, route) = if (pending > 0 && sent % 2 == 0) {
            Triple(
                "Payments waiting for review",
                "$pending detected payment${if (pending == 1) " is" else "s are"} ready. Approve only what you want to track.",
                "upi_review"
            )
        } else prompts[sent % prompts.size]
        NotificationHelper.showAndStoreNotification(
            context = context,
            title = title,
            body = body,
            type = "engagement",
            extraData = mapOf("navigate_to" to route),
            stableId = "engagement_${today}_$sent".hashCode()
        )
        p.edit().putString("sent_day", today).putInt("sent_count", sent + 1).putLong("last_sent", now).apply()
    }
}
