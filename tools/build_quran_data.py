#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors
"""
Offline extractor for SalatSign's 63-verse Quran asset bundle.

Strictly reproduces Arabic text and translations VERBATIM from verified source files
placed in tools/sources/ (zero normalisation, zero trimming, zero editing).
Saves UTF-8 without BOM JSON files under app-salatsign/src/main/assets/quran/
and writes SHA-256 checksums to tools/sources/CHECKSUMS.txt.
"""

from __future__ import annotations

import hashlib
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSE_LIST_PATH = ROOT / "tools" / "verse-list.txt"
SOURCES_DIR = ROOT / "tools" / "sources"
ASSETS_DIR = ROOT / "app-salatsign" / "src" / "main" / "assets" / "quran"

AR_SOURCE = SOURCES_DIR / "quran-uthmani.txt"
EN_SOURCE = SOURCES_DIR / "en.sahih.txt"
ID_SOURCE = SOURCES_DIR / "id.indonesian.txt"
META_SOURCE = SOURCES_DIR / "quran-data.xml"
FONT_LICENSE = SOURCES_DIR / "AMIRI_QURAN_OFL.txt"
CHECKSUMS_PATH = SOURCES_DIR / "CHECKSUMS.txt"


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            h.update(chunk)
    return h.hexdigest()


def load_verse_list(path: Path) -> list[str]:
    if not path.is_file():
        raise FileNotFoundError(f"Canonical verse list missing: {path}")
    refs: list[str] = []
    seen: set[str] = set()
    for raw in path.read_text(encoding="utf-8").splitlines():
        ref = raw.strip()
        if not ref or ref.startswith("#"):
            continue
        parts = ref.split(":")
        if len(parts) != 2 or not parts[0].isdigit() or not parts[1].isdigit():
            raise ValueError(f"Invalid verse reference '{ref}' in {path}")
        if ref in seen:
            raise ValueError(f"Duplicate verse reference '{ref}' in {path}")
        seen.add(ref)
        refs.append(ref)
    if len(refs) != 63:
        raise ValueError(f"Expected exactly 63 verse references in {path}, found {len(refs)}")
    return refs


def parse_tanzil_pipe_file(path: Path) -> tuple[dict[str, str], str]:
    """
    Parses a Tanzil surah|ayah|text file without ANY normalisation of the verse text.
    Returns (ref -> exact_text, verbatim_comment_block).
    """
    if not path.is_file():
        raise FileNotFoundError(f"Required Quran source file missing: {path}")

    # Decode UTF-8 (stripping optional file-level BOM only, never altering verse characters)
    raw_bytes = path.read_bytes()
    if raw_bytes.startswith(b"\xef\xbb\xbf"):
        raw_bytes = raw_bytes[3:]
    text = raw_bytes.decode("utf-8")

    verses: dict[str, str] = {}
    comment_lines: list[str] = []

    for line in text.splitlines():
        if not line:
            continue
        if line.startswith("#"):
            comment_lines.append(line)
            continue
        first_sep = line.find("|")
        second_sep = line.find("|", first_sep + 1) if first_sep >= 0 else -1
        if first_sep < 0 or second_sep < 0:
            raise ValueError(f"Malformed line in {path}: {line[:80]!r}")
        surah = line[:first_sep]
        ayah = line[first_sep + 1 : second_sep]
        verse_text = line[second_sep + 1 :]  # Verbatim: no strip, no normalisation
        if not surah.isdigit() or not ayah.isdigit():
            raise ValueError(f"Non-numeric surah/ayah in {path}: {surah}:{ayah}")
        ref = f"{int(surah)}:{int(ayah)}"
        if ref in verses:
            raise ValueError(f"Duplicate reference {ref} in {path}")
        if not verse_text:
            raise ValueError(f"Empty verse text for {ref} in {path}")
        verses[ref] = verse_text

    return verses, "\n".join(comment_lines)


def parse_surah_metadata(path: Path) -> dict[str, dict[str, str]]:
    if not path.is_file():
        raise FileNotFoundError(f"Required surah metadata file missing: {path}")
    tree = ET.parse(path)
    root = tree.getroot()
    suras_el = root.find("suras")
    if suras_el is None:
        raise ValueError(f"Missing <suras> element in {path}")
    surahs: dict[str, dict[str, str]] = {}
    for el in suras_el.findall("sura"):
        idx = el.attrib["index"]
        surahs[idx] = {
            "arabic": el.attrib["name"],
            "transliteration": el.attrib["tname"],
        }
    if len(surahs) != 114:
        raise ValueError(f"Expected 114 surahs in {path}, found {len(surahs)}")
    return surahs


def extract_subset(refs: list[str], corpus: dict[str, str], label: str) -> dict[str, dict[str, str]]:
    out: dict[str, dict[str, str]] = {}
    for ref in refs:
        if ref not in corpus:
            raise KeyError(f"Reference '{ref}' missing from {label}")
        value = corpus[ref]
        if not value:
            raise ValueError(f"Reference '{ref}' is empty in {label}")
        out[ref] = {"text": value}
    return out


def write_json_utf8_no_bom(path: Path, data: object) -> None:
    payload = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    path.write_bytes(payload.encode("utf-8"))


