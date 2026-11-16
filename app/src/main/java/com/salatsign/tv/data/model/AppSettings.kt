package com.salatsign.tv.data.model

data class AppSettings(
    val isMosqueMode: Boolean = false,
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
