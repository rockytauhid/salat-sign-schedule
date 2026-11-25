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
1. Open Admin Settings -> **Slideshow & Slides**.
2. Navigate to **Add Image Slide (USB/TV)** with the remote control and press `OK`.
3. In the Storage File Picker screen:
   - Select the **USB Drive** tab.
   - Navigate down the folder list to find `test_poster_4k.jpg`.
   - Press `OK` on `test_poster_4k.jpg`, then set the slide title and duration in the Image Slide dialog and select **Save Slide**.
4. **Verify:**
   - The file is downscaled from 4K to Full HD (1920x1080) and copied to app-private storage (`filesDir/content/images/`).
   - The new slide appears in the slides list with the chosen title and duration.
   - Return to live signage; verify the slide displays full-screen with zero overlays and no memory warnings or UI lag.

### Test Procedure B: Import Custom Mosque Logo
1. Open Admin Settings -> **Mosque Branding** -> **Mosque Emblem / Icon**.
2. Select preset **Custom Logo / USB** and press `OK` (opens the USB picker immediately).
3. Browse the USB drive and select `mosque_logo_large.png`.
4. **Verify:**
   - Logo is downscaled to 512x512 and saved to `filesDir/content/logo/`.
   - The custom logo immediately updates in the live header preview.

### Test Procedure C: Import Custom Adhan & Constraint Verification
1. Open **SalatSchedule** Settings -> **Reminders & Adhan** -> **Adhan Audio per Prayer Time**.
2. On any prayer (e.g. Fajr), navigate to **USB / File** button and press `OK`.
3. Select `oversized_adhan.mp3` (>10MB).
4. **Verify:**
   - Import is rejected.
   - Error banner displays: *"Audio file exceeds the 10MB limit"*.
   - No file is created in `filesDir/content/audio/`.
5. Now select `custom_adhan_subuh.mp3` (valid).
6. **Verify:**
   - Import succeeds; the prayer's Adhan updates to the custom audio.
   - Use **Test Adhan Audio** (Play / Pause / Stop) or trigger an Adhan alarm via broadcast: verify the custom audio plays clearly through TV speakers.

---

## MT-3.1: Admin PIN, Lockout Protection & Inactivity Auto-Dismiss

**Objective:** Verify that SalatSign gates settings access behind a secure PBKDF2-hashed PIN, locks out brute-force attempts with exponential backoff (persisted across restarts), and automatically dismisses settings after 2 minutes of TV inactivity.

### Test Setup
- Device: Android TV running API 26+ (e.g. Sony Bravia or Emulator).
- SalatSign installed with setup completed.

### Test Procedure A: PIN Verification & Change
1. Launch SalatSign.
2. Press `MENU` or hold `DPAD_CENTER` for 3 seconds to request Settings.
3. Verify the `PinDialog` appears with masked dots (`● ● ● ●`) and D-pad numeric keypad (1..9, CLEAR, 0, OK).
4. Enter default PIN `0000` using the D-pad keypad or remote number buttons -> Settings screen opens.
5. Verify the default-PIN security warning banner appears: *"Using default PIN (0000). Change your Admin PIN to protect settings."*
6. Select **Change PIN** -> Enter new PIN `5678` -> Confirm `5678`.
7. Close Settings and return to Mosque signage.
8. Trigger Settings open again:
   - Enter `0000` -> Rejected (*"Incorrect PIN. 4 attempts remaining"*).
   - Enter `5678` -> Accepted; Settings opens.

### Test Procedure B: Lockout Escalation & Persistence
1. Trigger the PIN dialog.
2. Deliberately enter incorrect PINs 5 consecutive times.
3. **Verify:**
   - On the 5th failure, screen displays: *"PIN Locked! Try again in 30 seconds"* in red.
   - Keypad and remote number inputs are disabled during countdown.
4. Force-stop the app via `adb shell am force-stop com.salatsign.tv` and re-launch.
5. Trigger Settings open -> **Verify:** Lockout timestamp is persisted in DataStore; remaining lockout seconds are still enforced.
6. Wait for timer to reach 0; enter wrong PIN again -> **Verify:** Lockout escalates to 60 seconds (and doubles up to 15 minutes on subsequent failures).
7. Enter correct PIN -> **Verify:** Failed attempts counter resets to 0.

