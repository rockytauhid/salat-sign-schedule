# SalatSign (Android TV) — Remediation & Localization Plan

**Audience:** an LLM coding agent working inside the IDE on this repository.
**Source:** a code review of the project (manifest, Gradle, engine, services, settings, slideshow ViewModel, MainActivity, git history). The Compose UI screens (`HomeScreen`, `SettingsScreen`, `SetupWizardScreen`, `MosqueSignageScreen`) were **not** reviewed in detail, so read them before changing them. Nothing was built or run during the review.

---

## 0. Ground rules for the agent

1. Work **phase by phase** (Section 3), one Git branch per phase (`fix/phase-1-reliability`, …), **one commit per task ID** (e.g. `P1.2: chain prayer alarms`).
2. After each task run `./gradlew testDebugUnitTest`. After each phase also run `./gradlew lintDebug assembleDebug`. Do not leave the build red.
3. Keep `minSdk = 26`, package `com.salatsign.tv`, Kotlin + Compose for TV. Do not rename packages.
4. Do **not** add a dependency without listing it in the commit message and in `docs/THIRD_PARTY.md` (created in P7.3).
5. **From now on, every new user-visible string goes into `strings.xml`**, never into Kotlin. (The big extraction happens in Phase 6, but do not add new hardcoded strings in the meantime.)
6. The two current `res/raw/*.wav` files are **locally generated placeholders**. Replace them only through task P2.4, using files supplied by the owner (licensing is a human task, see Section 4). Never download or add audio yourself.
7. Where this document says *verify*, check the current official docs or the library source before acting. Several items depend on API details I could not confirm offline.
8. If a task is blocked by an open decision (Section 2), use the **assumed default** and record the assumption in `docs/DECISIONS.md`. Do not stop to ask.
9. Do not delete features that are not listed here. Do not reformat unrelated files.
10. Add or update unit tests for every behavior change. Instrumented/UI testing is a human task.

---

## 1. Project snapshot (so you don't have to rediscover it)

- Single module `:app`, package `com.salatsign.tv`.
- Modes: **Home** (adhan audio + 15-min reminder) and **Mosque** (silent signage slideshow: prayer schedule, then announcement images; settings hidden).
- Prayer times: `com.batoulapps.adhan:adhan:1.2.1` via `engine/PrayerCalculationEngine.kt`. Hijri: `engine/HijriCalendarEngine.kt` (`java.time` `HijrahDate`).
- Storage: DataStore Preferences (`data/repository/SettingsRepository.kt`). Slides are stored as a delimiter-joined string under `slides_raw`.
- Alarms: `service/PrayerAlarmScheduler.kt` (`setAlarmClock`), `PrayerAlarmReceiver`, `AdhanPlaybackService`, `BootAndTimeReceiver`, `DailyScheduleWorker` (WorkManager, 00:05).
- Current UI language: **Indonesian, hardcoded in Kotlin** (`strings.xml` only has `app_name`). Roughly 250+ string literals across ~20 files.
- Planned next: a separate phone controller app that edits announcements/settings over the local network. Phase 8 prepares for it. Do not implement it yet.

---

## 2. Decisions (confirmed by the owner, plus assumed defaults)

Record each in `docs/DECISIONS.md`. Items marked **(assumed)** were not explicitly confirmed; use the default and flag it in the commit message.

| # | Decision | Value |
|---|----------|-------|
| D1 | Languages at launch | **English only (en-AU).** Keep the existing Indonesian text, extracted into `values-in/`, in the repo but **not shipped** until the owner (a native Indonesian speaker) has reviewed it (H5); use `localeFilters` to ship `en` only until then. Arabic/RTL is deferred (but write layout code with `start`/`end` so RTL is cheap later) **(assumed: reading "English" as English-only launch)** |
| D2 | Default language and formats | English with **Australian spelling** (colour, centre, programme). Use `en-AU` for date/time formatting. 12-hour clock by default (e.g. `5:12 am`) with a 12/24-hour setting **(assumed)** |
| D3 | Audience | **Australia first.** City presets list Australian cities first (see P4.5). **Daylight saving must be handled** (Sydney, Melbourne, Adelaide, Hobart, Canberra observe it; Brisbane, Perth, Darwin do not; Adelaide and Darwin are on half-hour offsets) |
| D4 | Calculation method | **No named preset.** The setup wizard asks for the method and madhab. Preselect `MUSLIM_WORLD_LEAGUE` as a temporary default pending the owner's comparison with a local timetable (H4) **(assumed)** |
| D5 | Adhan audio stream | Keep `USAGE_ALARM` and add audio focus; revisit after testing on the owner's Sony TV (H3) |
| D6 | Custom assets | **Keep TV-side import** of custom adhan, logo and slide images from the TV's local storage or a USB drive (P2.3). Files are copied into app-private storage |
| D7 | Admin PIN | **Required, default `0000`**, nudge the user to change it (P3.1) |
| D8 | Hijri rollover | **At Maghrib by default**, optional setting "Change date at midnight instead" (P4.6) |
| D9 | Digits | Latin digits (0–9) |
| D10 | Iqamah | In scope: per-prayer minutes after adhan, display only |
| D11 | License | **Copyleft: GPL-3.0** (AGPL-3.0 is an acceptable alternative), plus a trademark/branding policy for the name "SalatSign" (P7.1). Replace the README's MIT claim |
| D12 | Application ID | **`com.salatsign.tv` is final (confirmed by the owner).** It is already both the `namespace` and the `applicationId`; do not change either |
| D13 | Madhab | Chosen in the **setup wizard** and changeable in **Settings** (P4.1) |
| D14 | Mosque mode audio/notifications | **Quiet by default:** no adhan sound, no reminders, no notifications. Optional toggles to enable them (P4.7) |
| D15 | Adhan audio files | Replace the placeholders with open-licensed/public-domain or commissioned recordings: a **standard** adhan and a **Fajr** adhan (P2.4) |

---

## 3. Work plan

Priority legend: **P1** = fix before any release, **P2** = should fix before public repo/Play, **P3** = nice to have.

### Phase 1 — Reliability bugs (P1)

