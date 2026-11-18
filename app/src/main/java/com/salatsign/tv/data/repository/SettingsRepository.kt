// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

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
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.FridayAdhanTiming
import com.salatsign.tv.data.model.JumuahMode
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

class SettingsRepository(private val context: Context) : ISettingsRepository {

    private object PreferencesKeys {
        val IS_SETUP_COMPLETED = booleanPreferencesKey("is_setup_completed")
        val IS_MOSQUE_MODE = booleanPreferencesKey("is_mosque_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val CITY_NAME = stringPreferencesKey("city_name")
        val ZONE_ID = stringPreferencesKey("zone_id")
        val CALCULATION_METHOD = stringPreferencesKey("calculation_method")
        val MADHAB = stringPreferencesKey("madhab")
        val HIGH_LATITUDE_RULE = stringPreferencesKey("high_latitude_rule")
        val HIJRI_OFFSET_DAYS = intPreferencesKey("hijri_offset_days")
        val HIJRI_CHANGE_AT_MIDNIGHT = booleanPreferencesKey("hijri_change_at_midnight")
        val PRE_PRAYER_REMINDER = booleanPreferencesKey("pre_prayer_reminder")

        // Per-prayer offsets (-30..+30 mins)
        val OFFSET_SUBUH = intPreferencesKey("offset_subuh")
        val OFFSET_SUNRISE = intPreferencesKey("offset_sunrise")
        val OFFSET_ZUHUR = intPreferencesKey("offset_zuhur")
        val OFFSET_ASHR = intPreferencesKey("offset_ashr")
        val OFFSET_MAGRIB = intPreferencesKey("offset_magrib")
        val OFFSET_ISYA = intPreferencesKey("offset_isya")

        // Per-prayer Iqamah (minutes after adhan)
        val IQAMAH_SUBUH = intPreferencesKey("iqamah_subuh")
        val IQAMAH_ZUHUR = intPreferencesKey("iqamah_zuhur")
        val IQAMAH_ASHR = intPreferencesKey("iqamah_ashr")
        val IQAMAH_MAGRIB = intPreferencesKey("iqamah_magrib")
        val IQAMAH_ISYA = intPreferencesKey("iqamah_isya")

        // Mosque Mode quiet settings & visual strike (P4.7)
        val MOSQUE_MODE_ADHAN_ENABLED = booleanPreferencesKey("mosque_mode_adhan_enabled")
        val MOSQUE_MODE_REMINDERS_ENABLED = booleanPreferencesKey("mosque_mode_reminders_enabled")
        val SHOW_STRIKE_HIGHLIGHT = booleanPreferencesKey("show_strike_highlight")

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

        // Jumu'ah (Friday) settings - Mosque Mode only (D16)
        val JUMUAH_MODE = stringPreferencesKey("jumuah_mode")
        val JUMUAH_TIMES = stringPreferencesKey("jumuah_times")
        val FRIDAY_ADHAN_TIMING = stringPreferencesKey("friday_adhan_timing")

        // Security / Admin PIN keys
        val HOME_MODE_PIN_REQUIRED = booleanPreferencesKey("home_mode_pin_required")
        val HAS_CUSTOM_PIN = booleanPreferencesKey("has_custom_pin")
        val PIN_SALT_HEX = stringPreferencesKey("pin_salt_hex")
        val PIN_HASH_HEX = stringPreferencesKey("pin_hash_hex")
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_LOCKED_UNTIL_EPOCH_MILLIS = longPreferencesKey("pin_locked_until_epoch_millis")
        val DEFAULT_PIN_PROMPT_DISMISSED = booleanPreferencesKey("default_pin_prompt_dismissed")
        val ADHAN_HINT_DISMISSED = booleanPreferencesKey("adhan_hint_dismissed")

        // Localization & Display Option keys
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val IS_24_HOUR_FORMAT = booleanPreferencesKey("is_24_hour_format")
        val SHOW_ARABIC_PRAYER_NAMES = booleanPreferencesKey("show_arabic_prayer_names")
    }

