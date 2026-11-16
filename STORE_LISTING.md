# Google Play Store Submission Guide — SalatSign (Android TV)

This document contains all metadata, store listings, graphic asset guidelines, privacy policy, and Play Console compliance checklists for submitting **SalatSign** to the Google Play Store under the **Android TV** track.

---

## 📋 1. Store Listing Metadata

### App Title (Max 30 characters)
- **English:** `SalatSign: Prayer Times & TV`
- **Indonesian:** `SalatSign: Jadwal Shalat TV`

### Short Description (Max 80 characters)
- **English:** `Offline Islamic prayer times, Hijri calendar, Adhan audio & mosque TV signage.`
- **Indonesian:** `Jadwal shalat offline, kalender Hijriyah, Adhan & digital signage Android TV.`

### Full Description (Max 4,000 characters)

#### English Listing:
```text
SalatSign is a modern, offline-first Android TV application designed specifically for televisions and digital displays in homes, offices, and mosques.

Key Features:

1. 100% Offline Astronomical Prayer Engine
Accurately computes the five obligatory daily prayer times (Subuh/Fajr, Zuhur/Dhuhr, Ashr/Asr, Magrib/Maghrib, Isya/Isha) based on precise geographical coordinates using trusted astronomical calculations. No internet connection is needed.

2. Dual Calendar Header
Displays both the Gregorian date and the Islamic Hijriyah date simultaneously. Supports user-configurable Hijri day adjustments (-2 to +2 days) to conform with local moonsighting declarations.

3. Home / Personal Mode
• Next Prayer Spotlight: The upcoming prayer is rendered in a prominent card with a live, real-time ticking countdown (HH:MM:SS).
• 15-Minute Pre-Prayer Reminder: Heads-up TV notification before each prayer time.
• Foreground Adhan Playback: Plays your chosen Adhan audio at exact prayer times, clearly audible over running apps (streaming video) or on the TV Home screen.
• Audio Customization: Choose standard Adhan, specialized Subuh Adhan, silent mode, or load custom audio files from USB or local storage.
• D-Pad Remote Browsing: Easily navigate between past, present, and future prayer schedules using remote directional keys.

4. Mosque & Kiosk Digital Signage Mode
• Full White-Labeling: Automatically strips all app branding and displays your custom Mosque Name, Subtitle, and Mosque Logo.
• Automated Slideshow Loop: Rotates between the full-screen prayer schedule and announcement posters (donation QR codes, event flyers, community notices).
• Silent Exact-Time Interruption: When prayer time strikes, announcement slides are instantly interrupted, returning to the schedule screen with a prominent 5-minute visual pulsing highlight—with zero sound or popups.
• Keep Screen Awake: Integrates screen wake lock (FLAG_KEEP_SCREEN_ON) to keep signage screens continuously on.
• Hidden Admin Access: Remote Menu key or a 3-second long-press on the D-pad Center opens settings without cluttering the screen.

Optimized for 10-foot TV viewing with high-contrast D-pad focus indicators and overscan margin protection.
```