#### P1.1 Fix the collector leak in the slideshow loop
- **File:** `ui/mosque/MosqueSlideshowViewModel.kt`, `startSlideshowLoop()`.
- **Problem:** each loop iteration calls `settingsRepository.appSettingsFlow.stateIn(viewModelScope).value`. Every call starts a new collector in `viewModelScope` that is never cancelled. A kiosk running all day leaks hundreds of collectors.
- **Change:**
  - Declare once, **above the `init` block** (property initialization order matters):
    `private val settingsState = settingsRepository.appSettingsFlow.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())`
  - Use `settingsState.value` inside the loop. Reuse `settingsState` in the `uiState` `combine` instead of collecting `appSettingsFlow` again.
  - Recompute `today` and `prayerTimes` at the start of **every** slide, not once per loop, so the schedule rolls over at midnight even mid-loop.
- **Acceptance:** no `stateIn` call inside any loop; slideshow still alternates schedule/slides; strike interruption still works.

#### P1.2 Chain prayer alarms instead of scheduling only "today"
- **Files:** `service/PrayerAlarmScheduler.kt`, `PrayerAlarmReceiver.kt`, `engine/PrayerCalculationEngine.kt`, `SalatSignApp.kt`, `SettingsViewModel.kt`, `BootAndTimeReceiver.kt`, `DailyScheduleWorker.kt`.
- **Problem:** `scheduleAlarmsForToday` schedules nothing after Isya, so tomorrow's Subuh depends on the inexact WorkManager 00:05 job. If it runs late, Subuh adhan is missed. Also, prayers with `SILENT` adhan are not scheduled at all, which would break a chain.
- **Design:**
  1. Add to the engine: `nextPrayerEvent(now: ZonedDateTime, config): PrayerEvent(prayer, at: ZonedDateTime)`. It evaluates today's and tomorrow's times and returns the earliest event strictly after `now`. Use `ZonedDateTime` with an explicit date, never `LocalTime` + "today".
  2. Scheduler exposes `scheduleNext(settings)`. Use **two single-slot alarms**: one for the next adhan (fixed request code, e.g. 100) and one for the next 15-minute reminder (fixed request code, e.g. 200). Put `prayer id` and `scheduledAtEpochMillis` in the intent extras.
  3. The receiver, on fire: (a) in Mosque mode do nothing unless the optional Mosque-mode toggles from P4.7 are enabled; (b) in Home mode play adhan only if that prayer's option is not `SILENT`, or show the reminder if enabled; (c) **always** call `scheduleNext(settings)` afterwards, even for silent prayers.
  4. If the alarm fires more than ~5 minutes after `scheduledAtEpochMillis` (device was off or asleep), skip playback but still reschedule.
  5. Call `scheduleNext` on: app start, any settings change that affects times or audio (location, timezone, method, madhab, offsets, adhan options, reminder toggle, mode), boot, time change, timezone change, package replaced. Keep `DailyScheduleWorker` as a safety net that calls `scheduleNext`.
  6. Keep `setAlarmClock`. Remove the per-prayer request-code scheme and the `Prayer.defaultRequestCode*` fields if they become unused.
- **Acceptance (unit tests):** after Isya the next event is tomorrow's Subuh; around midnight the next event is correct; works for `Australia/Brisbane` (no DST), `Australia/Sydney` across both DST changes, `Australia/Adelaide` (half-hour offset) and `Asia/Jakarta`; silent prayer still advances the chain; late-fire logic skips playback.

#### P1.3 Receivers must use `goAsync()`
- **Files:** `PrayerAlarmReceiver.kt`, `BootAndTimeReceiver.kt`.
- **Problem:** both launch `CoroutineScope(Dispatchers.IO).launch {…}` and return from `onReceive` immediately. The process can be killed before the DataStore read finishes or before `startForegroundService` is called.
- **Change:**
  ```kotlin
  override fun onReceive(context: Context, intent: Intent) {
      val pending = goAsync()
      CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
          try {
              withTimeout(8_000) { /* read settings, act, scheduleNext */ }
          } finally {
              pending.finish()
          }
      }
  }
  ```
- Also remove `LOCKED_BOOT_COMPLETED` from the `BootAndTimeReceiver` intent filter (DataStore lives in credential-protected storage and the receiver is not direct-boot aware). `BOOT_COMPLETED` is enough.
- **Acceptance:** `pending.finish()` is reached on every path, including exceptions.

#### P1.4 Adhan playback: audio focus, stop control, async prepare
- **File:** `service/AdhanPlaybackService.kt`, `MainActivity.kt`.
- **Problems:** (a) no audio focus is requested, so the adhan mixes over YouTube/Netflix instead of pausing them, although the README claims it plays over other apps; (b) a notification action is not a practical way to stop audio on a TV; (c) `MediaPlayer.prepare()` blocks the main thread (risky for slow custom sources later).
- **Change:**
  - Request `AudioFocusRequest` with `AUDIOFOCUS_GAIN_TRANSIENT` using the same `AudioAttributes`. Stop playback if focus is permanently lost. Abandon focus when finished/destroyed.
  - Expose playing state (e.g. `AdhanPlaybackService.isPlaying: StateFlow<Boolean>` in a companion). In `MainActivity`, when the adhan is playing and the user presses Back or OK, send `ACTION_STOP_ADHAN`.
  - Use `prepareAsync()` + `OnPreparedListener` and start playback in the callback.
  - Add a code comment: `USAGE_ALARM` may not follow the TV's volume control on some devices; this needs testing on real hardware (human task H3).
- **Acceptance:** unit-testable logic is minimal here; document manual test steps in `docs/MANUAL_TESTS.md` (create it): play adhan while a video app is open → video pauses/ducks and resumes.

---

### Phase 2 — Data layer: replace the fragile slide storage (P1)

**Why now:** the phone controller will let people type titles and upload images. The current `;;`/`::` delimiter format loses data if a title contains those characters, and `content://` or remote URIs are not durable.

#### P2.1 New content model and store
- **Files:** `data/model/AnnouncementSlide.kt`, new `data/repository/ContentRepository.kt`, `SettingsRepository.kt`, `MosqueSlideshowViewModel.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`.
- Add the Kotlin serialization plugin (`org.jetbrains.kotlin.plugin.serialization`, same version as Kotlin) and `kotlinx-serialization-json` (*verify* compatibility with the AGP/Kotlin versions in `libs.versions.toml`; if it cannot be made to work, use `org.json` instead).
- New model:
  ```kotlin
  @Serializable
  data class AnnouncementSlide(
      val id: String,
      val type: SlideType = SlideType.IMAGE,   // IMAGE, TEXT
      val title: String = "",
      val body: String = "",
      val imageFile: String? = null,           // file NAME only, relative to content/images/
      val durationSeconds: Int = 10,
      val enabled: Boolean = true,
      val startDate: String? = null,           // ISO yyyy-MM-dd, in the mosque's timezone
      val endDate: String? = null,
      val sortOrder: Int = 0
  )
  ```
