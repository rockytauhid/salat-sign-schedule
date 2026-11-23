// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

/**
 * Self-contained, offline mobile-first web UI served directly from SalatSign TV over LAN.
 * Matches the TV Settings Hub categories for Day-to-Day Mosque Administration:
 * Slides (with 6-slot budget, edit/toggle/delete, Islamic Calendar & Quran Verse slides),
 * Friday Jumu'ah (intuitive time pickers + add/remove sittings) & Iqamah,
 * Silent Screen (manual + Jumu'ah + daily schedules), and Mosque Branding & Display.
 */
object PhoneControllerHtml {

    fun renderHtml(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
<title>SalatSign Remote Controller</title>
<style>
  :root {
    --bg: #071912; --card: #0f2a1f; --card2: #143628; --border: #1e4d38;
    --emerald: #10b981; --gold: #fbbf24; --text: #f3f4f6; --muted: #9ca3af; --danger: #ef4444;
  }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); padding: 14px; }
  .wrap { max-width: 600px; margin: 0 auto 40px; }
  .topbar { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-bottom: 10px; }
  h1 { margin: 0; font-size: 1.4rem; color: var(--text); }
  h1 span { color: var(--gold); }
  .sub { color: var(--muted); font-size: 0.85rem; margin-top: 2px; }
  .tabs { display: grid; grid-template-columns: repeat(4, 1fr); gap: 6px; margin-bottom: 14px; position: sticky; top: 0; background: var(--bg); padding: 6px 0; z-index: 5; }
  .tab { margin: 0; padding: 10px 6px; border-radius: 10px; background: var(--card); color: var(--muted); border: 1px solid var(--border); font-weight: 700; font-size: 0.82rem; text-align: center; cursor: pointer; }
  .tab.active { background: var(--card2); color: var(--emerald); border-color: var(--emerald); }
  .card { background: var(--card); border: 1px solid var(--border); border-radius: 14px; padding: 16px; margin-bottom: 14px; }
  .card h2 { margin: 0 0 10px; font-size: 1.02rem; color: var(--emerald); }
  .card h3 { margin: 14px 0 6px; font-size: 0.92rem; color: var(--gold); }
  label { display: block; font-size: 0.82rem; color: var(--muted); margin: 10px 0 4px; }
  input, select, textarea { width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border); background: #071912; color: var(--text); font-size: 0.96rem; }
  textarea { min-height: 92px; resize: vertical; line-height: 1.4; }
  button { width: 100%; margin-top: 12px; padding: 11px 14px; border: 0; border-radius: 10px; background: var(--emerald); color: #052014; font-weight: 700; font-size: 0.93rem; cursor: pointer; }
  button.secondary { background: #1c3f30; color: var(--text); }
  button.sm { width: auto; margin-top: 0; padding: 6px 10px; font-size: 0.8rem; border-radius: 8px; }
  button.danger { background: var(--danger); color: #fff; }
  button.danger-full { background: var(--danger); color: #fff; width: 100%; margin-top: 10px; }
  button.outline { background: transparent; color: var(--text); border: 1px solid var(--border); }
  .row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .row3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .flex-between { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
  .flex-gap { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
  .pill { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 0.78rem; font-weight: 700; }
  .pill.active { background: #7f1d1d; color: #fecaca; }
  .pill.idle { background: #1c3f30; color: #a7f3d0; }
  .pill.warn { background: #78350f; color: #fde68a; }
  .item-box { padding: 10px 12px; border-radius: 10px; background: #071912; border: 1px solid var(--border); margin-top: 8px; }
  .meta { font-size: 0.8rem; color: var(--muted); }
  .sitting-row { display: grid; grid-template-columns: 92px 1fr auto; gap: 8px; align-items: center; margin-top: 8px; padding: 10px; border-radius: 10px; background: #071912; border: 1px solid var(--border); }
  .iq-row { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 9px 10px; border-radius: 10px; background: #071912; border: 1px solid var(--border); margin-top: 7px; }
  .iq-chips { display: flex; gap: 4px; flex-wrap: wrap; }
  .chip { margin: 0; width: auto; padding: 5px 9px; font-size: 0.78rem; border-radius: 7px; background: var(--card); color: var(--text); border: 1px solid var(--border); font-weight: 600; }
  .chip.sel { background: var(--emerald); color: #052014; border-color: var(--emerald); }
  .msg { padding: 10px 12px; border-radius: 8px; margin-bottom: 12px; font-size: 0.88rem; display: none; }
  .msg.ok { display: block; background: #064e3b; color: #a7f3d0; }
  .msg.err { display: block; background: #7f1d1d; color: #fecaca; }
  .warn-box { padding: 10px 12px; border-radius: 10px; background: #78350f; color: #fde68a; font-size: 0.85rem; margin-bottom: 10px; }
  .hidden { display: none; }
</style>
</head>
<body>
<div class="wrap">
  <div class="topbar">
    <div>
      <h1>Salat<span>Sign</span> Remote</h1>
      <div class="sub" id="statusLine">Local LAN Controller</div>
    </div>
    <button id="logoutBtn" class="sm outline hidden" onclick="logout()">Sign Out</button>
  </div>
  <div id="msg" class="msg"></div>

  <div id="loginCard" class="card">
    <h2>Admin PIN Sign-In</h2>
    <div id="defaultPinNotice" class="warn-box hidden">
      Remote login requires a custom Admin PIN. On the TV, open <strong>Settings → Remote, PIN &amp; About</strong> and change the default PIN first.
    </div>
    <label for="pinInput">Enter Mosque Admin PIN (4–8 digits)</label>
    <input id="pinInput" type="password" inputmode="numeric" maxlength="8" placeholder="••••" />
    <button onclick="login()">Unlock TV Controller</button>
  </div>

  <div id="app" class="hidden">
    <div class="tabs">
      <button class="tab active" id="tab-slides" onclick="openTab('slides')">Slides</button>
      <button class="tab" id="tab-schedule" onclick="openTab('schedule')">Jumu'ah</button>
      <button class="tab" id="tab-silent" onclick="openTab('silent')">Silent</button>
      <button class="tab" id="tab-branding" onclick="openTab('branding')">Display</button>
    </div>

    <!-- TAB 1: SLIDES & BUILT-IN SLIDES -->
    <div id="pane-slides">
      <div class="card">
        <div class="flex-between">
          <h2>Announcement Slides</h2>
          <span id="slideBudgetBadge" class="pill idle">0 / 6 active</span>
        </div>
        <label>Prayer Schedule Display Duration</label>
        <div class="flex-gap" id="schedDurChips">
          <button class="chip" onclick="setSchedDur(30)">30s</button>
          <button class="chip" onclick="setSchedDur(60)">60s</button>
          <button class="chip" onclick="setSchedDur(90)">90s</button>
          <button class="chip" onclick="setSchedDur(120)">120s</button>
        </div>

        <h3>Saved Slides</h3>
        <div id="slidesList"></div>
      </div>

      <div class="card">
        <h2 id="textFormHeading">Add Text Announcement Slide</h2>
        <input id="editingSlideId" type="hidden" value="" />
        <label>Title</label>
        <input id="slideTitle" type="text" maxlength="80" placeholder="e.g. Friday Khutbah &amp; Community Dinner" />
        <label>Message (up to 6 rows)</label>
        <textarea id="slideBody" maxlength="400" placeholder="Type announcement message..." oninput="enforceBodyLines()"></textarea>
        <div class="meta" id="bodyCounter">1/6 rows • 0/400 chars</div>
        <label>Display Duration (seconds)</label>
        <select id="slideDuration">
          <option value="10">10s</option>
          <option value="15" selected>15s</option>
          <option value="20">20s</option>
          <option value="30">30s</option>
          <option value="45">45s</option>
          <option value="60">60s</option>
        </select>
        <div class="row">
          <button id="saveTextSlideBtn" onclick="saveTextSlide()">Add Text Slide</button>
          <button id="cancelEditBtn" class="secondary hidden" onclick="cancelEditSlide()">Cancel Edit</button>
        </div>
      </div>

      <div class="card">
        <h2>Upload Image Slide</h2>
        <label>Slide Title</label>
        <input id="imgSlideTitle" type="text" maxlength="80" placeholder="e.g. Ramadan Poster" />
        <div class="row">
          <div>
            <label>Display Duration</label>
            <select id="imgSlideDuration">
              <option value="10">10s</option>
              <option value="15" selected>15s</option>
              <option value="20">20s</option>
              <option value="30">30s</option>
              <option value="45">45s</option>
              <option value="60">60s</option>
            </select>
          </div>
          <div>
            <label>Image File (JPG/PNG/WebP)</label>
            <input id="slideImageFile" type="file" accept="image/*" />
          </div>
        </div>
        <button class="secondary" onclick="uploadImageSlide()">Upload Image Slide</button>
      </div>

      <div class="card">
        <h2>Built-in Slides (1 slot each)</h2>
        <div class="row">
          <div>
            <label>Islamic Calendar Slide</label>
            <select id="calSlideEnabled">
              <option value="false">Disabled</option>
              <option value="true">Enabled</option>
            </select>
          </div>
          <div>
            <label>Calendar Duration</label>
            <select id="calSlideDuration">
              <option value="10">10s</option><option value="15">15s</option><option value="20">20s</option>
              <option value="30">30s</option><option value="45">45s</option><option value="60">60s</option>
            </select>
          </div>
        </div>
        <div class="row3" style="margin-top:8px">
          <div>
            <label>Quran Verse Slide</label>
            <select id="quranEnabled">
              <option value="false">Disabled</option>
              <option value="true">Enabled</option>
            </select>
          </div>
          <div>
            <label>Translation</label>
            <select id="quranTrans">
              <option value="true">Shown</option>
              <option value="false">Arabic Only</option>
            </select>
          </div>
          <div>
            <label>Quran Duration</label>
            <select id="quranDur">
              <option value="0">Auto</option><option value="15">15s</option><option value="20">20s</option>
              <option value="30">30s</option><option value="45">45s</option><option value="60">60s</option>
            </select>
          </div>
        </div>
        <button onclick="saveBuiltInSlides()">Save Built-in Slides</button>
      </div>
    </div>

    <!-- TAB 2: FRIDAY JUMU'AH, IQAMAH & HIJRI -->
    <div id="pane-schedule" class="hidden">
      <div class="card">
        <h2>Friday Jumu'ah Sittings</h2>
        <label>Calculation Mode</label>
        <select id="jumuahMode" onchange="onJumuahModeChange()">
          <option value="FIXED">Fixed Clock Time (e.g. 13:00)</option>
          <option value="AFTER_ZUHR">Minutes After Dhuhr (e.g. +45 min)</option>
        </select>
        <div id="sittingsContainer"></div>
        <button id="addSittingBtn" class="secondary" onclick="addSitting()">+ Add Sitting (max 3)</button>
      </div>

      <div class="card">
        <h2>Iqamah Delays (Minutes After Adhan)</h2>
        <div id="iqamahContainer"></div>
      </div>

      <div class="card">
        <h2>Hijri Calendar Adjustment</h2>
        <div class="row">
          <div>
            <label>Hijri Day Offset (-2 to +2)</label>
            <select id="hijriOffset">
              <option value="-2">-2 days</option><option value="-1">-1 day</option>
              <option value="0">0 (Umm al-Qura)</option>
              <option value="1">+1 day</option><option value="2">+2 days</option>
            </select>
          </div>
          <div>
            <label>Date Changes At</label>
            <select id="hijriMidnight">
              <option value="false">Maghrib (Sunset)</option>
              <option value="true">Midnight (00:00)</option>
            </select>
          </div>
        </div>
        <button onclick="saveSchedule()">Save Jumu'ah, Iqamah &amp; Hijri</button>
      </div>
    </div>

    <!-- TAB 3: SILENT SCREEN -->
    <div id="pane-silent" class="hidden">
      <div class="card">
        <h2>Manual Silent Screen Control</h2>
        <div id="silentStatus" class="pill idle">Inactive</div>
        <div class="meta">Immediately show "Prayer in progress" on the TV (overrides schedule and slides).</div>
        <div class="row3">
          <button onclick="startSilent(15)">Show 15 min</button>
          <button onclick="startSilent(30)">Show 30 min</button>
          <button onclick="startSilent(45)">Show 45 min</button>
        </div>
        <button id="stopSilentBtn" class="danger-full hidden" onclick="stopSilent()">Stop Silent Screen Now</button>
      </div>

      <div class="card">
        <h2>Friday Jumu'ah Silent Screen</h2>
        <div class="row3">
          <div>
            <label>Status</label>
            <select id="silentJumuahEnabled">
              <option value="true">Enabled</option>
              <option value="false">Disabled</option>
            </select>
          </div>
          <div>
            <label>Start Before</label>
            <select id="silentJumuahLead">
              <option value="0">0 min</option><option value="5">5 min</option>
              <option value="10">10 min</option><option value="15">15 min</option>
            </select>
          </div>
          <div>
            <label>Duration</label>
            <select id="silentJumuahDur">
              <option value="30">30 min</option><option value="45">45 min</option>
              <option value="60">60 min</option><option value="75">75 min</option><option value="90">90 min</option>
            </select>
          </div>
        </div>

        <h3>Daily Prayers (Optional)</h3>
        <div id="dailySilentContainer"></div>
        <button onclick="saveSilentSettings()">Save Silent Screen Schedule</button>
      </div>
    </div>

    <!-- TAB 4: BRANDING & DISPLAY -->
    <div id="pane-branding" class="hidden">
      <div class="card">
        <h2>Mosque Branding &amp; Display</h2>
        <label>Mosque Name</label>
        <input id="mosqueName" type="text" maxlength="60" placeholder="e.g. Canberra Islamic Centre" />
        <label>Subtitle / Tagline</label>
        <input id="mosqueSubtitle" type="text" maxlength="80" placeholder="e.g. Please silence your phone" />
        <div class="row">
          <div>
            <label>Mosque Emblem Preset</label>
            <select id="mosqueIconPreset">
              <option value="DOME_CRESCENT">Dome &amp; Crescent</option>
              <option value="MINARET">Minaret</option>
              <option value="MIHRAB">Mihrab Arch</option>
              <option value="CUSTOM">Custom Logo</option>
            </select>
          </div>
          <div>
            <label>Mosque Name Font</label>
            <select id="mosqueFontPreset">
              <option value="MODERN_SANS">Modern Sans</option>
              <option value="TRADITIONAL_SERIF">Traditional Serif</option>
              <option value="ELEGANT_DISPLAY">Elegant Display</option>
              <option value="CLEAN_TECH">Clean Tech</option>
            </select>
          </div>
        </div>
        <div class="row">
          <div>
            <label>Theme Mode</label>
            <select id="themeMode">
              <option value="ALWAYS_DARK">Always Dark</option>
              <option value="ALWAYS_LIGHT">Always Light</option>
              <option value="AUTO_DAY_NIGHT">Auto Day/Night</option>
            </select>
          </div>
          <div>
            <label>Time Format</label>
            <select id="is24Hour">
              <option value="false">12-Hour (AM/PM)</option>
              <option value="true">24-Hour (00:00)</option>
            </select>
          </div>
        </div>
        <div class="row">
          <div>
            <label>Arabic Prayer Names</label>
            <select id="showArabic">
              <option value="true">Shown</option>
              <option value="false">Hidden</option>
            </select>
          </div>
          <div>
            <label>Exact-Time Visual Strike</label>
            <select id="showStrike">
              <option value="true">Active (5m pulse)</option>
              <option value="false">Disabled</option>
            </select>
          </div>
        </div>
        <button onclick="saveBranding()">Save Branding &amp; Display</button>

        <h3>Custom Mosque Logo</h3>
        <input id="logoFile" type="file" accept="image/*" />
        <button class="secondary" onclick="uploadLogo()">Upload Mosque Logo</button>
      </div>
    </div>
  </div>
</div>

<script>
let token = sessionStorage.getItem('salatsign_token') || '';
let cachedSlides = [];
let currentSchedDur = 60;
let jumuahSittings = ['13:00'];
let iqamahValues = { SUBUH: 0, ZUHUR: 0, ASHR: 0, MAGRIB: 0, ISYA: 0 };
let dailySilent = {};
const PRAYERS = [
  { key: 'SUBUH', label: 'Fajr' },
  { key: 'ZUHUR', label: 'Dhuhr' },
  { key: 'ASHR', label: 'Asr' },
  { key: 'MAGRIB', label: 'Maghrib' },
  { key: 'ISYA', label: 'Isha' }
];

function showMsg(text, ok) {
  const el = document.getElementById('msg');
  el.textContent = text;
  el.className = 'msg ' + (ok ? 'ok' : 'err');
}
function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function openTab(name) {
  ['slides','schedule','silent','branding'].forEach(t => {
    document.getElementById('pane-' + t).classList.toggle('hidden', t !== name);
    document.getElementById('tab-' + t).classList.toggle('active', t === name);
  });
}
async function api(path, opts = {}) {
  const headers = opts.headers || {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(path, { ...opts, headers });
  const data = await res.json().catch(() => ({}));
  if (res.status === 401 && path !== '/api/v1/auth') {
    token = '';
    sessionStorage.removeItem('salatsign_token');
    document.getElementById('app').classList.add('hidden');
    document.getElementById('logoutBtn').classList.add('hidden');
    document.getElementById('loginCard').classList.remove('hidden');
  }
  return { ok: res.ok, status: res.status, data };
}
async function loadStatus() {
  const res = await api('/api/v1/status');
  if (res.ok) {
    document.getElementById('statusLine').textContent = 'SalatSign TV • v' + (res.data.version || '1.0');
    document.getElementById('defaultPinNotice').classList.toggle('hidden', !!res.data.pinConfigured);
  }
}
async function login() {
  const pin = document.getElementById('pinInput').value.trim();
  const res = await api('/api/v1/auth', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pin })
  });
  if (res.ok && res.data.token) {
    token = res.data.token;
    sessionStorage.setItem('salatsign_token', token);
    document.getElementById('pinInput').value = '';
    document.getElementById('loginCard').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    document.getElementById('logoutBtn').classList.remove('hidden');
    showMsg('Connected to SalatSign TV.', true);
    await loadAll();
  } else {
    showMsg(res.data.message || 'Authentication failed.', false);
  }
}
async function logout() {
  await api('/api/v1/logout', { method: 'POST' });
  token = '';
  sessionStorage.removeItem('salatsign_token');
  document.getElementById('app').classList.add('hidden');
  document.getElementById('logoutBtn').classList.add('hidden');
  document.getElementById('loginCard').classList.remove('hidden');
  showMsg('Signed out.', true);
}
async function loadAll() {
  const s = await api('/api/v1/settings');
  if (s.ok) {
    const d = s.data;
    document.getElementById('statusLine').textContent =
      (d.mosqueName || 'SalatSign TV') + (d.cityName ? ' • ' + d.cityName : '');
    document.getElementById('mosqueName').value = d.mosqueName || '';
    document.getElementById('mosqueSubtitle').value = d.mosqueSubtitle || '';
    document.getElementById('mosqueIconPreset').value = d.mosqueIconPreset || 'DOME_CRESCENT';
    document.getElementById('mosqueFontPreset').value = d.mosqueFontPreset || 'MODERN_SANS';
    document.getElementById('themeMode').value = d.themeMode || 'ALWAYS_DARK';
    document.getElementById('is24Hour').value = String(!!d.is24HourFormat);
    document.getElementById('showArabic').value = String(d.showArabicPrayerNames !== false);
    document.getElementById('showStrike').value = String(d.showStrikeHighlight !== false);
    currentSchedDur = d.scheduleDurationSeconds || 60;
    renderSchedDurChips();

    document.getElementById('jumuahMode').value = d.jumuahMode || 'FIXED';
    jumuahSittings = (d.jumuahTimes && d.jumuahTimes.length) ? d.jumuahTimes.slice(0, 3) : (d.jumuahMode === 'AFTER_ZUHR' ? ['45'] : ['13:00']);
    renderSittings();

    iqamahValues = {
      SUBUH: d.iqamahSubuh || 0,
      ZUHUR: d.iqamahZuhur || 0,
      ASHR: d.iqamahAshr || 0,
      MAGRIB: d.iqamahMagrib || 0,
      ISYA: d.iqamahIsya || 0
    };
    renderIqamah();

    document.getElementById('hijriOffset').value = String(d.hijriOffsetDays || 0);
    document.getElementById('hijriMidnight').value = String(!!d.hijriChangeAtMidnight);

    document.getElementById('calSlideEnabled').value = String(!!d.islamicCalendarSlideEnabled);
    document.getElementById('calSlideDuration').value = String(d.islamicCalendarSlideDurationSeconds || 20);
    document.getElementById('quranEnabled').value = String(!!d.quranVerseEnabled);
    document.getElementById('quranTrans').value = String(d.quranShowTranslation !== false);
    document.getElementById('quranDur').value = String(d.quranVerseDurationSeconds || 0);

    document.getElementById('silentJumuahEnabled').value = String(d.silentJumuahEnabled !== false);
    document.getElementById('silentJumuahLead').value = String(d.silentJumuahLeadMinutes ?? 5);
    document.getElementById('silentJumuahDur').value = String(d.silentJumuahDurationMinutes || 45);
    dailySilent = d.silentDailyPrayers || {};
    renderDailySilent();
    updateSilentUi(!!d.manualSilentActive, d.manualSilentRemainingMinutes || 0);
  }
  const sl = await api('/api/v1/slides');
  if (sl.ok) {
    cachedSlides = sl.data.slides || [];
    const used = sl.data.usedSlots ?? 0;
    const max = sl.data.maxRotationSlides ?? 6;
    const b = document.getElementById('slideBudgetBadge');
    b.textContent = used + ' / ' + max + ' active';
    b.className = 'pill ' + (used >= max ? 'warn' : 'idle');
    renderSlides(cachedSlides);
  }
}

function renderSchedDurChips() {
  const chips = document.querySelectorAll('#schedDurChips .chip');
  [30, 60, 90, 120].forEach((val, idx) => {
    if (chips[idx]) chips[idx].classList.toggle('sel', currentSchedDur === val);
  });
}
async function setSchedDur(sec) {
  currentSchedDur = sec;
  renderSchedDurChips();
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ scheduleDurationSeconds: sec })
  });
  showMsg(res.ok ? 'Schedule duration set to ' + sec + 's.' : (res.data.message || 'Save failed.'), res.ok);
}

function enforceBodyLines() {
  const ta = document.getElementById('slideBody');
  const lines = ta.value.replace(/\r\n/g, '\n').split('\n');
  if (lines.length > 6) {
    ta.value = lines.slice(0, 6).join('\n');
  }
  const lineCount = ta.value ? ta.value.split('\n').length : 1;
  document.getElementById('bodyCounter').textContent = lineCount + '/6 rows • ' + ta.value.length + '/400 chars';
}

function renderSlides(slides) {
  const list = document.getElementById('slidesList');
  if (!slides.length) {
    list.innerHTML = '<div class="meta">No announcement slides yet.</div>';
    return;
  }
  list.innerHTML = '';
  slides.forEach((s, idx) => {
    const box = document.createElement('div');
    box.className = 'item-box';
    const top = document.createElement('div');
    top.className = 'flex-between';
    const info = document.createElement('div');
    info.innerHTML = '<strong>#' + (idx + 1) + ' ' + escapeHtml(s.title || '(Untitled)') + '</strong>' +
      '<div class="meta">' + escapeHtml(s.type) + ' • ' + s.durationSeconds + 's • ' + (s.enabled ? 'Active' : 'Disabled') + '</div>';
    const actions = document.createElement('div');
    actions.className = 'flex-gap';

    const editBtn = document.createElement('button');
    editBtn.className = 'sm secondary';
    editBtn.textContent = 'Edit';
    editBtn.onclick = () => startEditSlide(s);

    const toggleBtn = document.createElement('button');
    toggleBtn.className = 'sm ' + (s.enabled ? 'outline' : '');
    toggleBtn.textContent = s.enabled ? 'Disable' : 'Enable';
    toggleBtn.onclick = () => toggleSlide(s);

    const delBtn = document.createElement('button');
    delBtn.className = 'sm danger';
    delBtn.textContent = 'Delete';
    delBtn.onclick = () => deleteSlide(s.id);

    actions.appendChild(editBtn);
    actions.appendChild(toggleBtn);
    actions.appendChild(delBtn);
    top.appendChild(info);
    top.appendChild(actions);
    box.appendChild(top);
    list.appendChild(box);
  });
}

function startEditSlide(s) {
  if (s.type === 'IMAGE') {
    const newTitle = prompt('Edit Image Slide Title:', s.title || 'Poster');
    if (newTitle === null) return;
    const newDurStr = prompt('Display Duration in seconds (3–120):', String(s.durationSeconds || 15));
    if (newDurStr === null) return;
    const dur = Math.max(3, Math.min(120, parseInt(newDurStr, 10) || 15));
    patchSlide(s.id, { title: newTitle.trim() || 'Poster', durationSeconds: dur });
    return;
  }
  document.getElementById('editingSlideId').value = s.id;
  document.getElementById('textFormHeading').textContent = 'Edit Text Announcement Slide';
  document.getElementById('saveTextSlideBtn').textContent = 'Save Changes';
  document.getElementById('cancelEditBtn').classList.remove('hidden');
  document.getElementById('slideTitle').value = s.title || '';
  document.getElementById('slideBody').value = s.body || '';
  document.getElementById('slideDuration').value = String([10,15,20,30,45,60].includes(s.durationSeconds) ? s.durationSeconds : 15);
  enforceBodyLines();
  document.getElementById('slideTitle').scrollIntoView({ behavior: 'smooth' });
}

function cancelEditSlide() {
  document.getElementById('editingSlideId').value = '';
  document.getElementById('textFormHeading').textContent = 'Add Text Announcement Slide';
  document.getElementById('saveTextSlideBtn').textContent = 'Add Text Slide';
  document.getElementById('cancelEditBtn').classList.add('hidden');
  document.getElementById('slideTitle').value = '';
  document.getElementById('slideBody').value = '';
  document.getElementById('slideDuration').value = '15';
  enforceBodyLines();
}

async function saveTextSlide() {
  const editId = document.getElementById('editingSlideId').value;
  const title = document.getElementById('slideTitle').value.trim();
  const body = document.getElementById('slideBody').value.trim();
  const durationSeconds = parseInt(document.getElementById('slideDuration').value, 10) || 15;
  if (!title && !body) { showMsg('Enter a title or message first.', false); return; }
  if (editId) {
    await patchSlide(editId, { title, body, durationSeconds });
    cancelEditSlide();
    return;
  }
  const res = await api('/api/v1/slides/text', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, body, durationSeconds })
  });
  showMsg(res.ok ? 'Text slide added.' : (res.data.message || 'Failed to add slide.'), res.ok);
  if (res.ok) { cancelEditSlide(); await loadAll(); }
}

async function patchSlide(id, payload) {
  const res = await api('/api/v1/slides/' + encodeURIComponent(id), {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  showMsg(res.ok ? 'Slide updated.' : (res.data.message || 'Update failed.'), res.ok);
  if (res.ok) await loadAll();
}

async function toggleSlide(s) {
  await patchSlide(s.id, { enabled: !s.enabled });
}

async function uploadImageSlide() {
  const file = document.getElementById('slideImageFile').files[0];
  if (!file) { showMsg('Choose an image file first.', false); return; }
  const dur = parseInt(document.getElementById('imgSlideDuration').value, 10) || 15;
  const rawTitle = document.getElementById('imgSlideTitle').value.trim() || file.name.replace(/\.[^.]+$/, '');
  const title = encodeURIComponent(rawTitle);
  const res = await api('/api/v1/slides/image?title=' + title + '&duration=' + dur, {
    method: 'POST',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file
  });
  showMsg(res.ok ? 'Image slide uploaded.' : (res.data.message || 'Upload failed.'), res.ok);
  if (res.ok) {
    document.getElementById('slideImageFile').value = '';
    document.getElementById('imgSlideTitle').value = '';
    await loadAll();
  }
}

async function deleteSlide(id) {
  const res = await api('/api/v1/slides/' + encodeURIComponent(id), { method: 'DELETE' });
  showMsg(res.ok ? 'Slide deleted.' : (res.data.message || 'Delete failed.'), res.ok);
  if (res.ok) await loadAll();
}

async function saveBuiltInSlides() {
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      islamicCalendarSlideEnabled: document.getElementById('calSlideEnabled').value === 'true',
      islamicCalendarSlideDurationSeconds: parseInt(document.getElementById('calSlideDuration').value, 10) || 20,
      quranVerseEnabled: document.getElementById('quranEnabled').value === 'true',
      quranShowTranslation: document.getElementById('quranTrans').value === 'true',
      quranVerseDurationSeconds: parseInt(document.getElementById('quranDur').value, 10) || 0
    })
  });
  showMsg(res.ok ? 'Built-in slides saved.' : (res.data.message || 'Save failed.'), res.ok);
  if (res.ok) await loadAll();
}

// --- INTUITIVE JUMU'AH SITTINGS ---
function onJumuahModeChange() {
  const mode = document.getElementById('jumuahMode').value;
  jumuahSittings = mode === 'FIXED' ? ['13:00'] : ['45'];
  renderSittings();
}
function renderSittings() {
  const mode = document.getElementById('jumuahMode').value;
  const container = document.getElementById('sittingsContainer');
  container.innerHTML = '';
  jumuahSittings.forEach((val, idx) => {
    const row = document.createElement('div');
    row.className = 'sitting-row';
    const lbl = document.createElement('div');
    lbl.innerHTML = '<strong>Sitting ' + (idx + 1) + '</strong>';
    const ctrl = document.createElement('div');
    if (mode === 'FIXED') {
      const inp = document.createElement('input');
      inp.type = 'time';
      inp.value = /^\d{2}:\d{2}$/.test(val) ? val : '13:00';
      inp.onchange = () => { jumuahSittings[idx] = inp.value || '13:00'; };
      ctrl.appendChild(inp);
    } else {
      ctrl.className = 'flex-gap';
      const minus = document.createElement('button');
      minus.className = 'sm secondary';
      minus.textContent = '-5m';
      const num = document.createElement('span');
      const mins = parseInt(val, 10) || 0;
      num.textContent = '+' + mins + ' min after Dhuhr';
      num.style.fontWeight = '700';
      const plus = document.createElement('button');
      plus.className = 'sm secondary';
      plus.textContent = '+5m';
      minus.onclick = () => { jumuahSittings[idx] = String(Math.max(0, (parseInt(jumuahSittings[idx], 10) || 0) - 5)); renderSittings(); };
      plus.onclick = () => { jumuahSittings[idx] = String(Math.min(180, (parseInt(jumuahSittings[idx], 10) || 0) + 5)); renderSittings(); };
      ctrl.appendChild(minus);
      ctrl.appendChild(num);
      ctrl.appendChild(plus);
    }
    row.appendChild(lbl);
    row.appendChild(ctrl);
    if (jumuahSittings.length > 1) {
      const rm = document.createElement('button');
      rm.className = 'sm danger';
      rm.textContent = 'Remove';
      rm.onclick = () => { jumuahSittings.splice(idx, 1); renderSittings(); };
      row.appendChild(rm);
    } else {
      row.appendChild(document.createElement('div'));
    }
    container.appendChild(row);
  });
  document.getElementById('addSittingBtn').classList.toggle('hidden', jumuahSittings.length >= 3);
}
function addSitting() {
  if (jumuahSittings.length >= 3) return;
  const mode = document.getElementById('jumuahMode').value;
  jumuahSittings.push(mode === 'FIXED' ? (jumuahSittings.length === 1 ? '13:45' : '14:15') : String(45 * (jumuahSittings.length + 1)));
  renderSittings();
}

// --- INTUITIVE IQAMAH DELAYS ---
function renderIqamah() {
  const container = document.getElementById('iqamahContainer');
  container.innerHTML = '';
  PRAYERS.forEach(p => {
    const val = iqamahValues[p.key] || 0;
    const row = document.createElement('div');
    row.className = 'iq-row';
    const left = document.createElement('div');
    left.innerHTML = '<strong>' + p.label + '</strong><div class="meta">' + (val === 0 ? 'Off' : '+' + val + ' min') + '</div>';
    const chips = document.createElement('div');
    chips.className = 'iq-chips';
    [0, 5, 10, 15, 20].forEach(m => {
      const b = document.createElement('button');
      b.className = 'chip' + (val === m ? ' sel' : '');
      b.textContent = m === 0 ? 'Off' : m + 'm';
      b.onclick = () => { iqamahValues[p.key] = m; renderIqamah(); };
      chips.appendChild(b);
    });
    const minus = document.createElement('button');
    minus.className = 'chip';
    minus.textContent = '-1';
    minus.onclick = () => { iqamahValues[p.key] = Math.max(0, (iqamahValues[p.key] || 0) - 1); renderIqamah(); };
    const plus = document.createElement('button');
    plus.className = 'chip';
    plus.textContent = '+1';
    plus.onclick = () => { iqamahValues[p.key] = Math.min(60, (iqamahValues[p.key] || 0) + 1); renderIqamah(); };
    chips.appendChild(minus);
    chips.appendChild(plus);
    row.appendChild(left);
    row.appendChild(chips);
    container.appendChild(row);
  });
}

async function saveSchedule() {
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jumuahMode: document.getElementById('jumuahMode').value,
      jumuahTimes: jumuahSittings,
      iqamahSubuh: iqamahValues.SUBUH,
      iqamahZuhur: iqamahValues.ZUHUR,
      iqamahAshr: iqamahValues.ASHR,
      iqamahMagrib: iqamahValues.MAGRIB,
      iqamahIsya: iqamahValues.ISYA,
      hijriOffsetDays: parseInt(document.getElementById('hijriOffset').value, 10) || 0,
      hijriChangeAtMidnight: document.getElementById('hijriMidnight').value === 'true'
    })
  });
  showMsg(res.ok ? 'Jumu\'ah, Iqamah & Hijri saved.' : (res.data.message || 'Save failed.'), res.ok);
}

// --- SILENT SCREEN ---
function updateSilentUi(active, remainingMinutes) {
  const badge = document.getElementById('silentStatus');
  const stopBtn = document.getElementById('stopSilentBtn');
  if (active) {
    badge.className = 'pill active';
    badge.textContent = 'Active • ' + remainingMinutes + ' min remaining';
    stopBtn.classList.remove('hidden');
  } else {
    badge.className = 'pill idle';
    badge.textContent = 'Inactive';
    stopBtn.classList.add('hidden');
  }
}
function renderDailySilent() {
  const container = document.getElementById('dailySilentContainer');
  container.innerHTML = '';
  PRAYERS.forEach(p => {
    const cfg = dailySilent[p.key] || { enabled: false, anchor: 'IQAMAH', leadMinutes: 0, durationMinutes: 15 };
    dailySilent[p.key] = cfg;
    const row = document.createElement('div');
    row.className = 'iq-row';
    const left = document.createElement('div');
    left.innerHTML = '<strong>' + p.label + '</strong><div class="meta">' + (cfg.enabled ? (cfg.anchor + ' • ' + cfg.durationMinutes + 'm') : 'Off') + '</div>';
    const ctrls = document.createElement('div');
    ctrls.className = 'flex-gap';

    const toggle = document.createElement('button');
    toggle.className = 'chip' + (cfg.enabled ? ' sel' : '');
    toggle.textContent = cfg.enabled ? 'On' : 'Off';
    toggle.onclick = () => { cfg.enabled = !cfg.enabled; renderDailySilent(); };
    ctrls.appendChild(toggle);

    if (cfg.enabled) {
      const anc = document.createElement('button');
      anc.className = 'chip';
      anc.textContent = cfg.anchor === 'IQAMAH' ? 'Iqamah' : 'Adhan';
      anc.onclick = () => { cfg.anchor = cfg.anchor === 'IQAMAH' ? 'ADHAN' : 'IQAMAH'; renderDailySilent(); };
      const dur = document.createElement('button');
      dur.className = 'chip';
      dur.textContent = cfg.durationMinutes + 'm';
      dur.onclick = () => {
        const opts = [10, 15, 20, 30];
        const i = opts.indexOf(cfg.durationMinutes);
        cfg.durationMinutes = opts[(i + 1) % opts.length];
        renderDailySilent();
      };
      ctrls.appendChild(anc);
      ctrls.appendChild(dur);
    }
    row.appendChild(left);
    row.appendChild(ctrls);
    container.appendChild(row);
  });
}
async function startSilent(minutes) {
  const res = await api('/api/v1/silent/start', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ durationMinutes: minutes })
  });
  if (res.ok) {
    updateSilentUi(true, res.data.manualSilentRemainingMinutes || minutes);
    showMsg('Silent screen started for ' + minutes + ' minutes.', true);
  } else {
    showMsg(res.data.message || 'Failed to start silent screen.', false);
  }
}
async function stopSilent() {
  const res = await api('/api/v1/silent/stop', { method: 'POST' });
  if (res.ok) {
    updateSilentUi(false, 0);
    showMsg('Silent screen stopped.', true);
  } else {
    showMsg(res.data.message || 'Failed to stop silent screen.', false);
  }
}
async function saveSilentSettings() {
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      silentJumuahEnabled: document.getElementById('silentJumuahEnabled').value === 'true',
      silentJumuahLeadMinutes: parseInt(document.getElementById('silentJumuahLead').value, 10) || 0,
      silentJumuahDurationMinutes: parseInt(document.getElementById('silentJumuahDur').value, 10) || 45,
      silentDailyPrayers: dailySilent
    })
  });
  showMsg(res.ok ? 'Silent screen schedule saved.' : (res.data.message || 'Save failed.'), res.ok);
}

