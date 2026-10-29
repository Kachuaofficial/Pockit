package com.invatech.pockit.ui.space.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.auth.FirebaseAuth
import com.invatech.pockit.ui.theme.Lavender
import com.invatech.pockit.ui.theme.Mint
import com.invatech.pockit.ui.theme.Peach
import com.invatech.pockit.ui.theme.Rose

/**
 * Returns the currently signed-in user's Firebase UID, or fallback for demo/offline.
 */
val CurrentUserId: String
    get() = FirebaseAuth.getInstance().currentUser?.uid ?: "me"

val SpaceAccents: List<Color> = listOf(Mint, Lavender, Peach, Rose)
val SpaceEmojis = listOf("🏠", "✈️", "🍕", "🎉", "🏖️", "🛒", "🎬", "💼")

/** Green used for "you get back" text on light surfaces. */
val OwedColor = Color(0xFF2E9B82)

data class Member(
    val id: String,
    val name: String
) {
    val isYou: Boolean get() = id == CurrentUserId
    val displayName: String get() = if (isYou) "You" else name
    val sentenceName: String get() = if (isYou) "you" else name
}

data class SpaceExpense(
    val id: String,
    val title: String,
    val amount: Double,
    val paidById: String,
    /** memberId -> amount that member owes for this expense. */
    val shares: Map<String, Double>,
    val dateLabel: String,
    val isSettlement: Boolean = false,
    val isConfirmed: Boolean = true
)

data class Space(
    val id: String,
    val name: String,
    val emoji: String,
    val themeIndex: Int,
    val inviteCode: String,
    val members: List<Member>,
    /** Newest first. */
    val expenses: List<SpaceExpense> = emptyList(),
    val createdBy: String = ""
) {
    val accent: Color get() = SpaceAccents[themeIndex.mod(SpaceAccents.size)]
    val totalSpent: Double get() = expenses.filter { !it.isSettlement && it.isConfirmed }.sumOf { it.amount }
    val isCreator: Boolean
        get() = (createdBy.isNotEmpty() && createdBy == CurrentUserId) ||
                (createdBy.isEmpty() && members.firstOrNull()?.isYou == true)
}

enum class SplitMode { Equal, Custom }

sealed interface JoinResult {
    data class Success(val spaceId: String) : JoinResult
    object NotFound : JoinResult
    object AlreadyMember : JoinResult
}
