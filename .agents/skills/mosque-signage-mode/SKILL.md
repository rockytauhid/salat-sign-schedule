---
name: mosque-signage-mode
description: Implements silent white-label Mosque Mode with configurable prayer schedule and announcement image slideshow rotation.
---

# Skill: Mosque Mode White-Label Signage Engine

## Implementation Rules
1. **White-Label Header:**
   - When `isMosqueMode == true`, NEVER render the application's own name, package title, or default launcher icon.
   - Read `mosqueName`, `mosqueSubtitle`, and `mosqueLogoUri` from `SettingsRepository`.
   - Render `mosqueLogoUri` via Coil (`AsyncImage`) alongside `mosqueName`. If both are blank/null, render only the live digital clock and Gregorian/Hijriyah dates.
2. **Keep Screen Awake:**
   - Attach a `DisposableEffect(isMosqueMode)` in the root TV Activity/Composable that adds `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` when `isMosqueMode == true` and clears it when `false`.
3. **Slideshow State Machine (`MosqueSlideshowViewModel`):**
   - Maintain a sealed state:
     - `SlideState.PrayerSchedule(durationSeconds: Int)` (default `60`)
     - `SlideState.AnnouncementImage(slideIndex: Int, imageUri: String, durationSeconds: Int)` (configurable `3`, `5`, `10`, or custom seconds per image)
   - Run a coroutine timer loop:
     - Show `PrayerSchedule` for `scheduleDurationSeconds`.
     - If `announcementSlides.isNotEmpty()`, iterate through each slide for its configured `durationSeconds` using `Crossfade(animationSpec = tween(800))`.
     - Loop back to `PrayerSchedule`.
4. **Silent Prayer-Time Interruption:**
   - Observe a 1-second ticker flow comparing `LocalTime.now()` against today's 5 prayer times.
   - When the current minute matches any prayer time (`Subuh`, `Zuhur`, `Ashr`, `Magrib`, `Isya`):
     - **Do NOT play any audio or show any toast/notification.**
     - Immediately force `SlideState.PrayerSchedule` (interrupting any active announcement slide) and set `activePrayerHighlight = currentPrayer` for 5 minutes so the congregation sees the active prayer pulsing clearly on screen.
5. **Hidden Settings Trigger:**
   - In Mosque Mode, hide all Settings icons/buttons from the screen.
   - Intercept `Key.Menu` or a 3-second long-press on `Key.DirectionCenter` to open the Settings screen.