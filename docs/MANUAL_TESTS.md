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
