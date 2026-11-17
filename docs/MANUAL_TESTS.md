# SalatSign — Manual Test Procedures

This document tracks test cases that require real Android TV hardware or emulator interaction (e.g., audio focus across third-party apps, physical D-pad remote controls, and HDMI CEC).

---

## MT-1.4: Adhan Audio Focus & Stop Control Verification

**Objective:** Verify that Adhan playback transiently gains audio focus over media apps (e.g., YouTube, Netflix), abandons focus when playback completes or is stopped, and can be immediately dismissed using the TV remote control.

### Test Setup
- Device: Android TV running API 26+ (e.g., Sony Bravia KD-55X9300D or Android TV Emulator).
- Audio App: YouTube, Netflix, or Spotify running in the background playing continuous media.
- SalatSign Mode: Home Mode with Adhan Audio enabled for upcoming prayer.

### Test Procedure A: Audio Focus Preemption
1. Open a media app (e.g., YouTube) and start playing video with active sound.
2. Trigger an Adhan alarm (either wait for exact prayer time or send an explicit test broadcast):
   ```bash
   adb shell am broadcast -a com.salatsign.tv.action.PLAY_ADHAN -n com.salatsign.tv/.service.PrayerAlarmReceiver --es extra_prayer_name "MAGRIB" --es extra_prayer_display_name "Magrib"
   ```
3. **Verify:**
   - Background media in YouTube immediately pauses (or ducks) as SalatSign requests `AUDIOFOCUS_GAIN_TRANSIENT`.
   - Adhan audio plays clearly through the TV speakers.
4. Allow Adhan playback to complete naturally.
5. **Verify:**
   - SalatSign abandons audio focus upon completion.
   - Background media (YouTube) automatically resumes playback.

### Test Procedure B: TV Remote Immediate Stop Control
1. Trigger Adhan playback while on the home screen or inside the SalatSign app.
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
