// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Typed JSON serializer and depth guard shared across the SalatSign Phone Controller API
 * (replaces the hand-rolled SimpleJson parser).
 */
object PhoneControllerJson {
    const val MAX_JSON_DEPTH: Int = 32

    @OptIn(ExperimentalSerializationApi::class)
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /**
     * Validates JSON bracket/brace nesting depth before deserialization to prevent
     * stack exhaustion from deeply nested untrusted payloads.
     */
    fun validateDepth(raw: String, maxDepth: Int = MAX_JSON_DEPTH) {
        var depth = 0
        var inString = false
        var escaped = false
        for (ch in raw) {
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (ch == '\\') {
                    escaped = true
                } else if (ch == '"') {
                    inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{', '[' -> {
                    depth++
                    if (depth > maxDepth) {
                        throw IllegalArgumentException("JSON nesting exceeds maximum depth ($maxDepth)")
                    }
                }
                '}', ']' -> {
                    depth--
                    if (depth < 0) {
                        throw IllegalArgumentException("Malformed JSON brackets")
                    }
                }
            }
        }
    }

    inline fun <reified T> decode(raw: String): T {
        validateDepth(raw)
        return try {
            json.decodeFromString<T>(raw)
        } catch (e: SerializationException) {
            throw IllegalArgumentException("Malformed JSON payload", e)
        }
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)
}

@Serializable
data class ApiErrorResponse(
    val error: String,
    val message: String,
    val attemptsLeft: Int? = null,
    val remainingSeconds: Int? = null,
    val currentConfigVersion: Long? = null
)

@Serializable
data class StatusResponse(
    val app: String = "SalatSign",
    val apiVersion: Int = ApiConstants.API_VERSION,
    val version: String,
    val port: Int,
    val pinConfigured: Boolean,
    val deviceId: String,
    val deviceName: String,
    val roomGroup: String,
    val configVersion: Long
)

@Serializable
data class AuthRequest(
    val pin: String = ""
)

@Serializable
data class AuthResponse(
    val token: String,
    val expiresInSeconds: Long
)

@Serializable
data class LogoutResponse(
    val loggedOut: Boolean = true
)

@Serializable
data class DailySilentPrayerDto(
    val enabled: Boolean? = null,
    val anchor: String? = null,
    val leadMinutes: Int? = null,
    val durationMinutes: Int? = null
)

@Serializable
data class SettingsResponse(
    val deviceId: String,
    val deviceName: String,
    val roomGroup: String,
    val configVersion: Long,
    val mosqueName: String,
    val mosqueSubtitle: String,
    val mosqueLogoUri: String? = null,
    val mosqueIconPreset: String,
    val mosqueFontPreset: String,
    val appLanguage: String,
    val cityName: String,
    val zoneId: String,
    val calculationMethodName: String,
    val madhab: String,
    val themeMode: String,
    val scheduleDurationSeconds: Int,
    val hijriOffsetDays: Int,
    val hijriChangeAtMidnight: Boolean,
    val is24HourFormat: Boolean,
    val showArabicPrayerNames: Boolean,
    val showStrikeHighlight: Boolean,
    val jumuahMode: String,
    val jumuahTimes: List<String>,
    val iqamahSubuh: Int,
    val iqamahZuhur: Int,
    val iqamahAshr: Int,
    val iqamahMagrib: Int,
    val iqamahIsya: Int,
    val islamicCalendarSlideEnabled: Boolean,
    val islamicCalendarSlideDurationSeconds: Int,
    val quranVerseEnabled: Boolean,
    val quranShowTranslation: Boolean,
    val quranVerseDurationSeconds: Int,
    val silentJumuahEnabled: Boolean,
    val silentJumuahLeadMinutes: Int,
    val silentJumuahDurationMinutes: Int,
    val silentDailyPrayers: Map<String, DailySilentPrayerDto>,
    val manualSilentUntilEpochMillis: Long,
    val manualSilentActive: Boolean,
    val manualSilentRemainingMinutes: Long
)

@Serializable
data class SettingsPatchRequest(
    val deviceName: String? = null,
    val roomGroup: String? = null,
    val mosqueName: String? = null,
    val mosqueSubtitle: String? = null,
    val mosqueIconPreset: String? = null,
    val mosqueFontPreset: String? = null,
    val appLanguage: String? = null,
    val themeMode: String? = null,
    val scheduleDurationSeconds: Int? = null,
    val hijriOffsetDays: Int? = null,
    val hijriChangeAtMidnight: Boolean? = null,
    val is24HourFormat: Boolean? = null,
    val showArabicPrayerNames: Boolean? = null,
    val showStrikeHighlight: Boolean? = null,
    val jumuahMode: String? = null,
    val jumuahTimes: List<String>? = null,
    val iqamahSubuh: Int? = null,
    val iqamahZuhur: Int? = null,
    val iqamahAshr: Int? = null,
    val iqamahMagrib: Int? = null,
    val iqamahIsya: Int? = null,
    val islamicCalendarSlideEnabled: Boolean? = null,
    val islamicCalendarSlideDurationSeconds: Int? = null,
    val quranVerseEnabled: Boolean? = null,
    val quranShowTranslation: Boolean? = null,
    val quranVerseDurationSeconds: Int? = null,
    val silentJumuahEnabled: Boolean? = null,
    val silentJumuahLeadMinutes: Int? = null,
    val silentJumuahDurationMinutes: Int? = null,
    val silentDailyPrayers: Map<String, DailySilentPrayerDto>? = null
)

@Serializable
data class ManualSilentStartRequest(
    val durationMinutes: Int = 15
)

@Serializable
data class ManualSilentResponse(
    val manualSilentActive: Boolean,
    val manualSilentUntilEpochMillis: Long,
    val manualSilentRemainingMinutes: Long,
    val durationMinutes: Int? = null
)

@Serializable
data class SlideLineDto(
    val text: String = "",
    val align: String = "CENTER",
    val fontSize: String = "MEDIUM"
)

@Serializable
data class SlideDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val lines: List<SlideLineDto> = emptyList(),
    val imageFile: String? = null,
    val durationSeconds: Int,
    val enabled: Boolean,
    val textAlign: String = "CENTER",
    val sortOrder: Int = 0
)

