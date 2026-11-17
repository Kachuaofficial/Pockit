package com.invatech.pockit.data.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.invatech.pockit.data.local.database.PockitDatabase
import com.invatech.pockit.data.local.entity.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PockitFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            FcmTokenManager.updateTokenInFirestore(currentUser.uid, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Pockit Expense Update"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "A shared expense was updated."

        val type = remoteMessage.data["type"] ?: "general"
        val targetId = remoteMessage.data["spaceId"]
            ?: remoteMessage.data["transactionId"]
            ?: remoteMessage.data["friendId"]

        val extraData = remoteMessage.data.toMutableMap().apply {
            put("navigate_to", "notifications")
        }

        // Show push notification in device tray
        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            extraData = extraData
        )

        // Persist locally in Room DB so it immediately appears in the Notifications screen
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val db = PockitDatabase.getDatabase(applicationContext)
                val row = NotificationEntity(
                    id = remoteMessage.messageId?.replace("/", "_") ?: java.util.UUID.randomUUID().toString(),
                    title = title, body = body, type = type, targetId = targetId)
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    db.notificationDao().insertNotification(row)
                    com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(uid)
                        .collection("notifications").document(row.id).set(row)
                }

            }.onFailure { e ->
                Log.e(TAG, "Failed to persist incoming notification", e)
            }
        }
    }

    companion object {
        private const val TAG = "PockitFCMService"
    }
}
