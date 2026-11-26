# Google Play Store Submission Guide — SalatSchedule (Android TV Home Prayer Times)

This document contains all metadata, store listings, graphic asset guidelines, and privacy policy for submitting **SalatSchedule** (`com.salatschedule.tv`) to the Google Play Store under the **Android TV** track.

---

## 1. Store Listing Metadata

- **Application ID:** `com.salatschedule.tv`
- **Category:** Lifestyle
- **Audio & Notifications:** Silent by default (import custom Adhan from USB/local storage) + optional 15-minute pre-prayer reminders

### App Title (Max 30 characters)
- **English:** `SalatSchedule: TV Prayer Times`

### Short Description (Max 80 characters)
- **English:** `Offline Islamic prayer times, screensaver, calendar & Adhan for Android TV.`

### Full Description (Max 4,000 characters)
```text
SalatSchedule is a clean, 100% offline Android TV prayer timetable and screensaver designed for households and living rooms.

Key Features:

1. 100% Offline Astronomical Prayer Engine
Accurately calculates daily prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) and Sunrise based on your city and preferred calculation method & madhab, with full daylight saving (DST) support and Australian city presets first. Zero internet permission required.

2. Dual Gregorian & Hijri Calendar + Islamic Events
• Displays Gregorian and Hijri dates side by side with Maghrib rollover by default (or midnight rollover) and configurable day offsets (-2 to +2 days).
• Built-in ±3-year navigable Islamic Calendar covering 16 major Hijri dates with an optional next-event summary on the home screen.

3. Live Countdown, Prayer-Time Alert Banner & D-Pad Browsing
• Center Spotlight: Displays the active/next prayer (with optional Arabic name) alongside a large live ticking countdown (HH:MM:SS) and "until <Next Prayer>" indicator.
• Prayer-Time Alert Banner: 5-minute glowing visual banner featuring Surah An-Nisa' 4:103 when prayer time arrives.
• Remote Date Navigation: Browse yesterday, today, and upcoming dates effortlessly with Left/Right D-pad keys and a one-click Today shortcut.

4. Built-In Android TV Screensaver (DreamService)
• Strictly silent, dark-mode screensaver rotating up to three slides while your TV is idle: Prayer Schedule, Islamic Calendar, and 63 curated offline Quran verses (Arabic + English/Indonesian translation).
• Subtle minute-by-minute pixel shift to help protect TV panels from burn-in; closes immediately on any remote key press.

5. 15-Minute Pre-Prayer Reminders & Custom Adhan Audio
• Heads-Up Reminders: Optional TV notification 15 minutes before each prayer time.
• Custom Adhan Playback & Preview: Ships silent by default. Choose the built-in Beep-Beep alert or import your preferred Adhan audio files (.mp3, .wav, .m4a, .ogg) from a USB drive or local TV storage per prayer, and preview them in Settings with Test Adhan Audio. Adhan playback pauses other media apps and follows your TV remote's volume (a muted TV remains silent).

6. Designed for Android TV
10-foot high-contrast UI, overscan-safe margins, 12/24-hour clock format (12-hour default), Dark, Light, or Auto Day/Night themes, and English, Indonesian, and Arabic support.
```

---

## 2. Graphic Asset Specifications

| Asset Type | Dimension | Format | Notes |
| :--- | :--- | :--- | :--- |
| **App Icon** | `512 x 512 px` | 32-bit PNG | Square, no rounded corners. |
| **Feature Graphic** | `1024 x 500 px` | JPG / 24-bit PNG | Highlight SalatSchedule home TV prayer timetable. |
| **TV Banner** | `1280 x 720 px` | JPG / 24-bit PNG | **Mandatory for Android TV** (`16:9`). |
| **TV Screenshots** | `1920 x 1080 px` | PNG / JPG | Reuse `docs/screenshots/salatschedule/01-main-schedule.png` through `08-screensaver-quran.png` (main schedule, Islamic calendar, settings, Adhan/reminders, screensaver settings, and all 3 screensaver slides). |

---

## 3. Privacy Policy Summary & Play Data Safety

**SalatSchedule** (`com.salatschedule.tv`) operates **100% offline** with **zero** `INTERNET` permission, zero analytics, zero telemetry, and zero personal data collection. All preferences and imported custom audio files remain strictly in app-private storage on the television.

**Google Play Data Safety Answers:**
- **Does your app collect or share any of the required user data types?** `No`
