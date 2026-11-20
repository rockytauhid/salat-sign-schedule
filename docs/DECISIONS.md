# SalatSign — Architecture & Product Decisions

This document records the confirmed product and architectural decisions (**D1–D17**) and working assumptions for **SalatSign** (`com.salatsign.tv`).

---

## Confirmed Decisions (D1–D17)

| # | Decision | Value |
|---|---|---|
| **D1** | Languages at launch | **English only (`en-AU`).** Keep the existing Indonesian text, extracted into `values-in/`, in the repository but **not shipped** until the owner (a native Indonesian speaker) has reviewed it (human task H5); use `androidResources.localeFilters` to ship `en` only until then. Arabic/RTL (`values-ar/`) is deferred (layout code uses `start`/`end` so RTL is cheap to enable later). *(Assumed: English-only launch until translations are reviewed.)* |
| **D2** | Default language and formats | English with **Australian spelling** (colour, centre, programme). Use `en-AU` for date and time formatting. **12-hour clock by default** (e.g. `5:12 am`) with a 12/24-hour setting. *(Assumed.)* |
| **D3** | Audience | **Australia first.** City presets list Australian cities first (Canberra, Sydney, Melbourne, Brisbane, Adelaide, Perth, Hobart, Darwin). **Daylight saving must be handled** (Sydney, Melbourne, Adelaide, Hobart, Canberra observe DST; Brisbane, Perth, Darwin do not; Adelaide and Darwin are on half-hour offsets). |
| **D4** | Calculation method | **No named preset.** The setup wizard asks for the calculation method and madhab. Preselect `MUSLIM_WORLD_LEAGUE` as a temporary default pending the owner's comparison with a local timetable (human task H4). *(Assumed.)* |
| **D5** | Adhan audio stream | Keep `AudioAttributes.USAGE_ALARM` and request transient audio focus; revisit after testing volume behaviour on the owner's Sony TV (human task H3). |
| **D6** | Custom assets | **Keep TV-side import** of custom adhan, mosque logo, and slide images from the TV's local storage or a USB drive. Imported files are copied into app-private storage (`filesDir/content/{images,logo,audio}`) with automatic orphan cleanup. |
| **D7** | Admin PIN | **Required in Mosque Mode (optional in Home Mode), default `0000`**, nudge the user to change it. Stored as PBKDF2-salted hash with progressive lockout; corrupted custom PIN storage locks settings and requires clearing app data rather than falling back to `0000`. |
| **D8** | Hijri rollover | **At Maghrib by default**, with an optional setting "Change date at midnight instead". Configurable day offset `-2` to `+2`. |
| **D9** | Digits | Latin digits (`0–9`), including when Arabic locale is eventually enabled (`ar-u-nu-latn`). |
| **D10** | Iqamah | In scope: per-prayer minutes after adhan (`0` or empty = disabled). Display only. When at least one is configured, Mosque Mode displays the Iqamah time; Friday Jumu'ah rows do not apply the Iqamah delay (the sitting time replaces it). |
| **D11** | License | **Copyleft: GPL-3.0-or-later** (`LICENSE`), plus a trademark/branding policy for the name "SalatSign" (`TRADEMARKS.md`). Copyright holder: Rocky Adam Tauhid (`rockytauhid@gmail.com`). |
| **D12** | Application ID | **`com.salatsign.tv` is final (confirmed by the owner).** Used for both `namespace` and `applicationId`; do not change either. |
| **D13** | Madhab | Chosen in the **setup wizard** and changeable in **Settings** (`SHAFI` / `HANAFI`). |
| **D14** | Mosque mode audio/notifications | **Quiet by default:** no adhan sound, no reminders, no notifications. Optional toggles in Settings allow mosques to opt in. |
| **D15** | Adhan audio files | Placeholder WAVs are silent; per-prayer default is `AdhanAudioType.SILENT` with a one-time Home-mode hint until open-licensed/public-domain or commissioned recordings (a **standard** adhan and a **Fajr** adhan) are supplied (human task H1). |
| **D16** | Jumu'ah (Friday) time | **Set by each mosque, not calculated. Mosque mode only.** Configurable mode (`FIXED` clock time or `AFTER_ZUHR` minute offset) with up to 3 sittings inside the Zuhr-to-Asr window, validated across the next 53 Fridays (handling DST shifts). Home mode always shows Friday as plain "Zuhr" with the calculated time. |
| **D17** | Product focus | **Home mode stays the default** and both Home Mode and Mosque Mode remain first-class equal modes. Home-mode alarm reliability and real adhan audio are release-blocking. |
| **D18** | Multi-module product split | Split into two separate Android TV apps in a multi-module monorepo: **SalatSign** (`com.salatsign.tv`, `:app-salatsign`) is a **strictly silent** Mosque digital signage app (no audio/alarm/notification services or permissions; Jumu'ah, Iqamah, slides, branding, PIN, `FLAG_KEEP_SCREEN_ON`); **SalatSchedule** (`com.salatschedule.tv`, `:app-salatschedule`) is a dedicated Home prayer times app (Adhan audio, 15-min reminders, D-pad date browsing; no PIN, Iqamah, Mosque branding, slides, or Jumu'ah). Shared non-UI logic lives in `:core` (reusable by a future Android phone SalatSign controller) and shared Compose TV UI lives in `:core-tv-ui`. Per-app Play Store metadata lives in `docs/salatsign/STORE_LISTING.md` and `docs/salatschedule/STORE_LISTING.md`. |

---

## Working Assumptions

1. **Friday Adhan Timing Default (`AT_ZUHR`):** When Mosque Mode's optional adhan toggle is enabled, `fridayAdhanTiming` defaults to `AT_ZUHR` (calling adhan at the calculated Zuhr time), with an option to switch to `AT_JUMUAH` (at the first Jumu'ah sitting). Pending owner confirmation.
2. **Default Calculation Method (`MUSLIM_WORLD_LEAGUE`):** Preselected in the setup wizard and default `AppSettings` pending local Australian timetable comparison (H4).
3. **English-Only Launch (`en-AU`):** Only `en` resources are packaged in release APKs via `androidResources.localeFilters` until `docs/i18n/review-id.csv` is reviewed and approved by the owner (H5), and Arabic is reviewed by a native speaker.
4. **Target Hardware Floor (`minSdk 26`):** The primary physical test device is a **Sony KD-55X9300D** running **Android 8.0.0 (API 26)**. All APIs used in production code must exist on API 26 or be safely guarded with `Build.VERSION.SDK_INT` checks.
