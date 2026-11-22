// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.model

enum class AppLanguage {
    SYSTEM,
    EN,
    ID,
    AR;

    companion object {
        val ShippedLanguages: List<AppLanguage> = listOf(EN) // Scoped to English only for launch release (F3)
        val isMultiLanguageShipped: Boolean get() = ShippedLanguages.size > 1
    }
}

enum class ThemeMode {
    ALWAYS_DARK,
    ALWAYS_LIGHT,
    AUTO_DAY_NIGHT
}

enum class JumuahMode {
    FIXED,
    AFTER_ZUHR
}

enum class FridayAdhanTiming {
    AT_ZUHR,
    AT_JUMUAH
}

enum class MosqueIconPreset {
    DOME_CRESCENT,
    MINARET,
    MIHRAB,
    CUSTOM
}

enum class MosqueFontPreset {
    MODERN_SANS,
    TRADITIONAL_SERIF,
    ELEGANT_DISPLAY,
    CLEAN_TECH
}

data class CityPreset(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val calculationMethod: String,
    val zoneId: String
)

val PopularCityPresets = listOf(
    // Australian presets first (en-AU primary)
    CityPreset("Canberra, ACT, Australia", -35.2809, 149.1300, "MUSLIM_WORLD_LEAGUE", "Australia/Sydney"),
    CityPreset("Sydney, NSW, Australia", -33.8688, 151.2093, "MUSLIM_WORLD_LEAGUE", "Australia/Sydney"),
    CityPreset("Melbourne, VIC, Australia", -37.8136, 144.9631, "MUSLIM_WORLD_LEAGUE", "Australia/Melbourne"),
    CityPreset("Brisbane, QLD, Australia", -27.4698, 153.0251, "MUSLIM_WORLD_LEAGUE", "Australia/Brisbane"),
    CityPreset("Adelaide, SA, Australia", -34.9285, 138.6007, "MUSLIM_WORLD_LEAGUE", "Australia/Adelaide"),
    CityPreset("Perth, WA, Australia", -31.9505, 115.8605, "MUSLIM_WORLD_LEAGUE", "Australia/Perth"),
    CityPreset("Hobart, TAS, Australia", -42.8821, 147.3272, "MUSLIM_WORLD_LEAGUE", "Australia/Hobart"),
    CityPreset("Darwin, NT, Australia", -12.4634, 130.8456, "MUSLIM_WORLD_LEAGUE", "Australia/Darwin"),
    CityPreset("Gold Coast, QLD, Australia", -28.0167, 153.4000, "MUSLIM_WORLD_LEAGUE", "Australia/Brisbane"),
    CityPreset("Newcastle, NSW, Australia", -32.9283, 151.7817, "MUSLIM_WORLD_LEAGUE", "Australia/Sydney"),
    CityPreset("Wollongong, NSW, Australia", -34.4278, 150.8931, "MUSLIM_WORLD_LEAGUE", "Australia/Sydney"),
    CityPreset("Geelong, VIC, Australia", -38.1499, 144.3617, "MUSLIM_WORLD_LEAGUE", "Australia/Melbourne"),
    CityPreset("Cairns, QLD, Australia", -16.9186, 145.7781, "MUSLIM_WORLD_LEAGUE", "Australia/Brisbane"),
    CityPreset("Townsville, QLD, Australia", -19.2590, 146.8169, "MUSLIM_WORLD_LEAGUE", "Australia/Brisbane"),
    CityPreset("Toowoomba, QLD, Australia", -27.5598, 151.9507, "MUSLIM_WORLD_LEAGUE", "Australia/Brisbane"),
    CityPreset("Launceston, TAS, Australia", -41.4332, 147.1441, "MUSLIM_WORLD_LEAGUE", "Australia/Hobart"),

    // New Zealand
    CityPreset("Auckland, New Zealand", -36.8485, 174.7633, "MUSLIM_WORLD_LEAGUE", "Pacific/Auckland"),
    CityPreset("Wellington, New Zealand", -41.2865, 174.7762, "MUSLIM_WORLD_LEAGUE", "Pacific/Auckland"),
    CityPreset("Christchurch, New Zealand", -43.5321, 172.6362, "MUSLIM_WORLD_LEAGUE", "Pacific/Auckland"),

    // Indonesian presets (Major cities & provincial capitals)
    CityPreset("Jakarta, Indonesia", -6.2088, 106.8456, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Surabaya, Indonesia", -7.2575, 112.7521, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Bandung, Indonesia", -6.9175, 107.6191, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Medan, Indonesia", 3.5952, 98.6722, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Semarang, Indonesia", -6.9667, 110.4167, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Yogyakarta, Indonesia", -7.7956, 110.3695, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Makassar, Indonesia", -5.1477, 119.4327, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Palembang, Indonesia", -2.9761, 104.7754, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Bandar Lampung, Indonesia", -5.4292, 105.2610, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Padang, Indonesia", -0.9471, 100.4172, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Pekanbaru, Indonesia", 0.5071, 101.4478, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Batam, Indonesia", 1.0456, 104.0305, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Banda Aceh, Indonesia", 5.5483, 95.3238, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Jambi, Indonesia", -1.6101, 103.6131, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Bengkulu, Indonesia", -3.8004, 102.2655, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Bogor, Indonesia", -6.5971, 106.8060, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Depok, Indonesia", -6.4025, 106.7942, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Tangerang, Indonesia", -6.1783, 106.6319, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Bekasi, Indonesia", -6.2383, 106.9756, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Malang, Indonesia", -7.9666, 112.6326, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Surakarta (Solo), Indonesia", -7.5755, 110.8243, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Denpasar, Bali, Indonesia", -8.6705, 115.2126, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Mataram, Indonesia", -8.5833, 116.1167, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Pontianak, Indonesia", -0.0263, 109.3425, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Banjarmasin, Indonesia", -3.3194, 114.5908, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Balikpapan, Indonesia", -1.2379, 116.8529, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Samarinda, Indonesia", -0.5022, 117.1536, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Manado, Indonesia", 1.4748, 124.8421, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Ambon, Indonesia", -3.6954, 128.1814, "SINGAPORE", "Asia/Jayapura"),
    CityPreset("Jayapura, Indonesia", -2.5916, 140.6690, "SINGAPORE", "Asia/Jayapura"),
    CityPreset("Kupang, Indonesia", -10.1772, 123.6070, "SINGAPORE", "Asia/Makassar"),

    // Malaysia, Singapore & Brunei
    CityPreset("Kuala Lumpur, Malaysia", 3.1390, 101.6869, "SINGAPORE", "Asia/Kuala_Lumpur"),
    CityPreset("Penang, Malaysia", 5.4141, 100.3288, "SINGAPORE", "Asia/Kuala_Lumpur"),
    CityPreset("Johor Bahru, Malaysia", 1.4927, 103.7414, "SINGAPORE", "Asia/Kuala_Lumpur"),
    CityPreset("Kota Kinabalu, Malaysia", 5.9804, 116.0735, "SINGAPORE", "Asia/Kuala_Lumpur"),
    CityPreset("Singapore", 1.3521, 103.8198, "SINGAPORE", "Asia/Singapore"),
    CityPreset("Bandar Seri Begawan, Brunei", 4.9031, 114.9398, "SINGAPORE", "Asia/Brunei"),

    // Middle East
    CityPreset("Makkah, Saudi Arabia", 21.4225, 39.8262, "UMM_AL_QURA", "Asia/Riyadh"),
    CityPreset("Madinah, Saudi Arabia", 24.5247, 39.5692, "UMM_AL_QURA", "Asia/Riyadh"),
    CityPreset("Riyadh, Saudi Arabia", 24.7136, 46.6753, "UMM_AL_QURA", "Asia/Riyadh"),
    CityPreset("Jeddah, Saudi Arabia", 21.4858, 39.1925, "UMM_AL_QURA", "Asia/Riyadh"),
    CityPreset("Dubai, UAE", 25.2048, 55.2708, "DUBAI", "Asia/Dubai"),
    CityPreset("Abu Dhabi, UAE", 24.4539, 54.3773, "DUBAI", "Asia/Dubai"),
    CityPreset("Sharjah, UAE", 25.3463, 55.4209, "DUBAI", "Asia/Dubai"),
    CityPreset("Doha, Qatar", 25.2854, 51.5310, "QATAR", "Asia/Qatar"),
    CityPreset("Istanbul, Turkey", 41.0082, 28.9784, "TURKEY", "Europe/Istanbul"),
    CityPreset("Ankara, Turkey", 39.9334, 32.8597, "TURKEY", "Europe/Istanbul"),
    CityPreset("Izmir, Turkey", 38.4237, 27.1428, "TURKEY", "Europe/Istanbul"),
    CityPreset("Cairo, Egypt", 30.0444, 31.2357, "EGYPTIAN", "Africa/Cairo"),
    CityPreset("Alexandria, Egypt", 31.2001, 29.9187, "EGYPTIAN", "Africa/Cairo"),

    // South Asia
    CityPreset("Karachi, Pakistan", 24.8607, 67.0011, "KARACHI", "Asia/Karachi"),
    CityPreset("Lahore, Pakistan", 31.5204, 74.3587, "KARACHI", "Asia/Karachi"),
    CityPreset("Islamabad, Pakistan", 33.6844, 73.0479, "KARACHI", "Asia/Karachi"),
    CityPreset("Mumbai, India", 19.0760, 72.8777, "KARACHI", "Asia/Kolkata"),
    CityPreset("New Delhi, India", 28.6139, 77.2090, "KARACHI", "Asia/Kolkata"),
    CityPreset("Hyderabad, India", 17.3850, 78.4867, "KARACHI", "Asia/Kolkata"),
    CityPreset("Bengaluru, India", 12.9716, 77.5946, "KARACHI", "Asia/Kolkata"),
    CityPreset("Dhaka, Bangladesh", 23.8103, 90.4125, "KARACHI", "Asia/Dhaka"),
    CityPreset("Chittagong, Bangladesh", 22.3569, 91.7832, "KARACHI", "Asia/Dhaka"),

    // Europe
    CityPreset("London, United Kingdom", 51.5074, -0.1278, "MUSLIM_WORLD_LEAGUE", "Europe/London"),
    CityPreset("Birmingham, United Kingdom", 52.4862, -1.8904, "MUSLIM_WORLD_LEAGUE", "Europe/London"),
    CityPreset("Manchester, United Kingdom", 53.4808, -2.2426, "MUSLIM_WORLD_LEAGUE", "Europe/London"),
    CityPreset("Bradford, United Kingdom", 53.7960, -1.7594, "MUSLIM_WORLD_LEAGUE", "Europe/London"),
    CityPreset("Paris, France", 48.8566, 2.3522, "MUSLIM_WORLD_LEAGUE", "Europe/Paris"),
    CityPreset("Lyon, France", 45.7640, 4.8357, "MUSLIM_WORLD_LEAGUE", "Europe/Paris"),
    CityPreset("Marseille, France", 43.2965, 5.3698, "MUSLIM_WORLD_LEAGUE", "Europe/Paris"),
    CityPreset("Berlin, Germany", 52.5200, 13.4050, "MUSLIM_WORLD_LEAGUE", "Europe/Berlin"),
    CityPreset("Munich, Germany", 48.1351, 11.5820, "MUSLIM_WORLD_LEAGUE", "Europe/Berlin"),
    CityPreset("Frankfurt, Germany", 50.1109, 8.6821, "MUSLIM_WORLD_LEAGUE", "Europe/Berlin"),

    // North America
    CityPreset("New York, USA", 40.7128, -74.0060, "NORTH_AMERICA", "America/New_York"),
    CityPreset("Los Angeles, USA", 34.0522, -118.2437, "NORTH_AMERICA", "America/Los_Angeles"),
    CityPreset("Chicago, USA", 41.8781, -87.6298, "NORTH_AMERICA", "America/Chicago"),
    CityPreset("Houston, USA", 29.7604, -95.3698, "NORTH_AMERICA", "America/Chicago"),
    CityPreset("Dearborn, MI, USA", 42.3223, -83.1763, "NORTH_AMERICA", "America/Detroit"),
    CityPreset("Toronto, Canada", 43.6532, -79.3832, "NORTH_AMERICA", "America/Toronto"),
    CityPreset("Montreal, Canada", 45.5017, -73.5673, "NORTH_AMERICA", "America/Toronto"),
    CityPreset("Vancouver, Canada", 49.2827, -123.1207, "NORTH_AMERICA", "America/Vancouver"),
    CityPreset("Calgary, Canada", 51.0447, -114.0719, "NORTH_AMERICA", "America/Edmonton"),

    // Africa
    CityPreset("Johannesburg, South Africa", -26.2041, 28.0473, "MUSLIM_WORLD_LEAGUE", "Africa/Johannesburg"),
    CityPreset("Cape Town, South Africa", -33.9249, 18.4241, "MUSLIM_WORLD_LEAGUE", "Africa/Johannesburg"),
    CityPreset("Durban, South Africa", -29.8587, 31.0218, "MUSLIM_WORLD_LEAGUE", "Africa/Johannesburg"),
    CityPreset("Casablanca, Morocco", 33.5731, -7.5898, "MUSLIM_WORLD_LEAGUE", "Africa/Casablanca"),
    CityPreset("Lagos, Nigeria", 6.5244, 3.3792, "EGYPTIAN", "Africa/Lagos"),
    CityPreset("Nairobi, Kenya", -1.2921, 36.8219, "EGYPTIAN", "Africa/Nairobi"),

    // East & Southeast Asia
    CityPreset("Tokyo, Japan", 35.6762, 139.6503, "MUSLIM_WORLD_LEAGUE", "Asia/Tokyo"),
    CityPreset("Osaka, Japan", 34.6937, 135.5023, "MUSLIM_WORLD_LEAGUE", "Asia/Tokyo"),
    CityPreset("Seoul, South Korea", 37.5665, 126.9780, "MUSLIM_WORLD_LEAGUE", "Asia/Seoul"),
    CityPreset("Bangkok, Thailand", 13.7563, 100.5018, "SINGAPORE", "Asia/Bangkok"),
    CityPreset("Manila, Philippines", 14.5995, 120.9842, "SINGAPORE", "Asia/Manila"),

    // Additional Middle East & Europe
    CityPreset("Amman, Jordan", 31.9454, 35.9284, "MUSLIM_WORLD_LEAGUE", "Asia/Amman"),
    CityPreset("Kuwait City, Kuwait", 29.3759, 47.9774, "KUWAIT", "Asia/Kuwait"),
    CityPreset("Muscat, Oman", 23.5880, 58.3829, "UMM_AL_QURA", "Asia/Muscat"),
    CityPreset("Manama, Bahrain", 26.2285, 50.5860, "QATAR", "Asia/Bahrain"),
    CityPreset("Amsterdam, Netherlands", 52.3676, 4.9041, "MUSLIM_WORLD_LEAGUE", "Europe/Amsterdam"),
    CityPreset("Madrid, Spain", 40.4168, -3.7038, "MUSLIM_WORLD_LEAGUE", "Europe/Madrid"),
    CityPreset("Rome, Italy", 41.9028, 12.4964, "MUSLIM_WORLD_LEAGUE", "Europe/Rome"),
    CityPreset("Stockholm, Sweden", 59.3293, 18.0686, "MUSLIM_WORLD_LEAGUE", "Europe/Stockholm"),
    CityPreset("Sarajevo, Bosnia", 43.8563, 18.4131, "MUSLIM_WORLD_LEAGUE", "Europe/Sarajevo")
)

