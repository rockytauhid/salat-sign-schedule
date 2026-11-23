# Quran Verse Asset Generation (`tools/build_quran_data.py`)

SalatSign bundles 63 short, single-screen Quran verses as static read-only JSON assets under `app-salatsign/src/main/assets/quran/` (no runtime network access, no SQLite database).

## Absolute Rule: Verbatim Extraction Only
Never type, recall, edit, normalise, or shorten any Quranic Arabic text or translation by hand. All bundled JSON files must be generated directly from verified upstream Tanzil files by running `tools/build_quran_data.py`.

## Upstream Sources
Download these files into `tools/sources/` (ignored by Git; only SHA-256 checksums are committed in `tools/sources/CHECKSUMS.txt`):

1. **Arabic Uthmani text (`quran-uthmani.txt`)** — Tanzil Project (CC-BY 3.0, verbatim only):
   `https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=txt-2&agree=true`
2. **English translation (`en.sahih.txt`)** — Saheeh International via Tanzil.net (CC-BY 3.0, verbatim only):
   `https://tanzil.net/trans/en.sahih`
3. **Indonesian translation (`id.indonesian.txt`)** — Kementerian Agama Republik Indonesia via Tanzil.net:
   `https://tanzil.net/trans/id.indonesian`
4. **Surah metadata (`quran-data.xml`)** — Tanzil Project (CC-BY):
   `https://tanzil.net/res/text/metadata/quran-data.xml`
5. **Arabic Quran Font (`app-salatsign/src/main/res/font/amiri_quran.ttf`)** — Amiri Quran (SIL OFL 1.1):
   `https://github.com/google/fonts/tree/main/ofl/amiriquran`

## Regenerating the Assets

```bash
python tools/build_quran_data.py
```

The script:
- Reads the canonical 63 references from `tools/verse-list.txt`.
- Extracts exact UTF-8 (no BOM) text with **zero normalisation** into:
  - `app-salatsign/src/main/assets/quran/verses_ar.json`
  - `app-salatsign/src/main/assets/quran/translations_en.json`
  - `app-salatsign/src/main/assets/quran/translations_id.json`
  - `app-salatsign/src/main/assets/quran/surahs.json`
  - `app-salatsign/src/main/assets/quran/ATTRIBUTION.md` (with verbatim upstream license blocks)
- Updates `tools/sources/CHECKSUMS.txt` with SHA-256 digests of the source files.
- Fails loudly if any reference is missing, duplicated, or empty.

## Verification
Run the JVM data integrity tests after regenerating:

```bash
./gradlew :app-salatsign:testDebugUnitTest
```
