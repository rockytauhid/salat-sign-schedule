# SalatSign & SalatSchedule — Android TV Mosque Signage & Home Prayer Times

[![Android API](https://img.shields.io/badge/API-26%2B%20(Android%208.0)-emerald.svg)](https://android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35%20(Android%2015)-blue.svg)](https://developer.android.com)
[![Compose TV](https://img.shields.io/badge/Jetpack%20Compose%20for%20TV-1.0.0-purple.svg)](https://developer.android.com/tv/compose)
[![Offline-First](https://img.shields.io/badge/Engine-100%25%20Offline-success.svg)](https://github.com/batoulapps/adhan-java)

This multi-module monorepo builds two dedicated, offline-first Android TV applications engineered for the 10-foot television experience:

- **SalatSign** (`com.salatsign.tv`, `:app-salatsign`) — a **strictly silent**, white-label Mosque digital signage kiosk with Jumu'ah sitting schedules, Iqamah display, automated announcement slideshows, Admin PIN lockout protection, and `FLAG_KEEP_SCREEN_ON`.
- **SalatSchedule** (`com.salatschedule.tv`, `:app-salatschedule`) — an interactive **Home / Personal** prayer timetable with D-pad date browsing, 15-minute pre-prayer reminders, and exact-time Adhan audio playback over other TV apps.

---

## 🌟 Key Features

### 1. Offline Astronomical Prayer Engine & Dual Calendar
- **Astronomical Precision:** Calculates exact daily prayer times using Jean Meeus algorithms (`com.batoulapps.adhan`) without requiring an active internet connection.
- **Dual Date Display:** Simultaneous Gregorian (*e.g., Tuesday, 17 November 2026*) and Hijriyah date (*e.g., 7 Jumada al-Akhirah 1448 H*).
- **Configurable Hijri Offset:** Easily adjust Hijri dates by `-2`, `-1`, `0`, `+1`, or `+2` days to match local moonsighting declarations (Rukyatul Hilal).
- **Overnight Countdown:** Accurate live countdown (`HH:MM:SS`) to the next prayer, seamlessly handling transitions across midnight to the next morning's Subuh.

### 2. Dual Mode Comparison Matrix

| Capability | Home / Personal Mode | Mosque / Digital Signage Mode |
| :--- | :--- | :--- |
| **Header & Identity** | App Title & App Logo | **White-Label:** Custom Mosque Name, Subtitle, & Mosque Logo (zero app branding) |
| **Screen Wake Lock** | Standard system sleep | **Always Active:** `FLAG_KEEP_SCREEN_ON` prevents TV from sleeping |
| **Navigation** | Full D-pad browsing & visible Settings button | Automated slideshow loop; hidden Admin trigger |
| **15-Min Reminder** | Optional heads-up notification before prayer | **Disabled** (no notifications or popups) |
| **Adhan Audio** | Plays per-prayer selected Adhan at exact time | **Silent** (zero audio playback) |
| **Background Execution** | Fires over other apps (YouTube/Netflix) or TV Home | Continuous foreground kiosk display |
| **Exact Prayer Trigger** | Plays Adhan + heads-up alert | Interrupts slideshow, shows schedule & pulses active prayer row (5 mins) |

### 3. Home Mode Highlights
- **Cross-App Adhan Audio:** Exact prayer times trigger `AdhanPlaybackService` (`ForegroundService`) using `AudioAttributes.USAGE_ALARM` with `CONTENT_TYPE_SONIFICATION`, ensuring the Adhan is clearly heard even while streaming video or using other TV apps.
- **Per-Prayer Audio Selector:** Ships silent by default (bundled audio pending licensed recordings); configure per prayer or import custom local audio files (.mp3, .ogg) from USB or TV storage.
- **15-Minute Pre-Prayer Heads-Up:** Optional system notification 15 minutes before prayer.
- **Interactive D-Pad Date Traversal:** Browse past and future prayer schedules with Left/Right remote buttons, plus a dedicated "Today" shortcut.

### 4. Mosque Mode Signage & Kiosk Highlights
- **Complete White-Labeling:** Strips all SalatSign logos and titles, displaying only your custom **Mosque Name**, **Mosque Logo** (loaded via Coil), and **Tagline/Address**.
- **Automated Slideshow Loop:** Displays the full Prayer Schedule for $T_{\text{schedule}}$ seconds (default: 60s), then smoothly transitions (`Crossfade` 800ms) through announcement posters, donation QR codes, and event flyers.
- **Silent Exact-Time Interruption:** If prayer time strikes during an announcement slide, the timer is immediately interrupted, the view returns to the prayer schedule, and the active prayer row pulses with a glowing border for 5 minutes with **zero sound**.
- **Hidden Admin Access & Security:** No on-screen settings icons. Press `KEYCODE_MENU` or hold `DPAD_CENTER` for 3 seconds to access administrative configuration.
- **Admin PIN & Lockout Protection:** Settings in Mosque Mode is gated behind a PBKDF2-hashed PIN (default `0000`). Brute-force protection locks the keypad for 30s after 5 wrong attempts (doubling up to 15 min), persisted across reboots. Settings automatically auto-closes after 2 minutes of inactivity.
- **Kiosk Mode Protection:** In Mosque digital signage mode, the remote's `Back` key is intercepted to prevent accidental exit to the TV launcher.
- **PIN Recovery Policy:** There is deliberately no network backdoor or cloud recovery. If the admin PIN is forgotten, reset it by clearing app data via TV settings: `Android TV Settings -> Apps -> SalatSign -> Clear data`, which restores the default PIN (`0000`).

---

## 📺 10-Foot UI & Android TV Guidelines

SalatSign adheres strictly to Android TV design principles:
1. **Overscan Margins:** Enforces `Modifier.overscanPadding()` (`horizontal = 48.dp`, `vertical = 27.dp`) on all root screens to prevent content clipping on legacy TV panels.
2. **Focus Visibility:** Interactive components feature `tvFocusable()` with `1.05f` scale expansion and high-contrast borders (`2.5.dp`).
3. **Typography Hierarchy:**
   - Active/Next Prayer Time: `72.sp` to `84.sp` Bold
   - Inactive Prayer Times: `36.sp` to `42.sp` SemiBold
   - Mosque Title / Header: `34.sp` to `48.sp` Bold
   - Minimum legible text: `16.sp` to `18.sp`

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 100% (Coroutines + Flow)
- **Minimum SDK:** API 26 (Android 8.0 Oreo) | **Target SDK:** API 35 (Android 15)
- **UI Framework:** Jetpack Compose for TV (`androidx.tv:tv-foundation`, `androidx.tv:tv-material`)
- **Architecture:** Clean Architecture + MVVM with Unidirectional Data Flow (`StateFlow`)
- **Prayer Engine:** `com.batoulapps.adhan:adhan`
- **Hijri Engine:** `java.time.chrono.HijrahDate`
- **Preferences:** `androidx.datastore:datastore-preferences`
- **Image Loading:** `io.coil-kt.coil3:coil-compose`
- **Audio Engine:** Android `MediaPlayer` configured with `AudioAttributes.USAGE_ALARM`
- **Background Scheduling:** `AlarmManager` (`setAlarmClock`) + `ForegroundService` + `WorkManager` (recurring daily 00:05 AM scheduler). Uses `setAlarmClock()` without requiring `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`.
- **Offline First & Network:** Core prayer calculations, Hijri calendar, Adhan audio, and Mosque digital signage work 100% offline; network access (`INTERNET`) is retained exclusively for local remote control (planned phone controller).
- **Supported Languages:** English (default, en-AU formatting), Indonesian (Bahasa Indonesia), and Arabic (العربية with Latin digits). Full runtime language switching without clearing app data.

---

## 📂 Project Architecture (Multi-Module)

```
salatsign/
├── core/                                    # Shared Android library (com.salatsign.core)
│   └── Models, PrayerCalculationEngine, PrayerDayCache, HijriCalendarEngine,
│       JumuahCalculator, PinHasher, SettingsRepository, ContentRepository,
│       AssetImporter, ImageImporter, ApiConstants (TV + future phone controller)
├── core-tv-ui/                              # Shared Compose TV UI (com.salatsign.core.tv)
│   └── SalatSignTheme, TvTypography, TvFocusModifiers, LiveClockHeader,
│       LocaleHelper, Labels, MosqueBrandComponents, StorageFilePickerScreen,
│       shared strings.xml / arrays.xml
├── app-salatsign/                           # SalatSign TV app (com.salatsign.tv)
│   └── Strictly silent Mosque digital signage (MosqueSignageScreen, Jumu'ah,
│       Iqamah, Announcement slideshow, Admin PIN, FLAG_KEEP_SCREEN_ON)
├── app-salatschedule/                       # SalatSchedule TV app (com.salatschedule.tv)
│   └── Home prayer times (HomeScreen, AdhanPlaybackService, PrayerAlarmScheduler,
│       BootAndTimeReceiver, DailyScheduleWorker, 15-min reminders)
└── docs/
    ├── DECISIONS.md                         # Confirmed decisions D1–D18
    ├── salatsign/STORE_LISTING.md           # SalatSign Play Store listing
    └── salatschedule/STORE_LISTING.md       # SalatSchedule Play Store listing
```

---

## 🧪 Running Unit Tests

Execute the comprehensive suite of unit tests verifying prayer calculations, Hijri calendar offsets, and settings persistence:

```powershell
./gradlew testDebugUnitTest
```

---

## 📦 Building for Production & Google Play Store

### 1. Build Debug APKs
```powershell
./gradlew :app-salatsign:assembleDebug :app-salatschedule:assembleDebug
```
Outputs:
- `app-salatsign/build/outputs/apk/debug/app-salatsign-debug.apk`
- `app-salatschedule/build/outputs/apk/debug/app-salatschedule-debug.apk`

### 2. Build Release Android App Bundles (.aab)
```powershell
./gradlew :app-salatsign:bundleRelease :app-salatschedule:bundleRelease
```
Outputs:
- `app-salatsign/build/outputs/bundle/release/app-salatsign-release.aab`
- `app-salatschedule/build/outputs/bundle/release/app-salatschedule-release.aab`

### 3. Signing Configuration
For Google Play distribution, provide per-app release keystores via environment variables or Gradle properties (or shared `RELEASE_STORE_*` fallback):
```properties
# SalatSign
SALATSIGN_STORE_FILE=path/to/salatsign-release.keystore
SALATSIGN_STORE_PASSWORD=your_store_password
SALATSIGN_KEY_ALIAS=your_key_alias
SALATSIGN_KEY_PASSWORD=your_key_password

# SalatSchedule
SALATSCHEDULE_STORE_FILE=path/to/salatschedule-release.keystore
SALATSCHEDULE_STORE_PASSWORD=your_store_password
SALATSCHEDULE_KEY_ALIAS=your_key_alias
SALATSCHEDULE_KEY_PASSWORD=your_key_password
```
*(Release `assemble*Release` and `bundle*Release` tasks require a valid keystore and fail fast if unconfigured).*

---

## License

SalatSign is free software: you can redistribute it and/or modify it under the terms
of the GNU General Public License, version 3 or (at your option) any later version.
See [LICENSE](LICENSE).

Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors.

The name "SalatSign" and the logo are not covered by this license; see
[TRADEMARKS.md](TRADEMARKS.md). You are welcome to fork the project under a
different name.

Third-party libraries, fonts and audio are listed in
[docs/THIRD_PARTY.md](docs/THIRD_PARTY.md) and
[docs/AUDIO_SOURCES.md](docs/AUDIO_SOURCES.md). The project does not distribute
third-party adhan recordings that lack a clear license.