- Store: `filesDir/content/slides.json` = `{"revision": <int>, "slides": [...]}`; images in `filesDir/content/images/`. Rules:
  - Atomic writes (write temp file then move/rename), guarded by a `Mutex`. Keep `slides.json.bak` of the last good version.
  - `revision` increments on every successful write.
  - Corrupt/unreadable JSON: fall back to `.bak`, then to empty list. Never crash.
  - Never store absolute paths, `content://` URIs, or `http(s)` URLs in slides.
  - Validation: title ≤ 80 chars, body ≤ 400, duration 3–120 s, max 30 slides.
- `ContentRepository` exposes `slides: Flow<SlideList>` (with `revision`), `replaceAll(expectedRevision, slides)`, and `add/update/delete` helpers. A stale `expectedRevision` must fail with a conflict result. (The phone controller will use this later.)
- The slideshow shows only slides that are `enabled` and within `startDate..endDate` (evaluated in the configured timezone, see P4.5). It reads from `ContentRepository`, not from `AppSettings`.
- Add a **TEXT slide renderer** (title + body, large type, RTL-aware) next to the image renderer in `MosqueSignageScreen.kt`. Image slides load `File(imageFile)` through Coil.
- Remove `addSampleAnnouncementSlide()` and the Unsplash URL. Replace it with a bundled sample drawable and sample text, available only in **debug** builds (or remove the button).
- Remove `slides_raw` handling from `SettingsRepository` after migration (P2.2). `AppSettings.announcementSlides` goes away.

#### P2.2 One-time migration
- On first launch of the new version, if `slides_raw` exists: parse it with the old split rules (best effort), **drop any slide whose URI starts with `http`** (this removes the Unsplash sample), skip entries it cannot parse, write the rest to the new store (image slides whose URI is not a readable local file are dropped), then delete the key. Never crash.

**Acceptance (unit tests):** JSON round-trip with titles containing `::`, `;;`, quotes, emoji, Arabic and newlines; migration from legacy strings; corrupt file recovery; revision conflict; date-range filtering; stale `expectedRevision` rejected.

