# Product Specification — SalatSign & SalatSchedule (Android TV)

> **Multi-App Split (Decision D18):** This repository ships two separate Android TV apps sharing `:core` and `:core-tv-ui`:
> - **SalatSchedule (`com.salatschedule.tv`, `:app-salatschedule`):** Interactive home prayer timetable (D-pad date browsing, 15-min reminders, Adhan audio, and Android TV screensaver).
> - **SalatSign (`com.salatsign.tv`, `:app-salatsign`):** **Strictly silent** white-label mosque digital signage kiosk (Jumu'ah, Iqamah, announcement & QR slides, Silent Screen, Urgent Notices, Admin PIN, and LAN web Phone Controller).

## 1. Prayer Times & Dual Calendar
- **Five Daily Prayers:** Subuh (Fajr), Zuhur (Dhuhr), Ashr (Asr), Magrib (Maghrib), Isya (Isha), plus Sunrise.
- **Dual Date Header:** Displays both the **Gregorian date** (e.g., *Monday, 16 November 2026*) and the **Hijriyah date** (e.g., *6 Jumada Al-Akhirah 1448 AH*) with a user-configurable offset (`-2`, `-1`, `0`, `+1`, `+2` days) and Maghrib or midnight rollover.
- **Date Navigation (SalatSchedule):** Users can press Left/Right on the D-pad (or open a date selector) to view **past** or **future** dates. A "Today" shortcut quickly resets to the current date.
- **Center Spotlight & Countdown:** When viewing the current date, the hero banner displays the active/next prayer name (with optional Arabic name) alongside a large live ticking countdown (`HH:MM:SS`) and `"Until <Next Prayer>"` indicator.

---

## 2. Two-App Comparison Matrix

| Capability | SalatSchedule (`com.salatschedule.tv`) | SalatSign (`com.salatsign.tv`) |
| :--- | :--- | :--- |
| **Audience** | Households & living rooms | Mosques, musallas & Islamic centres |
| **Header & Identity** | SalatSchedule branding | **White-Label:** Custom Mosque Name, Subtitle & Mosque Logo (zero app branding) |
| **Navigation** | Full D-pad date browsing & visible Settings/Calendar buttons | Automated slideshow loop; hidden Admin trigger (`Menu` or 3s `OK` hold) |
| **15-Min Reminder** | Optional system heads-up notification before prayer | **Disabled** (zero notifications or popups) |
| **Adhan Audio** | Plays per-prayer selected Adhan at exact prayer time | **Strictly Silent** (zero audio playback) |
| **Screensaver** | Built-in Android TV `DreamService` (Schedule, Calendar, Quran) | Continuous foreground kiosk (`FLAG_KEEP_SCREEN_ON`) |
| **Remote Control** | TV remote only (zero `INTERNET` permission) | Browser-based LAN Phone Controller with pairing & Multi-TV sync |
| **Exact Prayer Trigger** | Plays Adhan + 5-min Prayer-Time Alert Banner | Interrupts slideshow, shows 5-min visual Prayer-Time Alert Banner |

---

## 3. SalatSchedule Features (`com.salatschedule.tv`)
1. **15-Minute Pre-Prayer Reminder:** Toggleable setting that fires a clean Android TV notification 15 minutes before each prayer.
2. **Per-Prayer Adhan Audio Selection:**
   - Independent audio selector for **Fajr**, **Dhuhr**, **Asr**, **Maghrib**, and **Isha** (silent by default; supports importing custom local `.mp3`/`.ogg`/`.wav` audio files from storage/USB).
3. **Cross-App & Idle Execution:**
   - Fires accurately whether the user is inside the app, watching another app, or sitting on the Android TV launcher.
4. **Android TV Screensaver (`DreamService`):**
   - Strictly silent, dark-mode screensaver rotating Prayer Schedule, Islamic Calendar, and Offline Quran Verses with subtle minute-by-minute burn-in pixel shift; exits immediately on any key press.
5. **Islamic Calendar & Offline Quran Verses:**
   - ±3-year navigable Islamic Calendar screen (16 Hijri events) and 63 offline curated Quran verses with Arabic + English/Indonesian translations.

---

## 4. SalatSign Features (`com.salatsign.tv`)
1. **White-Label Branding:**
   - Completely strips all app titles, app icons, and watermarks on the live display.
   - Displays user-configured **Mosque Name**, **Mosque Icon/Logo** (loaded from local storage/USB or uploaded via Phone Controller), and optional **Subtitle/Tagline**.
2. **Configurable Slideshow Loop (6-Slot Rotation Limit, D20):**
   - Rotates between the Prayer Schedule (`T_schedule`, default 60s) and up to 6 active slides: text announcements (with per-row alignment, font size, and offline `qrc:` QR code rows), image posters, built-in Islamic Calendar, and Offline Quran Verses.
   - Multi-TV groups stay visually aligned via pure offline wall-clock scheduling (`epochSeconds % totalCycleSeconds`, Decision D25).
3. **Silent Exact-Time Alert Banner & Silent Screen (D19):**
   - When prayer time arrives, pauses slides and displays a 5-minute glowing visual Prayer-Time Alert Banner featuring Surah An-Nisa' 4:103.
   - Dedicated near-black Silent Screen (`"Jumu'ah in progress"` / `"Prayer in progress"`) for Friday Jumu'ah windows, optional per-prayer windows, and manual 15/30/45-min overrides.
4. **Urgent Notice (Phase 3c):**
   - Full-screen high-contrast visual alert card with automatic expiry (`Clears in M:SS` countdown), overriding schedule & slides while yielding to Silent Screen during active prayer.
5. **Hidden Admin Access & PIN Security:**
   - No on-screen settings buttons. Open Settings via long-pressing `DPAD_CENTER` (3 seconds) or pressing `KEYCODE_MENU`.
   - Protected by a PBKDF2-salted Admin PIN (auto-upgraded to `120,000` iterations) with persisted progressive lockout and 2-minute inactivity auto-close. No cloud backdoor; recovery via Android TV system app data clear.
6. **Kiosk Back Interception & Always-On:**
   - Remote `Back` is intercepted to prevent accidental exit to the TV launcher; optional Always-On boot startup auto-launches SalatSign on TV boot.
7. **Browser-Based LAN Phone Controller (D24):**
   - TV-hosted local web UI (no companion app to install) with QR/6-digit browser pairing, `MANAGE` and `NOTIFY` roles, instant revocation synced across TV groups, ECIES-encrypted secret rotation, and multi-TV group config & Urgent Notice broadcasting.
