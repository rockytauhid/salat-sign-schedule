// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.remote

/**
 * Self-contained, offline mobile-first web UI served directly from SalatSign TV over LAN.
 * Allows mosque administrators to sign in with their Admin PIN and manage branding, Jumu'ah,
 * Iqamah delays, and announcement slides/images from any phone browser on the same Wi-Fi.
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
    --bg: #071912; --card: #0f2a1f; --border: #1e4d38;
    --emerald: #10b981; --gold: #fbbf24; --text: #f3f4f6; --muted: #9ca3af; --danger: #ef4444;
  }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: system-ui, -apple-system, sans-serif; background: var(--bg); color: var(--text); padding: 16px; }
  .wrap { max-width: 560px; margin: 0 auto; }
  h1 { margin: 0 0 4px; font-size: 1.5rem; color: var(--text); }
  h1 span { color: var(--gold); }
  .sub { color: var(--muted); font-size: 0.9rem; margin-bottom: 16px; }
  .card { background: var(--card); border: 1px solid var(--border); border-radius: 14px; padding: 16px; margin-bottom: 14px; }
  .card h2 { margin: 0 0 12px; font-size: 1.05rem; color: var(--emerald); }
  label { display: block; font-size: 0.85rem; color: var(--muted); margin: 10px 0 4px; }
  input, select, textarea { width: 100%; padding: 10px 12px; border-radius: 8px; border: 1px solid var(--border); background: #071912; color: var(--text); font-size: 1rem; }
  textarea { min-height: 76px; resize: vertical; }
  button { width: 100%; margin-top: 12px; padding: 11px 14px; border: 0; border-radius: 10px; background: var(--emerald); color: #052014; font-weight: 700; font-size: 0.95rem; cursor: pointer; }
  button.secondary { background: #1c3f30; color: var(--text); }
  button.danger { background: var(--danger); color: #fff; width: auto; padding: 6px 12px; margin-top: 0; font-size: 0.85rem; }
  button.danger-full { background: var(--danger); color: #fff; width: 100%; margin-top: 10px; }
  .row { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
  .row3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
  .row5 { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
  .pill { display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 0.8rem; font-weight: 700; margin-bottom: 8px; }
  .pill.active { background: #7f1d1d; color: #fecaca; }
  .pill.idle { background: #1c3f30; color: #a7f3d0; }
  .slide-item { display: flex; justify-content: space-between; align-items: center; gap: 10px; padding: 10px; border-radius: 8px; background: #071912; border: 1px solid var(--border); margin-top: 8px; }
  .slide-meta { font-size: 0.82rem; color: var(--muted); }
  .msg { padding: 10px 12px; border-radius: 8px; margin-bottom: 12px; font-size: 0.9rem; display: none; }
  .msg.ok { display: block; background: #064e3b; color: #a7f3d0; }
  .msg.err { display: block; background: #7f1d1d; color: #fecaca; }
  .hidden { display: none; }
</style>
</head>
<body>
<div class="wrap">
  <h1>Salat<span>Sign</span> Remote</h1>
  <div class="sub" id="statusLine">Local LAN Controller</div>
  <div id="msg" class="msg"></div>

  <div id="loginCard" class="card">
    <h2>Admin PIN Sign-In</h2>
    <label for="pinInput">Enter Mosque Admin PIN (4–8 digits)</label>
    <input id="pinInput" type="password" inputmode="numeric" maxlength="8" placeholder="••••" />
    <button onclick="login()">Unlock TV Controller</button>
  </div>

  <div id="app" class="hidden">
    <div class="card">
      <h2>Silent Screen (Manual Control)</h2>
      <div id="silentStatus" class="pill idle">Inactive</div>
      <div class="slide-meta">Immediately show "Prayer in progress" on the TV (overrides schedule and slides).</div>
      <div class="row3">
        <button onclick="startSilent(15)">Show 15 min</button>
        <button onclick="startSilent(30)">Show 30 min</button>
        <button onclick="startSilent(45)">Show 45 min</button>
      </div>
      <button id="stopSilentBtn" class="danger-full hidden" onclick="stopSilent()">Stop Silent Screen Now</button>
    </div>

    <div class="card">
      <h2>Mosque Branding &amp; Logo</h2>
      <label>Mosque Name</label>
      <input id="mosqueName" type="text" placeholder="e.g. Canberra Islamic Centre" />
      <label>Subtitle / Tagline</label>
      <input id="mosqueSubtitle" type="text" placeholder="e.g. Please silence your phone" />
      <div class="row">
        <div>
          <label>Schedule Duration (sec)</label>
          <input id="scheduleDuration" type="number" min="10" max="600" />
        </div>
        <div>
          <label>Hijri Day Offset (-2..+2)</label>
          <input id="hijriOffset" type="number" min="-2" max="2" />
        </div>
      </div>
      <div class="row">
        <div>
          <label>Time Format</label>
          <select id="is24Hour">
            <option value="false">12-Hour (AM/PM)</option>
            <option value="true">24-Hour (00:00)</option>
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

      <label style="margin-top:16px">Upload Custom Mosque Logo (PNG/JPG)</label>
      <input id="logoFile" type="file" accept="image/*" />
      <button class="secondary" onclick="uploadLogo()">Upload Mosque Logo</button>
    </div>

    <div class="card">
      <h2>Friday Jumu'ah &amp; Iqamah</h2>
      <div class="row">
        <div>
          <label>Jumu'ah Mode</label>
          <select id="jumuahMode">
            <option value="FIXED">Fixed Clock Time</option>
            <option value="AFTER_ZUHR">Minutes After Zuhr</option>
          </select>
        </div>
        <div>
          <label>Sittings (comma separated)</label>
          <input id="jumuahTimes" type="text" placeholder="13:00, 13:45" />
        </div>
      </div>
      <label>Iqamah Delays (minutes after Adhan: 0 = off)</label>
      <div class="row5">
        <div><label>Fajr</label><input id="iqSubuh" type="number" min="0" max="60" /></div>
        <div><label>Dhuhr</label><input id="iqZuhur" type="number" min="0" max="60" /></div>
        <div><label>Asr</label><input id="iqAshr" type="number" min="0" max="60" /></div>
        <div><label>Maghrib</label><input id="iqMagrib" type="number" min="0" max="60" /></div>
        <div><label>Isha</label><input id="iqIsya" type="number" min="0" max="60" /></div>
      </div>
      <button onclick="saveSchedule()">Save Jumu'ah &amp; Iqamah</button>
    </div>

    <div class="card">
      <h2>Announcement Slides</h2>
      <div id="slidesList"></div>
      <label style="margin-top:16px">Add Text Slide</label>
      <input id="slideTitle" type="text" placeholder="Announcement Title" />
      <textarea id="slideBody" placeholder="Announcement details..."></textarea>
      <label>Duration (seconds)</label>
      <input id="slideDuration" type="number" min="3" max="120" value="15" />
      <button onclick="addTextSlide()">Add Text Slide</button>

      <label style="margin-top:16px">Upload Poster Image Slide (JPG/PNG/WebP)</label>
      <input id="slideImageFile" type="file" accept="image/*" />
      <button class="secondary" onclick="uploadImageSlide()">Upload Image Slide</button>
    </div>
  </div>
</div>

<script>
let token = sessionStorage.getItem('salatsign_token') || '';
function showMsg(text, ok) {
  const el = document.getElementById('msg');
  el.textContent = text;
  el.className = 'msg ' + (ok ? 'ok' : 'err');
}
async function api(path, opts = {}) {
  const headers = opts.headers || {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(path, { ...opts, headers });
  const data = await res.json().catch(() => ({}));
  if (res.status === 401) {
    token = '';
    sessionStorage.removeItem('salatsign_token');
    document.getElementById('app').classList.add('hidden');
    document.getElementById('loginCard').classList.remove('hidden');
  }
  return { ok: res.ok, status: res.status, data };
}
async function loadStatus() {
  const res = await api('/api/v1/status');
  if (res.ok) {
    document.getElementById('statusLine').textContent =
      (res.data.mosqueName || 'SalatSign TV') + ' • ' + (res.data.cityName || '') + ' • v' + (res.data.version || '1.0');
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
    showMsg('Connected to SalatSign TV.', true);
    await loadAll();
  } else {
    showMsg(res.data.message || 'Authentication failed.', false);
  }
}
async function loadAll() {
  const s = await api('/api/v1/settings');
  if (s.ok) {
    const d = s.data;
    document.getElementById('mosqueName').value = d.mosqueName || '';
    document.getElementById('mosqueSubtitle').value = d.mosqueSubtitle || '';
    document.getElementById('scheduleDuration').value = d.scheduleDurationSeconds || 60;
    document.getElementById('hijriOffset').value = d.hijriOffsetDays || 0;
    document.getElementById('is24Hour').value = String(!!d.is24HourFormat);
    document.getElementById('showStrike').value = String(d.showStrikeHighlight !== false);
    document.getElementById('jumuahMode').value = d.jumuahMode || 'FIXED';
    document.getElementById('jumuahTimes').value = (d.jumuahTimes || []).join(', ');
    document.getElementById('iqSubuh').value = d.iqamahSubuh || 0;
    document.getElementById('iqZuhur').value = d.iqamahZuhur || 0;
    document.getElementById('iqAshr').value = d.iqamahAshr || 0;
    document.getElementById('iqMagrib').value = d.iqamahMagrib || 0;
    document.getElementById('iqIsya').value = d.iqamahIsya || 0;
    updateSilentUi(!!d.manualSilentActive, d.manualSilentRemainingMinutes || 0);
  }
  const sl = await api('/api/v1/slides');
  if (sl.ok) renderSlides(sl.data.slides || []);
}
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
function renderSlides(slides) {
  const list = document.getElementById('slidesList');
  if (!slides.length) {
    list.innerHTML = '<div class="slide-meta">No announcement slides yet.</div>';
    return;
  }
  list.innerHTML = '';
  slides.forEach(s => {
    const row = document.createElement('div');
    row.className = 'slide-item';
    const info = document.createElement('div');
    info.innerHTML = '<strong>' + escapeHtml(s.title || s.type) + '</strong><div class="slide-meta">' +
      escapeHtml(s.type) + ' • ' + s.durationSeconds + 's</div>';
    const btn = document.createElement('button');
    btn.className = 'danger';
    btn.textContent = 'Delete';
    btn.onclick = () => deleteSlide(s.id);
    row.appendChild(info);
    row.appendChild(btn);
    list.appendChild(row);
  });
}
function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
async function saveBranding() {
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      mosqueName: document.getElementById('mosqueName').value.trim(),
      mosqueSubtitle: document.getElementById('mosqueSubtitle').value.trim(),
      scheduleDurationSeconds: parseInt(document.getElementById('scheduleDuration').value, 10) || 60,
      hijriOffsetDays: parseInt(document.getElementById('hijriOffset').value, 10) || 0,
      is24HourFormat: document.getElementById('is24Hour').value === 'true',
      showStrikeHighlight: document.getElementById('showStrike').value === 'true'
    })
  });
  showMsg(res.ok ? 'Branding & display settings saved.' : (res.data.message || 'Save failed.'), res.ok);
  if (res.ok) loadStatus();
}
async function saveSchedule() {
  const times = document.getElementById('jumuahTimes').value.split(',').map(x => x.trim()).filter(Boolean);
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      jumuahMode: document.getElementById('jumuahMode').value,
      jumuahTimes: times,
      iqamahSubuh: parseInt(document.getElementById('iqSubuh').value, 10) || 0,
      iqamahZuhur: parseInt(document.getElementById('iqZuhur').value, 10) || 0,
      iqamahAshr: parseInt(document.getElementById('iqAshr').value, 10) || 0,
      iqamahMagrib: parseInt(document.getElementById('iqMagrib').value, 10) || 0,
      iqamahIsya: parseInt(document.getElementById('iqIsya').value, 10) || 0
    })
  });
  showMsg(res.ok ? 'Jumu\'ah & Iqamah saved.' : (res.data.message || 'Save failed.'), res.ok);
}
async function addTextSlide() {
  const title = document.getElementById('slideTitle').value.trim();
  const body = document.getElementById('slideBody').value.trim();
  const durationSeconds = parseInt(document.getElementById('slideDuration').value, 10) || 15;
  if (!title && !body) { showMsg('Enter a title or body first.', false); return; }
  const res = await api('/api/v1/slides/text', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, body, durationSeconds })
  });
  showMsg(res.ok ? 'Text slide added.' : (res.data.message || 'Failed to add slide.'), res.ok);
  if (res.ok) {
    document.getElementById('slideTitle').value = '';
    document.getElementById('slideBody').value = '';
    await loadAll();
  }
}
async function uploadImageSlide() {
  const file = document.getElementById('slideImageFile').files[0];
  if (!file) { showMsg('Choose an image file first.', false); return; }
  const dur = parseInt(document.getElementById('slideDuration').value, 10) || 15;
  const title = encodeURIComponent(document.getElementById('slideTitle').value.trim() || file.name);
  const res = await api('/api/v1/slides/image?title=' + title + '&duration=' + dur, {
    method: 'POST',
    headers: { 'Content-Type': file.type || 'application/octet-stream' },
    body: file
  });
  showMsg(res.ok ? 'Image slide uploaded & optimized.' : (res.data.message || 'Upload failed.'), res.ok);
  if (res.ok) { document.getElementById('slideImageFile').value = ''; await loadAll(); }
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
  if (res.ok) { document.getElementById('logoFile').value = ''; }
}
async function deleteSlide(id) {
  const res = await api('/api/v1/slides/' + encodeURIComponent(id), { method: 'DELETE' });
  showMsg(res.ok ? 'Slide deleted.' : (res.data.message || 'Delete failed.'), res.ok);
  if (res.ok) await loadAll();
}
loadStatus();
if (token) {
  document.getElementById('loginCard').classList.add('hidden');
  document.getElementById('app').classList.remove('hidden');
  loadAll();
}
</script>
</body>
</html>
    """.trimIndent()
}
