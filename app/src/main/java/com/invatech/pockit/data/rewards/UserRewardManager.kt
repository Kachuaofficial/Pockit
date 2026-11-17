package com.invatech.pockit.data.rewards

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Manages user rewards, including:
 * 1. Ad-Free mode granted via watching Rewarded Ads (e.g. 24 hours of zero ads across all screens).
 * 2. Daily Coin Earning with consecutive streak rewards.
 * 3. Bonus Coin accumulation for future in-app purchases, cute themes, and avatar cosmetics.
 * 4. Automatic Cloud Synchronization with Firebase Firestore for authenticated users.
 */
object UserRewardManager {

    private const val PREFS_NAME = "pockit_rewards_prefs"
    private const val KEY_AD_FREE_UNTIL = "ad_free_until"
    private const val KEY_TOTAL_COINS = "total_coins"
    private const val KEY_DAILY_STREAK = "daily_streak"
    private const val KEY_LAST_CLAIM_DATE = "last_claim_date"
    private const val KEY_UNLOCKED_ITEMS = "unlocked_shop_items"
    private const val KEY_REDEEMED_CODES = "redeemed_promo_codes"
    private const val KEY_EQUIPPED_AVATAR = "equipped_avatar"

    private const val AD_FREE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 Hours

    private lateinit var prefs: SharedPreferences
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _adFreeFlow = MutableStateFlow(false)
    val adFreeFlow: StateFlow<Boolean> = _adFreeFlow.asStateFlow()

    private val _coinsFlow = MutableStateFlow(0)
    val coinsFlow: StateFlow<Int> = _coinsFlow.asStateFlow()

    private val _streakFlow = MutableStateFlow(0)
    val streakFlow: StateFlow<Int> = _streakFlow.asStateFlow()

    private val _unlockedItemsFlow = MutableStateFlow<Set<String>>(setOf("theme_pockit", "avatar_default"))
    val unlockedItemsFlow: StateFlow<Set<String>> = _unlockedItemsFlow.asStateFlow()

    private val _equippedAvatarFlow = MutableStateFlow("avatar_default")
    val equippedAvatarFlow: StateFlow<String> = _equippedAvatarFlow.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        refreshState()

