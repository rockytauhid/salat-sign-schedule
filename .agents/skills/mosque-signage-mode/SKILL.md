---
name: mosque-signage-mode
description: Implements silent white-label SalatSign mosque signage with configurable prayer schedule and announcement image slideshow rotation.
---

# Skill: SalatSign White-Label Signage Engine

## Implementation Rules
1. **White-Label Header:**
   - In **SalatSign** (`:app-salatsign`), NEVER render the application's own name, package title, or default launcher icon on the live display.
   - Read `mosqueName`, `mosqueSubtitle`, and `mosqueLogoUri` from `SettingsRepository`.
   - Render `mosqueLogoUri` via Coil (`AsyncImage`) alongside `mosqueName`. If both are blank/null, render only the live digital clock and Gregorian/Hijriyah dates.
2. **Keep Screen Awake:**
   - Keep `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` active on the root TV Activity while SalatSign is in the foreground.
3. **Slideshow State Machine (`MosqueSlideshowViewModel`):**
   - Maintain a sealed state:
     - `SlideState.PrayerSchedule(durationSeconds: Int)` (default `60`)
     - `SlideState.Announcement(slide: SignageSlide)` (rendering title, body, optional image)
   - Use pure offline wall-clock scheduling (`WallClockSlideshowScheduler`, Decision D25) so multiple TVs with identical slides transition together within ~1 second.
4. **Silent Prayer-Time Interruption:**
   - Observe a 1-second ticker flow comparing `LocalTime.now()` against today's 5 prayer times.
   - When the current minute matches any prayer time (`Subuh`, `Zuhur`, `Ashr`, `Magrib`, `Isya`):
     - **Do NOT play any audio or show any toast/notification.**
     - Immediately force `SlideState.PrayerSchedule` (interrupting any active announcement slide) and set `activePrayerHighlight = currentPrayer` for 5 minutes so the congregation sees the active prayer pulsing clearly on screen.
5. **Hidden Settings Trigger & PIN Protection:**
   - In SalatSign, hide all Settings icons/buttons from the live display.
   - Intercept `Key.Menu` or a 3-second long-press on `Key.DirectionCenter` to trigger Settings access.
   - Access is gated behind a PBKDF2 PIN dialog with lockout protection (persisted across reboots). Recovery is via Android TV system app data reset.