const val MAX_MOSQUE_NAME_LENGTH = 36
const val MAX_MOSQUE_TAGLINE_LENGTH = 60

data class AppSettings(
    val isSetupCompleted: Boolean = false,
    val isMosqueMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.ALWAYS_DARK,
    val latitude: Double = -35.2809,
    val longitude: Double = 149.1300,
    val cityName: String = "Canberra, ACT, Australia",
    val zoneId: String = "Australia/Sydney",
    val calculationMethodName: String = "MUSLIM_WORLD_LEAGUE",
    val madhab: String = "SHAFI", // "SHAFI", "HANAFI"
    val highLatitudeRule: String? = null, // null (NONE), "MIDDLE_OF_THE_NIGHT", "SEVENTH_OF_THE_NIGHT", "TWILIGHT_ANGLE"
    val hijriOffsetDays: Int = 0,
    val hijriChangeAtMidnight: Boolean = false, // default false: advances at Maghrib
    val prePrayerReminderEnabled: Boolean = true,
    val adhanSubuh: AdhanAudioOption = AdhanAudioOption.Silent,
    val adhanZuhur: AdhanAudioOption = AdhanAudioOption.Silent,
    val adhanAshr: AdhanAudioOption = AdhanAudioOption.Silent,
    val adhanMagrib: AdhanAudioOption = AdhanAudioOption.Silent,
    val adhanIsya: AdhanAudioOption = AdhanAudioOption.Silent,
    val adhanHintDismissed: Boolean = false,
    // Per-prayer offsets (-30..+30 mins)
    val offsetSubuh: Int = 0,
    val offsetSunrise: Int = 0,
    val offsetZuhur: Int = 0,
    val offsetAshr: Int = 0,
    val offsetMagrib: Int = 0,
    val offsetIsya: Int = 0,
    // Per-prayer Iqamah (minutes after adhan, 0 = disabled)
    val iqamahSubuh: Int = 0,
    val iqamahZuhur: Int = 0,
    val iqamahAshr: Int = 0,
    val iqamahMagrib: Int = 0,
    val iqamahIsya: Int = 0,
    // Mosque Mode Quiet-by-default (P4.7)
    val mosqueModeAdhanEnabled: Boolean = false,
    val mosqueModeRemindersEnabled: Boolean = false,
    val showStrikeHighlight: Boolean = true,
    // Jumu'ah (Friday) settings - Mosque Mode only (D16)
    val jumuahMode: JumuahMode = JumuahMode.FIXED,
    val jumuahTimes: List<String> = emptyList(),
    val fridayAdhanTiming: FridayAdhanTiming = FridayAdhanTiming.AT_ZUHR,
    // Localization & Display Options
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val is24HourFormat: Boolean = false,
    val showArabicPrayerNames: Boolean = true,
    // Mosque branding settings
    val mosqueName: String = "",
    val mosqueSubtitle: String = "",
    val mosqueLogoUri: String? = null,
    val mosqueIconPreset: MosqueIconPreset = MosqueIconPreset.DOME_CRESCENT,
    val mosqueFontPreset: MosqueFontPreset = MosqueFontPreset.MODERN_SANS,
    val scheduleDurationSeconds: Int = 60,
    // Security / Admin PIN
    val homeModePinRequired: Boolean = false,
    val hasCustomPin: Boolean = false,
    val pinSaltHex: String = "",
    val pinHashHex: String = "",
    val pinFailedAttempts: Int = 0,
    val pinLockedUntilEpochMillis: Long = 0L,
    val defaultPinPromptDismissed: Boolean = false
) {
    fun adhanFor(prayer: Prayer): AdhanAudioOption {
        return when (prayer) {
            Prayer.SUBUH -> adhanSubuh
            Prayer.ZUHUR -> adhanZuhur
            Prayer.ASHR -> adhanAshr
            Prayer.MAGRIB -> adhanMagrib
            Prayer.ISYA -> adhanIsya
        }
    }

    fun hasConfiguredAudibleAdhan(): Boolean =
        adhanSubuh.type != AdhanAudioType.SILENT ||
        adhanZuhur.type != AdhanAudioType.SILENT ||
        adhanAshr.type != AdhanAudioType.SILENT ||
        adhanMagrib.type != AdhanAudioType.SILENT ||
        adhanIsya.type != AdhanAudioType.SILENT

    fun offsetFor(prayer: Prayer): Int {
        return when (prayer) {
            Prayer.SUBUH -> offsetSubuh
            Prayer.ZUHUR -> offsetZuhur
            Prayer.ASHR -> offsetAshr
            Prayer.MAGRIB -> offsetMagrib
            Prayer.ISYA -> offsetIsya
        }
    }

    fun iqamahFor(prayer: Prayer): Int {
        return when (prayer) {
            Prayer.SUBUH -> iqamahSubuh
            Prayer.ZUHUR -> iqamahZuhur
            Prayer.ASHR -> iqamahAshr
            Prayer.MAGRIB -> iqamahMagrib
            Prayer.ISYA -> iqamahIsya
        }
    }

    fun iqamahOffsets(): Map<Prayer, Int> {
        return mapOf(
            Prayer.SUBUH to iqamahSubuh,
            Prayer.ZUHUR to iqamahZuhur,
            Prayer.ASHR to iqamahAshr,
            Prayer.MAGRIB to iqamahMagrib,
            Prayer.ISYA to iqamahIsya
        )
    }

    fun prayerConfig(): PrayerConfig = PrayerConfig.fromAppSettings(this)
}
