package com.salatsign.tv.data.model

enum class ThemeMode(val displayName: String) {
    ALWAYS_DARK("Mode Gelap (OLED/Malam)"),
    ALWAYS_LIGHT("Mode Terang (Siang)"),
    AUTO_DAY_NIGHT("Otomatis (Siang Terang / Malam Gelap)")
}

enum class MosqueIconPreset(val displayName: String) {
    DOME_CRESCENT("Kubah & Bulan Sabit"),
    MINARET("Menara Masjid"),
    GEOMETRIC_STAR("Bintang 8 (Rub el Hizb)"),
    MIHRAB("Mihrab Klasik"),
    CUSTOM("Logo Kustom / USB")
}

enum class MosqueFontPreset(val displayName: String) {
    MODERN_SANS("Modern Sans Bold"),
    TRADITIONAL_SERIF("Klasik Tradisional"),
    ELEGANT_DISPLAY("Elegan Kaligrafi"),
    CLEAN_TECH("Minimalis Bersih")
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

    // Indonesian presets
    CityPreset("Jakarta, Indonesia", -6.2088, 106.8456, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Surabaya, Indonesia", -7.2575, 112.7521, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Bandung, Indonesia", -6.9175, 107.6191, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Medan, Indonesia", 3.5952, 98.6722, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Makassar, Indonesia", -5.1477, 119.4327, "SINGAPORE", "Asia/Makassar"),
    CityPreset("Yogyakarta, Indonesia", -7.7956, 110.3695, "SINGAPORE", "Asia/Jakarta"),
    CityPreset("Semarang, Indonesia", -6.9667, 110.4167, "SINGAPORE", "Asia/Jakarta"),

    // World presets
    CityPreset("Singapore", 1.3521, 103.8198, "SINGAPORE", "Asia/Singapore"),
    CityPreset("Kuala Lumpur, Malaysia", 3.1390, 101.6869, "SINGAPORE", "Asia/Kuala_Lumpur"),
    CityPreset("Makkah, Saudi Arabia", 21.4225, 39.8262, "UMM_AL_QURA", "Asia/Riyadh"),
    CityPreset("Madinah, Saudi Arabia", 24.5247, 39.5692, "UMM_AL_QURA", "Asia/Riyadh"),
    CityPreset("Cairo, Egypt", 30.0444, 31.2357, "EGYPTIAN", "Africa/Cairo"),
    CityPreset("London, United Kingdom", 51.5074, -0.1278, "MUSLIM_WORLD_LEAGUE", "Europe/London"),
    CityPreset("New York, USA", 40.7128, -74.0060, "NORTH_AMERICA", "America/New_York")
)

val MosqueNamePresets = listOf(
    "Masjid Raya",
    "Masjid Agung",
    "Masjid Al-Ikhlas",
    "Masjid At-Taqwa",
    "Masjid Al-Muhajirin",
    "Masjid Baiturrahman",
    "Masjid Al-Huda",
    "Masjid Nurul Iman",
    "Masjid Darussalam",
    "Musholla Al-Barokah"
)

val MosqueSubtitlePresets = listOf(
    "Luruskan dan Rapatkan Shaf",
    "Harap Menonaktifkan Nada Dering Ponsel",
    "Rapatkan Shaf Demi Kesempurnaan Shalat",
    "Selamat Datang Jamaah Shalat",
    "Jagalah Kebersihan & Kesucian Masjid"
)

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
    val adhanSubuh: AdhanAudioOption = AdhanAudioOption.DefaultSubuh,
    val adhanZuhur: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
    val adhanAshr: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
    val adhanMagrib: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
    val adhanIsya: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
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
    // Mosque branding settings
    val mosqueName: String = "Masjid Raya",
    val mosqueSubtitle: String = "Luruskan dan Rapatkan Shaf",
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
