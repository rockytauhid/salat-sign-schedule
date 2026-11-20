# SalatSign — Manual Test Procedures

This document tracks test cases that require real Android TV hardware or emulator interaction (e.g., audio focus across third-party apps, physical D-pad remote controls, and HDMI CEC).

---

## MT-1.4: Adhan Audio Focus & Stop Control Verification

**Objective:** Verify that Adhan playback transiently gains audio focus over media apps (e.g., YouTube, Netflix), abandons focus when playback completes or is stopped, and can be immediately dismissed using the TV remote control.

### Test Setup
- Device: Android TV running API 26+ (e.g., Sony Bravia KD-55X9300D or Android TV Emulator).
- Audio App: YouTube, Netflix, or Spotify running in the background playing continuous media.
- App: **SalatSchedule** (`com.salatschedule.tv`) with Adhan Audio enabled for upcoming prayer.

### Test Procedure A: Audio Focus Preemption
1. Open a media app (e.g., YouTube) and start playing video with active sound.
2. Trigger an Adhan alarm (either wait for exact prayer time or send an explicit test broadcast):
   ```bash
   adb shell am broadcast -a com.salatschedule.tv.action.PLAY_ADHAN -n com.salatschedule.tv/.service.PrayerAlarmReceiver --es extra_prayer_name "MAGRIB" --es extra_adhan_type "DEFAULT_STANDARD"
   ```
3. **Verify:**
   - Background media in YouTube immediately pauses (or ducks) as SalatSchedule requests `AUDIOFOCUS_GAIN_TRANSIENT`.
   - Adhan audio plays clearly through the TV speakers.
4. Allow Adhan playback to complete naturally.
5. **Verify:**
   - SalatSchedule abandons audio focus upon completion.
   - Background media (YouTube) automatically resumes playback.

### Test Procedure B: TV Remote Immediate Stop Control
1. Trigger Adhan playback while on the home screen or inside the SalatSchedule app.
2. While Adhan is actively playing, press the **Back** key or **DPAD_CENTER (OK)** key on the remote.
3. **Verify:**
   - Adhan audio stops immediately.
   - Foreground service notification is dismissed.
   - Any ducked/paused background media resumes without delay.

---

## MT-2.3: USB Drive Asset Import Verification

**Objective:** Verify that custom adhan audio, mosque logos, and announcement slides can be imported from a physical USB flash drive on an Android TV device using the D-pad remote control, downscaled safely, and played/rendered without crashes or OOM.

### Test Setup
1. Format a standard USB flash drive as FAT32 or exFAT.
2. Prepare test files on the USB drive:
   - `test_poster_4k.jpg`: High-resolution poster (e.g., 3840x2160, ~5MB).
   - `mosque_logo_large.png`: Mosque emblem image (1024x1024).
   - `custom_adhan_subuh.mp3`: Adhan audio file (~3MB, ~3 minutes).
   - `oversized_adhan.mp3`: Audio file larger than 10MB (or >10 minutes).
3. Insert the USB drive into the TV's USB port.
4. Launch SalatSign on Android TV.

### Test Procedure A: Import Announcement Slide Image
1. Switch to Mosque Mode or open Settings -> **Pengaturan Durasi Jadwal & Poster Slide**.
2. Navigate to **Tambah Slide Gambar (USB/TV)** with the remote control and press `OK`.
3. In the Storage File Picker screen:
   - Select the **USB Flashdisk** tab.
   - Navigate down the folder list to find `test_poster_4k.jpg`.
   - Press `OK` on `test_poster_4k.jpg`.
4. **Verify:**
   - The file is downscaled from 4K to Full HD (1920x1080) and copied to app-private storage (`filesDir/content/images/`).
   - The new slide appears in the slides list with duration 15s.
   - Return to Mosque Mode; verify the slide displays smoothly in the signage rotation with no memory warnings or UI lag.

### Test Procedure B: Import Custom Mosque Logo
1. Open Settings -> **Pilihan Ikon / Simbol Masjid**.
2. Select preset **Logo Kustom / USB**.
3. Select **Pilih Logo Masjid dari USB Flashdisk** and press `OK`.
4. Browse USB drive and select `mosque_logo_large.png`.
5. **Verify:**
   - Logo is downscaled to 512x512 and saved to `filesDir/content/logo/`.
   - The custom logo immediately updates in the live header preview.

### Test Procedure C: Import Custom Adhan & Constraint Verification
1. Switch to Home Mode -> **Pengaturan Suara Adhan**.
2. On any prayer (e.g. Subuh), navigate to **USB / File** button and press `OK`.
3. Select `oversized_adhan.mp3` (>10MB).
4. **Verify:**
   - Import is rejected.
   - Error banner displays: *"Ukuran file audio melebihi batas 10MB"*.
   - No file is created in `filesDir/content/audio/`.
5. Now select `custom_adhan_subuh.mp3` (valid).
6. **Verify:**
   - Import succeeds; the prayer's adhan updates to the custom audio.
   - Trigger Adhan alarm via broadcast: verify the custom audio plays clearly through TV speakers.

---

## MT-3.1: Admin PIN, Lockout Protection & Inactivity Auto-Dismiss

**Objective:** Verify that Mosque Mode (and optionally Home Mode) gates settings access behind a secure PBKDF2-hashed PIN, locks out brute-force attempts with exponential backoff (persisted across restarts), and automatically dismisses settings after 2 minutes of TV inactivity.

### Test Setup
- Device: Android TV running API 26+ (e.g. Sony Bravia or Emulator).
- SalatSign configured in Mosque Mode.

