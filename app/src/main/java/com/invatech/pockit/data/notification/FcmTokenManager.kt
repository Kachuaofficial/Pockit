package com.invatech.pockit.data.notification

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    private const val TAG = "FcmTokenManager"

    suspend fun syncCurrentToken() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            updateTokenInFirestore(user.uid, token)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get or save FCM token", e)
        }
    }

    fun updateTokenInFirestore(uid: String, token: String) {
        val firestore = FirebaseFirestore.getInstance()
        val data = hashMapOf(
            "fcmToken" to token,
            "deviceModel" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "lastTokenSync" to FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated successfully for $uid")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving FCM token", e)
            }
    }
}
