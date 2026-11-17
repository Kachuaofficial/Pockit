package com.invatech.pockit.data.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background CoroutineWorker for Pockit's Smart Local Engagement Notifications.
 * Runs efficiently even when app is closed with zero Firebase Cloud Messaging cost.
 */
class PockitEngagementWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("EngagementWorker", "PockitEngagementWorker executing scheduled check...")
        return try {
            PockitEngagementNotificationManager.evaluateAndTriggerNotification(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e("EngagementWorker", "Worker failed: ${e.message}", e)
            Result.retry()
        }
    }
}