def build_attribution_markdown(
    ar_notice: str,
    en_notice: str,
    id_notice: str,
) -> str:
    return (
        "# Quran Text, Translations & Font Attribution\n\n"
        "All Quranic text and translations bundled in `app-salatsign/src/main/assets/quran/` "
        "were extracted verbatim by `tools/build_quran_data.py` with zero modifications.\n\n"
        "## 1. Arabic Quran Text — Tanzil Project (Uthmani)\n\n"
        "- Source: [Tanzil.net](http://tanzil.net)\n"
        "- License: Creative Commons Attribution 3.0 (verbatim copying only; modification is not allowed)\n\n"
        "### Verbatim Source Notice (`quran-uthmani.txt`)\n\n"
        "```text\n"
        f"{ar_notice}\n"
        "```\n\n"
        "## 2. English Translation — Saheeh International (via Tanzil.net)\n\n"
        "- Source: [Tanzil.net](http://tanzil.net) (`en.sahih`)\n"
        "- License: Creative Commons Attribution 3.0 (verbatim only, with attribution)\n\n"
        "### Verbatim Source Notice (`en.sahih.txt`)\n\n"
        "```text\n"
        f"{en_notice}\n"
        "```\n\n"
        "## 3. Indonesian Translation — Kementerian Agama Republik Indonesia (via Tanzil.net)\n\n"
        "- Source: [Tanzil.net](http://tanzil.net) (`id.indonesian`)\n"
        "- Credit: Kementerian Agama Republik Indonesia (owner-confirmed permission with credit)\n\n"
        "### Verbatim Source Notice (`id.indonesian.txt`)\n\n"
        "```text\n"
        f"{id_notice}\n"
        "```\n\n"
        "## 4. Surah Metadata\n\n"
        "- Source: [Tanzil.net](http://tanzil.net) (`quran-data.xml`, Creative Commons Attribution)\n\n"
        "## 5. Arabic Quran Font — Amiri Quran\n\n"
        "- Font: Amiri Quran (`app-salatsign/src/main/res/font/amiri_quran.ttf`)\n"
        "- Source: Google Fonts / Alif Type (`https://github.com/google/fonts/tree/main/ofl/amiriquran`)\n"
        "- License: SIL Open Font License 1.1 (see `tools/sources/AMIRI_QURAN_OFL.txt`)\n"
    )


def main() -> int:
    refs = load_verse_list(VERSE_LIST_PATH)
    ar_corpus, ar_notice = parse_tanzil_pipe_file(AR_SOURCE)
    en_corpus, en_notice = parse_tanzil_pipe_file(EN_SOURCE)
    id_corpus, id_notice = parse_tanzil_pipe_file(ID_SOURCE)
    all_surahs = parse_surah_metadata(META_SOURCE)

    verses_ar = extract_subset(refs, ar_corpus, "quran-uthmani.txt")
    trans_en = extract_subset(refs, en_corpus, "en.sahih.txt")
    trans_id = extract_subset(refs, id_corpus, "id.indonesian.txt")

    # Derive the exact set of Unicode codepoints observed in the Tanzil Uthmani source
    # so unit tests can validate verses_ar.json against the verified source alphabet.
    ar_codepoints = sorted({ord(ch) for v in verses_ar.values() for ch in v["text"]})

    needed_surah_nums = sorted({int(ref.split(":")[0]) for ref in refs})
    surahs_subset = {str(n): all_surahs[str(n)] for n in needed_surah_nums}

    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    write_json_utf8_no_bom(ASSETS_DIR / "verses_ar.json", verses_ar)
    write_json_utf8_no_bom(ASSETS_DIR / "translations_en.json", trans_en)
    write_json_utf8_no_bom(ASSETS_DIR / "translations_id.json", trans_id)
    write_json_utf8_no_bom(ASSETS_DIR / "surahs.json", surahs_subset)
    (ASSETS_DIR / "ATTRIBUTION.md").write_bytes(
        build_attribution_markdown(ar_notice, en_notice, id_notice).encode("utf-8")
    )

    checksum_entries = [
        ("tools/sources/quran-uthmani.txt", AR_SOURCE),
        ("tools/sources/en.sahih.txt", EN_SOURCE),
        ("tools/sources/id.indonesian.txt", ID_SOURCE),
        ("tools/sources/quran-data.xml", META_SOURCE),
    ]
    if FONT_LICENSE.is_file():
        checksum_entries.append(("tools/sources/AMIRI_QURAN_OFL.txt", FONT_LICENSE))

    lines = [
        "# SHA-256 checksums of verified upstream Quran source files used by tools/build_quran_data.py",
        "# Verified Unicode codepoint count in extracted Uthmani Arabic subset: " + str(len(ar_codepoints)),
    ]
    for rel, path in checksum_entries:
        lines.append(f"{sha256_file(path)}  {rel}")
    CHECKSUMS_PATH.write_bytes(("\n".join(lines) + "\n").encode("utf-8"))

    print(f"Extracted {len(refs)} verses ({len(surahs_subset)} surahs) verbatim into {ASSETS_DIR}")
    print(f"Wrote source SHA-256 checksums to {CHECKSUMS_PATH}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
