# SalatSign Phone Controller Plan (Web-Only)

**Status:** Approved (25 November 2026)  
**Supersedes:** App-based "SalatSign Phone Companion Plan" (no native companion app will be built).

The TV-hosted web Phone Controller in `:app-salatsign` is the primary remote administration interface and is extended to support browser pairing, always-on operation, multi-TV control, urgent notices, and room-level slide synchronization.

---

## Confirmed Architectural Decisions

1. **Boot & Always-On (D18 preserved):** When "Keep Phone Controller always on" is enabled, a `RECEIVE_BOOT_COMPLETED` receiver auto-launches `SalatSign` (`MainActivity`) on TV boot and runs the HTTP server inside the app process. Zero `FOREGROUND_SERVICE` or `POST_NOTIFICATIONS` permissions are added to `:app-salatsign`.
2. **Group-Synced Pairing & Revocation:** Paired device token hashes (`deviceId`, `tokenHash`, `role`, `createdAt`, `revokedAt`, `lastSeenAt`) sync across the TV group with revocation tombstones. A phone paired with TV A works seamlessly against TV B/C/D, and revoking on any TV revokes access everywhere.
3. **Browser Token Lifetime:** Paired tokens in `localStorage` remain valid until revoked, with an optional TV-side inactivity auto-expiry setting (`Never` default, optional `30` / `90` days).
4. **Group vs. Local Config Split:**
   - **Synced across the TV group (`GroupSyncConfig`, versioned by `configVersion`):** Mosque branding, location & prayer calculation settings, offsets, Iqamah, Jumu'ah, announcement slides, built-in slide settings, Silent Screen settings, Urgent Notices, and paired-device records/revocations.
   - **Local per TV (`LocalDeviceConfig`, never overwritten by peers):** `deviceId`, `deviceName`, `roomGroup`, Theme, and Always-On Phone Controller toggle.
5. **Display Precedence Hierarchy:**
   `Critical Emergency (Phase 5, deferred) > Silent Screen > Urgent Notice > Prayer Schedule & Slideshow`
   - **Urgent Notice (Phase 3c):** Full-screen high-contrast alert card (title, bold message, remaining minutes) that overrides Prayer Schedule & Slideshow, but yields to Silent Screen during active Jumu'ah/prayer so it never interrupts prayer.
6. **Transport:** Plain HTTP on the local LAN (no self-signed HTTPS browser warnings). Damage of a captured token is minimized via `Authorization` header only (never URLs), hashed token storage, instant revocation, TV pairing notice, optional inactivity expiry, and mandatory Urgent Notice expiry.

---

## Phase 0: Foundations *(Complete)*

- [x] Fix CI release keystore verification (R3-1) and auto-upgrade legacy 10k PBKDF2 PIN hashes on successful login (R3-2).
- [x] Move API request/response DTOs and JSON serialization into `:core` using `kotlinx.serialization`, replacing hand-rolled `SimpleJson`.
- [x] Add persistent TV identity (`deviceId` stable UUID, `deviceName`, `roomGroup`) and return them from `/api/v1/status` and mDNS TXT records.
- [x] Add monotonic `configVersion`, `GET`/`PUT /api/v1/config`, `ETag` + `If-Match` (`412 Precondition Failed`) on writes, and client-supplied UUID / idempotent slide writes.
- [x] Serve the Phone Controller web UI from `app-salatsign/src/main/assets/phone_controller/index.html` instead of a Kotlin string, preserving CSP, Host validation, and security headers.
- [x] Amend `AGENTS.md` Rule 6 so the web Phone Controller is explicitly the primary remote UI.

**Done when:** Web UI and tests call the same typed `:core` API contract, every write is retry-safe with `If-Match`, and CI passes with debug + release builds.

---

## Phase 1: Browser Pairing & Auth *(Complete)*

- [x] PIN-gated TV screen (**Settings → PIN & Remote → Pair Phone**) showing a short-lived single-use 6-digit pairing code + offline ZXing QR code (`/#pair=<code>`).
- [x] Browser exchanges code via `POST /api/v1/pair` for a 256-bit random bearer token stored in `localStorage`; TV stores only SHA-256 hash + metadata (`deviceId`, `deviceName`, `role`, `createdAt`, `lastSeenAt`, `revokedAt`).
- [x] Paired devices list on the TV with instant revoke + brief 6-second on-TV notice when a new device pairs.
- [x] Roles: `MANAGE` (slides, settings, Silent Screen, devices) and `NOTIFY` (Urgent Notices). (`CRITICAL` reserved for Phase 5.)
- [x] Per-client IP rate limiting & progressive lockout on wrong pairing codes; once ≥ 1 device is paired, "Allow PIN login over LAN" defaults to OFF.
- [x] Optional TV-side inactivity token expiry (`Never` default, `30` / `90` days).

---

## Phase 2: Always-On Controller

- Persisted "Keep Phone Controller always on" toggle.
- `RECEIVE_BOOT_COMPLETED` receiver auto-launches `MainActivity` on boot when enabled and starts the local server in-process (no foreground service / notifications).
- TV Settings displays server status, paired device count, `deviceId`, `deviceName`, `roomGroup`, URL, and QR code.

---

## Phase 3: Multi-TV Group Control & Urgent Notice

- **3a. TV Group Setup:** Link TVs via one-time group join code (shares 256-bit group secret + peer list). TV-to-TV requests signed with HMAC-SHA256 + timestamp + nonce; mDNS (`_salatsign._tcp`) re-discovers peers when IPs change.
- **3b. Multi-TV Control:** Browser opened on any TV loads the peer list and targets all TVs, a room group, or selected TVs, showing per-TV status (`delivered`, `pending`, `failed`) with retry. Images uploaded once to the entry TV and fetched by peers via content hash. Offline catch-up pulls newer `configVersion` (tiebreaker: `deviceId`).
- **3c. Urgent Notice:** Custom text + neutral presets, 5/10/15-min expiry or manual clear (`NOTIFY` role). Enforced locally from receive time; renders full-screen above schedule/slides and yields to Silent Screen.

---

## Phase 4: Leader/Follower Sync per Room

- Per-room-group leader announces `"slide N, switch at time T"` over the signed TV-to-TV channel; followers estimate clock offset via ping-pong and fall back to local timers if messages stop.
- Seeded Quran verse selection (`date + slot + configVersion`) keeps both room TVs showing the exact same verse.

---

## Phase 5: Critical Emergency *(Deferred)*

- Full-screen red emergency override sitting above Silent Screen, 1-tap presets with confirm step, and `CRITICAL` role once Phases 1–3 are field-tested.
