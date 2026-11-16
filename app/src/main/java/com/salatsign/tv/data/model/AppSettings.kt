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
    val calculationMethod: String
)

val PopularCityPresets = listOf(
    CityPreset("Jakarta, Indonesia", -6.2088, 106.8456, "SINGAPORE"),
    CityPreset("Surabaya, Indonesia", -7.2575, 112.7521, "SINGAPORE"),
    CityPreset("Bandung, Indonesia", -6.9175, 107.6191, "SINGAPORE"),
    CityPreset("Medan, Indonesia", 3.5952, 98.6722, "SINGAPORE"),
    CityPreset("Makassar, Indonesia", -5.1477, 119.4327, "SINGAPORE"),
    CityPreset("Yogyakarta, Indonesia", -7.7956, 110.3695, "SINGAPORE"),
    CityPreset("Semarang, Indonesia", -6.9667, 110.4167, "SINGAPORE"),
    CityPreset("Singapore", 1.3521, 103.8198, "SINGAPORE"),
    CityPreset("Kuala Lumpur, Malaysia", 3.1390, 101.6869, "SINGAPORE"),
    CityPreset("Makkah, Saudi Arabia", 21.4225, 39.8262, "UMM_AL_QURA"),
    CityPreset("Madinah, Saudi Arabia", 24.5247, 39.5692, "UMM_AL_QURA"),
    CityPreset("Cairo, Egypt", 30.0444, 31.2357, "EGYPTIAN"),
    CityPreset("London, United Kingdom", 51.5074, -0.1278, "MUSLIM_WORLD_LEAGUE"),
    CityPreset("New York, USA", 40.7128, -74.0060, "NORTH_AMERICA")
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
    val latitude: Double = -6.2088,
    val longitude: Double = 106.8456,
    val cityName: String = "Jakarta",
    val calculationMethodName: String = "SINGAPORE",
    val hijriOffsetDays: Int = 0,
    val prePrayerReminderEnabled: Boolean = true,
    val adhanSubuh: AdhanAudioOption = AdhanAudioOption.DefaultSubuh,
    val adhanZuhur: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
    val adhanAshr: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
    val adhanMagrib: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
    val adhanIsya: AdhanAudioOption = AdhanAudioOption.DefaultStandard,
    val mosqueName: String = "Masjid Raya",
    val mosqueSubtitle: String = "Luruskan dan Rapatkan Shaf",
    val mosqueLogoUri: String? = null,
    val mosqueIconPreset: MosqueIconPreset = MosqueIconPreset.DOME_CRESCENT,
    val mosqueFontPreset: MosqueFontPreset = MosqueFontPreset.MODERN_SANS,
    val scheduleDurationSeconds: Int = 60,
    val announcementSlides: List<AnnouncementSlide> = emptyList()
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
}
