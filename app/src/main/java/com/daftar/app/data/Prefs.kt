package com.daftar.app.data

import android.content.Context
import android.util.Base64
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
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

    // ---- app lock (optional)
    // Only a salted, slow hash of the PIN is kept, never the PIN itself.

    val hasPin: Boolean get() = store.contains(KEY_PIN_HASH)

    /** Number of digits in the PIN, so the lock screen can check it as soon as it is complete. */
    val pinLength: Int get() = store.getInt(KEY_PIN_LENGTH, 0)

    var biometricUnlock: Boolean
        get() = store.getBoolean(KEY_BIOMETRIC, false)
        set(value) = store.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /** Show figures on the home-screen widget even while the app lock is on. */
    var widgetShowFigures: Boolean
        get() = store.getBoolean(KEY_WIDGET_FIGURES, false)
        set(value) = store.edit().putBoolean(KEY_WIDGET_FIGURES, value).apply()

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        store.edit()
            .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_PIN_HASH, Base64.encodeToString(hash(pin, salt), Base64.NO_WRAP))
            .putInt(KEY_PIN_LENGTH, pin.length)
            .apply()
    }

    /** Slow on purpose (tens of milliseconds); call it off the main thread. */
    fun checkPin(pin: String): Boolean {
        val salt = Base64.decode(store.getString(KEY_PIN_SALT, "") ?: "", Base64.NO_WRAP)
        val expected = Base64.decode(store.getString(KEY_PIN_HASH, "") ?: "", Base64.NO_WRAP)
        return expected.isNotEmpty() && MessageDigest.isEqual(expected, hash(pin, salt))
    }

    fun clearPin() {
        store.edit().remove(KEY_PIN_SALT).remove(KEY_PIN_HASH).remove(KEY_PIN_LENGTH).remove(KEY_BIOMETRIC).apply()
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 60_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_BIOMETRIC = "biometric_unlock"
        private const val KEY_WIDGET_FIGURES = "widget_show_figures"
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
