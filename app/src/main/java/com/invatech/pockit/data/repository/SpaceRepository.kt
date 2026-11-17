package com.invatech.pockit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.invatech.pockit.PockitApplication
import com.invatech.pockit.data.local.dao.SpaceDao
import com.invatech.pockit.data.local.entity.SpaceEntity
import com.invatech.pockit.data.local.entity.SpaceExpenseEntity
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.JoinResult
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.ui.space.model.SpaceExpense
import com.invatech.pockit.ui.space.model.SpaceType
import com.invatech.pockit.ui.space.util.Settlement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SpaceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val localDao: SpaceDao? = runCatching { PockitApplication.instance.database.spaceDao() }.getOrNull()
    val currentUserId: String
        get() = auth.currentUser?.uid ?: "me"

    val currentUserName: String
        get() = auth.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "You"

    /**
     * Realtime flow of all spaces the current user belongs to.
     * Automatically syncs when any member creates, joins, or updates a space.
     */
    fun observeSpaces(): Flow<List<Space>> = callbackFlow {
        val uid = currentUserId

        val listener = firestore.collection("spaces")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val spacesList = snapshot.documents.mapNotNull { doc ->
                    mapDocumentToSpace(doc)
                }.sortedByDescending { it.id } // Sort locally to avoid index requirements

                trySend(spacesList)

                localDao?.let { dao ->
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            dao.insertSpaces(spacesList.map { SpaceEntity.fromSpace(it) })
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Realtime flow for expenses belonging to a specific space.
     * Includes a default limit of 100 to prevent unbounded document reads.
     */
    fun observeExpenses(spaceId: String, limit: Long = 100): Flow<List<SpaceExpense>> = callbackFlow {
        val listener = firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val expenses = snapshot.documents.mapNotNull { doc ->
                    mapDocumentToExpense(doc)
                }.sortedByDescending { doc ->
                    // Sort descending by timestamp or date
                    doc.dateLabel
                }

                trySend(expenses)

                localDao?.let { dao ->
                    CoroutineScope(Dispatchers.IO).launch {
                        runCatching {
                            dao.insertExpenses(expenses.map { SpaceExpenseEntity.fromSpaceExpense(spaceId, it) })
                        }
                    }
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Creates a new space document in Firestore.
     */
    suspend fun createSpace(
        name: String,
        emoji: String,
        themeIndex: Int,
        type: SpaceType = SpaceType.BILL_SPLIT
    ): String {
        val spaceId = UUID.randomUUID().toString()
        val inviteCode = generateInviteCode()
        val uid = currentUserId
        val userName = currentUserName

        val spaceDoc = mapOf(
            "id" to spaceId,
            "name" to name.trim(),
            "emoji" to emoji,
            "themeIndex" to themeIndex,
            "inviteCode" to inviteCode,
            "createdBy" to uid,
            "type" to type.name,
            "memberIds" to listOf(uid),
            "members" to listOf(
                mapOf(
                    "id" to uid,
                    "name" to userName
                )
            ),
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("spaces")
            .document(spaceId)
            .set(spaceDoc)
            .await()

        return spaceId
    }

    /**
     * Joins an existing space using a 6-character invite code.
     */
    suspend fun joinSpace(code: String): JoinResult {
        val cleaned = code.trim().uppercase()
        val uid = currentUserId
        val userName = currentUserName

        return try {
            val querySnapshot = firestore.collection("spaces")
                .whereEqualTo("inviteCode", cleaned)
                .limit(1)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return JoinResult.NotFound
            }

            val doc = querySnapshot.documents.first()
            val spaceId = doc.id
            @Suppress("UNCHECKED_CAST")
            val memberIds = (doc.get("memberIds") as? List<String>) ?: emptyList()

            if (uid in memberIds) {
                return JoinResult.AlreadyMember
            }

            val newMember = mapOf(
                "id" to uid,
                "name" to userName
            )

            firestore.collection("spaces")
                .document(spaceId)
                .update(
                    "memberIds", FieldValue.arrayUnion(uid),
                    "members", FieldValue.arrayUnion(newMember)
                )
                .await()

            JoinResult.Success(spaceId)
        } catch (e: Exception) {
            JoinResult.NotFound
        }
    }

    /**
     * Adds a friend by name to the space.
     */
    suspend fun addMember(spaceId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        val memberId = UUID.randomUUID().toString()
        val newMember = mapOf(
            "id" to memberId,
            "name" to trimmed
        )

        firestore.collection("spaces")
            .document(spaceId)
            .update(
                "memberIds", FieldValue.arrayUnion(memberId),
                "members", FieldValue.arrayUnion(newMember)
            )
            .await()
    }

    /**
     * Adds an expense to the space subcollection and updates space total.
     */
    /**
     * Adds an expense to the space subcollection.
     * Enforces that the payer is always the authenticated user (cannot add expense for someone else).
     */
    suspend fun addExpense(
        spaceId: String,
        title: String,
        amount: Double,
        paidById: String,
        shares: Map<String, Double>
    ) {
        val amountPaise = com.invatech.pockit.domain.Money.parse(java.math.BigDecimal.valueOf(amount).toPlainString()) ?: error("Enter an amount with at most two decimal places.")
        require(amountPaise in 1..com.invatech.pockit.domain.Money.MAX_AMOUNT && title.isNotBlank())
        val sharesPaise = shares.mapValues { com.invatech.pockit.domain.Money.legacyPaise(it.value) }
        require(sharesPaise.values.all { it >= 0 } && com.invatech.pockit.domain.Money.sum(sharesPaise.values) == amountPaise) { "Shares must add up to the total." }
        val actualPayerId = currentUserId // Enforce current user as payer
        val expenseId = UUID.randomUUID().toString()
        val dateLabel = todayLabel()

        val expenseData = mapOf(
            "id" to expenseId,
            "title" to title.trim(),
            "amountPaise" to amountPaise,
            "sharesPaise" to sharesPaise,
            "amount" to amount,
            "paidById" to actualPayerId,
            "shares" to shares,
            "dateLabel" to dateLabel,
            "isSettlement" to false,
            "isConfirmed" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .document(expenseId)
            .set(expenseData)
            .await()
    }

    /**
     * Records a settlement payment between two members.
     * Initialized with isConfirmed = false until the receiver confirms payment receipt.
     */
    suspend fun settle(spaceId: String, settlement: Settlement) {
        val expenseId = UUID.randomUUID().toString()
        val dateLabel = todayLabel()

        val settlementData = mapOf(
            "id" to expenseId,
            "title" to "Settled up",
            "amountPaise" to com.invatech.pockit.domain.Money.legacyPaise(settlement.amount),
            "sharesPaise" to mapOf(settlement.toId to com.invatech.pockit.domain.Money.legacyPaise(settlement.amount)),
            "amount" to settlement.amount,
            "paidById" to settlement.fromId,
            "shares" to mapOf(settlement.toId to settlement.amount),
            "dateLabel" to dateLabel,
            "isSettlement" to true,
            "isConfirmed" to false, // Requires receiver confirmation
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .document(expenseId)
            .set(settlementData)
            .await()
    }

    /**
     * Confirms that a settlement payment was received. Only the receiver (creditor) can confirm.
     */
    suspend fun confirmSettlement(spaceId: String, expenseId: String): Result<Unit> = runCatching {
        val expenseRef = firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .document(expenseId)

        val doc = expenseRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val shares = doc.get("shares") as? Map<String, Any> ?: emptyMap()
        val receiverId = shares.keys.firstOrNull() ?: ""

        if (receiverId.isNotEmpty() && receiverId != currentUserId) {
            throw IllegalStateException("Only the receiver of this payment can confirm it")
        }

        expenseRef.update("isConfirmed", true).await()
    }

    /**
     * Declines or cancels an unconfirmed settlement payment.
     */
    suspend fun rejectSettlement(spaceId: String, expenseId: String): Result<Unit> = runCatching {
        val expenseRef = firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .document(expenseId)

        val doc = expenseRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val shares = doc.get("shares") as? Map<String, Any> ?: emptyMap()
        val receiverId = shares.keys.firstOrNull() ?: ""
        val paidById = doc.getString("paidById") ?: ""

        if (currentUserId != receiverId && currentUserId != paidById) {
            throw IllegalStateException("Not authorized to decline this payment")
        }

        expenseRef.delete().await()
    }

    /**
     * Deletes an expense document from the space subcollection in Firestore.
     * Only the transaction owner who paid for the expense can delete it.
     */
    suspend fun deleteExpense(spaceId: String, expenseId: String): Result<Unit> = runCatching {
        val expenseRef = firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .document(expenseId)

        val doc = expenseRef.get().await()
        val paidById = doc.getString("paidById") ?: ""

        if (paidById.isNotEmpty() && paidById != currentUserId) {
            throw IllegalStateException("Only the owner of this transaction can delete it")
        }

        expenseRef.delete().await()
    }

    /**
     * Restores a previously deleted expense document back to Firestore.
     */
    suspend fun restoreExpense(spaceId: String, expense: SpaceExpense) {
        val expenseData = mapOf(
            "id" to expense.id,
            "title" to expense.title,
            "amount" to expense.amount,
            "paidById" to expense.paidById,
            "shares" to expense.shares,
            "dateLabel" to expense.dateLabel,
            "isSettlement" to expense.isSettlement,
            "isConfirmed" to expense.isConfirmed,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .document(expense.id)
            .set(expenseData)
            .await()
    }

    /**
     * Renames an existing space in Firestore.
     */
    suspend fun renameSpace(spaceId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        firestore.collection("spaces")
            .document(spaceId)
            .update("name", trimmed)
            .await()
    }

    /**
     * Removes a member from a space. Only the creator of the space can do it.
     */
    suspend fun removeMember(spaceId: String, memberId: String): Result<Unit> = runCatching {
        val spaceRef = firestore.collection("spaces").document(spaceId)
        val doc = spaceRef.get().await()
        val creatorId = doc.getString("createdBy") ?: ""

        if (creatorId.isNotEmpty() && creatorId != currentUserId) {
            throw IllegalStateException("Only the creator of the space can remove members")
        }
        if (memberId == creatorId) {
            throw IllegalArgumentException("Cannot remove the creator of the space")
        }

        @Suppress("UNCHECKED_CAST")
        val currentMembers = (doc.get("members") as? List<Map<String, String>>) ?: emptyList()
        val updatedMembers = currentMembers.filterNot { it["id"] == memberId }

        @Suppress("UNCHECKED_CAST")
        val currentMemberIds = (doc.get("memberIds") as? List<String>) ?: emptyList()
        val updatedMemberIds = currentMemberIds.filterNot { it == memberId }

        spaceRef.update(
            mapOf(
                "members" to updatedMembers,
                "memberIds" to updatedMemberIds
            )
        ).await()
    }

    /**
     * Deletes a space along with all expenses inside its subcollection.
     * Only the creator of the space can do it.
     */
    suspend fun deleteSpace(spaceId: String): Result<Unit> = runCatching {
        val spaceRef = firestore.collection("spaces").document(spaceId)
        val doc = spaceRef.get().await()
        val creatorId = doc.getString("createdBy") ?: ""

        if (creatorId.isNotEmpty() && creatorId != currentUserId) {
            throw IllegalStateException("Only the creator of the space can delete it")
        }

        // 1. Delete all expenses in the subcollection
        val expensesSnapshot = spaceRef.collection("expenses").get().await()
        val chunks = expensesSnapshot.documents.chunked(450)
        for (chunk in chunks) {
            val batch = firestore.batch()
            for (expenseDoc in chunk) {
                batch.delete(expenseDoc.reference)
            }
            batch.commit().await()
        }

        // 2. Delete the space document itself
        spaceRef.delete().await()
    }

    private fun mapDocumentToSpace(doc: com.google.firebase.firestore.DocumentSnapshot): Space? {
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: return null
        val emoji = doc.getString("emoji") ?: "🏠"
        val themeIndex = (doc.getLong("themeIndex") ?: 0L).toInt()
        val inviteCode = doc.getString("inviteCode") ?: ""
        val createdBy = doc.getString("createdBy") ?: ""
        val typeStr = doc.getString("type") ?: SpaceType.BILL_SPLIT.name
        val spaceType = try {
            SpaceType.valueOf(typeStr)
        } catch (e: Exception) {
            SpaceType.BILL_SPLIT
        }

        @Suppress("UNCHECKED_CAST")
        val membersRaw = doc.get("members") as? List<Map<String, String>> ?: emptyList()
        val members = membersRaw.mapNotNull { m ->
            val mId = m["id"] ?: return@mapNotNull null
            val mName = m["name"] ?: return@mapNotNull null
            Member(mId, mName)
        }

        return Space(
            id = id,
            name = name,
            emoji = emoji,
            themeIndex = themeIndex,
            inviteCode = inviteCode,
            members = members,
            expenses = emptyList(),
            createdBy = createdBy,
            type = spaceType
        )
    }

    private fun mapDocumentToExpense(doc: com.google.firebase.firestore.DocumentSnapshot): SpaceExpense? {
        val id = doc.getString("id") ?: doc.id
        val title = doc.getString("title") ?: return null
        val amount = doc.getLong("amountPaise")?.let { it / 100.0 } ?: doc.getDouble("amount") ?: (doc.getLong("amount")?.toDouble() ?: return null)
        val paidById = doc.getString("paidById") ?: ""

        @Suppress("UNCHECKED_CAST")
        val minorUnits = doc.contains("sharesPaise")
        val sharesRaw = doc.get(if (minorUnits) "sharesPaise" else "shares") as? Map<String, Any> ?: emptyMap()
        val shares = sharesRaw.mapNotNull { (key, value) ->
            val shareVal = when (value) {
                is Double -> value
                is Long -> value.toDouble()
                is Number -> value.toDouble()
                else -> null
            }
            if (shareVal != null) key to (if (minorUnits) shareVal / 100.0 else shareVal) else null
        }.toMap()

        val dateLabel = doc.getString("dateLabel") ?: "Today"
        val isSettlement = doc.getBoolean("isSettlement") ?: false
        val isConfirmed = doc.getBoolean("isConfirmed") ?: true

        return SpaceExpense(
            id = id,
            title = title,
            amount = amount,
            paidById = paidById,
            shares = shares,
            dateLabel = dateLabel,
            isSettlement = isSettlement,
            isConfirmed = isConfirmed
        )
    }

    private fun todayLabel(): String =
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date())

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet.random() }.joinToString("")
    }
}
