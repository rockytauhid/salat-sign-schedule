package com.salatsign.tv.data.model

import java.time.ZoneId

data class PrayerConfig(
    val latitude: Double,
    val longitude: Double,
    val zoneId: ZoneId,
    val methodName: String = "SINGAPORE",
    val madhab: String = "SHAFI", // "SHAFI", "HANAFI"
    val highLatitudeRule: String? = null, // null (NONE), "MIDDLE_OF_THE_NIGHT", "SEVENTH_OF_THE_NIGHT", "TWILIGHT_ANGLE"
    val offsetsMinutes: Map<Prayer, Int> = emptyMap(),
    val sunriseOffsetMinutes: Int = 0
) {
    companion object {
        fun fromAppSettings(settings: AppSettings): PrayerConfig {
            val zone = runCatching { ZoneId.of(settings.zoneId) }.getOrDefault(ZoneId.of("Australia/Sydney"))
            val offsets = mapOf(
                Prayer.SUBUH to settings.offsetSubuh,
                Prayer.ZUHUR to settings.offsetZuhur,
                Prayer.ASHR to settings.offsetAshr,
                Prayer.MAGRIB to settings.offsetMagrib,
                Prayer.ISYA to settings.offsetIsya
            )
            return PrayerConfig(
                latitude = settings.latitude,
                longitude = settings.longitude,
                zoneId = zone,
                methodName = settings.calculationMethodName,
                madhab = settings.madhab,
                highLatitudeRule = settings.highLatitudeRule,
                offsetsMinutes = offsets,
                sunriseOffsetMinutes = settings.offsetSunrise
            )
        }
    }
}