#### P2.3 Import custom adhan, logo and slide images from TV storage or USB (D6)
- **Files:** new `data/importing/` (`ImportRepository`, validators), new `ui/importing/FilePickerScreen.kt`, `SettingsScreen.kt`, `SettingsViewModel.kt`, `AppSettings.kt`, `AndroidManifest.xml`.
- **Reality check:** many Android TV firmwares have **no system document picker** (no `DocumentsUI`), so `ACTION_OPEN_DOCUMENT` may do nothing. Implement a **picker strategy chain**:
  1. If an activity resolves `ACTION_OPEN_DOCUMENT` (check with `resolveActivity`), use it.
  2. Otherwise use an **in-app, D-pad friendly file browser**:
     - API ≤ 28 (the owner's Sony KD-55X9300D tops out at Android 8.0, see P5.4): list files directly under the storage volumes (`/storage/*`, including removable USB), with the `READ_EXTERNAL_STORAGE` runtime permission. Scoped storage does not apply on these devices.
     - API ≥ 29: query `MediaStore` (audio and images, `VOLUME_EXTERNAL`, which includes USB volumes) or use a SAF tree if available. *Verify* the required permission per API level (`READ_EXTERNAL_STORAGE` ≤ 32, `READ_MEDIA_AUDIO`/`READ_MEDIA_IMAGES` on 33+) and Google Play's photo/video permission policy before declaring them.
  3. If nothing works, show a clear message ("Plug in a USB drive with .mp3/.ogg/.jpg files").
- Browser UX: focusable list, shows volume names (Internal, USB), folders, supported files only, image thumbnail preview, audio **preview play/stop**, Back goes up a folder.
- **Always copy** the chosen file into app-private storage (`filesDir/content/{audio,images,logo}/<uuid>.<ext>`) and store only the **file name**. Never keep `content://` URIs or absolute external paths. Delete orphaned files when slides/settings are removed.
- Validation and processing:
  - Audio: `.mp3`, `.ogg`, `.m4a`, `.wav`; ≤ 10 MB; ≤ 10 minutes (read duration with `MediaMetadataRetriever`; reject files it cannot parse).
  - Images: `.jpg/.jpeg/.png/.webp`; read bounds first and reject absurd sizes; **downscale** slide images to ≤ 1920×1080 and logos to ≤ 512 px, re-encode (this strips EXIF), because the 2016 TV has limited memory.
- Settings model: per-prayer adhan source `SILENT | BUNDLED_STANDARD | BUNDLED_FAJR | CUSTOM(fileName)` (persist only the type and file name, never a display label). Defaults: Subuh = `BUNDLED_FAJR`, other prayers = `BUNDLED_STANDARD`. Logo setting = file name or none.
- Add "Import from storage / USB" actions to the slide manager, the logo setting and the adhan setting.
- **Acceptance:** unit tests for the validators and the copy/downscale logic (use small fixture files); manual test on the Sony TV with a USB stick (FAT32 and exFAT) is written into `docs/MANUAL_TESTS.md`.

#### P2.4 Replace the placeholder adhan with properly licensed audio (D15)
- **Inputs from the owner (human task H1):** two source files plus license evidence. The agent must not download or generate audio.
- **License rules** (record the decision in `docs/DECISIONS.md`):
  - Accepted: **CC0 / public domain**, or **CC BY** (attribution shown in an About screen and in `docs/THIRD_PARTY.md`), or a written release from the performer.
  - Rejected: any **NonCommercial (NC)** or **NoDerivatives (ND)** license, "free for personal use", and site-specific licenses such as Pixabay, Mixkit, Zapsplat or Artlist (they do not allow redistributing the raw file in an open repository), and anything with unclear provenance.
  - Prefer a clean recording of one muezzin (no traffic, sirens, echo or overlapping callers). Ambient street recordings are unsuitable.
- **Not usable as a source:** the GitHub collection `Kiwifu/adhan-mp3`. It has no license shown and no provenance, it consists of recordings of named muezzins and broadcast adhans (so the rights belong to the performers or broadcasters, not the repository owner), and its tagline "free for ... personal use" is not a license to redistribute inside an open-source app. Do not copy, bundle or link-download files from it. Users may import such files themselves through P2.3, on their own responsibility; the README must say the project does not distribute third-party adhan recordings.
- **Fallback if no properly licensed audio is available by release:** ship **without** a bundled adhan. Per-prayer default becomes `SILENT` until the user imports a file (P2.3), and Home mode shows a one-time hint explaining how. Mosque mode is silent by default anyway (D14).
- **Two tracks:** `adhan_standard` (Dhuhr, Asr, Maghrib, Isha, and Jumu'ah) and `adhan_fajr` (includes the added phrase for Fajr).
- **Technical spec:** OGG Vorbis (or AAC `.m4a`), 96–128 kbps, 44.1/48 kHz, trimmed leading/trailing silence, short fade-in/out, loudness-normalised to a consistent level (about −16 LUFS integrated, true peak ≤ −1 dBTP), ≤ 3 MB each. Put them in `res/raw/adhan_standard.ogg` and `res/raw/adhan_fajr.ogg`.
- **Integrate:** point `BUNDLED_STANDARD`/`BUNDLED_FAJR` at them, delete the two placeholder `.wav` files, fix all references.
- **Document provenance** in `docs/AUDIO_SOURCES.md`, one entry per file: title, performer, source URL, license + license URL, date retrieved, modifications made, attribution line, evidence file path (screenshot/archive link or signed release). Add a unit test that fails if a file in `res/raw` has no matching entry.
- See Appendix D for a release template if the owner commissions a recording.

---

### Phase 3 — Admin PIN and kiosk key handling (P1)

#### P3.1 Admin PIN
- **Files:** `AppSettings.kt`, `SettingsRepository.kt`, `SetupWizardScreen.kt`, `SettingsScreen.kt`, new `ui/common/PinDialog.kt`, new `security/PinHasher.kt`.
- Store a salted hash (e.g. PBKDF2WithHmacSHA256 with a per-install random salt; tune iterations for slow TV hardware, minimum 10k) and salt in DataStore. Never store the PIN.
- 4–8 digits. Add a **TV-friendly numeric keypad** (focusable grid, D-pad navigable).
- **Default PIN is `0000` (D7).** While no custom PIN is stored, `0000` is the effective PIN. Do not store a hash for it; store a "custom PIN set" flag instead.
- Setup wizard: a "Set admin PIN" step is offered when Mosque mode is selected, with a clear "Skip and keep default (0000)" option. If the default is still in use, show a one-time prompt the first time Settings is opened ("You are using the default PIN. Change it now?") and a small persistent reminder inside Settings. A default of `0000` is easy to guess on a public screen, so the reminder matters.
- Opening Settings in Mosque mode requires the PIN (default or custom). In Home mode it is optional (toggle).
- Lockout: after 5 wrong attempts, lock 30 s, doubling each further failure up to 15 min. Persist the failure counter and lock-until timestamp so a restart does not reset it.
- Auto-close Settings after 2 minutes of inactivity and return to signage.
- No remote recovery path; document in README that clearing app data resets the PIN to `0000`.
- **Acceptance (unit tests):** hash verify, wrong PIN, lockout timing (inject a clock), persistence of counters.

#### P3.2 Reliable admin gesture
- **File:** `MainActivity.kt`.
- **Problem:** the 3-second hold detection relies on key-repeat events, which not every remote sends. Also Compose may consume DPAD_CENTER before `Activity.onKeyDown` sees it.
- **Change:** handle keys at the root with `Modifier.onPreviewKeyEvent` (or `dispatchKeyEvent`). On `DPAD_CENTER` ACTION_DOWN start a 3 s timer coroutine; cancel it on ACTION_UP. `KEYCODE_MENU` still opens Settings directly. Both paths go through the PIN check when required.
- In Mosque mode, intercept Back so visitors cannot accidentally leave the app.
- **Acceptance:** documented manual test in `docs/MANUAL_TESTS.md`.

---

### Phase 4 — Prayer-time correctness and mosque features (P2)

#### P4.1 Calculation configuration
- **Files:** `PrayerCalculationEngine.kt`, `AppSettings.kt`, `SettingsRepository.kt`, `SettingsScreen.kt`.
- Introduce `PrayerConfig(latitude, longitude, zoneId, methodName, madhab, highLatitudeRule, offsetsMinutes: Map<Prayer, Int>)` and replace the long parameter lists with it.
- **Madhab (D13)**: setting with two choices, labelled "Standard (Shafi'i, Maliki, Hanbali)" and "Hanafi", with one line explaining that it only changes the Asr time. *Verify* the enum names in adhan 1.2.1 (`Madhab.SHAFI`, `Madhab.HANAFI`). Currently it is hardcoded to Shafi in `getCalculationParameters`.
  - Ask it in the **setup wizard** (preselect Standard) and make it editable in **Settings**.
  - Wizard order: Language (only when more than one language ships) → Country/City (sets coordinates and timezone) → Calculation method → Madhab → Mode (Home/Mosque) → Admin PIN (Mosque) → Mosque name/logo.
- **Per-prayer offsets**, −30…+30 minutes, applied through the library's adjustment fields (*verify* `CalculationParameters.adjustments` / `PrayerAdjustments` exist in adhan 1.2.1). Mosques use these for ihtiyath/local conventions. **Do not hardcode an official-authority offset set** (see D4/H4).
- **High-latitude rule** setting (*verify* `highLatitudeRule` in adhan 1.2.1); default none.
- **Acceptance (unit tests):** offsets shift times by exactly N minutes; madhab changes Asr; existing engine tests still pass.

#### P4.2 Null/failed times: remove the silent 12:00 fallback
- `dateToLocalTime` returns `LocalTime.of(12, 0)` when the library gives `null`. Change `PrayerTimeItem.time` to nullable, show `--:--` in the UI, skip null prayers in scheduling and next-prayer logic, and show a warning in Settings that the location/method may be unsupported (e.g. high latitude).

#### P4.3 Active-prayer semantics
- A prayer is *active* from its time until the next prayer's time; **Isya stays active until Subuh, including after midnight**. Remove the misleading "within 20 minutes" comment in the engine (that logic does not exist).
- Keep the Mosque-mode 5-minute "strike pulse" as a separate concept (`getPrayerAtExactTimeStrike`).

#### P4.4 Friday (Jumu'ah) and iqamah
- On Fridays (in the configured timezone) the Zuhur row label becomes the localized Jumu'ah name (see Appendix A). The adhan behavior does not change.
- **Iqamah (D10):** per-prayer "minutes after adhan" (0 or empty = disabled). If at least one is set, the Mosque schedule shows an Iqamah column. Display only.

#### P4.5 Timezone belongs to the location
- **Problem:** the engine uses `ZoneId.systemDefault()`. If the TV's timezone is wrong the times are silently wrong.
- **Change:** add `zoneId` (IANA string) to `CityPreset` and `AppSettings` (Australian presets first: Canberra → `Australia/Sydney`, Sydney `Australia/Sydney`, Melbourne `Australia/Melbourne`, Brisbane `Australia/Brisbane`, Adelaide `Australia/Adelaide`, Perth `Australia/Perth`, Hobart `Australia/Hobart`, Darwin `Australia/Darwin`; keep the existing Indonesian and world presets after them). Replace all `LocalTime.now()` / `LocalDate.now()` in the engine, ViewModels, scheduler and UI with values derived from `ZonedDateTime.now(zoneId)` through one injectable `Clock`/`AppClock`. The displayed clock then follows the mosque's zone too. If the device timezone offset differs from the configured zone's offset, show a non-blocking notice in Settings.
- **Acceptance (unit tests):** same instant yields correct local prayer times for `Australia/Brisbane`, `Australia/Sydney` (test the days either side of both DST changes), `Australia/Adelaide` and `Asia/Jakarta`; day rollover happens at midnight in the configured zone; the scheduler produces the right instants across a DST change.

#### P4.6 Hijri calendar
- `HijrahDate.from(...)` uses the Umm al-Qura chronology. That can differ by a day from local (e.g. MABIMS) declarations; the existing ±2 offset handles this. Document it in the README.
- **Maghrib rollover (D8):** by default the Hijri date advances at Maghrib (the Islamic day begins at sunset). Add a setting "Change date at midnight instead" (off by default). Implement in `HijriCalendarEngine` by passing `now: ZonedDateTime` and that day's Maghrib time: if the rollover is Maghrib and `now >= maghrib`, format the Hijri date for `date + 1`; after midnight the normal date is correct again. Keep the ±2 day manual offset; many Australian communities follow local or overseas moon-sighting announcements, which the offset covers.
- Strengthen `HijriCalendarEngineTest`: the current tests only assert non-blank/not-equal. Add: for `2026-11-17`, the month index is 6 (Jumada al-Akhirah); the day increments across a month boundary; with Maghrib rollover the date advances at Maghrib and not before; the midnight option does not.
- **README bug:** it shows "7 Jumada al-Ula 1448 H" for 17 Nov 2026. The correct month is Jumada al-Akhirah (the spec file already says that for 16 Nov). Fix the README example after verifying the engine's output.

#### P4.7 Quiet-by-default Mosque mode (D14)
- **Files:** `AppSettings.kt`, `SettingsRepository.kt`, `PrayerAlarmScheduler.kt`, `PrayerAlarmReceiver.kt`, `SetupWizardScreen.kt`, `SettingsScreen.kt`.
- Add settings, all **default off**: `mosqueModeAdhanEnabled`, `mosqueModeRemindersEnabled`. In Mosque mode with defaults: no alarms are scheduled, no adhan audio, no reminders, no notifications, no foreground service, and no notification channels are created or permission requested.
- If the owner turns on either toggle, the chained scheduler (P1.2) schedules only those events. The on-screen strike highlight is visual only and stays (add a `showStrikeHighlight` toggle, default on).
- Setup wizard: when Mosque mode is chosen, show "Mosque mode is silent by default. You can turn on adhan sound in Settings."
- **Acceptance (unit tests):** Mosque mode with defaults schedules zero alarms; enabling the adhan toggle schedules adhan only; Home mode behaviour unchanged.

---

### Phase 5 — Manifest, build and Play readiness (P2)

#### P5.1 Permissions
- Remove `SCHEDULE_EXACT_ALARM` and `USE_EXACT_ALARM`. The app only uses `AlarmManager.setAlarmClock`, which, per the Android docs, does not need them (*verify* in the current AlarmManager docs, and grep that no `setExact*` call exists). `USE_EXACT_ALARM` in particular triggers a Play Console declaration/policy review.
- Storage permissions are now needed for P2.3 (import from TV storage/USB). Declare `READ_EXTERNAL_STORAGE` with `android:maxSdkVersion="32"`, and `READ_MEDIA_AUDIO`/`READ_MEDIA_IMAGES` only if the MediaStore path is used on API 33+ (*verify* Google Play's photo/video permission policy first; prefer SAF where available). Request them at the moment of import, never at startup. In Mosque mode with no import in progress the app asks for nothing.
- `POST_NOTIFICATIONS`: *verify* whether it is requested at runtime anywhere. If not, add a request in the setup wizard where the platform supports it, and make sure the adhan still plays when notifications are denied.
- `INTERNET`: keep **only** if needed for the future local-network server (opening server sockets needs it). If so, say in the README: "core features work fully offline; network access is used only for optional local remote control." If no remote images remain after P2.1, remove `coil-network-okhttp`.
- `allowBackup`: set to `false` for now (the future pairing secrets and PIN hash must never be backed up). Revisit when the controller exists.

#### P5.2 Signing
- `app/build.gradle.kts` currently falls back to the **debug keystore** when release properties are missing. Change it so that when a release task is requested (`assembleRelease`, `bundleRelease`) and no release keystore is configured, the build **fails with a clear message**. Debug builds are unaffected.

#### P5.3 Target API and dependencies
- *Verify* Google Play's current target API level requirement (I believe it moved to API 36 in August 2026) and update `compileSdk`/`targetSdk` accordingly, then check behavior changes that affect alarms, foreground services and receivers.
- Dependencies are dated (Compose BOM 2024.11, `tv-foundation` alpha). Update **one group at a time** (Compose BOM + TV libs, lifecycle/activity, Coil, WorkManager, Media3), running tests after each. Remove the unused `adhan2` entry from `libs.versions.toml` (or migrate to it deliberately).
- Unused Media3 dependencies: the service uses `MediaPlayer`. Either remove `media3-*` or migrate; do not keep both without reason.

#### P5.4 Baseline device: Android 8.0 (API 26)
- **Confirmed by the owner:** the Sony KD-55X9300D runs **Android 8.0.0 (API 26)**. That is exactly the project's `minSdk 26`, so the app installs, but there is **no headroom**: do not raise `minSdk`, and every API used must exist on API 26. Treat this TV as the baseline test device.
- Run lint with `NewApi` as an error; add an API 26 Android TV emulator image to the test notes in `docs/MANUAL_TESTS.md`.
- Things that behave differently on API 26 (*verify* each against the docs before relying on it):
  - **Background execution limits (Oreo):** manifest-declared receivers for most implicit broadcasts do not fire. `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` are exempt; check whether `TIME_SET` and `TIMEZONE_CHANGED` are on the exemption list. If not, do not depend on them: keep the P1.2 chained alarms plus the daily WorkManager safety net so times self-correct.
  - **Foreground services:** an alarm-triggered receiver may call `startForegroundService`, but the service must call `startForeground` within about 5 seconds. Keep that ordering in `AdhanPlaybackService`.
  - **Notification channels are mandatory.** Create them before posting anything (and not at all in quiet Mosque mode, P4.7).
  - **No scoped storage:** direct file access under `/storage/*` works with `READ_EXTERNAL_STORAGE` (this is the P2.3 strategy that matters on this TV). No `requestLegacyExternalStorage` needed.
  - `foregroundServiceType` and the newer `FOREGROUND_SERVICE_*` permissions are ignored on this version; keeping them is harmless.
- Hardware is a 2016 TV: keep memory low (P2.3 downscaling, small Coil caches) and avoid heavy animations in the slideshow.

---

### Phase 6 — Localization (P2)

**Scope (D1/D2):** launch in **English (en-AU)**. Do the full string extraction now so the existing Indonesian text lands in `values-in/` (kept in the repo, not shipped until reviewed) and so adding Arabic, Malay, Urdu, etc. later is a pure resources task. Arabic/RTL (P6.4 and the Arabic parts of P6.5) is **deferred**, not abandoned.

#### P6.0 Principles
1. **No user-visible text in Kotlin.** Everything goes through string resources: Compose text, notifications (including the foreground-service notification), enum labels, presets, error messages, Hijri month names, content descriptions.
2. Resource folders: `values/` (English, default), `values-in/` (Indonesian — use `in`, the legacy code, for maximum compatibility), `values-ar/` (Arabic).
3. Key naming: `area_element_detail`, e.g. `home_next_prayer`, `settings_section_mosque`, `prayer_subuh`, `notif_reminder_title`. Always **positional placeholders** (`%1$s`, `%2$d`); use `<plurals>` for counts; never concatenate translated fragments.
4. `app_name` is `translatable="false"`.
5. **Keep models free of Android resource types.** Do not put `@StringRes` in `Prayer`, `ThemeMode`, `MosqueIconPreset`, `MosqueFontPreset`, `AdhanAudioOption` (the shared `core` module in Phase 8 cannot depend on Android resources). Map enum → string resource in UI-layer extension functions, e.g. `ui/common/Labels.kt`: `fun Prayer.labelRes(isFriday: Boolean): Int`.

#### P6.1 Extraction inventory (work through every file)
Run `grep -rnE '"[A-Za-z][^"]{3,}"' app/src/main/java` and handle each hit. Largest clusters first:
- `data/model/AppSettings.kt`: enum `displayName`s, `MosqueNamePresets`, `MosqueSubtitlePresets`, `PopularCityPresets`.
- `ui/settings/SettingsScreen.kt`, `ui/setup/SetupWizardScreen.kt`, `ui/home/HomeScreen.kt`, `ui/mosque/MosqueSignageScreen.kt`, `ui/common/LiveClockHeader.kt`, `ui/common/MosqueBrandComponents.kt`.
- `service/AdhanPlaybackService.kt` and `PrayerAlarmReceiver.kt`: notification titles/texts/channel names/actions ("Pengingat Shalat", "Waktu Shalat … Telah Tiba", "Hentikan Adhan", …).
- `data/model/Prayer.kt`: `displayName`, `arabicName`.
- `engine/HijriCalendarEngine.kt`: month names, the `"H"` suffix, the fallback strings, and the hardcoded `Locale("id","ID")`.

Specific fixes while extracting:
- **Do not persist display names.** `SettingsRepository` stores `adhan_name_<prayer>` (a localized label) in DataStore. For built-in audio types, derive the label from the type at render time; persist a name only for custom files (which no longer exist after D6, so the key can go).
- **User-entered data is never translated:** mosque name, subtitle, slide title/body. Remove translated defaults from stored values. When the mosque name is blank, show a localized placeholder at render time instead of storing "Masjid Raya".
- Name/subtitle **presets** become `<string-array>` resources per language (they are suggestions, not data).
- City names are proper nouns; keep them as data (optionally localized later).

#### P6.2 Language setting and runtime switching
- Add `appLanguage` to `AppSettings`: `SYSTEM | EN | ID | AR` (DataStore).
- **Language chooser** as the first setup-wizard step and in Settings, **shown only when more than one language is shipped** (so not at the English-only launch). Preselect from the device locale if supported, else English.
- Apply the language by wrapping the Activity context: override `attachBaseContext` in `MainActivity` to return `createConfigurationContext(config)` with the chosen `Locale` (and layout direction), then `recreate()` on change. `MainActivity` extends `ComponentActivity`, not `AppCompatActivity`, so the AppCompat per-app-locale backport does not apply below API 33; the wrapper works on all supported API levels. Many TV firmwares have no per-app language screen, which is why the in-app setting is required.
- Optionally also add `res/xml/locales_config.xml` and `android:localeConfig` for API 33+ system per-app language (*verify* it does not conflict with the in-app setting).
- **Services, receivers and notifications** must use the same helper (`LocaleHelper.wrap(context, language)`), because they do not run inside the Activity.
- Use `androidResources.localeFilters` (or `resourceConfigurations`) to ship only `en`, `in`, `ar`.

#### P6.3 Prayer names, Friday, Hijri and Gregorian dates
- Prayer names per Appendix A. The Arabic-script name can be shown as a secondary line via a `showArabicNames` setting (default on in English/Indonesian; in Arabic UI the primary label is already Arabic, so do not duplicate).
- Hijri month names come from `<string-array name="hijri_months">` per language (Appendix B). Date format string: `hijri_date_format` = `%1$d %2$s %3$d %4$s`, with `hijri_year_suffix` = `H` (en: `AH`, id: `H`, ar: `هـ`). Remove the hardcoded English list and the `"Bulan n"` fallback.
- Gregorian date: replace the hardcoded pattern/locale with a locale-aware formatter (`DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)` with the selected locale, or `DateFormat.getBestDateTimePattern`). The comment in the engine claims "Senin, 17 November 2026" but 17 Nov 2026 is a Tuesday (Selasa); tests must assert the weekday correctly.
- **Digits (D9):** build the formatting `Locale` with Latin numbering (e.g. `Locale.forLanguageTag("ar-u-nu-latn")`) so Arabic does not silently switch to Arabic-Indic digits. Add the optional Arabic-Indic setting later if wanted.
- Time format: **12-hour by default for English (en-AU)**, 24-hour for Indonesian/Arabic when those ship, with a 12/24-hour setting. Format dates and times with `Locale.forLanguageTag("en-AU")` rather than hardcoded patterns (do not assume the exact separator/comma; use the locale formatter).

#### P6.4 RTL (deferred until Arabic ships; keep `start`/`end` usage now)
- Manifest already has `supportsRtl="true"`. In Compose use `start`/`end` (not left/right) in `padding`, `Alignment.Start/End`, `Arrangement`. Mirror directional icons (`Icons.AutoMirrored`).
- Time strings and countdowns are rendered LTR even in RTL layouts (`TextDirection.Ltr`).
- D-pad: Home-mode previous/next-day navigation follows layout direction (in RTL the Right key goes to the previous day). Check `TvFocusModifiers` and `HomeScreen`.
- Review overscan padding, the schedule table, and the slide text renderer in RTL.

#### P6.5 Fonts for Arabic and numerals
- Bundle a Latin family such as **Plus Jakarta Sans** (OFL) now. When Arabic ships, also bundle **Noto Naskh Arabic** (OFL) rather than relying on the TV's system fonts, in `res/font/`; wire them into `ui/theme/Type.kt` and the `MosqueFontPreset` options (currently `Type.kt` uses only `FontFamily.SansSerif/Serif/Monospace`). Verify mixed Latin + Arabic text falls back correctly.
- Replace the `Monospace` countdown with a proportional font plus tabular numerals (`fontFeatureSettings = "tnum"`) so digits don't jitter.
- Add the font licenses to `docs/THIRD_PARTY.md`.

#### P6.6 Text expansion and robustness
- Indonesian and Arabic strings are often longer than English. No fixed widths for text; use `maxLines` + ellipsis or wrapping; test mosque names of ~60 characters and long slide titles on the schedule and slide screens.
- Enable `pseudoLocalesEnabled = true` for debug builds (`en-XA`, `ar-XB`) to catch clipping and mirroring problems.
- Make lint `MissingTranslation` **fatal** for release.

#### P6.7 Translation workflow and review
- English is the source of truth. The agent writes English; Indonesian comes from the existing text; Arabic is drafted only when it is scheduled. **A native speaker must review Islamic terminology before any non-English language ships** (human task H5). Keep a `docs/GLOSSARY.md` of agreed terms (adhan, iqamah, Subuh/Fajr, Jumat/Jumu'ah, …) and use it consistently.
- Generate `docs/i18n/review-id.csv` with columns `key, english, indonesian, notes, status` so the owner can review quickly. Only strings with `status = approved` ship; unreviewed Indonesian stays out of release builds.
- Localize `STORE_LISTING.md` (English and Indonesian exist; add Arabic) and plan localized screenshots per language.
- User-authored announcements are not translated by the app. (Possible future: a `language` tag per slide for bilingual rotation. Out of scope now.)

#### P6.8 Acceptance
- At launch: every screen, notification and the Hijri/Gregorian dates are English (en-AU) with Australian spelling. When more than one language ships: changing language in the wizard and in Settings updates every screen, plus notifications, without clearing app data.
- `grep` finds no user-visible string literal left in Kotlin (allowed exceptions: log tags/messages, enum `name`s, keys, and `@Preview` text).
- (When Arabic ships) layout reviewed visually in RTL on a TV emulator and the Sony TV.
- Unit tests: Hijri month names per language; prayer label mapping including Friday; Gregorian formatting uses Latin digits for `ar`; locale wrapper returns the right resources.

---

### Phase 7 — Repository hygiene and docs (P2)

- **P7.1 License and branding (D11):** the README claims MIT and no `LICENSE` file exists. Replace with **GPL-3.0** (`LICENSE`, plus an SPDX header `// SPDX-License-Identifier: GPL-3.0-or-later` on source files; use AGPL-3.0 instead if the owner prefers a network-use clause). Update the README badge/text. Add `TRADEMARKS.md`: the name "SalatSign" and the logo are not licensed for use by forks; forks must rename. Add `CONTRIBUTING.md` requiring a DCO sign-off (`Signed-off-by`) on contributions. Check compatibility: Adhan (MIT), AndroidX/Coil (Apache-2.0) and OFL fonts are compatible with GPL-3.0; audio files follow P2.4. Note for the owner: a copyleft license makes closed-source copies a violation, but it **cannot** stop someone rebranding and selling a copy that ships with its source; only the trademark policy and store policies address the name.
- **P7.2 `.gitignore` / tracked files:** `.idea/` is committed. Remove it from the index (`git rm -r --cached .idea`) and ignore it fully. Also ignore `.kotlin/`, `*.jks`, `*.keystore`, `*.apk`, `*.aab`, `/captures`. Do **not** rewrite history.
- **P7.3 `docs/THIRD_PARTY.md`:** list every library and font with license (Adhan: MIT; AndroidX/Compose/Coil: Apache-2.0; fonts: OFL) and the source and license of both adhan recordings (see `docs/AUDIO_SOURCES.md`, P2.4).
- **P7.4 Fix documentation claims:**
  - README, `STORE_LISTING.md`, `PRODUCT_SPEC.md`: keep the "custom adhan/logo/images from USB" claims **only once P2.3 works on the Sony TV**; until then mark them "planned". Say remote management (phone controller) is **planned**.
  - README: "100% offline" wording (P5.1), the Hijri example (P4.6), the settings description (PIN), supported languages.
  - Update `AGENTS.md` and `.agents/skills/*` where they contradict this plan (e.g. the `imageUri` field in `mosque-signage-mode`, the unprotected hidden settings trigger).
  - `STORE_LISTING.md`: do not advertise features that do not exist; update the privacy section if network features are added.
- **P7.5 Tests:** keep `ExampleUnitTest`/`ExampleInstrumentedTest` only if useful; otherwise delete the templates.

---

### Phase 8 — Prepare for the phone controller (P3; no implementation yet)

- Move to a multi-module layout **only when starting the controller**; for now make it possible:
  - Keep `data/model` pure Kotlin (no Android types such as `Uri`, `@StringRes`, `Context`).
  - Define `ContentRepository` and settings access behind interfaces so a future HTTP server can call them.
  - Add `const val API_VERSION = 1` in a single file.
  - Add an `ImageImporter` utility (decode with bounds, downscale to ≤ 1920×1080, re-encode, strip metadata, generate a server-side filename). Unit-test it with sample images.
- Planned module split (for later): `:core` (models, API types, validation, prayer logic), `:tv-app`, `:phone-app`.
- When the server is added: foreground service with an appropriate type (*verify* `connectedDevice` vs `specialUse`), mDNS/NSD, per-device tokens, TLS with a pinned self-signed certificate, rate-limited pairing, no secrets in backups.

---

## 4. Human-only tasks (the agent must not attempt these)

| ID | Task |
|----|------|
| H1 | Supply the **standard and Fajr adhan** recordings with license evidence (see P2.4 and Appendix D). The current `.wav` files are placeholders. |
| H2 | Confirm GPL-3.0 vs AGPL-3.0, supply the copyright holder name, and decide the trademark policy wording. Confirm the application ID (`com.salatsign.tv` vs the `com.salatsign.com` you wrote). |
| H3 | Test on the **Sony KD-55X9300D (Android 8.0.0, confirmed)**: adhan volume and which stream follows the TV volume, audio focus with a video app, alarm accuracy after standby, remote key behaviour (Menu, long-press OK), USB import (FAT32 and exFAT) and whether the TV has a system file picker, memory with 1080p slides. |
| H4 | Compare calculated times with your local official timetable for several dates; decide offsets/ihtiyath; supply them if you want a named preset. |
| H5 | Owner reviews the Indonesian translation (using `docs/i18n/review-id.csv`, see P6.7). Arabic needs a separate native reviewer, only when Arabic is scheduled. |
| H6 | Create the release keystore and store it outside the repo; configure Play Console (target API declarations, data safety, listing). |

---

## 5. Definition of done

- [ ] Phases 1–5 complete; `./gradlew testDebugUnitTest lintDebug assembleDebug` is green; release build fails loudly without a keystore.
- [ ] No leaked collectors; alarms chain across midnight and after Isya; receivers use `goAsync`.
- [ ] Slides stored as JSON with revision, migration done, no remote/sample slides in release.
- [ ] Admin PIN works with lockout; settings gesture reliable; Back intercepted in Mosque mode.
- [ ] Timezone stored with location; no 12:00 fallback; madhab/offsets/Jumu'ah/iqamah available.
- [ ] All UI text in resources (English shipped, Indonesian preserved in `values-in/`); Latin font bundled; lint `MissingTranslation` fatal.
- [ ] Import from USB/TV storage works on the Sony TV; real, documented adhan audio replaces the placeholders (`docs/AUDIO_SOURCES.md`).
- [ ] Mosque mode is silent by default; madhab is in the wizard and Settings; Hijri date rolls at Maghrib by default.
- [ ] `LICENSE`, `docs/THIRD_PARTY.md`, `docs/DECISIONS.md`, `docs/MANUAL_TESTS.md`, `docs/GLOSSARY.md` exist; README/store listing match reality.
- [ ] H1–H6 tracked as issues for the owner.

---

## Appendix A — Prayer names

| Prayer | English | Indonesian | Arabic |
|--------|---------|------------|--------|
| Subuh | Fajr | Subuh | الفجر |
| Zuhur | Dhuhr | Zuhur | الظهر |
| Ashr | Asr | Ashr | العصر |
| Magrib | Maghrib | Magrib | المغرب |
| Isya | Isha | Isya | العشاء |
| Friday label (replaces Zuhur) | Jumu'ah | Jumat | الجمعة |
| Sunrise | Sunrise | Terbit | الشروق |
| Iqamah | Iqamah | Iqamah | الإقامة |

(The Indonesian spellings match the current code: Subuh, Zuhur, Ashr, Magrib, Isya.)

## Appendix B — Hijri month names (index 1–12)

| # | English | Indonesian | Arabic |
|---|---------|------------|--------|
| 1 | Muharram | Muharram | مُحَرَّم |
| 2 | Safar | Safar | صَفَر |
| 3 | Rabi' al-Awwal | Rabiul Awal | رَبِيع الأوَّل |
| 4 | Rabi' al-Thani | Rabiul Akhir | رَبِيع الآخِر |
| 5 | Jumada al-Ula | Jumadil Awal | جُمَادَى الأولى |
| 6 | Jumada al-Akhirah | Jumadil Akhir | جُمَادَى الآخِرة |
| 7 | Rajab | Rajab | رَجَب |
| 8 | Sha'ban | Sya'ban | شَعْبَان |
| 9 | Ramadan | Ramadhan | رَمَضَان |
| 10 | Shawwal | Syawal | شَوَّال |
| 11 | Dhu al-Qi'dah | Dzulqa'dah | ذُو القَعْدَة |
| 12 | Dhu al-Hijjah | Dzulhijjah | ذُو الحِجَّة |

## Appendix C — Suggested resource layout

```
app/src/main/res/
  values/strings.xml          # English (source)
  values/arrays.xml           # hijri_months, mosque_name_presets, mosque_subtitle_presets
  values-in/strings.xml
  values-in/arrays.xml
  values-ar/strings.xml
  values-ar/arrays.xml
  font/                       # Noto Naskh Arabic, Plus Jakarta Sans (OFL)
  xml/locales_config.xml      # optional, API 33+
```

## Appendix D — Adhan recording release (template)

Use this if the owner commissions a recording from a local muezzin. It is a plain-language template, not legal advice.

> I, **[full name]**, recorded the adhan (call to prayer) titled **[Standard adhan / Fajr adhan]** on **[date]**. I am the performer and I own the rights in this recording. I release it for use in the open-source app **SalatSign**, under **[CC0 1.0 Universal (public domain dedication) / CC BY 4.0, attribution: "Adhan recited by [name]"]**, which allows anyone to use, copy, modify and redistribute it, including in commercial products.
> Signed: ______  Date: ______

Keep the signed copy (or an email confirmation) and reference it in `docs/AUDIO_SOURCES.md`. Record in a quiet room with a decent microphone, one voice, no background noise, then export lossless (WAV/FLAC) for processing.
