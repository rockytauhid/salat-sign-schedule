# SalatSign — Audio Sources & Licensing Provenance

This document tracks the provenance, licensing, checksums, and attribution for all bundled audio assets in `app/src/main/res/raw/`.

Every audio asset bundled with the application **must** be listed here with verified licensing evidence before release (Decision D15 / Task H1).

---

## Bundled Audio Registry

### 1. `adhan_default.wav`
- **File Name:** `adhan_default.wav`
- **Path:** `app/src/main/res/raw/adhan_default.wav`
- **SHA-256 Checksum:** `8b8fbafe8679076454429756fa72f11d5f442c87381cc6a4285451d826a9e629`
- **Format:** RIFF PCM WAV, 44 bytes
- **Performer / Artist:** Locally generated synthetic silence placeholder
- **Status:** **PLACEHOLDER TONE** (Pending human task H1 delivery of licensed standard Adhan)
- **Source / Origin:** Synthetically generated local WAV header (zero media payload) for offline build and service testing
- **License:** CC0 1.0 Universal (Public Domain Dedication)
- **License URL:** https://creativecommons.org/publicdomain/zero/1.0/
- **Date Added:** 2026-11-17
- **Modifications:** None
- **Attribution Line:** Development placeholder tone (CC0)
- **Target Replacement File:** `adhan_standard.ogg` (OGG Vorbis, ~96-128 kbps, -16 LUFS integrated loudness)

---

### 2. `adhan_subuh.wav`
- **File Name:** `adhan_subuh.wav`
- **Path:** `app/src/main/res/raw/adhan_subuh.wav`
- **SHA-256 Checksum:** `8b8fbafe8679076454429756fa72f11d5f442c87381cc6a4285451d826a9e629`
- **Format:** RIFF PCM WAV, 44 bytes
- **Performer / Artist:** Locally generated synthetic silence placeholder
- **Status:** **PLACEHOLDER TONE** (Pending human task H1 delivery of licensed Fajr Adhan)
- **Source / Origin:** Synthetically generated local WAV header (zero media payload) for offline build and service testing
- **License:** CC0 1.0 Universal (Public Domain Dedication)
- **License URL:** https://creativecommons.org/publicdomain/zero/1.0/
- **Date Added:** 2026-11-17
- **Modifications:** None
- **Attribution Line:** Development placeholder tone (CC0)
- **Target Replacement File:** `adhan_fajr.ogg` (OGG Vorbis, ~96-128 kbps, includes *As-salatu khayrum minan-nawm*)

---

## Licensing Requirements & Acceptance Criteria (Decision D15)

In accordance with `SALATSIGN_REMEDIATION_PLAN.md`:

### Accepted Licenses:
1. **CC0 1.0 Universal / Public Domain**
2. **Creative Commons Attribution (CC BY 4.0 / CC BY 3.0)** — requires attribution in the app's About screen and `docs/THIRD_PARTY.md`.
3. **Direct Written Release / Commission Agreement** signed by the recording artist / muezzin granting irrevocable, royalty-free, worldwide redistribution under GPL-3.0-or-later or CC BY.

### Prohibited Sources (STRICT):
- **NonCommercial (NC)** or **NoDerivatives (ND)** licenses (e.g., CC BY-NC, CC BY-ND) — incompatible with open distribution and repository integrity.
- **"Free for personal use"** licenses — does not grant application redistribution rights.
- **Stock audio libraries with restrictive site-specific terms** (Pixabay, Mixkit, Zapsplat, Artlist, Freesound non-commercial) — their terms forbid redistributing raw sound files in public code repositories.
- **YouTube or web rips without explicit written copyright license** (e.g. `Kiwifu/adhan-mp3` repository on GitHub) — prohibited from inclusion in this repository.

### Fallback Strategy (Release Guarantee):
If properly licensed recordings are not delivered by human task H1 prior to Google Play submission, SalatSign will ship **quiet by default**:
- Per-prayer default adhan audio will be set to `SILENT`.
- Users may freely import their own preferred adhan recordings from a USB flash drive or TV local storage via the in-app file browser (Phase 2.3).
