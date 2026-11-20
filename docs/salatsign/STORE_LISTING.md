# Google Play Store Submission Guide — SalatSign (Android TV Mosque Digital Signage)

This document contains all metadata, store listings, graphic asset guidelines, and privacy policy for submitting **SalatSign** (`com.salatsign.tv`) to the Google Play Store under the **Android TV** track.

---

## 1. Store Listing Metadata

- **Application ID:** `com.salatsign.tv`
- **Category:** Lifestyle / Productivity (Digital Signage)
- **Audio & Notifications:** **Strictly silent** (no audio, no notification popups, no background alarm services)

### App Title (Max 30 characters)
- **English:** `SalatSign: Mosque TV Signage`

### Short Description (Max 80 characters)
- **English:** `Silent offline mosque prayer timetable & announcement signage for Android TV.`

### Full Description (Max 4,000 characters)
```text
SalatSign is a dedicated, strictly silent, offline-first Android TV digital signage application engineered for mosques, musallas, and Islamic centres.

Key Features:

1. 100% Offline Astronomical Prayer Engine
Accurately computes the five daily prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) using trusted astronomical calculations with full daylight saving (DST) handling and Australian city presets first. No internet connection is required.

2. Mosque-Specific Scheduling (Jumu'ah & Iqamah)
• Custom Jumu'ah Sittings: Configure 1 to 3 Friday sittings using fixed wall-clock times or minutes after Dhuhr, with automatic 53-Friday DST validation and a clear "Zuhr begins …" secondary display.
• Iqamah Display: Configure per-prayer Iqamah delays (minutes after Adhan) shown clearly alongside prayer times.

3. Complete White-Label Branding
Automatically displays your custom Mosque Name, Subtitle/Tagline, and custom Mosque Logo (imported from USB or local TV storage) with zero distracting app watermarks.

4. Automated Announcement Slideshow
Rotates smoothly between the full-screen prayer schedule and announcement slides (event flyers, donation QR codes, community notices).

5. Strictly Silent Exact-Time Visual Strike
SalatSign never plays audio or displays system notification popups. When prayer time (or a Jumu'ah sitting) strikes during an announcement slide, the slideshow pauses immediately and returns to the schedule with a 5-minute glowing visual highlight.

6. Kiosk Protection & Admin PIN
• Always-On Display: Keeps the TV screen continuously awake (FLAG_KEEP_SCREEN_ON).
• Hidden Admin Access: Press the Menu button or hold the D-pad Center (OK) button for 3 seconds to open Settings.
• Admin PIN Protection: Settings are protected by a salted PBKDF2 PIN with progressive lockout protection.

Built for 10-foot viewing with overscan-safe margins and full D-pad remote control navigation.
```

---

## 2. Graphic Asset Specifications

| Asset Type | Dimension | Format | Notes |
| :--- | :--- | :--- | :--- |
| **App Icon** | `512 x 512 px` | 32-bit PNG | Square, no rounded corners. |
| **Feature Graphic** | `1024 x 500 px` | JPG / 24-bit PNG | Highlight SalatSign emerald mosque signage branding. |
| **TV Banner** | `1280 x 720 px` | JPG / 24-bit PNG | **Mandatory for Android TV** (`16:9`). |
| **TV Screenshots** | `1920 x 1080 px` | PNG / JPG | Mosque schedule, Jumu'ah sittings, announcement slide, and Admin Settings. |

---

## 3. Privacy Policy Summary

**SalatSign** (`com.salatsign.tv`) operates 100% offline for all prayer calculations, calendar conversions, and digital signage slideshows. It collects **zero** personal data, analytics, telemetry, or location tracking. All settings, logos, and announcement images remain strictly in app-private storage on the television.
