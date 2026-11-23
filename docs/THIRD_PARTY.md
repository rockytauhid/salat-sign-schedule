# Third-Party Dependencies and Licensing

This document tracks all external libraries, SDKs, fonts, and assets used in SalatSign, along with their versions, licenses, and compatibility with the project's **GNU General Public License v3.0 or later (GPL-3.0-or-later)**.

---

## 1. Runtime Dependencies & Libraries

| Dependency | Group / Artifact | Version | License | License Compatibility | Purpose |
|------------|------------------|---------|---------|-----------------------|---------|
| **Adhan Calculation Engine** | `com.batoulapps.adhan:adhan` | 1.2.1 | Apache-2.0 | Compatible with GPL-3.0 | High-precision offline astronomical prayer time calculation |
| **AndroidX Core KTX** | `androidx.core:core-ktx` | 1.15.0 | Apache-2.0 | Compatible with GPL-3.0 | Core Kotlin extensions for Android framework |
| **AndroidX AppCompat** | `androidx.appcompat:appcompat` | 1.7.0 | Apache-2.0 | Compatible with GPL-3.0 | Compatibility support across Android versions |
| **Google Material Components** | `com.google.android.material:material` | 1.12.0 | Apache-2.0 | Compatible with GPL-3.0 | Material design components |
| **Jetpack Compose BOM** | `androidx.compose:compose-bom` | 2024.11.00 | Apache-2.0 | Compatible with GPL-3.0 | Bill of Materials for Compose UI libraries |
| **Jetpack Compose UI** | `androidx.compose.ui:ui` | BOM | Apache-2.0 | Compatible with GPL-3.0 | Declarative UI toolkit |
| **Compose Material Icons** | `androidx.compose.material:material-icons-extended` | BOM | Apache-2.0 | Compatible with GPL-3.0 | Directional, control, setting, and Silent Screen (`VoiceOverOff`, `MobileOff`) vector icons |
| **Jetpack Compose for TV (Foundation)** | `androidx.tv:tv-foundation` | 1.0.0-alpha12 | Apache-2.0 | Compatible with GPL-3.0 | 10-foot TV directional navigation and focus modifiers |
| **Jetpack Compose for TV (Material)** | `androidx.tv:tv-material` | 1.0.0 | Apache-2.0 | Compatible with GPL-3.0 | TV-optimized cards, surfaces, buttons, and typography |
| **Activity Compose** | `androidx.activity:activity-compose` | 1.9.3 | Apache-2.0 | Compatible with GPL-3.0 | Integration between Android Activity lifecycle and Compose |
| **Lifecycle Runtime KTX / Compose** | `androidx.lifecycle:lifecycle-*` | 2.8.7 | Apache-2.0 | Compatible with GPL-3.0 | Lifecycle-aware StateFlow collection and ViewModel support |
| **DataStore Preferences** | `androidx.datastore:datastore-preferences` | 1.1.1 | Apache-2.0 | Compatible with GPL-3.0 | Offline key-value and preferences persistence |
| **Coil Compose** | `io.coil-kt.coil3:coil-compose` | 3.0.4 | Apache-2.0 | Compatible with GPL-3.0 | Local asynchronous image rendering for mosque logos & slides |
| **Kotlinx Coroutines** | `org.jetbrains.kotlinx:kotlinx-coroutines-*` | 1.9.0 | Apache-2.0 | Compatible with GPL-3.0 | Asynchronous programming and background execution |
| **Kotlinx Serialization JSON** | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.3 | Apache-2.0 | Compatible with GPL-3.0 | Type-safe JSON serialization for announcement slides & storage |
| **WorkManager Runtime KTX** | `androidx.work:work-runtime-ktx` | 2.10.0 | Apache-2.0 | Compatible with GPL-3.0 | Daily periodic midnight alarm maintenance and resilience |

---

## 2. Test Dependencies (Not Bundled in Distribution)

| Dependency | Group / Artifact | Version | License | Purpose |
|------------|------------------|---------|---------|---------|
| **JUnit 4** | `junit:junit` | 4.13.2 | EPL-1.0 | Unit test runner |
| **AndroidX JUnit** | `androidx.test.ext:junit` | 1.2.1 | Apache-2.0 | AndroidX test runner integration |
| **Espresso Core** | `androidx.test.espresso:espresso-core` | 3.6.1 | Apache-2.0 | UI and instrumentation testing |

---

## 3. Audio Assets

Audio assets bundled with SalatSign are subject to strict copyright verification (Decision D15 / Task H1).
Details on audio provenance, file checksums, and license status are maintained in:
👉 [docs/AUDIO_SOURCES.md](AUDIO_SOURCES.md)

| Asset File | Status | License | Attribution |
|------------|--------|---------|-------------|
| `adhan_default.wav` | Placeholder tone (silent WAV header) | CC0 1.0 Universal | Public Domain |
| `adhan_subuh.wav` | Placeholder tone (silent WAV header) | CC0 1.0 Universal | Public Domain |

---

## 3b. Quran Text, Translations & Font Assets (`:app-salatsign` only)

All 63 verses are extracted verbatim by `tools/build_quran_data.py` with SHA-256 source checksums tracked in `tools/sources/CHECKSUMS.txt` and verbatim upstream notices preserved in `app-salatsign/src/main/assets/quran/ATTRIBUTION.md`.

| Asset / Resource | Source | License | License Compatibility | Attribution / Notes |
|------------------|--------|---------|-----------------------|---------------------|
| `assets/quran/verses_ar.json` & `surahs.json` | [Tanzil Project](http://tanzil.net) (Uthmani v1.1) | Creative Commons Attribution 3.0 (CC-BY 3.0, verbatim only) | Compatible with GPL-3.0 (unmodified asset) | Credit: "Quran text: Tanzil.net" (`http://tanzil.net`). Verbatim copying only; no edits permitted. |
| `assets/quran/translations_en.json` | Saheeh International via [Tanzil.net](http://tanzil.net) (`en.sahih`) | Creative Commons Attribution 3.0 (CC-BY 3.0, verbatim only) | Compatible with GPL-3.0 (unmodified asset) | Credit: "English translation: Saheeh International, via Tanzil.net". |
| `assets/quran/translations_id.json` | Indonesian Ministry of Religious Affairs via [Tanzil.net](http://tanzil.net) (`id.indonesian`) | Owner-confirmed permission with credit | Bundled with required attribution | Credit: "Indonesian translation: Kementerian Agama Republik Indonesia". |
| `res/font/amiri_quran.ttf` | Amiri Quran (Alif Type / Google Fonts) | SIL Open Font License 1.1 (`tools/sources/AMIRI_QURAN_OFL.txt`) | Compatible with GPL-3.0 | Full Uthmani Quranic diacritic and annotation mark rendering offline on API 26+. |

---

## 4. License Compatibility Analysis (GPL-3.0-or-later)

- **Apache License 2.0:** Compatible with GNU GPL version 3.0 according to the Free Software Foundation (FSF). Software licensed under Apache-2.0 can be combined with GPLv3 code, provided the resulting work as a whole is distributed under the terms of GPLv3.
- **Creative Commons Zero (CC0 1.0):** CC0 is a universal public domain dedication that is fully compatible with GPLv3.
- **SIL Open Font License (OFL 1.1):** Recognized by FSF as free software compatible for bundling with GPL-licensed software.
