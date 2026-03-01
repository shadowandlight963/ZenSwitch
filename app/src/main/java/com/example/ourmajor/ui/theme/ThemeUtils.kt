package com.example.ourmajor.ui.theme

import android.app.Activity
import android.content.Context
import com.example.ourmajor.R

object ThemeUtils {

    enum class ZenTheme(
        val id: String,
        val styleRes: Int,
        val backgroundImageRes: Int
    ) {
        SAGE_FOREST(
            id = THEME_SAGE,
            styleRes = R.style.Theme_ZenSwitch_Sage,
            backgroundImageRes = R.drawable.bg_sage_forest
        ),
        OCEAN_BREEZE(
            id = THEME_OCEAN,
            styleRes = R.style.Theme_ZenSwitch_Ocean,
            backgroundImageRes = R.drawable.bg_ocean_breeze
        ),
        SUNSET_SERENITY(
            id = THEME_SUNSET,
            styleRes = R.style.Theme_ZenSwitch_Sunset,
            backgroundImageRes = R.drawable.bg_sunset_serenity
        ),
        MINIMALIST_MIST(
            id = THEME_MINIMAL,
            styleRes = R.style.Theme_ZenSwitch_Minimal,
            backgroundImageRes = R.drawable.bg_minimalist_mist
        );

        companion object {
            fun fromId(id: String): ZenTheme {
                return entries.firstOrNull { it.id == id } ?: SAGE_FOREST
            }
        }
    }

    private const val PREFS = "theme_prefs"
    private const val KEY_THEME_ID = "theme_id"

    const val THEME_SAGE = "sage"
    const val THEME_OCEAN = "ocean"
    const val THEME_SUNSET = "sunset"
    const val THEME_MINIMAL = "minimal"

    fun saveTheme(context: Context, themeId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_ID, themeId)
            .apply()
    }

    fun saveThemeIfChanged(context: Context, themeId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_THEME_ID, THEME_SAGE) ?: THEME_SAGE
        if (current == themeId) return false
        prefs.edit().putString(KEY_THEME_ID, themeId).apply()
        return true
    }

    fun getSavedTheme(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME_ID, THEME_SAGE)
            ?: THEME_SAGE
    }

    fun currentTheme(context: Context): ZenTheme {
        return ZenTheme.fromId(getSavedTheme(context))
    }

    fun applyTheme(activity: Activity) {
        activity.setTheme(currentTheme(activity).styleRes)
    }
}