#### Indonesian Listing:
```text
SalatSign adalah aplikasi jadwal shalat dan digital signage masjid offline untuk Android TV, dirancang khusus untuk kenyamanan layar TV ruang keluarga maupun display masjid.

Fitur Utama:

1. Perhitungan Waktu Shalat 100% Offline
Menghitung waktu 5 shalat fardhu (Subuh, Zuhur, Ashr, Magrib, Isya) secara astronomis berdasarkan koordinat lokasi tanpa memerlukan koneksi internet.

2. Kalender Ganda (Masehi & Hijriyah)
Menampilkan tanggal Masehi dan Hijriyah secara bersamaan. Dilengkapi pengaturan offset hari Hijriyah (-2 hingga +2 hari) sesuai penetapan hisab/rukyat setempat.

3. Mode Rumah (Personal)
• Sorotan Shalat Berikutnya: Tampilan kartu besar dengan hitung mundur langsung (HH:MM:SS) menuju waktu shalat berikutnya.
• Pengingat 15 Menit: Notifikasi TV 15 menit sebelum masuk waktu shalat.
• Adhan Tepat Waktu: Memutar suara Adhan otomatis bahkan saat Anda sedang membuka aplikasi lain atau berada di layar utama TV.
• Pilihan Suara Per-Shalat: Pilihan Adhan standar, Adhan khusus Subuh, mode hening, atau file audio dari USB/penyimpanan lokal.
• Navigasi Remote D-Pad: Geser ke kiri/kanan untuk melihat jadwal shalat hari kemarin maupun esok hari.

4. Mode Masjid (Digital Signage)
• White-Label Penuh: Tanpa logo atau watermark aplikasi. Menampilkan Nama Masjid, Logo Masjid, dan Tagline/Alamat Anda sendiri.
• Slideshow Otomatis: Rotasi mulus antara jadwal shalat dan poster pengumuman (informasi infaq, kajian, pengumuman Jumat).
• Interupsi Hening Tepat Waktu: Begitu waktu shalat tiba saat slide pengumuman aktif, tampilan langsung kembali ke jadwal shalat dengan animasi berkedip selama 5 menit tanpa suara.
• Layar Tetap Menyala: Mencegah TV masuk mode standby/sleep secara otomatis.
• Akses Admin Tersembunyi: Tekan tombol Menu remote atau tahan tombol Tengah selama 3 detik untuk masuk ke pengaturan.
```

---

## 🎨 2. Graphic Asset Specifications

| Asset Type | Dimension | Format | Notes |
| :--- | :--- | :--- | :--- |
| **App Icon** | `512 x 512 px` | 32-bit PNG | Square, no rounded corners (Google Play applies rounding mask). |
| **Feature Graphic** | `1024 x 500 px` | JPG / 24-bit PNG | No transparency. Highlight SalatSign emerald mosque branding. |
| **TV Banner** | `1280 x 720 px` | JPG / 24-bit PNG | **Mandatory for Android TV**. Matches `16:9` ratio. |
| **TV Screenshots** | `1920 x 1080 px` | PNG / JPG | Minimum 1 screenshot (recommend 4-6) showing Home Mode, Mosque Mode, Countdown, and Settings. |

---

## 🔒 3. Privacy Policy Draft

> **SalatSign Privacy Policy**
> 
> *Last Updated: November 2026*
> 
> **1. Information Collection & Usage:**
> SalatSign does NOT collect, store, transmit, or sell any personal or identifying user data.
> - **Location Data:** If configured, geographical coordinates (latitude and longitude) are stored strictly locally on the user's Android TV device to perform offline astronomical prayer time calculations. Coordinates are never sent to external servers.
> - **Audio & Files:** Any audio files or announcement images loaded by the user are read solely from local device storage or connected USB storage and never leave the device.
> 
> **2. Third-Party Sharing:**
> SalatSign does not integrate any third-party advertising SDKs, tracking tools, or external analytics services.
> 
> **3. Contact:**
> For questions or inquiries, please contact: `support@salatsign.com`

---

## ✅ 4. Google Play Console TV Track Checklist

1. **Android TV Track Opt-In:**
   - In Google Play Console, navigate to **Release > Advanced settings > Release types**.
   - Add and enable **Android TV**.
2. **Leanback Manifest Compliance:**
   - Verified: `<uses-feature android:name="android.software.leanback" android:required="true" />` is declared.
   - Verified: `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />` is declared.
   - Verified: Activity intent filter includes `android.intent.category.LEANBACK_LAUNCHER`.
   - Verified: `android:banner="@drawable/ic_tv_banner"` is declared on `<application>`.
3. **Exact Alarm & Foreground Service Declaration:**
   - When completing the Play Store policy questionnaire, declare **Alarm Clock / Reminder** for `SCHEDULE_EXACT_ALARM` and **Media Playback** for `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
4. **App Bundle Upload:**
   - Upload `app/build/outputs/bundle/release/app-release.aab` under the TV release track.
