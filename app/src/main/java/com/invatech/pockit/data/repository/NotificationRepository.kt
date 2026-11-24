package com.invatech.pockit.data.repository

/*
 * Owner-scoped notification inbox with Room-backed reads and cloud mutations.
 * Notification read/delete state is separate from the financial event that an alert describes.
 */

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.entity.NotificationEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

class NotificationRepository {
    private val owner = FirebaseAuth.getInstance().currentUser?.uid
    private val dao = PockitApplication.instance.database.notificationDao()
    private val db = FirebaseFirestore.getInstance()
    private fun collection() = db.collection("users").document(owner ?: error("Please sign in again.")).collection("notifications")
    private fun checkOwner() = check(owner != null && FirebaseAuth.getInstance().currentUser?.uid == owner) { "Please sign in again." }
    /**
     * Emits the local inbox first, create-only imports cached IDs, then streams cloud rows and
     * replaces the local snapshot. deleted=true documents are excluded from display but retained
     * remotely so imports do not recreate removed messages. Cancellation removes the listener.
     */
    fun observeNotifications(): Flow<List<NotificationEntity>> = flow {
        checkOwner()
        val local = dao.observeNotifications().first()
        emit(local)
        for (row in local) {
            val ref = collection().document(row.id)
            db.runTransaction { tx -> if (!tx.get(ref).exists()) tx.set(ref, row) }.await()
        }
        emitAll(callbackFlow {
            val listener = collection().addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.documents.orEmpty().filter { it.getBoolean("deleted") != true }.map { doc ->
                    NotificationEntity(doc.id, doc.getString("title").orEmpty(), doc.getString("body").orEmpty(),
                        doc.getString("type") ?: "general", doc.getLong("timestamp") ?: 0L,
                        doc.getBoolean("read") ?: doc.getBoolean("isRead") ?: false, doc.getString("targetId"))
                }.sortedByDescending { it.timestamp })
            }
            awaitClose { listener.remove() }
        }.onEach { rows -> dao.replaceCloudSnapshot(rows) })
    }
    fun observeUnreadCount(): Flow<Int> = observeNotifications().map { rows -> rows.count { !it.isRead } }
    suspend fun insertNotification(title: String, body: String, type: String = "general", targetId: String? = null): Long {
        checkOwner()
        val row = NotificationEntity(title = title, body = body, type = type, targetId = targetId)
        collection().document(row.id).set(row).await()
        return dao.insertNotification(row)
    }
    suspend fun markAsRead(id: String) { checkOwner(); collection().document(id).update("read", true).await(); dao.markAsRead(id) }
    suspend fun markAllAsRead() {
        checkOwner()
        collection().get().await().documents.chunked(400).forEach { chunk ->
            val batch = db.batch(); chunk.forEach { batch.update(it.reference, "read", true) }; batch.commit().await()
        }
        dao.markAllAsRead()
    }
    suspend fun deleteNotification(id: String) { checkOwner(); collection().document(id).update("deleted", true).await(); dao.deleteNotification(id) }
    suspend fun clearAll() {
        checkOwner()
        collection().get().await().documents.chunked(400).forEach { chunk ->
            val batch = db.batch(); chunk.forEach { batch.update(it.reference, "deleted", true) }; batch.commit().await()
        }
        dao.clearAllNotifications()
    }
    suspend fun ensureInitialWelcomeNotification() {
        checkOwner()
        val ref = collection().document("welcome")
        db.runTransaction { tx -> if (!tx.get(ref).exists()) tx.set(ref,
            NotificationEntity(id = "welcome", title = "Welcome to Pockit", body = "Track spending, plan your budget and split expenses with friends.")) }.await()
    }
}