    override val appSettingsFlow: Flow<AppSettings> = context.dataStore.data
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

            val latitude = preferences[PreferencesKeys.LATITUDE] ?: -35.2809
            val longitude = preferences[PreferencesKeys.LONGITUDE] ?: 149.1300
            val cityName = preferences[PreferencesKeys.CITY_NAME] ?: "Canberra, ACT, Australia"
            val zoneId = preferences[PreferencesKeys.ZONE_ID] ?: "Australia/Sydney"
            val calculationMethod = preferences[PreferencesKeys.CALCULATION_METHOD] ?: "MUSLIM_WORLD_LEAGUE"
            val madhab = preferences[PreferencesKeys.MADHAB] ?: "SHAFI"
            val highLatitudeRule = preferences[PreferencesKeys.HIGH_LATITUDE_RULE]
            val hijriOffset = preferences[PreferencesKeys.HIJRI_OFFSET_DAYS] ?: 0
            val hijriChangeAtMidnight = preferences[PreferencesKeys.HIJRI_CHANGE_AT_MIDNIGHT] ?: false
            val preReminder = preferences[PreferencesKeys.PRE_PRAYER_REMINDER] ?: true

            val offsetSubuh = preferences[PreferencesKeys.OFFSET_SUBUH] ?: 0
            val offsetSunrise = preferences[PreferencesKeys.OFFSET_SUNRISE] ?: 0
            val offsetZuhur = preferences[PreferencesKeys.OFFSET_ZUHUR] ?: 0
            val offsetAshr = preferences[PreferencesKeys.OFFSET_ASHR] ?: 0
            val offsetMagrib = preferences[PreferencesKeys.OFFSET_MAGRIB] ?: 0
            val offsetIsya = preferences[PreferencesKeys.OFFSET_ISYA] ?: 0

            val iqamahSubuh = preferences[PreferencesKeys.IQAMAH_SUBUH] ?: 0
            val iqamahZuhur = preferences[PreferencesKeys.IQAMAH_ZUHUR] ?: 0
            val iqamahAshr = preferences[PreferencesKeys.IQAMAH_ASHR] ?: 0
            val iqamahMagrib = preferences[PreferencesKeys.IQAMAH_MAGRIB] ?: 0
            val iqamahIsya = preferences[PreferencesKeys.IQAMAH_ISYA] ?: 0

            val mosqueModeAdhanEnabled = preferences[PreferencesKeys.MOSQUE_MODE_ADHAN_ENABLED] ?: false
            val mosqueModeRemindersEnabled = preferences[PreferencesKeys.MOSQUE_MODE_REMINDERS_ENABLED] ?: false
            val showStrikeHighlight = preferences[PreferencesKeys.SHOW_STRIKE_HIGHLIGHT] ?: true