        // Automatically sync rewards with Firebase when app launches or user signs in
        runCatching {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                if (auth.currentUser != null) {
                    syncWithFirebase()
                }
            }
        }
    }

    /**
     * Refreshes internal state and notifies StateFlow collectors.
     */
    fun refreshState() {
        if (!::prefs.isInitialized) return

        _adFreeFlow.value = false

        _coinsFlow.value = prefs.getInt(KEY_TOTAL_COINS, 0)
        _streakFlow.value = prefs.getInt(KEY_DAILY_STREAK, 0)

        val savedUnlocked = prefs.getStringSet(KEY_UNLOCKED_ITEMS, null) ?: setOf("theme_pockit", "avatar_default")
        _unlockedItemsFlow.value = savedUnlocked + setOf("theme_pockit", "avatar_default")

        _equippedAvatarFlow.value = prefs.getString(KEY_EQUIPPED_AVATAR, "avatar_default") ?: "avatar_default"
    }

    fun equipAvatar(avatarId: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_EQUIPPED_AVATAR, avatarId).apply()
        _equippedAvatarFlow.value = avatarId
        pushLocalStateToFirebase()
    }

    fun getEquippedAvatar(): String {
        if (!::prefs.isInitialized) return "avatar_default"
        return _equippedAvatarFlow.value
    }

    // ==========================================
    // AD-FREE PASS SYSTEM (DISABLED - ALL DEVICES RECEIVE ADS)
    // ==========================================

    fun isAdFreeActive(): Boolean {
        _adFreeFlow.value = false
        return false
    }

    fun getRemainingAdFreeHours(): Int = 0

    fun grantAdFreePass(hours: Int = 24) {
        _adFreeFlow.value = false
    }

    fun clearAdFreePass() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove(KEY_AD_FREE_UNTIL).apply()
        _adFreeFlow.value = false
        pushLocalStateToFirebase()
    }

    // ==========================================
    // DAILY COIN EARNING & STREAKS
    // ==========================================

    fun getTodayDateString(): String = dateFormat.format(Date())

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    /**
     * Checks if the user is eligible to claim today's daily reward.
     */
    fun canClaimDailyReward(): Boolean {
        if (!::prefs.isInitialized) return false
        val lastClaim = prefs.getString(KEY_LAST_CLAIM_DATE, null)
        return lastClaim != getTodayDateString()
    }

    /**
     * Calculates the coin reward amount based on consecutive day streak.
     * Day 1: 20 coins
     * Day 2: 30 coins
     * Day 3: 40 coins
     * Day 4: 50 coins
     * Day 5: 60 coins
     * Day 6: 80 coins
     * Day 7+: 100 coins
     */
    fun getRewardForDay(dayNumber: Int): Int {
        return when (dayNumber) {
            1 -> 20
            2 -> 30
            3 -> 40
            4 -> 50
            5 -> 60
            6 -> 80
            else -> 100
        }
    }

    /**
     * Next reward amount user will receive when they claim today.
     */
    fun getNextDailyRewardAmount(): Int {
        val lastClaim = prefs.getString(KEY_LAST_CLAIM_DATE, null)
        val yesterday = getYesterdayDateString()
        val currentStreak = _streakFlow.value

        val nextStreak = if (lastClaim == yesterday) {
            ((currentStreak % 7) + 1)
        } else {
            1
        }
        return getRewardForDay(nextStreak)
    }

    /**
     * Claims today's daily reward.
     * Returns a Pair of (coinsEarned, newStreak). Returns Pair(0, currentStreak) if already claimed.
     */
    fun claimDailyReward(): Pair<Int, Int> {
        if (!::prefs.isInitialized || !canClaimDailyReward()) {
            return Pair(0, _streakFlow.value)
        }

        val today = getTodayDateString()
        val yesterday = getYesterdayDateString()
        val lastClaim = prefs.getString(KEY_LAST_CLAIM_DATE, null)

        val newStreak = if (lastClaim == yesterday) {
            _streakFlow.value + 1
        } else {
            1
        }

        val dayIndexInCycle = ((newStreak - 1) % 7) + 1
        val earnedCoins = getRewardForDay(dayIndexInCycle)
        val updatedCoins = _coinsFlow.value + earnedCoins

        prefs.edit()
            .putInt(KEY_TOTAL_COINS, updatedCoins)
            .putInt(KEY_DAILY_STREAK, newStreak)
            .putString(KEY_LAST_CLAIM_DATE, today)
            .apply()

        _coinsFlow.value = updatedCoins
        _streakFlow.value = newStreak

        pushLocalStateToFirebase()

        return Pair(earnedCoins, newStreak)
    }

    /**
     * Adds bonus coins directly (e.g. earned from watching a rewarded ad, completing transactions).
     */
    fun addCoins(amount: Int) {
        if (!::prefs.isInitialized || amount <= 0) return
        val updatedCoins = _coinsFlow.value + amount
        prefs.edit().putInt(KEY_TOTAL_COINS, updatedCoins).apply()
        _coinsFlow.value = updatedCoins
        pushLocalStateToFirebase()
    }

    /**
     * Deducts coins if sufficient balance exists.
     */
    fun spendCoins(amount: Int): Boolean {
        if (!::prefs.isInitialized || amount <= 0) return false
        val current = _coinsFlow.value
        if (current < amount) return false
        val updated = current - amount
        prefs.edit().putInt(KEY_TOTAL_COINS, updated).apply()
        _coinsFlow.value = updated
        pushLocalStateToFirebase()
        return true
    }

    fun isItemUnlocked(itemId: String): Boolean {
        if (itemId == "theme_pockit" || itemId == "avatar_default") return true
        return _unlockedItemsFlow.value.contains(itemId)
    }

    fun unlockItem(itemId: String, cost: Int): Boolean {
        if (!::prefs.isInitialized) return false
        if (isItemUnlocked(itemId)) return true
        if (!spendCoins(cost)) return false

        val newSet = _unlockedItemsFlow.value + itemId
        prefs.edit().putStringSet(KEY_UNLOCKED_ITEMS, newSet).apply()
        _unlockedItemsFlow.value = newSet
        pushLocalStateToFirebase()
        return true
    }

    /**
     * Redeems a gift or promo code.
     */
    fun redeemPromoCode(inputCode: String): RedeemResult {
        if (!::prefs.isInitialized) return RedeemResult.Error("Service not ready")
        val cleanCode = inputCode.trim().uppercase(Locale.US)
        if (cleanCode.isBlank()) {
            return RedeemResult.Error("Please enter a gift or promo code")
        }

        val redeemed = prefs.getStringSet(KEY_REDEEMED_CODES, emptySet()) ?: emptySet()
        if (redeemed.contains(cleanCode)) {
            return RedeemResult.Error("Code '$cleanCode' has already been redeemed! 🎉")
        }

        val result = when (cleanCode) {
            "POCKIT2026" -> RedeemResult.Success("🎉 Welcome to Pockit 2026! +250 Coins added to your bank.", coinsAdded = 250)
            "CUTEKITTY" -> RedeemResult.Success("🐾 Purr-fect bonus! +150 Coins from Pockit Cat.", coinsAdded = 150)
            "FRESHSTART" -> RedeemResult.Success("🌱 Budget booster! +100 Coins added.", coinsAdded = 100)
            "ADFREE24" -> {
                addCoins(100)
                RedeemResult.Success("✨ Bonus code redeemed! +100 Coins added.", coinsAdded = 100)
            }
            "PROFIT" -> RedeemResult.Success("💎 Financial Master! +500 Coins added to your vault.", coinsAdded = 500)
            "DISCOUNT50" -> RedeemResult.Success("🎁 Surprise gift! +75 Coins added.", coinsAdded = 75)
            else -> return RedeemResult.Error("Invalid or expired promo code. Check spelling!")
        }

        if (result is RedeemResult.Success) {
            if (result.coinsAdded > 0) {
                addCoins(result.coinsAdded)
            }
            val newRedeemed = redeemed + cleanCode
            prefs.edit().putStringSet(KEY_REDEEMED_CODES, newRedeemed).apply()
            pushLocalStateToFirebase()
        }
        return result
    }

    // ==========================================
    // FIREBASE FIRESTORE CLOUD SYNCHRONIZATION
    // ==========================================

    /**
     * Performs a bidirectional sync between local SharedPreferences and Firebase Firestore.
     * Takes the higher coin balance, combines unlocked items, and ensures no progress is lost.
     */
    fun syncWithFirebase() {
        coroutineScope.launch {
            val uid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() ?: return@launch
            try {
                val firestore = FirebaseFirestore.getInstance()
                val userDocRef = firestore.collection("users").document(uid)
                val snapshot = userDocRef.get().await()

                if (snapshot.exists() && snapshot.contains("totalCoins")) {
                    val cloudCoins = snapshot.getLong("totalCoins")?.toInt() ?: 0
                    val cloudStreak = snapshot.getLong("dailyStreak")?.toInt() ?: 0
                    val cloudLastClaim = snapshot.getString("lastClaimDate")
                    val cloudUnlockedList = (snapshot.get("unlockedItems") as? List<*>)
                        ?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()
                    val cloudAvatar = snapshot.getString("equippedAvatar") ?: "avatar_default"
                    val cloudAdFreeUntil = snapshot.getLong("adFreeUntil") ?: 0L
                    val cloudRedeemed = (snapshot.get("redeemedPromoCodes") as? List<*>)
                        ?.mapNotNull { it?.toString() }?.toSet() ?: emptySet()

                    val localCoins = prefs.getInt(KEY_TOTAL_COINS, 0)
                    val localStreak = prefs.getInt(KEY_DAILY_STREAK, 0)
                    val localLastClaim = prefs.getString(KEY_LAST_CLAIM_DATE, null)
                    val localUnlocked = prefs.getStringSet(KEY_UNLOCKED_ITEMS, null)
                        ?: setOf("theme_pockit", "avatar_default")
                    val localAvatar = prefs.getString(KEY_EQUIPPED_AVATAR, "avatar_default") ?: "avatar_default"
                    val localAdFreeUntil = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
                    val localRedeemed = prefs.getStringSet(KEY_REDEEMED_CODES, emptySet()) ?: emptySet()

                    // Merge strategy (prioritize user earnings & highest progress)
                    val mergedCoins = maxOf(localCoins, cloudCoins)
                    val mergedStreak = maxOf(localStreak, cloudStreak)
                    val mergedLastClaim = when {
                        localLastClaim == getTodayDateString() || cloudLastClaim == getTodayDateString() -> getTodayDateString()
                        cloudLastClaim != null -> cloudLastClaim
                        else -> localLastClaim
                    }
                    val mergedUnlocked = localUnlocked + cloudUnlockedList + setOf("theme_pockit", "avatar_default")
                    val mergedAvatar = if (cloudAvatar != "avatar_default") cloudAvatar else localAvatar
                    val mergedAdFreeUntil = maxOf(localAdFreeUntil, cloudAdFreeUntil)
                    val mergedRedeemed = localRedeemed + cloudRedeemed

                    prefs.edit()
                        .putInt(KEY_TOTAL_COINS, mergedCoins)
                        .putInt(KEY_DAILY_STREAK, mergedStreak)
                        .putString(KEY_LAST_CLAIM_DATE, mergedLastClaim)
                        .putStringSet(KEY_UNLOCKED_ITEMS, mergedUnlocked)
                        .putString(KEY_EQUIPPED_AVATAR, mergedAvatar)
                        .putLong(KEY_AD_FREE_UNTIL, mergedAdFreeUntil)
                        .putStringSet(KEY_REDEEMED_CODES, mergedRedeemed)
                        .apply()

                    refreshState()

                    // Update cloud with merged values
                    val updates = mapOf(
                        "totalCoins" to mergedCoins,
                        "dailyStreak" to mergedStreak,
                        "lastClaimDate" to (mergedLastClaim ?: ""),
                        "unlockedItems" to mergedUnlocked.toList(),
                        "equippedAvatar" to mergedAvatar,
                        "adFreeUntil" to mergedAdFreeUntil,
                        "redeemedPromoCodes" to mergedRedeemed.toList()
                    )
                    userDocRef.set(updates, SetOptions.merge()).await()
                } else {
                    // Upload current local state to newly created/fresh user profile
                    pushLocalStateToFirebase(uid)
                }
            } catch (_: Exception) {
                // Offline or network error: local cache remains intact
            }
        }
    }

    /**
     * Asynchronously pushes current local reward state to Firebase Firestore.
     */
    private fun pushLocalStateToFirebase(explicitUid: String? = null) {
        if (!::prefs.isInitialized) return
        coroutineScope.launch {
            val uid = explicitUid ?: runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() ?: return@launch
            try {
                val firestore = FirebaseFirestore.getInstance()
                val userDocRef = firestore.collection("users").document(uid)
                val unlockedList = (prefs.getStringSet(KEY_UNLOCKED_ITEMS, null)
                    ?: setOf("theme_pockit", "avatar_default")).toList()
                val redeemedList = (prefs.getStringSet(KEY_REDEEMED_CODES, emptySet()) ?: emptySet()).toList()

                val updates = mapOf(
                    "totalCoins" to prefs.getInt(KEY_TOTAL_COINS, 0),
                    "dailyStreak" to prefs.getInt(KEY_DAILY_STREAK, 0),
                    "lastClaimDate" to (prefs.getString(KEY_LAST_CLAIM_DATE, "") ?: ""),
                    "unlockedItems" to unlockedList,
                    "equippedAvatar" to (prefs.getString(KEY_EQUIPPED_AVATAR, "avatar_default") ?: "avatar_default"),
                    "adFreeUntil" to 0L,
                    "redeemedPromoCodes" to redeemedList
                )
                userDocRef.set(updates, SetOptions.merge()).await()
            } catch (_: Exception) {
                // Offline or network error: will sync on next network availability
            }
        }
    }
}

sealed class RedeemResult {
    data class Success(val message: String, val coinsAdded: Int = 0, val adFreeHours: Int = 0) : RedeemResult()
    data class Error(val message: String) : RedeemResult()
}
