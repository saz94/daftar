package com.daftar.app.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small settings that are not business data: which business is open, and the theme. */
class Prefs(context: Context) {
    private val store = context.applicationContext.getSharedPreferences("daftar", Context.MODE_PRIVATE)

    private val _currentBusinessId = MutableStateFlow(store.getLong(KEY_BUSINESS, 0L))
    val currentBusinessId: StateFlow<Long> = _currentBusinessId

    fun setCurrentBusiness(id: Long) {
        store.edit().putLong(KEY_BUSINESS, id).apply()
        _currentBusinessId.value = id
    }

    /** "system", "light" or "dark". */
    var themeMode: String
        get() = store.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) {
            store.edit().putString(KEY_THEME, value).apply()
            applyTheme(value)
        }

    companion object {
        private const val KEY_BUSINESS = "current_business"
        private const val KEY_THEME = "theme"
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"

        fun applyTheme(mode: String) {
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                },
            )
        }

        /** "" (follow the phone), "en" or "ar". */
        fun currentLanguage(): String {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return when {
                tags.startsWith("ar") -> "ar"
                tags.startsWith("en") -> "en"
                else -> ""
            }
        }

        fun setLanguage(tag: String) {
            AppCompatDelegate.setApplicationLocales(
                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
            )
        }
    }
}
