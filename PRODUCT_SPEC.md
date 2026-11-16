# Product Specification — Android TV Prayer Times & Mosque Signage

## 1. Prayer Times & Dual Calendar
- **Five Daily Prayers:** Subuh (Fajr), Zuhur (Dhuhr), Ashr (Asr), Magrib (Maghrib), Isya (Isha).
- **Dual Date Header:** Displays both the **Gregorian date** (e.g., *Monday, 16 November 2026*) and the **Hijriyah date** (e.g., *6 Jumada Al-Akhirah 1448 AH*) with a user-configurable offset (`-2`, `-1`, `0`, `+1`, `+2` days).
- **Date Navigation (Home Mode):** Users can press Left/Right on the D-pad (or open a date selector) to view **past** or **future** dates. A "Today" shortcut quickly resets to the current date.
- **Next Prayer Highlight:** When viewing the current date, the upcoming prayer card/row must render significantly larger than the remaining prayers and display a live ticking countdown (`HH:MM:SS`).

---

## 2. Mode Comparison Matrix

| Capability | Home / Personal Mode | Mosque / Kiosk Mode |
| :--- | :--- | :--- |
| **Header & Identity** | App Name & App Logo | **White-Label:** Custom Mosque Name & Mosque Logo (zero app branding) |
| **Navigation** | Full D-pad browsing & visible Settings button | Automated slideshow loop; hidden Settings trigger |
| **15-Min Reminder** | Optional system heads-up notification before prayer | **Disabled** (no notifications or popups) |
| **Adhan Audio** | Plays per-prayer selected Adhan at exact prayer time | **Silent** (zero audio playback) |
| **Background Execution** | Fires over other apps (YouTube/Netflix) or Android TV Home | Always active in foreground (`FLAG_KEEP_SCREEN_ON`) |
| **Exact Prayer Trigger** | Plays Adhan + shows visual alert | Interrupts slideshow, shows schedule & pulses active prayer row |

---

## 3. Home Mode Features
1. **15-Minute Pre-Prayer Reminder:** Toggleable setting that fires a clean Android TV notification 15 minutes before each prayer.
2. **Per-Prayer Adhan Voice Selection:**
   - Independent audio selector for **Subuh**, **Zuhur**, **Ashr**, **Magrib**, and **Isya** (plus a "Silent / Off" option per prayer).
   - Includes bundled default Adhan tracks (including a dedicated Subuh Adhan) and supports picking custom local `.mp3`/`.ogg` audio files from storage/USB.
3. **Cross-App & Idle Execution:**
   - Must fire accurately whether the user is inside the app, watching another app, or sitting on the Android TV launcher.

---

## 4. Mosque Mode Features
1. **White-Label Branding:**
   - Completely strips all app titles, app icons, and watermarks.
   - Displays user-configured **Mosque Name**, **Mosque Icon/Logo** (loaded from local storage/USB), and optional **Subtitle/Tagline** (e.g., address or *"Please silence your phone"*).
   - If unconfigured, displays a clean date-and-clock-only header.
2. **Configurable Slideshow Loop:**
   - **Step 1:** Display the full Prayer Schedule screen for `T_schedule` seconds (default: `60s`, configurable).
   - **Step 2:** Transition smoothly through configured announcement images (advertisements, donation QR codes, event posters). Each image has a configurable duration (e.g., `3s`, `5s`, `10s`, or custom seconds).
   - **Step 3:** Repeat loop indefinitely.
3. **Silent Exact-Time Interruption:**
   - If the exact time of any prayer arrives while an announcement image is on screen, immediately cancel the current slide timer, transition back to the Prayer Schedule screen, and visually highlight/pulse the current prayer row without making any sound.
4. **Hidden Admin Access:**
   - Hide on-screen settings buttons in Mosque Mode. Open Settings only via long-pressing `DPAD_CENTER` (3 seconds) or pressing the remote `KEYCODE_MENU` button.