### Test Procedure C: 2-Minute Inactivity Auto-Dismiss
1. Open Settings in SalatSign.
2. Place the remote control down and leave untouched.
3. **Verify:** After exactly 2 minutes (120 seconds), Settings automatically closes and returns to the Mosque digital signage slideshow.

---

## MT-3.2: Reliable Admin Gesture & Kiosk Key Handling

**Objective:** Verify that the 3-second `DPAD_CENTER` hold gesture works reliably on remotes that do not send repeated `ACTION_DOWN` events, `KEYCODE_MENU` directly opens settings, and `KEYCODE_BACK` is intercepted in SalatSign to prevent accidental exit from the public kiosk display.

### Test Setup
- Device: Android TV with standard remote control or `adb shell input keyevent`.
- SalatSign running with setup completed.

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
   - In SalatSchedule, `channel_adhan_playback` and `channel_prayer_reminder` are created before posting alerts or notifications.
   - In SalatSign (strictly silent), notification channels are not created and no notifications are dispatched.
5. **Direct External Storage Access (No Scoped Storage on API 26):**
   - Direct file paths under `/storage/...` (e.g., USB drive mounts) work directly with `READ_EXTERNAL_STORAGE` (`android:maxSdkVersion="32"`). No Scoped Storage restrictions exist on Android 8.0.
6. **2016 Hardware Resource Constraints:**
   - SoC memory and CPU on 2016 TV hardware are constrained.
   - Slide images imported from USB must be downscaled to 1080p before caching.
   - Coil image cache and memory footprint must remain lightweight.

---

## MT-6.1: Mosque Silent Screen ("Jumu'ah / Prayer in progress") & Phone Controller Override

**Objective:** Verify that SalatSign (`com.salatsign.tv`) replaces the prayer schedule and announcement slideshow with the calm, low-glare dark Silent Screen during configured Jumu'ah sittings, enabled daily prayers, and manual Start/Stop overrides (from both TV Settings and the Local Phone Controller), and persists active manual mode across restarts.

### Test Setup
- Device: Sony Bravia KD-55X9300D (Android 8.0.0, API 26) or Android TV Emulator running **SalatSign** (`com.salatsign.tv`).
- Confirm **Light Theme (Day)** is active to verify the Silent Screen stays dark during daytime.
- Configure at least one Friday Jumu'ah sitting (e.g. `13:00`) and at least one daily prayer Iqamah offset (e.g. Subuh `10 min`, Maghrib `0 min`).

### Test Procedure A: Manual Start, Auto-Close, Hint Banner & Restart Persistence
1. Open Admin Settings (`MENU` or 3-second `OK` hold + PIN).
2. Scroll to **Silent Screen → Manual Control** and select **Show 15 min**.
3. **Verify:**
   - Settings closes immediately and the TV displays the dark `"Prayer in progress"` screen with the **No talking** (`Please keep silent`) and **Phone off** (`Switch off or silence your phone`) icons/labels, readable from the back of the hall without glare.
   - No clock, mosque logo, announcements, or ornaments are visible.
   - Pressing a D-pad arrow or Back does **not** exit the app and displays the neutral dark 4-second admin hint banner (`Hold OK for 3 seconds or press Menu to open Admin Settings`).
4. Force-stop and relaunch SalatSign (`adb shell am force-stop com.salatsign.tv && adb shell am start -n com.salatsign.tv/.MainActivity`).
5. **Verify:** The Silent Screen returns automatically because `manualSilentUntil` is persisted in DataStore.
6. Open Admin Settings (`MENU` + PIN), scroll to **Silent Screen → Manual Control**, verify `"Silent screen active (... min remaining)"` is shown, and click **Stop Now**.
7. Close Settings -> **Verify:** Normal schedule/slideshow resumes immediately.

