package com.invatech.pockit.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.invatech.pockit.ui.space.model.CurrentUserId
import com.invatech.pockit.ui.space.model.JoinResult
import com.invatech.pockit.ui.space.model.Member
import com.invatech.pockit.ui.space.model.Space
import com.invatech.pockit.ui.space.model.SpaceExpense
import com.invatech.pockit.ui.space.util.Settlement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SpaceRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
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
            }

        awaitClose { listener.remove() }
    }

    /**
     * Realtime flow for expenses belonging to a specific space.
     */
    fun observeExpenses(spaceId: String): Flow<List<SpaceExpense>> = callbackFlow {
        val listener = firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
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
            }

        awaitClose { listener.remove() }
    }

    /**
     * Creates a new space document in Firestore.
     */
    suspend fun createSpace(name: String, emoji: String, themeIndex: Int): String {
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
    suspend fun addExpense(
        spaceId: String,
        title: String,
        amount: Double,
        paidById: String,
        shares: Map<String, Double>
    ) {
        val expenseId = UUID.randomUUID().toString()
        val dateLabel = todayLabel()

        val expenseData = mapOf(
            "id" to expenseId,
            "title" to title.trim(),
            "amount" to amount,
            "paidById" to paidById,
            "shares" to shares,
            "dateLabel" to dateLabel,
            "isSettlement" to false,
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
     */
    suspend fun settle(spaceId: String, settlement: Settlement) {
        val expenseId = UUID.randomUUID().toString()
        val dateLabel = todayLabel()

        val settlementData = mapOf(
            "id" to expenseId,
            "title" to "Settled up",
            "amount" to settlement.amount,
            "paidById" to settlement.fromId,
            "shares" to mapOf(settlement.toId to settlement.amount),
            "dateLabel" to dateLabel,
            "isSettlement" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("spaces")
            .document(spaceId)
            .collection("expenses")
            .document(expenseId)
            .set(settlementData)
            .await()
    }

    private fun mapDocumentToSpace(doc: com.google.firebase.firestore.DocumentSnapshot): Space? {
        val id = doc.getString("id") ?: doc.id
        val name = doc.getString("name") ?: return null
        val emoji = doc.getString("emoji") ?: "🏠"
        val themeIndex = (doc.getLong("themeIndex") ?: 0L).toInt()
        val inviteCode = doc.getString("inviteCode") ?: ""

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
            expenses = emptyList() // Will be combined with real-time expenses
        )
    }

    private fun mapDocumentToExpense(doc: com.google.firebase.firestore.DocumentSnapshot): SpaceExpense? {
        val id = doc.getString("id") ?: doc.id
        val title = doc.getString("title") ?: return null
        val amount = doc.getDouble("amount") ?: (doc.getLong("amount")?.toDouble() ?: return null)
        val paidById = doc.getString("paidById") ?: ""

        @Suppress("UNCHECKED_CAST")
        val sharesRaw = doc.get("shares") as? Map<String, Any> ?: emptyMap()
        val shares = sharesRaw.mapNotNull { (key, value) ->
            val shareVal = when (value) {
                is Double -> value
                is Long -> value.toDouble()
                is Number -> value.toDouble()
                else -> null
            }
            if (shareVal != null) key to shareVal else null
        }.toMap()

        val dateLabel = doc.getString("dateLabel") ?: "Today"
        val isSettlement = doc.getBoolean("isSettlement") ?: false

        return SpaceExpense(
            id = id,
            title = title,
            amount = amount,
            paidById = paidById,
            shares = shares,
            dateLabel = dateLabel,
            isSettlement = isSettlement
        )
    }

    private fun todayLabel(): String =
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date())

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet.random() }.joinToString("")
    }
}
