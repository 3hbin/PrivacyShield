package com.privacyshield.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class SeedColor { TEAL, INDIGO, FOREST }

data class IndicatorSettings(
    val enabled: Boolean = false,
    val showCamera: Boolean = true,
    val showMic: Boolean = true,
    val showLocation: Boolean = true,
    val sizeDp: Float = 14f,
    val opacity: Float = 0.92f,
    val cameraX: Float = 0.62f,
    val cameraY: Float = 0.04f,
    val micX: Float = 0.74f,
    val micY: Float = 0.04f,
    val locX: Float = 0.86f,
    val locY: Float = 0.04f
)

data class ShieldSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val seedColor: SeedColor = SeedColor.TEAL,
    val notificationsEnabled: Boolean = true,
    val biometricEnabled: Boolean = false,
    val autoLockSeconds: Int = 0,
    val pinLength: Int = 4,
    val hasPin: Boolean = false,
    val githubOwner: String = "",
    val githubRepo: String = "",
    val indicators: IndicatorSettings = IndicatorSettings()
)

class ShieldPreferences(context: Context) {
    private val appContext = context.applicationContext
    private val settingsPrefs = appContext.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
    private val securePrefs = openSecurePrefs(appContext)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<ShieldSettings> = _settings.asStateFlow()

    private val _lockedPackages = MutableStateFlow(readLocked())
    val lockedPackages: StateFlow<Set<String>> = _lockedPackages.asStateFlow()

    fun updateSettings(transform: (ShieldSettings) -> ShieldSettings) {
        val next = transform(_settings.value)
        settingsPrefs.edit()
            .putString(KEY_THEME, next.themeMode.name)
            .putString(KEY_SEED, next.seedColor.name)
            .putBoolean(KEY_NOTIFICATIONS, next.notificationsEnabled)
            .putBoolean(KEY_BIOMETRIC, next.biometricEnabled)
            .putInt(KEY_TIMEOUT, next.autoLockSeconds)
            .putInt(KEY_PIN_LENGTH, next.pinLength)
            .putString(KEY_OWNER, next.githubOwner)
            .putString(KEY_REPO, next.githubRepo)
            .putBoolean(KEY_IND_ENABLED, next.indicators.enabled)
            .putBoolean(KEY_IND_CAM, next.indicators.showCamera)
            .putBoolean(KEY_IND_MIC, next.indicators.showMic)
            .putBoolean(KEY_IND_LOC, next.indicators.showLocation)
            .putFloat(KEY_IND_SIZE, next.indicators.sizeDp)
            .putFloat(KEY_IND_OPACITY, next.indicators.opacity)
            .putFloat(KEY_IND_CAM_X, next.indicators.cameraX)
            .putFloat(KEY_IND_CAM_Y, next.indicators.cameraY)
            .putFloat(KEY_IND_MIC_X, next.indicators.micX)
            .putFloat(KEY_IND_MIC_Y, next.indicators.micY)
            .putFloat(KEY_IND_LOC_X, next.indicators.locX)
            .putFloat(KEY_IND_LOC_Y, next.indicators.locY)
            .apply()
        _settings.value = next.copy(hasPin = hasPin())
    }

    fun hasPin(): Boolean = !securePrefs.getString(KEY_PIN_HASH, null).isNullOrBlank()

