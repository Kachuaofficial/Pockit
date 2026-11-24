package com.invatech.pockit.data.notification

/*
 * Periodic WorkManager task for due-subscription reminders.
 * A due-date reminder records no automatic payment and can run later than an exact clock time.
 */

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.invatech.pockit.data.repository.SubscriptionRepository
import java.util.concurrent.TimeUnit

class SubscriptionReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = SubscriptionRepository()

    override suspend fun doWork(): Result {
        return try {
            repository.syncFromFirebase()
            val subscriptions = repository.getAllSubscriptions().filter { it.isActive }
            for (sub in subscriptions) {
                val daysUntil = repository.getDaysUntilNextDue(sub.dueDayOfMonth)
                val amountRupees = sub.amountPaise / 100

                if (daysUntil == 0) {
                    NotificationHelper.showAndStoreNotification(
                        context = applicationContext,
                        title = "🔔 Today: ${sub.title} Due!",
                        body = "₹$amountRupees for ${sub.title} is due today via ${sub.paymentMethod}. Keep funds ready!",
                        type = "subscription",
                        extraData = mapOf("navigate_to" to "planner"),
                        stableId = "subscription_${sub.id}_today".hashCode()
                    )
                } else if (daysUntil <= sub.reminderDaysBefore) {
                    NotificationHelper.showAndStoreNotification(
                        context = applicationContext,
                        title = "⏰ Upcoming: ${sub.title} in $daysUntil day${if (daysUntil > 1) "s" else ""}",
                        body = "₹$amountRupees is due on day ${sub.dueDayOfMonth}. Plan ahead and stay stress-free.",
                        type = "subscription",
                        extraData = mapOf("navigate_to" to "planner"),
                        stableId = "subscription_${sub.id}_$daysUntil".hashCode()
                    )
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "pockit_subscription_reminder_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<SubscriptionReminderWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