// --- BRANDING & DISPLAY ---
async function saveBranding() {
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      mosqueName: document.getElementById('mosqueName').value.trim(),
      mosqueSubtitle: document.getElementById('mosqueSubtitle').value.trim(),
      mosqueIconPreset: document.getElementById('mosqueIconPreset').value,
      mosqueFontPreset: document.getElementById('mosqueFontPreset').value,
      themeMode: document.getElementById('themeMode').value,
      is24HourFormat: document.getElementById('is24Hour').value === 'true',
      showArabicPrayerNames: document.getElementById('showArabic').value === 'true',
      showStrikeHighlight: document.getElementById('showStrike').value === 'true'
    })
  });
  showMsg(res.ok ? 'Branding & display saved.' : (res.data.message || 'Save failed.'), res.ok);
  if (res.ok) await loadAll();
}
async function uploadLogo() {
  const file = document.getElementById('logoFile').files[0];
  if (!file) { showMsg('Choose a logo image first.', false); return; }
  const res = await api('/api/v1/logo', {
    method: 'POST',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file
  });
  showMsg(res.ok ? 'Custom mosque logo updated.' : (res.data.message || 'Logo upload failed.'), res.ok);
  if (res.ok) { document.getElementById('logoFile').value = ''; await loadAll(); }
}

loadStatus();
if (token) {
  document.getElementById('loginCard').classList.add('hidden');
  document.getElementById('app').classList.remove('hidden');
  document.getElementById('logoutBtn').classList.remove('hidden');
  loadAll();
}
</script>
</body>
</html>
    """.trimIndent()
}
