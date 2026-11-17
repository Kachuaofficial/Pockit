
package com.invatech.pockit.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser
        get() = firebaseAuth.currentUser

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun signInWithGoogleCredential(
        credential: AuthCredential
    ): Result<Unit> {
        return try {
            val authResult = firebaseAuth
                .signInWithCredential(credential)
                .await()

            val user = authResult.user
                ?: throw IllegalStateException(
                    "Firebase did not return a user."
                )

            val userDocument = firestore
                .collection("users")
                .document(user.uid)

            // Create the profile only if it doesn't exist.
            firestore.runTransaction { transaction ->
                val snapshot = transaction[userDocument]

                if (!snapshot.exists()) {
                    val profile = hashMapOf<String, Any>(
                        "uid" to user.uid,
                        "name" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    transaction.set(userDocument, profile)
                }
            }.await()

            com.invatech.pockit.data.rewards.UserRewardManager.syncWithFirebase()

            Result.success(Unit)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun createGoogleCredential(
        idToken: String
    ): AuthCredential {
        return com.google.firebase.auth.GoogleAuthProvider
            .getCredential(idToken, null)
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
