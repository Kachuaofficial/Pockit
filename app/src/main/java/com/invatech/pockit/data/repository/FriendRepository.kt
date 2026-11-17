package com.invatech.pockit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.dao.FriendDao
import com.invatech.pockit.data.local.entity.FriendEntity
import com.invatech.pockit.data.local.entity.FriendTransactionEntity
import com.invatech.pockit.ui.friends.model.ExpenseDirection
import com.invatech.pockit.ui.friends.model.Friend
import com.invatech.pockit.ui.friends.model.FriendConnectionPayload
import com.invatech.pockit.ui.friends.model.FriendTransaction
import com.invatech.pockit.ui.friends.model.TransactionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FriendRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val localDao: FriendDao? = runCatching { PockitApplication.instance.database.friendDao() }.getOrNull()
    val currentUserId: String
        get() = auth.currentUser?.uid ?: "current_user_me"

    val currentUserName: String
        get() = auth.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "You"

    val currentUserEmail: String
        get() = auth.currentUser?.email?.lowercase() ?: ""

    val currentUserQrPayload: String
        get() {
            return FriendConnectionPayload(
                userId = currentUserId,
                name = currentUserName,
                email = currentUserEmail,
                avatarEmoji = "🧑‍💻"
            ).toQrString()
        }

    /**
     * Realtime flow of all barcode-connected friendships for current user.
     */
    fun observeFriends(): Flow<List<Friend>> = callbackFlow {
        val uid = currentUserId

        val listener = firestore.collection("friendships")
            .whereArrayContains("users", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val friendsList = snapshot.documents.mapNotNull { doc ->
                    val users = doc.get("users") as? List<*> ?: return@mapNotNull null
                    val otherUserId = users.firstOrNull { it != uid }?.toString() ?: return@mapNotNull null

                    @Suppress("UNCHECKED_CAST")
                    val namesMap = doc.get("userNames") as? Map<String, String> ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    val emailsMap = doc.get("userEmails") as? Map<String, String> ?: emptyMap()
                    @Suppress("UNCHECKED_CAST")
                    val emojisMap = doc.get("userEmojis") as? Map<String, String> ?: emptyMap()

                    Friend(
                        id = otherUserId,
                        name = namesMap[otherUserId] ?: "Friend",
                        email = emailsMap[otherUserId] ?: "",
                        avatarEmoji = emojisMap[otherUserId] ?: "👤",
                        friendshipId = doc.id,
                        createdAt = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                    )
                }.sortedByDescending { it.createdAt }

                trySend(friendsList)

                localDao?.let { dao ->
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            dao.insertFriends(friendsList.map { FriendEntity.fromFriend(it) })
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Realtime flow of all transactions across friendships with a limit to avoid unbounded Firestore reads.
     */
    fun observeTransactions(limit: Long = 100): Flow<List<FriendTransaction>> = callbackFlow {
        val uid = currentUserId

        val listener = firestore.collection("friend_transactions")
            .whereArrayContains("participantIds", uid)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val txs = snapshot.documents.mapNotNull { doc ->
                    val directionStr = doc.getString("direction") ?: return@mapNotNull null
                    val dir = try {
                        ExpenseDirection.valueOf(directionStr)
                    } catch (_: Exception) {
                        ExpenseDirection.GAVE_UDHAR
                    }

                    val creatorId = doc.getString("creatorId") ?: ""
                    val targetFriendId = doc.getString("friendId") ?: ""
                    val payerId = doc.getString("payerId")
                    val rawAmount = doc.getLong("amountPaise")?.let { it / 100.0 } ?: doc.getDouble("amount") ?: 0.0

                    // Normalize friendId so that from current user's perspective:
                    // friendId is the other person
                    val friendId = if (creatorId == uid) targetFriendId else creatorId

                    // If other person created the transaction, invert perspective:
                    val effectiveDirection = if (creatorId == uid) {
                        dir
                    } else {
                        when (dir) {
                            ExpenseDirection.GAVE_UDHAR -> ExpenseDirection.TOOK_UDHAR
                            ExpenseDirection.TOOK_UDHAR -> ExpenseDirection.GAVE_UDHAR
                            ExpenseDirection.SETTLE_UP -> ExpenseDirection.SETTLE_UP
                        }
                    }

                    val effectiveAmount = if (dir == ExpenseDirection.SETTLE_UP) {
                        val absAmount = kotlin.math.abs(rawAmount)
                        if (payerId != null) {
                            // If current user paid, my debt is reduced -> positive change (+absAmount)
                            // If friend paid me, friend's debt to me is reduced -> negative change (-absAmount)
                            if (payerId == uid) absAmount else -absAmount
                        } else {
                            // Legacy fallback
                            if (creatorId == uid) rawAmount else -rawAmount
                        }
                    } else {
                        kotlin.math.abs(rawAmount)
                    }

                    val statusStr = doc.getString("status") ?: "CONFIRMED"
                    val status = try {
                        TransactionStatus.valueOf(statusStr)
                    } catch (_: Exception) {
                        TransactionStatus.CONFIRMED
                    }

                    FriendTransaction(
                        id = doc.id,
                        friendId = friendId,
                        amount = effectiveAmount,
                        direction = effectiveDirection,
                        description = doc.getString("description") ?: "Expense",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        note = doc.getString("note"),
                        payerId = payerId,
                        creatorId = creatorId,
                        status = status
                    )
                }.sortedByDescending { it.timestamp }

                trySend(txs)

                localDao?.let { dao ->
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            dao.insertTransactions(txs.map { FriendTransactionEntity.fromFriendTransaction(it) })
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Instantly connect with a friend by decoding their scanned QR/barcode.
     */
    suspend fun connectFriendViaBarcode(rawBarcode: String): Result<Friend> {
        val payload = FriendConnectionPayload.fromQrString(rawBarcode)
            ?: return Result.failure(IllegalArgumentException("Invalid Pockit barcode format"))

        val uid = currentUserId
        if (payload.userId == uid || (payload.email.isNotBlank() && payload.email.equals(currentUserEmail, ignoreCase = true))) {
            return Result.failure(IllegalArgumentException("You cannot scan your own barcode!"))
        }

        val friendshipId = getFriendshipId(uid, payload.userId)

        return try {
            val friendshipRef = firestore.collection("friendships").document(friendshipId)
            val friendshipData = hashMapOf(
                "id" to friendshipId,
                "users" to listOf(uid, payload.userId),
                "userNames" to mapOf(
                    uid to currentUserName,
                    payload.userId to payload.name
                ),
                "userEmails" to mapOf(
                    uid to currentUserEmail,
                    payload.userId to payload.email
                ),
                "userEmojis" to mapOf(
                    uid to "🧑‍💻",
                    payload.userId to payload.avatarEmoji
                ),
                "createdAtMillis" to System.currentTimeMillis()
            )

            friendshipRef.set(friendshipData, SetOptions.merge()).await()

            val friend = Friend(
                id = payload.userId,
                name = payload.name,
                email = payload.email,
                avatarEmoji = payload.avatarEmoji,
                friendshipId = friendshipId
            )

            Result.success(friend)
        } catch (e: Exception) {
            // Return friend anyway for optimistic local connection if offline
            val friend = Friend(
                id = payload.userId,
                name = payload.name,
                email = payload.email,
                avatarEmoji = payload.avatarEmoji,
                friendshipId = friendshipId
            )
            Result.success(friend)
        }
    }

    /**
     * Record an expense between current user and a connected friend.
     */
    suspend fun addExpense(
        friendId: String,
        direction: ExpenseDirection,
        amount: Double,
        description: String,
        note: String? = null,
        txId: String = UUID.randomUUID().toString(),
        payerId: String? = null,
        status: TransactionStatus = TransactionStatus.CONFIRMED
    ): Result<Unit> {
        val uid = currentUserId

        val txData = hashMapOf<String, Any>(
            "id" to txId,
            "creatorId" to uid,
            "creatorName" to currentUserName,
            "friendId" to friendId,
            "participantIds" to listOf(uid, friendId),
            "amountPaise" to com.invatech.pockit.domain.Money.legacyPaise(kotlin.math.abs(amount)),
            "amount" to kotlin.math.abs(amount),
            "direction" to direction.name,
            "description" to description.trim(),
            "status" to status.name,
            "timestamp" to System.currentTimeMillis()
        )
        if (note?.isNotBlank() == true) {
            txData["note"] = note.trim()
        }
        if (payerId != null) {
            txData["payerId"] = payerId
        }

        return try {
            firestore.collection("friend_transactions")
                .document(txId)
                .set(txData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update status of an expense or settlement (e.g. CONFIRMED or REJECTED by counterparty).
     */
    suspend fun updateTransactionStatus(txId: String, newStatus: TransactionStatus): Result<Unit> {
        return try {
            firestore.collection("friend_transactions")
                .document(txId)
                .update("status", newStatus.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove / unfriend a connection.
     */
    suspend fun removeFriend(friendId: String, friendshipId: String): Result<Unit> {
        val uid = currentUserId
        val docId = if (friendshipId.isNotBlank()) friendshipId else getFriendshipId(uid, friendId)
        return try {
            firestore.collection("friendships").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an expense transaction.
     */
    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            firestore.collection("friend_transactions").document(expenseId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFriendshipId(userA: String, userB: String): String {
        return if (userA < userB) "${userA}_$userB" else "${userB}_$userA"
    }
}