            val appLanguageStr = preferences[PreferencesKeys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name
            val appLanguage = runCatching { AppLanguage.valueOf(appLanguageStr) }.getOrDefault(AppLanguage.SYSTEM)
            val is24HourFormat = preferences[PreferencesKeys.IS_24_HOUR_FORMAT] ?: true
            val showArabicPrayerNames = preferences[PreferencesKeys.SHOW_ARABIC_PRAYER_NAMES] ?: true

            val adhanSubuh = readAdhanOption(preferences, Prayer.SUBUH, AdhanAudioOption.Silent)
            val adhanZuhur = readAdhanOption(preferences, Prayer.ZUHUR, AdhanAudioOption.Silent)
            val adhanAshr = readAdhanOption(preferences, Prayer.ASHR, AdhanAudioOption.Silent)
            val adhanMagrib = readAdhanOption(preferences, Prayer.MAGRIB, AdhanAudioOption.Silent)
            val adhanIsya = readAdhanOption(preferences, Prayer.ISYA, AdhanAudioOption.Silent)
            val adhanHintDismissed = preferences[PreferencesKeys.ADHAN_HINT_DISMISSED] ?: false

            val mosqueName = preferences[PreferencesKeys.MOSQUE_NAME] ?: ""
            val mosqueSubtitle = preferences[PreferencesKeys.MOSQUE_SUBTITLE] ?: ""
            val mosqueLogoUri = preferences[PreferencesKeys.MOSQUE_LOGO_URI]
            val iconPresetStr = preferences[PreferencesKeys.MOSQUE_ICON_PRESET] ?: MosqueIconPreset.DOME_CRESCENT.name
            val mosqueIconPreset = runCatching { MosqueIconPreset.valueOf(iconPresetStr) }.getOrDefault(MosqueIconPreset.DOME_CRESCENT)

            val fontPresetStr = preferences[PreferencesKeys.MOSQUE_FONT_PRESET] ?: MosqueFontPreset.MODERN_SANS.name
            val mosqueFontPreset = runCatching { MosqueFontPreset.valueOf(fontPresetStr) }.getOrDefault(MosqueFontPreset.MODERN_SANS)

            val scheduleDuration = preferences[PreferencesKeys.SCHEDULE_DURATION_SECONDS] ?: 60

            val jumuahModeStr = preferences[PreferencesKeys.JUMUAH_MODE] ?: JumuahMode.FIXED.name
            val jumuahMode = runCatching { JumuahMode.valueOf(jumuahModeStr) }.getOrDefault(JumuahMode.FIXED)
            val jumuahTimesRaw = preferences[PreferencesKeys.JUMUAH_TIMES] ?: ""
            val jumuahTimes = if (jumuahTimesRaw.isBlank()) emptyList() else jumuahTimesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val fridayAdhanTimingStr = preferences[PreferencesKeys.FRIDAY_ADHAN_TIMING] ?: FridayAdhanTiming.AT_ZUHR.name
            val fridayAdhanTiming = runCatching { FridayAdhanTiming.valueOf(fridayAdhanTimingStr) }.getOrDefault(FridayAdhanTiming.AT_ZUHR)

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
                zoneId = zoneId,
                calculationMethodName = calculationMethod,
                madhab = madhab,
                highLatitudeRule = highLatitudeRule,
                hijriOffsetDays = hijriOffset,
                hijriChangeAtMidnight = hijriChangeAtMidnight,
                prePrayerReminderEnabled = preReminder,
                adhanSubuh = adhanSubuh,
                adhanZuhur = adhanZuhur,
                adhanAshr = adhanAshr,
                adhanMagrib = adhanMagrib,
                adhanIsya = adhanIsya,
                offsetSubuh = offsetSubuh,
                offsetSunrise = offsetSunrise,
                offsetZuhur = offsetZuhur,
                offsetAshr = offsetAshr,
                offsetMagrib = offsetMagrib,
                offsetIsya = offsetIsya,
                iqamahSubuh = iqamahSubuh,
                iqamahZuhur = iqamahZuhur,
                iqamahAshr = iqamahAshr,
                iqamahMagrib = iqamahMagrib,
                iqamahIsya = iqamahIsya,
                mosqueModeAdhanEnabled = mosqueModeAdhanEnabled,
                mosqueModeRemindersEnabled = mosqueModeRemindersEnabled,
                showStrikeHighlight = showStrikeHighlight,
                jumuahMode = jumuahMode,
                jumuahTimes = jumuahTimes,
                fridayAdhanTiming = fridayAdhanTiming,
                appLanguage = appLanguage,
                is24HourFormat = is24HourFormat,
                showArabicPrayerNames = showArabicPrayerNames,
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
                defaultPinPromptDismissed = defaultPinPromptDismissed,
                adhanHintDismissed = adhanHintDismissed
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

    override suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_SETUP_COMPLETED] = completed
        }
    }

    override suspend fun setMosqueMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_MOSQUE_MODE] = enabled
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun selectCity(city: CityPreset) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LATITUDE] = city.latitude
            prefs[PreferencesKeys.LONGITUDE] = city.longitude
            prefs[PreferencesKeys.CITY_NAME] = city.name
            prefs[PreferencesKeys.CALCULATION_METHOD] = city.calculationMethod
            prefs[PreferencesKeys.ZONE_ID] = city.zoneId
        }
    }

    override suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        method: String?,
        zoneId: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LATITUDE] = latitude
            prefs[PreferencesKeys.LONGITUDE] = longitude
            prefs[PreferencesKeys.CITY_NAME] = cityName
            if (method != null) {
                prefs[PreferencesKeys.CALCULATION_METHOD] = method
            }
            if (zoneId != null) {
                prefs[PreferencesKeys.ZONE_ID] = zoneId
            }
        }
    }

    override suspend fun setCalculationMethod(method: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CALCULATION_METHOD] = method
        }
    }

    override suspend fun setMadhab(madhab: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MADHAB] = madhab
        }
    }

    override suspend fun setHighLatitudeRule(rule: String?) {
        context.dataStore.edit { prefs ->
            if (rule != null) {
                prefs[PreferencesKeys.HIGH_LATITUDE_RULE] = rule
            } else {
                prefs.remove(PreferencesKeys.HIGH_LATITUDE_RULE)
            }
        }
    }

    override suspend fun updateHijriOffset(offsetDays: Int) {
        val clamped = offsetDays.coerceIn(-2, 2)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HIJRI_OFFSET_DAYS] = clamped
        }
    }

    override suspend fun setHijriChangeAtMidnight(atMidnight: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HIJRI_CHANGE_AT_MIDNIGHT] = atMidnight
        }
    }

    override suspend fun setPrePrayerReminder(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PRE_PRAYER_REMINDER] = enabled
        }
    }

    override suspend fun updatePrayerOffset(prayer: Prayer, offsetMinutes: Int) {
        val clamped = offsetMinutes.coerceIn(-30, 30)
        context.dataStore.edit { prefs ->
            when (prayer) {
                Prayer.SUBUH -> prefs[PreferencesKeys.OFFSET_SUBUH] = clamped
                Prayer.ZUHUR -> prefs[PreferencesKeys.OFFSET_ZUHUR] = clamped
                Prayer.ASHR -> prefs[PreferencesKeys.OFFSET_ASHR] = clamped
                Prayer.MAGRIB -> prefs[PreferencesKeys.OFFSET_MAGRIB] = clamped
                Prayer.ISYA -> prefs[PreferencesKeys.OFFSET_ISYA] = clamped
            }
        }
    }

    override suspend fun updateSunriseOffset(offsetMinutes: Int) {
        val clamped = offsetMinutes.coerceIn(-30, 30)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.OFFSET_SUNRISE] = clamped
        }
    }

    override suspend fun updateIqamahOffset(prayer: Prayer, minutesAfter: Int) {
        val clamped = minutesAfter.coerceIn(0, 60)
        context.dataStore.edit { prefs ->
            when (prayer) {
                Prayer.SUBUH -> prefs[PreferencesKeys.IQAMAH_SUBUH] = clamped
                Prayer.ZUHUR -> prefs[PreferencesKeys.IQAMAH_ZUHUR] = clamped
                Prayer.ASHR -> prefs[PreferencesKeys.IQAMAH_ASHR] = clamped
                Prayer.MAGRIB -> prefs[PreferencesKeys.IQAMAH_MAGRIB] = clamped
                Prayer.ISYA -> prefs[PreferencesKeys.IQAMAH_ISYA] = clamped
            }
        }
    }

    override suspend fun setZoneId(zoneId: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ZONE_ID] = zoneId
        }
    }

    override suspend fun setPrayerOffset(prayer: Prayer, offsetMinutes: Int) = updatePrayerOffset(prayer, offsetMinutes)
    override suspend fun setSunriseOffset(offsetMinutes: Int) = updateSunriseOffset(offsetMinutes)
    override suspend fun setIqamahOffset(prayer: Prayer, minutesAfter: Int) = updateIqamahOffset(prayer, minutesAfter)
    override suspend fun setMosqueModeAdhan(enabled: Boolean) = setMosqueModeAdhanEnabled(enabled)
    override suspend fun setMosqueModeReminders(enabled: Boolean) = setMosqueModeRemindersEnabled(enabled)

    override suspend fun setMosqueModeAdhanEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOSQUE_MODE_ADHAN_ENABLED] = enabled
        }
    }

    override suspend fun setMosqueModeRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOSQUE_MODE_REMINDERS_ENABLED] = enabled
        }
    }

    override suspend fun setShowStrikeHighlight(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOW_STRIKE_HIGHLIGHT] = show
        }
    }

    override suspend fun updateAdhanOption(prayer: Prayer, option: AdhanAudioOption) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.adhanTypeKey(prayer)] = option.type.name
            if (option.type == AdhanAudioType.CUSTOM_FILE && option.displayName.isNotBlank()) {
                prefs[PreferencesKeys.adhanNameKey(prayer)] = option.displayName
            } else {
                prefs.remove(PreferencesKeys.adhanNameKey(prayer))
            }
            if (option.customUri != null) {
                prefs[PreferencesKeys.adhanUriKey(prayer)] = option.customUri
            } else {
                prefs.remove(PreferencesKeys.adhanUriKey(prayer))
            }
        }
    }

    override suspend fun updateMosqueBranding(name: String, subtitle: String, logoUri: String?) {
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

    override suspend fun setMosqueIconPreset(preset: MosqueIconPreset) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOSQUE_ICON_PRESET] = preset.name
        }
    }

    override suspend fun setMosqueFontPreset(preset: MosqueFontPreset) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.MOSQUE_FONT_PRESET] = preset.name
        }
    }

    override suspend fun updateScheduleDuration(seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SCHEDULE_DURATION_SECONDS] = seconds.coerceAtLeast(10)
        }
    }

    override suspend fun setCustomPin(pin: String) {
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

    override suspend fun resetToDefaultPin() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_CUSTOM_PIN] = false
            prefs.remove(PreferencesKeys.PIN_SALT_HEX)
            prefs.remove(PreferencesKeys.PIN_HASH_HEX)
            prefs[PreferencesKeys.DEFAULT_PIN_PROMPT_DISMISSED] = false
            prefs[PreferencesKeys.PIN_FAILED_ATTEMPTS] = 0
            prefs[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] = 0L
        }
    }

    override suspend fun setHomeModePinRequired(required: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.HOME_MODE_PIN_REQUIRED] = required
        }
    }

    override suspend fun recordPinSuccess() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIN_FAILED_ATTEMPTS] = 0
            prefs[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] = 0L
        }
    }

    override suspend fun recordPinFailure(newFailedAttempts: Int, lockedUntilEpochMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PIN_FAILED_ATTEMPTS] = newFailedAttempts
            prefs[PreferencesKeys.PIN_LOCKED_UNTIL_EPOCH_MILLIS] = lockedUntilEpochMillis
        }
    }

    override suspend fun dismissDefaultPinPrompt() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_PIN_PROMPT_DISMISSED] = true
        }
    }

    override suspend fun dismissAdhanHint() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ADHAN_HINT_DISMISSED] = true
        }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_LANGUAGE] = language.name
        }
    }

    override suspend fun set24HourFormat(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_24_HOUR_FORMAT] = enabled
        }
    }

    override suspend fun setShowArabicPrayerNames(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SHOW_ARABIC_PRAYER_NAMES] = enabled
        }
    }

    override suspend fun setJumuahMode(mode: JumuahMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.JUMUAH_MODE] = mode.name
        }
    }

    override suspend fun setJumuahTimes(times: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.JUMUAH_TIMES] = times.take(3).joinToString(",")
        }
    }

    override suspend fun setFridayAdhanTiming(timing: FridayAdhanTiming) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FRIDAY_ADHAN_TIMING] = timing.name
        }
    }
}
