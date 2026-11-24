package com.invatech.pockit.data.notification

/*
 * OS notification-listener callback for supported payment apps.
 * Checks opt-in and app selection before passing extracted text to the parser and review manager.
 */

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Android NotificationListenerService that detects outgoing UPI payments from payment apps
 * (Google Pay, PhonePe, Paytm, CRED, BHIM, etc.) and logs them automatically as expenses.
 * Strictly respects user privacy and user consent toggles.
 */
class UpiNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val context = applicationContext
        // Ensure user has enabled the feature in Pockit settings
        if (!UpiNotificationManager.isAutoTrackingEnabled(context)) {
            return
        }

        val packageName = sbn.packageName ?: return

        if (!UpiNotificationParser.isSupportedPackage(packageName)) return
        if (!UpiNotificationManager.isAppEnabled(context, packageName)) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        // Extract notification extras
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        // Parse notification
        val parsed = UpiNotificationParser.parse(packageName, title, text, bigText) ?: return


        UpiNotificationManager.processAndLogTransaction(context, parsed, sbn.key, sbn.postTime)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("UpiNotificationService", "UPI Notification Listener connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d("UpiNotificationService", "UPI Notification Listener disconnected.")
    }
}
