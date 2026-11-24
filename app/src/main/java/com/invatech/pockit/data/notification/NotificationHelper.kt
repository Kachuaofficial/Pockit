package com.invatech.pockit.data.notification

/*
 * Creates the Android notification channel and displays local system notifications with navigation extras.
 * Posting also depends on OS notification permission; displaying an alert is distinct from persisting a ledger event.
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.invatech.pockit.MainActivity
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.R
import com.invatech.pockit.data.local.entity.NotificationEntity
import com.invatech.pockit.data.repository.NotificationRepository

object NotificationHelper {

    const val CHANNEL_ID = "pockit_expenses_channel"
    private const val CHANNEL_NAME = "Expenses & Splits"
    private const val CHANNEL_DESC = "Notifications for new expenses, udhar, and settlements"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        extraData: Map<String, String> = emptyMap(),
        stableId: Int? = null
    ) {
        if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "notifications")
            extraData.forEach { (k, v) -> putExtra(k, v) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            stableId ?: System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notificationId = stableId ?: (System.currentTimeMillis() % 100000).toInt()
        notificationManager?.notify(notificationId, notification)
    }

    /** Posts to the system tray and mirrors the same message into Pockit's notification inbox. */
    suspend fun showAndStoreNotification(
        context: Context,
        title: String,
        body: String,
        type: String = "general",
        targetId: String? = null,
        extraData: Map<String, String> = emptyMap(),
        stableId: Int? = null
    ) {
        showNotification(context, title, body, extraData, stableId)
        try {
            NotificationRepository().insertNotification(title, body, type, targetId)
        } catch (_: Exception) {
            // Keep the inbox useful offline; the normal repository sync can upload it later.
            PockitApplication.instance.database.notificationDao().insertNotification(
                NotificationEntity(
                    id = stableId?.let { "local_$it" } ?: java.util.UUID.randomUUID().toString(),
                    title = title,
                    body = body,
                    type = type,
                    targetId = targetId
                )
            )
        }
    }
}
