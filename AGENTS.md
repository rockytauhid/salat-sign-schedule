# AGENTS.md — Android TV Islamic Prayer Times & Mosque Signage Monorepo

## Role & Objective
You are an expert Android TV & Jetpack Compose engineer. This repository is a multi-module monorepo (`:core`, `:core-tv-ui`, `:app-salatsign`, `:app-salatschedule`) that builds two offline-first Android TV applications:
- **SalatSign (`com.salatsign.tv`, `:app-salatsign`)** — strictly silent, white-labeled Mosque digital signage kiosk (Jumu'ah sittings, Iqamah display, announcement slideshows, Admin PIN, `FLAG_KEEP_SCREEN_ON`).
- **SalatSchedule (`com.salatschedule.tv`, `:app-salatschedule`)** — interactive Home prayer timetable (D-pad date browsing, 15-minute pre-prayer reminders, exact-time Adhan audio over other apps).

Always consult `PRODUCT_SPEC.md`, `docs/DECISIONS.md` (especially `D18`), and the specialized skill files under `.agents/skills/` before writing code.

---

## Tech Stack & Dependencies
- **Language:** Kotlin 100% (Coroutines + Flow)
- **Minimum SDK:** API 26 (Android 8.0) | **Target SDK:** API 35 (Android 15)
- **UI Framework:** Jetpack Compose for TV (`androidx.tv:tv-foundation`, `androidx.tv:tv-material`)
- **Architecture:** MVVM + Clean Architecture with Unidirectional Data Flow (`StateFlow`)
- **Prayer Time Calculation:** `com.batoulapps.adhan:adhan` (offline astronomical calculations)
- **Hijri Calendar:** `java.time.chrono.HijrahDate` (with configurable day offset `-2` to `+2`)
- **Local Storage:** `androidx.datastore:datastore-preferences`
- **Image Loading:** `io.coil-kt.coil3:coil-compose` (for Mosque logos and signage slides)
- **Audio Playback (`:app-salatschedule` only):** Android `MediaPlayer` with `AudioAttributes.USAGE_ALARM`
- **Background Scheduling (`:app-salatschedule` only):** `AlarmManager` (`setAlarmClock`) + `BroadcastReceiver` + `ForegroundService` + `WorkManager`

---

## Core Architectural Rules
1. **Offline-First:** Never require an internet connection to compute prayer times, convert Hijriyah dates, render Mosque slides, or play Adhan audio.
2. **Strict App Separation (Decision D18):**
   - **`:app-salatsign` (Mosque Digital Signage):**
     - **Strictly silent:** Zero audio, alarm, notification, or foreground service code/permissions.
     - **App branding MUST be hidden** on the live display (render custom Mosque Name & Logo instead).
     - **Screen Wake Lock MUST be active** (`FLAG_KEEP_SCREEN_ON`).
     - **Admin PIN Protection MUST be active** (PBKDF2-hashed PIN with lockout protection against brute force).
   - **`:app-salatschedule` (Home Prayer Times):**
     - Standard app header, D-pad date navigation, 15-minute pre-prayer notifications, and exact-time Adhan audio (stop immediately via `Back`/`DPAD_CENTER`).
     - No `INTERNET` permission, no Admin PIN, no Mosque signage/Jumu'ah/Iqamah.
3. **10-Foot UI & Overscan Safety:**
   - Apply minimum overscan padding (`horizontal = 48.dp`, `vertical = 27.dp`) on all root screens.
   - Never rely on touch events. Every interactive control must be reachable and clearly highlighted via D-pad directional keys.
4. **Manifest Requirements:**
   - Both apps declare `<uses-feature android:name="android.software.leanback" android:required="true" />`, `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`, and `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`).
   - `:app-salatsign` declares `INTERNET` (reserved for planned local phone controller) and NO audio/notification/alarm permissions.
   - `:app-salatschedule` declares `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK` (and NO `INTERNET`, `SCHEDULE_EXACT_ALARM`, or `USE_EXACT_ALARM`).
5. **Cross-App Visual & Design Parity (`:app-salatsign` ↔ `:app-salatschedule`):**
   - Both apps MUST share the exact same visual design system: color palette (always use `MaterialTheme.colorScheme.primary`/`secondary`/`surface`/`surfaceVariant`/`onSurface` — never hardcode `AccentGold` or `AccentEmerald` directly for text/icons), typography scale, proportions, margins, overscan padding, single-border focus styling (`tvSelectableCardBorder`/`tvSelectableSurfaceBorder`), and controlled focus scaling (`tvCardScale`/`tvSurfaceScale`).
   - Whenever a layout, spacing, color, typography, focus-behavior, or shared copy change is made to a screen in one app (Schedule, Settings, or Setup Wizard), apply the matching change to the corresponding screen in the other app—and prefer extracting shared UI into `:core-tv-ui` whenever possible so the two apps never drift.
6. **Phone Controller Parity (`:app-salatsign` TV Settings ↔ Phone Controller Web UI):**
   - Whenever a setting, validation rule, default value, or day-to-day Mosque admin control is added, renamed, reordered, or changed in `:app-salatsign` TV Settings (`MosqueSettingsScreen`, `MosqueSettingsPanes`, `SharedSettingsPanes`, `AddTextSlideDialog`, `ImageSlideEditorDialog`), update `PhoneControllerHtml.kt` and `SalatSignLocalServer.kt` in the same change so the Local Phone Controller stays 100% up to date with TV Settings.