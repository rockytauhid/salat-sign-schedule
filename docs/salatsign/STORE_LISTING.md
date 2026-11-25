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

1. Offline Astronomical Prayer Engine & Dual Calendar
Accurately computes the five daily prayer times (Fajr, Dhuhr, Asr, Maghrib, Isha) and Sunrise with full daylight saving (DST) handling, configurable Hijri offsets (-2 to +2 days), and Australian city presets first. Core prayer calculations and slideshows work 100% offline.

2. Mosque-Specific Scheduling (Jumu'ah & Iqamah)
• Custom Jumu'ah Sittings: Configure 1 to 3 Friday sittings using fixed wall-clock times or minutes after Dhuhr, with automatic 53-Friday DST validation.
• Iqamah Display: Configure per-prayer Iqamah delays (minutes after Adhan) shown clearly alongside prayer times.

3. Complete White-Label Branding
Displays your custom Mosque Name, Subtitle/Tagline, and custom Mosque Logo (imported from USB/TV storage or uploaded via the Phone Controller) with zero distracting app watermarks.

4. Automated Announcement, QR Code & Built-In Slideshow
• Rotates smoothly between the Prayer Schedule and up to 6 active slides: formatted text announcements, inline offline QR codes (qrc: lines for donations/Wi-Fi/registration), image posters, built-in Islamic Calendar events, and 63 curated offline Quran verses (Arabic + English/Indonesian).
• Multi-TV Wall-Clock Sync keeps multiple screens in the same mosque transitioning slides in step with zero network chatter.

5. Strictly Silent Exact-Time Banner, Silent Screen & Urgent Notice
• Prayer-Time Alert Banner: Pauses slides for 5 minutes when prayer time arrives with a glowing visual highlight and Surah An-Nisa' 4:103.
• Silent Screen: Calm near-black screen ("Jumu'ah in progress" / "Prayer in progress") with burn-in pixel shift during prayer windows or manual 15/30/45-minute overrides.
• Urgent Notice: High-contrast full-screen visual alert with automatic expiry countdown that yields to Silent Screen during active prayer.

6. Browser-Based Local Phone Controller & Multi-TV Groups
• Manage slides, Jumu'ah, Iqamah, Silent Screen, and Urgent Notices from any phone or laptop browser on the mosque Wi-Fi—no companion app to install.
• QR/code device pairing with Manage & Notify roles, instant revocation, and signed TV-to-TV group sync.

7. Kiosk Protection & Admin PIN
• Keeps the TV screen awake (FLAG_KEEP_SCREEN_ON) with optional Always-On boot launch.
• Hidden Admin Access (Menu key or 3-second OK hold) protected by a PBKDF2-hashed Admin PIN with progressive lockout and 2-minute inactivity auto-close.

Built for 10-foot viewing with overscan-safe margins and full D-pad remote control navigation.
```

---

## 2. Graphic Asset Specifications

| Asset Type | Dimension | Format | Notes |
| :--- | :--- | :--- | :--- |
| **App Icon** | `512 x 512 px` | 32-bit PNG | Square, no rounded corners. |
| **Feature Graphic** | `1024 x 500 px` | JPG / 24-bit PNG | Highlight SalatSign emerald mosque signage branding. |
| **TV Banner** | `1280 x 720 px` | JPG / 24-bit PNG | **Mandatory for Android TV** (`16:9`). |
| **TV Screenshots** | `1920 x 1080 px` | PNG / JPG | Reuse `docs/screenshots/salatsign/01-main-schedule.png` through `09-urgent-notice.png` (main schedule, PIN dialog, settings, Islamic calendar, Quran verse, donation QR image/text, announcement, urgent notice). |

---

## 3. Privacy Policy Summary & Play Data Safety

**SalatSign** (`com.salatsign.tv`) performs all prayer time calculations, Hijri calendar conversions, Quran verse rendering, and signage slideshows **100% offline** on the TV. It collects **zero** personal data, analytics, telemetry, ads, or cloud tracking, and never transmits data to external servers.

The `INTERNET` permission is declared solely for the optional **Local Phone Controller** and **Multi-TV Group Sync**, which serve a local web interface and signed peer-to-peer sync strictly over your local mosque Wi-Fi/LAN. All settings, logos, announcement images, and hashed pairing tokens remain strictly in app-private storage on the television.

**Google Play Data Safety Answers:**
- **Does your app collect or share any of the required user data types?** `No`
- **Is all of the user data collected by your app encrypted in transit?** N/A (no user data is collected or transmitted off the local network)
- **Can users request that data be deleted?** N/A (all configuration lives locally on the TV and is erased via Android TV App Storage → Clear Data)