### Test Procedure B: Local Phone Controller Manual Start & Stop
1. Enable **Local Phone Controller (LAN)** in TV Settings and open the displayed URL on a phone browser on the same Wi-Fi.
2. Sign in with the Mosque Admin PIN.
3. Under **Silent Screen (Manual Control)**, tap **Show 15 min**.
4. **Verify:** The TV immediately switches to `"Prayer in progress"`, and the phone UI updates to `Active • 15 min remaining` with the red **Stop Silent Screen Now** button.
5. Tap **Stop Silent Screen Now** on the phone -> **Verify:** The TV immediately returns to the normal schedule/slideshow.

### Test Procedure C: Scheduled Jumu'ah & Daily Prayer Windows
1. In TV Settings under **Silent Screen → Friday Jumu'ah Silent Screen**, confirm it is **Enabled** (default `0 min` lead, `45 min` duration) and verify the TV shows `"Jumu'ah in progress"` at the configured Friday sitting time and returns to the slideshow after 45 minutes.
2. Under **Daily Prayers (Optional)**, select a prayer without Iqamah (e.g. Maghrib = `0 min`) with anchor **Iqamah** -> **Verify:** Toggle cannot be enabled and displays `"Set an iqamah time first"`.
3. Select a prayer with Iqamah (e.g. Subuh = `10 min`), enable it (`Iqamah`, `0 min` lead, `15 min` duration), and click **Copy these settings to all prayers** -> **Verify:** Status message confirms prayers with Iqamah were enabled while prayers without Iqamah were skipped.
4. At that prayer's Iqamah time -> **Verify:** TV shows `"Prayer in progress"` for the configured duration and automatically resumes the slideshow afterward.

---

## MT-6.2: Islamic Calendar Events & 6-Slide Rotation Limit

**Objective:** Verify the navigable Islamic Calendar screen in **SalatSchedule** (`com.salatschedule.tv`), the read-only Islamic Calendar built-in slide in **SalatSign** (`com.salatsign.tv`), and the 6-slide rotation limit across announcements and built-in slides.

### Test Procedure A: SalatSchedule Home Islamic Calendar Screen (Human Tasks C2, C3)
1. Launch **SalatSchedule** and verify the **"Islamic Calendar"** button appears in the header beside Settings (and the subtle `"Next: …"` summary line appears below the header).
2. Select **Islamic Calendar** -> **Verify:** Opens on the current Hijri year, scrolled so the `TODAY` or `NEXT` row is visible, with distinct star/badge/outline marks and dimmed past events readable from viewing distance (`C3`).
3. Press Left/Right D-pad to browse ±3 Hijri years -> **Verify:** Clamped at ±3 years with limit notice; **Today** button returns to the current Hijri year.
4. Open Settings -> **Islamic Calendar Events**, toggle events on/off, and change **Hijri Date Offset** (`-2..+2`) -> **Verify:** Calendar dates shift accurately and match local community dates (`C2`).

### Test Procedure B: SalatSign Islamic Calendar Slide & 6-Slide Budget (Human Task C4)
1. Launch **SalatSign**, open Admin Settings, and scroll to **Slides in rotation**.
2. Enable **Islamic Calendar Slide** (`15s` default) -> **Verify:** Counter updates (e.g., `Slides in rotation: 1 of 6 (Islamic calendar)`).
3. Close Settings -> **Verify:** After the Prayer Schedule and any enabled announcement slides, the read-only Islamic Calendar slide appears with at most 6 rows, clear TODAY/NEXT/PAST marks, readable from the back of the hall (`C4`), and no truncated event names.
4. Disable all 16 Islamic events -> **Verify:** Slide is skipped cleanly at runtime (no blank slide) while still occupying 1 slot until the slide toggle itself is disabled.
5. Add/enable announcements until reaching 6 total slots -> **Verify:** Trying to add/enable a 7th slot shows the refusal message, while saving as **Disabled** is still allowed up to 30 stored announcements.

---

## MT-6.3: Offline Quran Verse Slide & Fit Verification (Human Tasks Q2, Q3)

**Objective:** Verify that SalatSign (`com.salatsign.tv`) renders the 63 verbatim Tanzil Uthmani verses offline using the bundled Amiri Quran font with clean diacritics, zero truncation/ellipsis, shuffle-bag persistence across restarts, and required About credits.