    fun setPin(pin: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() })
        val salt = securePrefs.getString(KEY_SALT, null) ?: PinCrypto.newSalt().also {
            securePrefs.edit().putString(KEY_SALT, it).apply()
        }
        securePrefs.edit().putString(KEY_PIN_HASH, PinCrypto.hash(pin, salt)).apply()
        settingsPrefs.edit().putInt(KEY_PIN_LENGTH, pin.length).apply()
        _settings.value = readSettings()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = securePrefs.getString(KEY_SALT, null) ?: return false
        val expected = securePrefs.getString(KEY_PIN_HASH, null) ?: return false
        return PinCrypto.hash(pin, salt) == expected
    }

    fun hasRecovery(): Boolean = !securePrefs.getString(KEY_RECOVERY_HASH, null).isNullOrBlank()

    fun setRecovery(phrase: String) {
        require(phrase.trim().length >= 8)
        val salt = securePrefs.getString(KEY_RECOVERY_SALT, null) ?: PinCrypto.newSalt().also {
            securePrefs.edit().putString(KEY_RECOVERY_SALT, it).apply()
        }
        securePrefs.edit().putString(KEY_RECOVERY_HASH, PinCrypto.hash(phrase.trim(), salt)).apply()
    }

    fun verifyRecovery(phrase: String): Boolean {
        val salt = securePrefs.getString(KEY_RECOVERY_SALT, null) ?: return false
        val expected = securePrefs.getString(KEY_RECOVERY_HASH, null) ?: return false
        return PinCrypto.hash(phrase.trim(), salt) == expected
    }

    fun secureString(key: String): String? = securePrefs.getString(key, null)

    fun putSecureString(key: String, value: String?) {
        securePrefs.edit().putString(key, value).apply()
    }

    fun lastNotifiedFindings(): Set<String> = settingsPrefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()

    fun setLastNotifiedFindings(packages: Set<String>) {
        settingsPrefs.edit().putStringSet(KEY_NOTIFIED, packages).apply()
    }

    fun reviewedPackages(): Set<String> = settingsPrefs.getStringSet(KEY_REVIEWED, emptySet()).orEmpty()

    fun setReviewed(packageName: String, reviewed: Boolean) {
        val next = reviewedPackages().toMutableSet()
        if (reviewed) next += packageName else next -= packageName
        settingsPrefs.edit().putStringSet(KEY_REVIEWED, next).apply()
    }

    fun wipeLocalData() {
        securePrefs.edit().clear().apply()
        settingsPrefs.edit().clear().apply()
        _settings.value = readSettings()
        _lockedPackages.value = emptySet()
    }

    fun isLocked(packageName: String): Boolean = packageName in _lockedPackages.value

    fun setLocked(packageName: String, locked: Boolean) {
        val next = _lockedPackages.value.toMutableSet()
        if (locked) next += packageName else next -= packageName
        settingsPrefs.edit().putStringSet(KEY_LOCKED, next).apply()
        _lockedPackages.value = next
    }

    fun isTemporarilyUnlocked(packageName: String): Boolean {
        val until = settingsPrefs.getLong(unlockKey(packageName), 0L)
        return System.currentTimeMillis() < until
    }

    fun markUnlocked(packageName: String) {
        val seconds = _settings.value.autoLockSeconds
        val graceSeconds = if (seconds <= 0) 2 else seconds
        settingsPrefs.edit()
            .putLong(unlockKey(packageName), System.currentTimeMillis() + graceSeconds * 1000L)
            .apply()
    }

    private fun readSettings(): ShieldSettings {
        val theme = settingsPrefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
        val seed = settingsPrefs.getString(KEY_SEED, SeedColor.TEAL.name)
        val indicators = IndicatorSettings(
            enabled = settingsPrefs.getBoolean(KEY_IND_ENABLED, false),
            showCamera = settingsPrefs.getBoolean(KEY_IND_CAM, true),
            showMic = settingsPrefs.getBoolean(KEY_IND_MIC, true),
            showLocation = settingsPrefs.getBoolean(KEY_IND_LOC, true),
            sizeDp = settingsPrefs.getFloat(KEY_IND_SIZE, 14f).coerceIn(8f, 28f),
            opacity = settingsPrefs.getFloat(KEY_IND_OPACITY, 0.92f).coerceIn(0.3f, 1f),
            cameraX = settingsPrefs.getFloat(KEY_IND_CAM_X, 0.62f).coerceIn(0f, 0.95f),
            cameraY = settingsPrefs.getFloat(KEY_IND_CAM_Y, 0.04f).coerceIn(0f, 0.9f),
            micX = settingsPrefs.getFloat(KEY_IND_MIC_X, 0.74f).coerceIn(0f, 0.95f),
            micY = settingsPrefs.getFloat(KEY_IND_MIC_Y, 0.04f).coerceIn(0f, 0.9f),
            locX = settingsPrefs.getFloat(KEY_IND_LOC_X, 0.86f).coerceIn(0f, 0.95f),
            locY = settingsPrefs.getFloat(KEY_IND_LOC_Y, 0.04f).coerceIn(0f, 0.9f)
        )
        return ShieldSettings(
            themeMode = runCatching { ThemeMode.valueOf(theme ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            seedColor = runCatching { SeedColor.valueOf(seed ?: "") }.getOrDefault(SeedColor.TEAL),
            notificationsEnabled = settingsPrefs.getBoolean(KEY_NOTIFICATIONS, true),
            biometricEnabled = settingsPrefs.getBoolean(KEY_BIOMETRIC, false),
            autoLockSeconds = settingsPrefs.getInt(KEY_TIMEOUT, 0),
            pinLength = settingsPrefs.getInt(KEY_PIN_LENGTH, 4).coerceIn(4, 6),
            hasPin = hasPin(),
            githubOwner = settingsPrefs.getString(KEY_OWNER, "").orEmpty(),
            githubRepo = settingsPrefs.getString(KEY_REPO, "").orEmpty(),
            indicators = indicators
        )
    }

    private fun readLocked(): Set<String> = settingsPrefs.getStringSet(KEY_LOCKED, emptySet()).orEmpty()

    private fun unlockKey(packageName: String) = "until_$packageName"

    companion object {
        private const val SETTINGS_NAME = "shield_settings"
        private const val SECURE_NAME = "shield_secure"
        private const val KEY_THEME = "theme"
        private const val KEY_SEED = "seed"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_TIMEOUT = "timeout"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_LOCKED = "locked"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_RECOVERY_HASH = "recovery_hash"
        private const val KEY_RECOVERY_SALT = "recovery_salt"
        private const val KEY_OWNER = "github_owner"
        private const val KEY_REPO = "github_repo"
        private const val KEY_NOTIFIED = "notified_findings"
        private const val KEY_REVIEWED = "reviewed"
        private const val KEY_IND_ENABLED = "ind_enabled"
        private const val KEY_IND_CAM = "ind_cam"
        private const val KEY_IND_MIC = "ind_mic"
        private const val KEY_IND_LOC = "ind_loc"
        private const val KEY_IND_SIZE = "ind_size"
        private const val KEY_IND_OPACITY = "ind_opacity"
        private const val KEY_IND_CAM_X = "ind_cam_x"
        private const val KEY_IND_CAM_Y = "ind_cam_y"
        private const val KEY_IND_MIC_X = "ind_mic_x"
        private const val KEY_IND_MIC_Y = "ind_mic_y"
        private const val KEY_IND_LOC_X = "ind_loc_x"
        private const val KEY_IND_LOC_Y = "ind_loc_y"

        @Volatile
        private var instance: ShieldPreferences? = null

        fun get(context: Context): ShieldPreferences {
            return instance ?: synchronized(this) {
                instance ?: ShieldPreferences(context.applicationContext).also { instance = it }
            }
        }

        private fun openSecurePrefs(context: Context): SharedPreferences {
            return try {
                val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                EncryptedSharedPreferences.create(
                    SECURE_NAME,
                    alias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                context.getSharedPreferences("${SECURE_NAME}_fallback", Context.MODE_PRIVATE)
            }
        }
    }
}
