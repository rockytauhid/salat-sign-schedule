# SalatSign — License, Trademark and Attribution Drafts

For the agent working on **Phase 7 (P7.1)** of `SALATSIGN_REMEDIATION_PLAN.md`. These are templates, not legal advice. The contact email is filled in (rockytauhid@gmail.com).

Owner facts (confirmed): copyright holder **Rocky Adam Tauhid**; code license **GPL-3.0-or-later**; the name "SalatSign" is **not registered** but the owner does not want others to use it; others may redistribute **unmodified** copies if they keep the owner's name/credit; forks are welcome under a **different name**.

---

## 1. Files to create or change

| File | Action |
|------|--------|
| `LICENSE` | Official, unmodified GPL-3.0 text (copy from gnu.org; do not paraphrase) |
| `TRADEMARKS.md` | Section 3 below |
| `CONTRIBUTING.md` | Section 5 below (DCO sign-off) |
| `README.md` | Replace the MIT badge/claim with Section 4 |
| Source files (`*.kt`, `*.kts`) | Add the header in Section 2 |
| About screen (app) | Add the credit line in Section 6 |
| `branding/` (only if logo/banner files exist) | Move them here; add `branding/README.md` with the logo terms in Section 3 |

---

## 2. Source file header

```kotlin
// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors
```

Notes:
- `or-later` lets future GPL versions apply. Use `GPL-3.0-only` instead if the owner wants to pin version 3.
- Do not add the header to generated files, resources, or the audio/font files (those follow `docs/THIRD_PARTY.md` and `docs/AUDIO_SOURCES.md`).

---

## 3. `TRADEMARKS.md` (draft)

```markdown
# SalatSign Trademark Policy

"SalatSign" and the SalatSign logo (together, the "Marks") are claimed as trademarks
of Rocky Adam Tauhid. The Marks are not registered; the owner relies on his rights
from using the Marks and reserves the right to register them.

The SalatSign source code is licensed under the GNU General Public License v3.0 or
later (see `LICENSE`). That license covers the code. It does not grant any right to
use the Marks. As permitted by section 7 of the GPL, no rights are granted to use
the Marks except as set out below.

## You may

1. **Redistribute unmodified copies.** You may redistribute the unmodified official
   source or the unmodified official builds under the name "SalatSign", as long as
   you:
   - keep all copyright notices, the `LICENSE` file, this policy, and the credit
     "SalatSign by Rocky Adam Tauhid" (including in the app's About screen);
   - do not claim to be the author, the official project, or endorsed by it.
2. **Refer to the project accurately,** for example "based on SalatSign", "a fork of
   SalatSign" or "works with SalatSign", in plain text and without implying
   endorsement.
3. **Fork and modify the code** under the GPL, provided you follow the rules below.

## If you modify SalatSign (fork)

- Use a **different name and logo.** Do not use "SalatSign", and do not use names
  that could be confused with it (for example "SalatSign Pro", "SalatSign 2",
  "Salat Sign", "SalatSigns").
- Keep the existing copyright notices, and state clearly that your version is based
  on SalatSign by Rocky Adam Tauhid and that you changed it.
- Do not present your version as official, and do not use the Marks in app store
  listings, package names, domains or social media handles in a way that suggests
  it is.

## Not allowed

- Publishing a modified, rebranded or otherwise changed version under the SalatSign
  name or logo.
- Using the Marks in a way that is likely to mislead people about who made the
  software or whether it is official.

## Questions or permission

To ask for permission to use the Marks in another way, or to report misuse,
contact: rockytauhid@gmail.com.

This policy may be updated; the version in the `main` branch applies.
```

**Logo terms (`branding/README.md`)**, if logo files exist:

```markdown
The files in this folder are the SalatSign logo and brand assets. They are NOT
covered by the GPL. All rights reserved by Rocky Adam Tauhid. You may use them only
to refer to the official SalatSign project, as described in `TRADEMARKS.md`.
```

Practical note for the owner (not for the file): app stores require a unique application ID, so a store listing built from the source under another developer account must change `applicationId`. That change is a modification, so such a version must also use a different name. Only the owner's signed official builds can legitimately carry the SalatSign name.

---

## 4. README section (replace the MIT badge and license text)

```markdown
## License

SalatSign is free software: you can redistribute it and/or modify it under the terms
of the GNU General Public License, version 3 or (at your option) any later version.
See [LICENSE](LICENSE).

Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors.

The name "SalatSign" and the logo are not covered by this license; see
[TRADEMARKS.md](TRADEMARKS.md). You are welcome to fork the project under a
different name.

Third-party libraries, fonts and audio are listed in
[docs/THIRD_PARTY.md](docs/THIRD_PARTY.md) and
[docs/AUDIO_SOURCES.md](docs/AUDIO_SOURCES.md). The project does not distribute
third-party adhan recordings that lack a clear license.
```

---

## 5. `CONTRIBUTING.md` (DCO)

```markdown
# Contributing

Thank you for helping. By contributing you agree that your contribution is licensed
under the project's license (GPL-3.0-or-later) and that you have the right to submit
it.

We use the Developer Certificate of Origin (https://developercertificate.org/).
Sign off every commit:

    git commit -s -m "Describe your change"

This adds a `Signed-off-by: Your Name <you@example.com>` line.

Please do not submit audio, images or fonts unless their license is clear and
compatible (CC0, public domain, CC BY, OFL or similar), and add them to
`docs/THIRD_PARTY.md` or `docs/AUDIO_SOURCES.md`.

Forks must follow `TRADEMARKS.md`.
```

---

## 6. In-app About screen credit (English string)

```xml
<string name="about_credit">SalatSign by Rocky Adam Tauhid</string>
<string name="about_license">Open source under GPL-3.0-or-later</string>
```

Show the credit and the license line on the About screen (create the screen if it does not exist, reachable from Settings), plus a link or text pointing to the repository.

---

## 7. Agent checklist

- [ ] `LICENSE` is the verbatim GPL-3.0 text.
- [ ] README no longer mentions MIT anywhere (search the whole repo for "MIT").
- [ ] `TRADEMARKS.md` added with the contact email rockytauhid@gmail.com.
- [ ] SPDX header present on all hand-written source files.
- [ ] `CONTRIBUTING.md` added.
- [ ] About screen shows the credit and license.
- [ ] `docs/THIRD_PARTY.md` lists libraries and fonts with their licenses and confirms compatibility with GPL-3.0.