### Test Procedure
1. Disconnect the TV from Wi-Fi / Ethernet to confirm 100% offline operation.
2. Open Admin Settings -> **Quran Verse Slide**:
   - Enable **Show Quran Verse Slide in Rotation** -> **Verify:** Rotation counter increments by 1 slot (`Quran verse`).
   - Click **Check verse fit** -> **Verify:** Reports whether all 63 verses fit cleanly at minimum 40 sp Arabic / 24 sp translation on the Sony TV (`Q3`).
   - Click **Preview a verse now** -> **Verify:** Plain, undecorated layout, crisp Uthmani Arabic diacritics (superscript alef, stop marks) in Amiri Quran font, readable translation, and `"Surah <Name> <S>:<A>"` reference (`Q2`, `Q3`).
3. Scroll to **About & Credits** -> **Verify:** Credits `Tanzil.net (http://tanzil.net)`, `Saheeh International, via Tanzil.net`, and `Kementerian Agama Republik Indonesia`.
4. Return to live signage -> **Verify:** Rotation shows Prayer Schedule → Enabled Announcements → Islamic Calendar (if enabled) → Quran Verse (1 verse per cycle) → Prayer Schedule, with Silent Screen taking priority whenever active.

---

## MT-6.4: SalatSchedule Android TV Screensaver (`DreamService`) (Human Tasks S1, S2, S3)

**Objective:** Verify that **SalatSchedule** (`com.salatschedule.tv`) registers as a standard Android TV screensaver (`DreamService`), runs completely silently in dark-only near-black mode with gentle burn-in pixel shift, rotates configured slides smoothly, and exits cleanly on any remote key press.

### Test Setup
- Device: Sony Bravia KD-55X9300D (Android 8.0.0, API 26) or Android TV device with standard Screensaver picker (note: Fire TV does not expose a third-party `DreamService` picker; use **Preview Screensaver** in Settings on Fire TV).
- Confirm Home Settings → **Screensaver** has Prayer Schedule, Islamic Calendar, and Quran Verse enabled.

### Test Procedure
1. **Settings & Preview (`S2`, `S3`):**
   - Open SalatSchedule → Settings → **Screensaver**.
   - Try turning off all 3 slide toggles -> **Verify:** The last enabled slide stays ON (cannot leave all 3 disabled).
   - Adjust **Prayer Schedule Duration** and **Islamic Calendar Duration** (`10s`..`60s`) -> **Verify:** Selected chip highlights clearly.
   - Select **How to Set as Screensaver** -> **Verify:** Clear 4-step instructions appear; if the device supports `ACTION_DREAM_SETTINGS`, the **Open TV Screensaver Settings** button opens the system picker.
   - Select **Preview Screensaver** -> **Verify:** Full-screen dark-only screensaver launches immediately; pressing any key (Back, D-pad, OK) returns cleanly to Settings.
   - Switch Display Language to **Bahasa Indonesia** and verify all Screensaver labels, durations, and help dialog copy read naturally (`S3`).
2. **System Screensaver Picker & Idle Launch (`S1`):**
   - Open Android TV Settings → Device Preferences → Screen saver → **SalatSchedule**.
   - Set **When to start** to 5 minutes (or trigger via `adb shell am start -n com.android.systemui/.Somnambulator` / dream command).
   - **Verify:** SalatSchedule screensaver launches automatically on idle.
3. **Visual, Silence & Burn-In Checks (`S2`):**
   - Observe at 10-foot living-room distance in a dim room -> **Verify:** Near-black `#060E0A` background, calm low-glare typography, minute-only clock (no seconds), configured timezone matches Home screen, and 500ms smooth crossfade between slides.
   - Verify **zero sound** is emitted (no click, chime, or Adhan from the screensaver; scheduled Adhan alarms still fire normally via `AdhanPlaybackService`).
   - Observe across 1+ minute -> **Verify:** Content shifts subtly (≤ 8 px) every 60 seconds without clipping inside overscan margins.
4. **Wake & Stability:**
   - Press any remote key -> **Verify:** Screensaver exits immediately back to Android TV.
   - Start and stop **Preview Screensaver** 10+ times -> **Verify:** Smooth transitions, no crashes, no orphaned windows or memory leaks.
