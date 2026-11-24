package com.invatech.pockit.data.repository

/*
 * Cloud-authoritative persistence for personal udhar, goals and UPI review records.
 * Room supplies cached reads; tombstones and create-only imports avoid restoring deleted cloud entries.
 */

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await

/** Firestore is authoritative. Room retains the last account-scoped snapshot for offline reading. */
class FinanceRepository(private val owner: String) {
    private val db = FirebaseFirestore.getInstance()
    private val dao = FinanceDatabase.get(PockitApplication.instance).records()
    private val migration = PockitApplication.instance.getSharedPreferences("finance_cloud_imports", 0)
    private fun records() = db.collection("users").document(owner).collection("financeRecords")
    private fun checkOwner() = check(owner.isNotBlank() && FirebaseAuth.getInstance().currentUser?.uid == owner) { "Please sign in again." }
    /**
     * Emits cached rows for this owner, imports previously unknown local IDs create-only, then
     * listens for the cloud snapshot. Deleted rows remain tombstones in storage but are filtered
     * out of the visible list. awaitClose detaches the cloud listener.
     */
    fun observe(): Flow<List<FinanceRecord>> = flow {
        checkOwner()
        emit(listOf("UDHAR", "GOAL", "UPI").flatMap { dao.list(owner, it) }.filter { it.status != "DELETED" })
        // Create-only import preserves existing cloud edits and deletion tombstones on later launches.
        val knownIds = migration.getStringSet(owner, emptySet()).orEmpty()
        for (kind in listOf("UDHAR", "GOAL", "UPI")) for (row in dao.list(owner, kind).filter { it.id !in knownIds }) {
            val ref = records().document(row.id)
            db.runTransaction { tx -> if (!tx.get(ref).exists()) tx.set(ref, row) }.await()
            migration.edit().putStringSet(owner, migration.getStringSet(owner, emptySet()).orEmpty() + row.id).apply()
        }
        emitAll(callbackFlow {
            val listener = records().addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val rows = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    FinanceRecord(doc.id, owner, data["kind"] as? String ?: return@mapNotNull null,
                        data["name"] as? String ?: "", (data["amountPaise"] as? Number)?.toLong() ?: 0L,
                        data["direction"] as? String ?: "", data["category"] as? String ?: "Other",
                        data["note"] as? String ?: "", data["status"] as? String ?: "PENDING",
                        (data["occurredAt"] as? Number)?.toLong() ?: 0L)
                }
                trySend(rows)
            }
            awaitClose { listener.remove() }
        }.onEach { rows ->
            rows.forEach { dao.insert(it); dao.update(it) }
            migration.edit().putStringSet(owner, migration.getStringSet(owner, emptySet()).orEmpty() + rows.map { it.id }).apply()
        }
            .map { rows -> rows.filter { it.status != "DELETED" }.sortedByDescending { it.occurredAt } })
    }
    /**
     * Checks owner identity before saving. Udhar events and their per-person aggregate are
     * committed together, and repeated event IDs do not apply their signed effect twice. Deletion
     * is a status tombstone so later local imports do not resurrect the event.
     */
    suspend fun save(row: FinanceRecord) {
        com.invatech.pockit.data.network.NetworkMonitor.requireOnline()
        checkOwner(); require(row.owner == owner)
        if (row.kind == "UDHAR") {
            val personId = java.security.MessageDigest.getInstance("SHA-256").digest(row.name.lowercase(java.util.Locale.ROOT).toByteArray()).joinToString("") { "%02x".format(it) }
            val balanceRef = db.collection("users").document(owner).collection("ledgerBalances").document(personId)
            val entryRef = records().document(row.id)
            val initial = records().whereEqualTo("kind", "UDHAR").get(com.google.firebase.firestore.Source.SERVER).await().documents
                .filter { it.getString("name").equals(row.name, true) && it.getString("status") != "DELETED" }
                .fold(0L) { total, doc -> Math.addExact(total, (doc.getLong("amountPaise") ?: 0L) * if (doc.getString("direction") in listOf("LENT", "REPAID")) 1 else -1) }
            db.runTransaction { tx ->
                val aggregate = tx.get(balanceRef)
                val existing = tx.get(entryRef)
                val current = aggregate.getLong("balancePaise") ?: initial
                val delta = row.amountPaise * if (row.direction in listOf("LENT", "REPAID")) 1 else -1
                val alreadyDeleted = existing.getString("status") == "DELETED"
                val next = if (row.status == "DELETED") {
                    if (existing.exists() && !alreadyDeleted) Math.subtractExact(current, delta) else current
                } else if (existing.exists()) current else {
                    if (row.direction == "RECEIVED") require(current > 0 && row.amountPaise <= current) { "Repayment exceeds the outstanding balance." }
                    if (row.direction == "REPAID") require(current < 0 && row.amountPaise <= -current) { "Repayment exceeds what you owe." }
                    Math.addExact(current, delta)
                }
                tx.set(balanceRef, mapOf("balancePaise" to next))
                tx.set(entryRef, row)
            }.await()
        } else records().document(row.id).set(row).await()
        dao.insert(row); dao.update(row)
    }
    suspend fun status(row: FinanceRecord, status: String) = save(row.copy(status = status))
    /**
     * Adds positive paise to an existing goal in a cloud transaction and rejects an amount above
     * the remaining target. In the current generic record schema, direction stores the saved
     * contribution total as a decimal string.
     */
    suspend fun contribute(row: FinanceRecord, amount: Long) {
        com.invatech.pockit.data.network.NetworkMonitor.requireOnline()
        checkOwner(); require(amount > 0)
        val ref = records().document(row.id)
        db.runTransaction { tx ->
            val snapshot = tx.get(ref)
            check(snapshot.exists() && snapshot.getString("status") != "DELETED") { "Goal no longer exists." }
            val current = snapshot.getString("direction")?.toLongOrNull() ?: 0L
            val target = snapshot.getLong("amountPaise") ?: 0L
            val total = Math.addExact(current, amount)
            require(total <= target) { "Amount exceeds the remaining goal." }
            tx.update(ref, "direction", total.toString())
        }.await()
    }
}