@Serializable
data class SlidesResponse(
    val revision: Int,
    val configVersion: Long,
    val usedSlots: Int,
    val maxRotationSlides: Int,
    val slides: List<SlideDto>
)

@Serializable
data class CreateTextSlideRequest(
    val id: String? = null,
    val title: String = "",
    val body: String = "",
    val lines: List<SlideLineDto>? = null,
    val durationSeconds: Int? = null,
    val enabled: Boolean? = null,
    val textAlign: String? = null
)

@Serializable
data class CreateSlideResponse(
    val id: String,
    val enabled: Boolean,
    val slideCount: Int? = null,
    val imageFile: String? = null,
    val configVersion: Long
)

@Serializable
data class PatchSlideRequest(
    val title: String? = null,
    val body: String? = null,
    val lines: List<SlideLineDto>? = null,
    val durationSeconds: Int? = null,
    val enabled: Boolean? = null,
    val textAlign: String? = null
)

@Serializable
data class PatchSlideResponse(
    val id: String,
    val title: String,
    val body: String,
    val durationSeconds: Int,
    val enabled: Boolean,
    val configVersion: Long
)

@Serializable
data class DeleteSlideResponse(
    val deleted: String,
    val slideCount: Int,
    val configVersion: Long
)

@Serializable
data class LogoUploadResponse(
    val logoFile: String,
    val configVersion: Long
)

@Serializable
data class FullConfigResponse(
    val apiVersion: Int = ApiConstants.API_VERSION,
    val configVersion: Long,
    val deviceId: String,
    val deviceName: String,
    val roomGroup: String,
    val settings: SettingsResponse,
    val slides: List<SlideDto>
)

@Serializable
data class FullConfigWriteRequest(
    val settings: SettingsPatchRequest? = null,
    val slides: List<SlideDto>? = null
)
