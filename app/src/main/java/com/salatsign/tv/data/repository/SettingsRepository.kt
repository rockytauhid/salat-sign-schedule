package com.salatsign.tv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AdhanAudioType
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.ThemeMode
import com.salatsign.tv.security.PinHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salatsign_preferences")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
        val IS_MOSQUE_MODE = booleanPreferencesKey("is_mosque_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val CITY_NAME = stringPreferencesKey("city_name")
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val HIJRI_OFFSET_DAYS = intPreferencesKey("hijri_offset_days")
        val PRE_PRAYER_REMINDER = booleanPreferencesKey("pre_prayer_reminder")

        // Per-prayer adhan keys
        fun adhanTypeKey(prayer: Prayer) = stringPreferencesKey("adhan_type_${prayer.id}")
        fun adhanNameKey(prayer: Prayer) = stringPreferencesKey("adhan_name_${prayer.id}")
        fun adhanUriKey(prayer: Prayer) = stringPreferencesKey("adhan_uri_${prayer.id}")

        // Mosque mode settings
        val MOSQUE_NAME = stringPreferencesKey("mosque_name")
        val MOSQUE_SUBTITLE = stringPreferencesKey("mosque_subtitle")
        val MOSQUE_LOGO_URI = stringPreferencesKey("mosque_logo_uri")
        val MOSQUE_ICON_PRESET = stringPreferencesKey("mosque_icon_preset")
        val MOSQUE_FONT_PRESET = stringPreferencesKey("mosque_font_preset")
        val SCHEDULE_DURATION_SECONDS = intPreferencesKey("schedule_duration_seconds")

        // Security / Admin PIN keys
        val HOME_MODE_PIN_REQUIRED = booleanPreferencesKey("home_mode_pin_required")
        val HAS_CUSTOM_PIN = booleanPreferencesKey("has_custom_pin")
        val PIN_SALT_HEX = stringPreferencesKey("pin_salt_hex")
        val PIN_HASH_HEX = stringPreferencesKey("pin_hash_hex")
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKED_UNTIL_EPOCH_MILLIS = longPreferencesKey("pin_locked_until_epoch_millis")
        val DEFAULT_PIN_PROMPT_DISMISSED = booleanPreferencesKey("default_pin_prompt_dismissed")
    }

    val appSettingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isSetupCompleted = preferences[PreferencesKeys.IS_SETUP_COMPLETED] ?: false
            val isMosqueMode = preferences[PreferencesKeys.IS_MOSQUE_MODE] ?: false
            val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.ALWAYS_DARK.name
            val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.ALWAYS_DARK)

            val latitude = preferences[PreferencesKeys.LATITUDE] ?: -6.2088
            val longitude = preferences[PreferencesKeys.LONGITUDE] ?: 106.8456
            val cityName = preferences[PreferencesKeys.CITY_NAME] ?: "Jakarta"
            val calculationMethod = preferences[PreferencesKeys.CALCULATION_METHOD] ?: "SINGAPORE"
            val hijriOffset = preferences[PreferencesKeys.HIJRI_OFFSET_DAYS] ?: 0
            val preReminder = preferences[PreferencesKeys.PRE_PRAYER_REMINDER] ?: true

            val adhanSubuh = readAdhanOption(preferences, Prayer.SUBUH, AdhanAudioOption.DefaultSubuh)
            val adhanZuhur = readAdhanOption(preferences, Prayer.ZUHUR, AdhanAudioOption.DefaultStandard)
            val adhanAshr = readAdhanOption(preferences, Prayer.ASHR, AdhanAudioOption.DefaultStandard)
            val adhanMagrib = readAdhanOption(preferences, Prayer.MAGRIB, AdhanAudioOption.DefaultStandard)
            val adhanIsya = readAdhanOption(preferences, Prayer.ISYA, AdhanAudioOption.DefaultStandard)

            val mosqueName = preferences[PreferencesKeys.MOSQUE_NAME] ?: "Masjid Raya"
            val mosqueSubtitle = preferences[PreferencesKeys.MOSQUE_SUBTITLE] ?: "Luruskan dan Rapatkan Shaf"
            val mosqueLogoUri = preferences[PreferencesKeys.MOSQUE_LOGO_URI]
            val iconPresetStr = preferences[PreferencesKeys.MOSQUE_ICON_PRESET] ?: MosqueIconPreset.DOME_CRESCENT.name
            val mosqueIconPreset = runCatching { MosqueIconPreset.valueOf(iconPresetStr) }.getOrDefault(MosqueIconPreset.DOME_CRESCENT)

            val fontPresetStr = preferences[PreferencesKeys.MOSQUE_FONT_PRESET] ?: MosqueFontPreset.MODERN_SANS.name
            val mosqueFontPreset = runCatching { MosqueFontPreset.valueOf(fontPresetStr) }.getOrDefault(MosqueFontPreset.MODERN_SANS)

            val scheduleDuration = preferences[PreferencesKeys.SCHEDULE_DURATION_SECONDS] ?: 60

            val homeModePinRequired = preferences[PreferencesKeys.HOME_MODE_PIN_REQUIRED] ?: false
            val hasCustomPin = preferences[PreferencesKeys.HAS_CUSTOM_PIN] ?: false
            val pinSaltHex = preferences[PreferencesKeys.PIN_SALT_HEX] ?: ""
            val pinHashHex = preferences[PreferencesKeys.PIN_HASH_HEX] ?: ""
            val pinFailedAttempts = preferences[PreferencesKeys.PIN_FAILED_ATTEMPTS] ?: 0
            val pinLockedUntil = preferences[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] ?: 0L
            val defaultPinPromptDismissed = preferences[PreferencesKeys.DEFAULT_PIN_PROMPT_DISMISSED] ?: false

            AppSettings(
                isSetupCompleted = isSetupCompleted,
                isMosqueMode = isMosqueMode,
                themeMode = themeMode,
                latitude = latitude,
                longitude = longitude,
                cityName = cityName,
                calculationMethodName = calculationMethod,
                hijriOffsetDays = hijriOffset,
                prePrayerReminderEnabled = preReminder,
                adhanSubuh = adhanSubuh,
                adhanZuhur = adhanZuhur,
                adhanAshr = adhanAshr,
                adhanMagrib = adhanMagrib,
                adhanIsya = adhanIsya,
                mosqueName = mosqueName,
                mosqueSubtitle = mosqueSubtitle,
                mosqueLogoUri = mosqueLogoUri,
                mosqueIconPreset = mosqueIconPreset,
                mosqueFontPreset = mosqueFontPreset,
                scheduleDurationSeconds = scheduleDuration,
                homeModePinRequired = homeModePinRequired,
                hasCustomPin = hasCustomPin,
                pinSaltHex = pinSaltHex,
                pinHashHex = pinHashHex,
                pinFailedAttempts = pinFailedAttempts,
                pinLockedUntilEpochMillis = pinLockedUntil,
                defaultPinPromptDismissed = defaultPinPromptDismissed
            )
        }

    private fun readAdhanOption(
        preferences: Preferences,
        prayer: Prayer,
        defaultOption: AdhanAudioOption
    ): AdhanAudioOption {
        val typeStr = preferences[PreferencesKeys.adhanTypeKey(prayer)] ?: defaultOption.type.name
        val name = preferences[PreferencesKeys.adhanNameKey(prayer)] ?: defaultOption.displayName
        val uri = preferences[PreferencesKeys.adhanUriKey(prayer)] ?: defaultOption.customUri
        val type = runCatching { AdhanAudioType.valueOf(typeStr) }.getOrDefault(defaultOption.type)
        return AdhanAudioOption(type = type, displayName = name, customUri = uri)
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_SETUP_COMPLETED] = completed
        }
    }

    suspend fun setMosqueMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_MOSQUE_MODE] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun selectCity(city: CityPreset) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LATITUDE] = city.latitude
            prefs[PreferencesKeys.LONGITUDE] = city.longitude
            prefs[PreferencesKeys.CITY_NAME] = city.name
            prefs[PreferencesKeys.CALCULATION_METHOD] = city.calculationMethod
        }
    }

    suspend fun updateLocation(latitude: Double, longitude: Double, cityName: String, method: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LATITUDE] = latitude
            prefs[PreferencesKeys.LONGITUDE] = longitude
            prefs[PreferencesKeys.CITY_NAME] = cityName
            if (method != null) {
                prefs[PreferencesKeys.CALCULATION_METHOD] = method
            }
        }
    }

    suspend fun updateHijriOffset(offsetDays: Int) {
        val clamped = offsetDays.coerceIn(-2, 2)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HIJRI_OFFSET_DAYS] = clamped
        }
    }

    suspend fun setPrePrayerReminder(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PRE_PRAYER_REMINDER] = enabled
        }
    }

    suspend fun updateAdhanOption(prayer: Prayer, option: AdhanAudioOption) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.adhanTypeKey(prayer)] = option.type.name
            prefs[PreferencesKeys.adhanNameKey(prayer)] = option.displayName
            if (option.customUri != null) {
                prefs[PreferencesKeys.adhanUriKey(prayer)] = option.customUri
            } else {
                prefs.remove(PreferencesKeys.adhanUriKey(prayer))
            }
        }
    }

    suspend fun updateMosqueBranding(name: String, subtitle: String, logoUri: String?) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOSQUE_NAME] = name
            prefs[PreferencesKeys.MOSQUE_SUBTITLE] = subtitle
            if (logoUri != null) {
                prefs[PreferencesKeys.MOSQUE_LOGO_URI] = logoUri
            } else {
                prefs.remove(PreferencesKeys.MOSQUE_LOGO_URI)
            }
        }
    }

    suspend fun setMosqueIconPreset(preset: MosqueIconPreset) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOSQUE_ICON_PRESET] = preset.name
        }
    }

    suspend fun setMosqueFontPreset(preset: MosqueFontPreset) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOSQUE_FONT_PRESET] = preset.name
        }
    }

    suspend fun updateScheduleDuration(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SCHEDULE_DURATION_SECONDS] = seconds.coerceAtLeast(10)
        }
    }

    suspend fun setCustomPin(pin: String) {
        val hashed = PinHasher.createHashedPin(pin)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_CUSTOM_PIN] = true
            prefs[PreferencesKeys.PIN_SALT_HEX] = hashed.saltHex
            prefs[PreferencesKeys.PIN_HASH_HEX] = hashed.hashHex
            prefs[PreferencesKeys.DEFAULT_PIN_PROMPT_DISMISSED] = true
            prefs[PreferencesKeys.PIN_FAILED_ATTEMPTS] = 0
            prefs[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] = 0L
        }
    }

    suspend fun resetToDefaultPin() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_CUSTOM_PIN] = false
            prefs.remove(PreferencesKeys.PIN_SALT_HEX)
            prefs.remove(PreferencesKeys.PIN_HASH_HEX)
            prefs[PreferencesKeys.DEFAULT_PIN_PROMPT_DISMISSED] = false
            prefs[PreferencesKeys.PIN_FAILED_ATTEMPTS] = 0
            prefs[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] = 0L
        }
    }

    suspend fun setHomeModePinRequired(required: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HOME_MODE_PIN_REQUIRED] = required
        }
    }

    suspend fun recordPinSuccess() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIN_FAILED_ATTEMPTS] = 0
            prefs[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] = 0L
        }
    }

    suspend fun recordPinFailure(newFailedAttempts: Int, lockedUntilEpochMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIN_FAILED_ATTEMPTS] = newFailedAttempts
            prefs[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] = lockedUntilEpochMillis
        }
    }

    suspend fun dismissDefaultPinPrompt() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_PIN_PROMPT_DISMISSED] = true
        }
    }
}