### Test Procedure A: PIN Verification & Change
1. Launch SalatSign in Mosque Mode.
2. Press `MENU` or hold `DPAD_CENTER` for 3 seconds to request Settings.
3. Verify the `PinDialog` appears with masked dots (`● ● ● ●`) and D-pad numeric keypad (1..9, CLEAR, 0, OK).
4. Enter default PIN `0000` using the D-pad keypad or remote number buttons -> Settings screen opens.
5. Verify the one-time warning dialog appears: *"Anda masih menggunakan PIN bawaan (0000). Ganti sekarang?"*.
6. Select **Ganti PIN Sekarang** -> Enter new PIN `5678` -> Confirm `5678`.
7. Close Settings and return to Mosque signage.
8. Trigger Settings open again:
   - Enter `0000` -> Rejected (*"PIN salah. Sisa percobaan: 4"*).
   - Enter `5678` -> Accepted; Settings opens.

### Test Procedure B: Lockout Escalation & Persistence
1. Trigger the PIN dialog.
2. Deliberately enter incorrect PINs 5 consecutive times.
3. **Verify:**
   - On the 5th failure, screen displays: *"PIN Terkunci! Coba lagi dalam 30 detik"* in red.
   - Keypad and remote number inputs are disabled during countdown.
4. Force-stop the app via `adb shell am force-stop com.salatsign.tv` and re-launch.
5. Trigger Settings open -> **Verify:** Lockout timestamp is persisted in DataStore; remaining lockout seconds are still enforced.
6. Wait for timer to reach 0; enter wrong PIN again -> **Verify:** Lockout escalates to 60 seconds (and doubles up to 15 minutes on subsequent failures).
7. Enter correct PIN -> **Verify:** Failed attempts counter resets to 0.

### Test Procedure C: 2-Minute Inactivity Auto-Dismiss
1. Open Settings in Mosque Mode.
2. Place the remote control down and leave untouched.
3. **Verify:** After exactly 2 minutes (120 seconds), Settings automatically closes and returns to the Mosque digital signage slideshow.

---

## MT-3.2: Reliable Admin Gesture & Kiosk Key Handling

**Objective:** Verify that the 3-second `DPAD_CENTER` hold gesture works reliably on remotes that do not send repeated `ACTION_DOWN` events, `KEYCODE_MENU` directly opens settings, and `KEYCODE_BACK` is intercepted in Mosque Mode to prevent accidental exit from the public kiosk display.

### Test Setup
- Device: Android TV with standard remote control or `adb shell input keyevent`.
- SalatSign running in Mosque Mode with setup completed.

### Test Procedure A: 3-Second Hold Gesture (No Repeat Dependency)
1. While Mosque digital signage is displaying, press and hold `DPAD_CENTER` (or `adb shell input keyevent --longpress 23`).
2. Hold down continuously for 3 seconds.
3. **Verify:** The 3-second coroutine timer fires and opens the Admin PIN dialog.
4. Release before 3 seconds (quick click <1s) -> **Verify:** Gesture is cancelled; signage continues uninterrupted.

### Test Procedure B: Remote Menu Key Shortcut
1. While on Mosque signage, press `KEYCODE_MENU` (or `adb shell input keyevent 82`).
2. **Verify:** Admin PIN dialog opens immediately.

### Test Procedure C: Kiosk Mode Back-Key Interception
1. While on Mosque digital signage (Settings closed), press `KEYCODE_BACK` (or `adb shell input keyevent 4`).
2. **Verify:** The back key is intercepted and consumed. The application **does not** minimize or exit to Android TV Launcher.
3. Open Settings -> press `KEYCODE_BACK` -> **Verify:** Back key closes Settings and returns to Mosque digital signage.

---

## MT-5.1: Baseline Hardware Verification — Android 8.0 (API 26)

**Baseline Device:** Sony Bravia KD-55X9300D (Runs Android 8.0.0 Oreo, API 26).
**Reference Emulator:** Android Studio AVD — `Android TV (1080p)`, System Image: `Android 8.0 ("Oreo") | armeabi-v7a or x86 | API Level 26`.

### API 26 Platform Constraints & Behavioral Invariants:
1. **Zero Headroom for minSdk:** `minSdk = 26` must be strictly maintained. Every API call must exist on API 26 or be guarded with `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES...)`. Lint enforces `NewApi` as an error.
2. **Background Execution Limits (Android 8.0 Oreo):**
   - Manifest-declared receivers do not fire for most implicit broadcasts.
   - `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, and `TIMEZONE_CHANGED` are officially on Android's implicit broadcast exemption list and invoke `BootAndTimeReceiver`.
   - Resilience redundancy: P1.2 chained alarms and daily `DailyScheduleWorker` (WorkManager) provide automatic fallback recovery if the device time changes.
3. **Foreground Service 5-Second Grace Window:**
   - On API 26+, `ContextCompat.startForegroundService()` must be followed by `Service.startForeground()` within ~5 seconds.
   - `AdhanPlaybackService.onStartCommand()` immediately calls `createNotificationChannel()` and `startForeground()` prior to preparing `MediaPlayer`.
4. **Mandatory Notification Channels:**
   - In Home Mode, `channel_adhan_playback` and `channel_prayer_reminder` are created before posting alerts or notifications.
   - In Mosque Mode (silent default), notification channels are not created and no notifications are dispatched.
5. **Direct External Storage Access (No Scoped Storage on API 26):**
   - Direct file paths under `/storage/...` (e.g., USB drive mounts) work directly with `READ_EXTERNAL_STORAGE` (`android:maxSdkVersion="32"`). No Scoped Storage restrictions exist on Android 8.0.
6. **2016 Hardware Resource Constraints:**
   - SoC memory and CPU on 2016 TV hardware are constrained.
   - Slide images imported from USB must be downscaled to 1080p before caching.
   - Coil image cache and memory footprint must remain lightweight.
