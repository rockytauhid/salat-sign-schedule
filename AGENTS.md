# AGENTS.md — Android TV Islamic Prayer Times & Mosque Signage App

## Role & Objective
You are an expert Android TV & Jetpack Compose engineer. Your task is to build an offline-first Android TV application that displays Islamic prayer times (Subuh, Zuhur, Ashr, Magrib, Isya) alongside Gregorian and Hijriyah dates, supporting both **Home Mode** (interactive, notifications + Adhan audio) and **Mosque Mode** (silent, white-labeled digital signage slideshow).

Always consult `PRODUCT_SPEC.md` for feature requirements and the specialized skill files under `.agents/skills/` before writing code.

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
- **Audio Playback:** Android `MediaPlayer` with `AudioAttributes.USAGE_ALARM`
- **Background Scheduling:** `AlarmManager` (`setAlarmClock`) + `BroadcastReceiver` + `ForegroundService` + `WorkManager`

---

## Core Architectural Rules
1. **Offline-First:** Never require an internet connection to compute prayer times, convert Hijriyah dates, render Mosque slides, or play Adhan audio.
2. **Strict Mode Isolation:**
   - All mode-dependent behavior must read from a single source of truth: `SettingsRepository.appSettingsFlow`.
   - When `isMosqueMode == true`:
     - **Audio & Notifications MUST be disabled** (cancel all pending `AlarmManager` alarms).
     - **App branding MUST be hidden** (render custom Mosque Name & Logo instead).
     - **Screen Wake Lock MUST be active** (`FLAG_KEEP_SCREEN_ON`).
     - **Admin PIN Protection MUST be active** (PBKDF2-hashed PIN with lockout protection against brute force).
   - When `isMosqueMode == false`:
     - Standard app header, D-pad date navigation, 15-minute pre-prayer notifications, and exact-time Adhan audio are enabled.
3. **10-Foot UI & Overscan Safety:**
   - Apply minimum overscan padding (`horizontal = 48.dp`, `vertical = 27.dp`) on all root screens.
   - Never rely on touch events. Every interactive control must be reachable and clearly highlighted via D-pad directional keys.
4. **Manifest Requirements:**
   - Declare `<uses-feature android:name="android.software.leanback" android:required="true" />`
   - Declare `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`
   - Declare permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `READ_EXTERNAL_STORAGE` (maxSdkVersion 32), `INTERNET`. (Note: `AlarmManager.setAlarmClock` does not require `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`).