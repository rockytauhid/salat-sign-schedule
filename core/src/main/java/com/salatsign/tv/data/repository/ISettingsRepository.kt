// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.repository

import com.salatsign.tv.data.model.AdhanAudioOption
import com.salatsign.tv.data.model.AppLanguage
import com.salatsign.tv.data.model.AppSettings
import com.salatsign.tv.data.model.CityPreset
import com.salatsign.tv.data.model.FridayAdhanTiming
import com.salatsign.tv.data.model.JumuahMode
import com.salatsign.tv.data.model.MosqueFontPreset
import com.salatsign.tv.data.model.MosqueIconPreset
import com.salatsign.tv.data.model.Prayer
import com.salatsign.tv.data.model.PrayerSilentConfig
import com.salatsign.tv.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Interface contract for application settings repository.
 * Decouples settings access and persistence from DataStore/Android Context,
 * allowing remote HTTP controllers, local services, and test harnesses
 * to inspect and modify settings cleanly.
 */
interface ISettingsRepository {
    val appSettingsFlow: Flow<AppSettings>

    suspend fun setSetupCompleted(completed: Boolean)
    suspend fun setMosqueMode(enabled: Boolean)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun selectCity(city: CityPreset)
    suspend fun updateLocation(
        latitude: Double,
        longitude: Double,
        cityName: String,
        method: String? = null,
        zoneId: String? = null
    )
    suspend fun setZoneId(zoneId: String)
    suspend fun setCalculationMethod(method: String)
    suspend fun setMadhab(madhab: String)
    suspend fun setHighLatitudeRule(rule: String?)
    suspend fun updateHijriOffset(offsetDays: Int)
    suspend fun setHijriChangeAtMidnight(atMidnight: Boolean)
    suspend fun setPrePrayerReminder(enabled: Boolean)
    suspend fun updatePrayerOffset(prayer: Prayer, offsetMinutes: Int)
    suspend fun setPrayerOffset(prayer: Prayer, offsetMinutes: Int)
    suspend fun updateSunriseOffset(offsetMinutes: Int)
    suspend fun setSunriseOffset(offsetMinutes: Int)
    suspend fun updateIqamahOffset(prayer: Prayer, minutesAfter: Int)
    suspend fun setIqamahOffset(prayer: Prayer, minutesAfter: Int)
    suspend fun setMosqueModeAdhan(enabled: Boolean)
    suspend fun setMosqueModeReminders(enabled: Boolean)
    suspend fun setMosqueModeAdhanEnabled(enabled: Boolean)
    suspend fun setMosqueModeRemindersEnabled(enabled: Boolean)
    suspend fun setShowStrikeHighlight(show: Boolean)
    suspend fun updateAdhanOption(prayer: Prayer, option: AdhanAudioOption)
    suspend fun updateMosqueBranding(name: String, subtitle: String, logoUri: String?)
    suspend fun setMosqueIconPreset(preset: MosqueIconPreset)
    suspend fun setMosqueFontPreset(preset: MosqueFontPreset)
    suspend fun updateScheduleDuration(seconds: Int)
    suspend fun setCustomPin(pin: String)
    suspend fun resetToDefaultPin()
    suspend fun setHomeModePinRequired(required: Boolean)
    suspend fun recordPinSuccess()
    suspend fun recordPinFailure(newFailedAttempts: Int, lockedUntilEpochMillis: Long)
    suspend fun dismissDefaultPinPrompt()
    suspend fun dismissAdhanHint()
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun set24HourFormat(enabled: Boolean)
    suspend fun setShowArabicPrayerNames(enabled: Boolean)
    suspend fun setJumuahMode(mode: JumuahMode)
    suspend fun setJumuahTimes(times: List<String>)
    suspend fun setFridayAdhanTiming(timing: FridayAdhanTiming)
    // Silent Screen settings (Mosque / SalatSign)
    suspend fun setSilentJumuahEnabled(enabled: Boolean)
    suspend fun setSilentJumuahLeadMinutes(minutes: Int)
    suspend fun setSilentJumuahDurationMinutes(minutes: Int)
    suspend fun setSilentDailyPrayerConfig(prayer: Prayer, config: PrayerSilentConfig)
    suspend fun setSilentDailyPrayerConfigs(configs: Map<Prayer, PrayerSilentConfig>)
    suspend fun setManualSilentUntil(epochMillis: Long)
    // Islamic Calendar Events settings
    suspend fun setEnabledIslamicEventIds(ids: Set<String>)
    suspend fun setShowNextEventOnHome(show: Boolean)
    suspend fun setIslamicCalendarSlideEnabled(enabled: Boolean)
    suspend fun setIslamicCalendarSlideDurationSeconds(seconds: Int)
    // Offline Quran Verse Slide settings (Mosque / SalatSign)
    suspend fun setQuranVerseEnabled(enabled: Boolean)
    suspend fun setQuranShowTranslation(show: Boolean)
    suspend fun setQuranVerseDurationSeconds(seconds: Int)
    suspend fun setQuranShuffleBagState(serializedState: String)
    // Android TV Screensaver settings (Home / SalatSchedule)
    suspend fun setScreensaverScheduleEnabled(enabled: Boolean)
    suspend fun setScreensaverCalendarEnabled(enabled: Boolean)
    suspend fun setScreensaverQuranEnabled(enabled: Boolean)
    suspend fun setScreensaverScheduleDurationSeconds(seconds: Int)
    suspend fun setScreensaverCalendarDurationSeconds(seconds: Int)
    // TV Identity & Multi-TV Sync Metadata (SalatSign Phone Controller Phase 0)
    suspend fun ensureDeviceId(): String
    suspend fun setDeviceIdentity(name: String, roomGroup: String)
    suspend fun bumpConfigVersion(): Long
    suspend fun setConfigVersion(version: Long)
    // Paired Phone Controller Devices & Auth Policy (SalatSign Phase 1)
    suspend fun addPairedDevice(record: com.salatsign.tv.security.PairedDeviceRecord)
    suspend fun revokePairedDevice(deviceId: String, revokedAtEpochMillis: Long)
    suspend fun touchPairedDeviceLastSeen(deviceId: String, lastSeenAtEpochMillis: Long)
    suspend fun setLanPinLoginEnabled(enabled: Boolean)
    suspend fun setPairedTokenExpiryDays(days: Int)
}
