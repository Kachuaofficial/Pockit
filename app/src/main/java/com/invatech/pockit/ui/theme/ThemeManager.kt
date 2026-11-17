package com.invatech.pockit.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "pockit_theme_preferences"
    private const val KEY_THEME_MODE = "pockit_theme_mode"
    private const val KEY_PALETTE = "pockit_theme_palette"

    private lateinit var prefs: SharedPreferences

    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _palette = MutableStateFlow(PockitPalette.Default)
    val palette: StateFlow<PockitPalette> = _palette.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedMode = prefs.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        _themeMode.value = runCatching { ThemeMode.valueOf(savedMode) }.getOrDefault(ThemeMode.LIGHT)

        _palette.value = PockitPalette.fromId(prefs.getString(KEY_PALETTE, null))
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        }
    }

    fun setPalette(palette: PockitPalette) {
        _palette.value = palette
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_PALETTE, palette.id).apply()
        }
    }
}
