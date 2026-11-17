package com.invatech.pockit.ui.common.mascot

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.invatech.pockit.data.rewards.UserRewardManager
import com.invatech.pockit.ui.characters.MascotMood
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MascotAccessory(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val price: Int,
    val description: String
) {
    NONE("avatar_default", "Classic", "🐾", 0, "Natural, cozy cat vibe"),
    ROYAL_CROWN("acc_crown", "Royal Crown", "👑", 120, "For mindful savings royalty"),
    COOL_SHADES("acc_shades", "Retro Shades", "🕶️", 100, "Too cool to overspend"),
    CHEF_HAT("acc_chef", "Chef Toque", "👨‍🍳", 80, "Cooking up great budgets"),
    CHERRY_BLOSSOM("acc_sakura", "Sakura Pin", "🌸", 60, "Calm floral serenity"),
    RUBY_BOWTIE("acc_bowtie", "Ruby Bowtie", "🎀", 70, "Dapper gentleman cat"),
    PARTY_CONE("acc_party", "Party Cone", "🥳", 90, "Celebrate financial milestones");

    companion object {
        fun fromId(id: String?): MascotAccessory =
            entries.find { it.id == id } ?: NONE
    }
}

enum class MascotFur(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val price: Int,
    val baseColor: Color,
    val accentColor: Color,
    val isDarkCoat: Boolean = false
) {
    SNOW_WHITE("fur_snow", "Snow White", "⚪", 0, Color(0xFFFFFDF9), Color(0xFFFFB2B2), false),
    GINGER_TABBY("fur_ginger", "Ginger Tabby", "🐱", 90, Color(0xFFFFE0B2), Color(0xFFF57C00), false),
    MIDNIGHT_LUCKY("fur_midnight", "Lucky Midnight", "🐈‍⬛", 110, Color(0xFF2B2523), Color(0xFFFFD54F), true),
    CALICO_PASTEL("fur_calico", "Pastel Calico", "🐾", 100, Color(0xFFFFF8E7), Color(0xFFFFAB91), false);

    companion object {
        fun fromId(id: String?): MascotFur =
            entries.find { it.id == id } ?: SNOW_WHITE
    }
}

/**
 * Manages virtual pet customization, unlocked accessories, fur colors, and interactive speech moods.
 */
object MascotCustomizationManager {
    private const val PREFS_NAME = "pockit_mascot_prefs"
    private const val KEY_EQUIPPED_ACC = "mascot_equipped_acc"
    private const val KEY_EQUIPPED_FUR = "mascot_equipped_fur"
    private const val KEY_EQUIPPED_MOOD = "mascot_equipped_mood"

    private lateinit var prefs: SharedPreferences

    private val _equippedAccessory = MutableStateFlow(MascotAccessory.NONE)
    val equippedAccessory: StateFlow<MascotAccessory> = _equippedAccessory.asStateFlow()

    private val _equippedFur = MutableStateFlow(MascotFur.SNOW_WHITE)
    val equippedFur: StateFlow<MascotFur> = _equippedFur.asStateFlow()

    private val _equippedMood = MutableStateFlow(MascotMood.Happy)
    val equippedMood: StateFlow<MascotMood> = _equippedMood.asStateFlow()

    val cuteSpeechQuotes = listOf(
        "Purr~ You're doing amazing with your wallet today! 🐾",
        "Every rupee saved is a step toward freedom! 🌱",
        "Thanks for the headpats! Give me more! 💖",
        "Staying mindful of expenses makes wealth grow! 🌟",
        "Meow~ Remember to log that chai or coffee! ☕",
        "Proud of your financial streak! Keep it going! 🔥",
        "You're the most responsible pet parent ever! 👑",
        "A penny saved is another treat for me! 🐟"
    )

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accId = prefs.getString(KEY_EQUIPPED_ACC, MascotAccessory.NONE.id)
        val furId = prefs.getString(KEY_EQUIPPED_FUR, MascotFur.SNOW_WHITE.id)
        val moodName = prefs.getString(KEY_EQUIPPED_MOOD, MascotMood.Happy.name)

        _equippedAccessory.value = MascotAccessory.fromId(accId)
        _equippedFur.value = MascotFur.fromId(furId)
        _equippedMood.value = runCatching { MascotMood.valueOf(moodName ?: MascotMood.Happy.name) }.getOrDefault(MascotMood.Happy)
    }

    fun equipMood(mood: MascotMood) {
        _equippedMood.value = mood
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_EQUIPPED_MOOD, mood.name).apply()
        }
    }

    fun isAccessoryUnlocked(accessory: MascotAccessory): Boolean {
        if (accessory == MascotAccessory.NONE) return true
        return UserRewardManager.isItemUnlocked(accessory.id)
    }

    fun unlockAccessory(accessory: MascotAccessory): Boolean {
        if (isAccessoryUnlocked(accessory)) return true
        val success = UserRewardManager.unlockItem(accessory.id, accessory.price)
        if (success) {
            equipAccessory(accessory)
        }
        return success
    }

    fun equipAccessory(accessory: MascotAccessory) {
        _equippedAccessory.value = accessory
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_EQUIPPED_ACC, accessory.id).apply()
        }
        UserRewardManager.equipAvatar(accessory.id)
        runCatching {
            com.invatech.pockit.glance.PockitWidgetManager.updateWidget(
                com.invatech.pockit.PockitApplication.instance
            )
        }
    }

    fun isFurUnlocked(fur: MascotFur): Boolean {
        if (fur == MascotFur.SNOW_WHITE) return true
        return UserRewardManager.isItemUnlocked(fur.id)
    }

    fun unlockFur(fur: MascotFur): Boolean {
        if (isFurUnlocked(fur)) return true
        val success = UserRewardManager.unlockItem(fur.id, fur.price)
        if (success) {
            equipFur(fur)
        }
        return success
    }

    fun equipFur(fur: MascotFur) {
        _equippedFur.value = fur
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_EQUIPPED_FUR, fur.id).apply()
        }
    }
}
