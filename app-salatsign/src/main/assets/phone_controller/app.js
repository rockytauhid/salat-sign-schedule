let token = localStorage.getItem('salatsign_token') || sessionStorage.getItem('salatsign_token') || '';
if (token && !localStorage.getItem('salatsign_token')) {
  localStorage.setItem('salatsign_token', token);
  sessionStorage.removeItem('salatsign_token');
}
function defaultDeviceName() {
  const ua = navigator.userAgent || '';
  if (/iPhone/i.test(ua)) return 'iPhone';
  if (/iPad/i.test(ua)) return 'iPad';
  if (/Android/i.test(ua)) {
    const m = ua.match(/Android\s[^;]+;\s*([^;)]+?)(?:\s+Build\/|[;)])/i);
    const model = m && m[1] ? m[1].trim() : '';
    if (model && model !== 'K' && model.length >= 2) return model.slice(0, 48);
    return 'Android Phone';
  }
  if (/Mac/i.test(ua)) return 'Mac Browser';
  if (/Windows/i.test(ua)) return 'Windows Browser';
  return 'Phone Browser';
}
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
  ['slides','schedule','silent','notice','branding'].forEach(t => {
    document.getElementById('pane-' + t).classList.toggle('hidden', t !== name);
    document.getElementById('tab-' + t).classList.toggle('active', t === name);
  });
}
async function api(path, opts = {}) {
  const headers = opts.headers || {};
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(path, { ...opts, headers });
  const data = await res.json().catch(() => ({}));
  if (res.status === 401 && path !== '/api/v1/auth' && path !== '/api/v1/pair') {
    token = '';
    localStorage.removeItem('salatsign_token');
    sessionStorage.removeItem('salatsign_token');
    document.getElementById('app').classList.add('hidden');
    document.getElementById('logoutBtn').classList.add('hidden');
    document.getElementById('loginCard').classList.remove('hidden');
    showMsg('Session expired or device revoked. Pair again from TV Settings.', false);
  }
  return { ok: res.ok, status: res.status, data };
}
async function loadStatus() {
  const res = await api('/api/v1/status');
  if (res.ok) {
    const label = res.data.deviceName ? res.data.deviceName : 'SalatSign TV';
    document.getElementById('statusLine').textContent = label + ' • v' + (res.data.version || '1.0');
    document.getElementById('defaultPinNotice').classList.toggle('hidden', !!res.data.pinConfigured);
    const pinAllowed = res.data.lanPinLoginEnabled !== false;
    document.getElementById('pinLoginSection').classList.toggle('hidden', !pinAllowed);
    document.getElementById('pinDisabledNotice').classList.toggle('hidden', pinAllowed);
  }
}
async function pairWithCode(overrideCode, fromQr) {
  const raw = (overrideCode || document.getElementById('pairCodeInput').value || '').replace(/\D+/g, '');
  if (raw.length !== 6) { showMsg('Enter the 6-digit pairing code shown on the TV.', false); return; }
  const devName = (document.getElementById('deviceNameInput').value || '').trim() || defaultDeviceName();
  const res = await api('/api/v1/pair', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ code: raw, deviceName: devName })
  });
  if (res.ok && res.data.token) {
    token = res.data.token;
    localStorage.setItem('salatsign_token', token);
    document.getElementById('pairCodeInput').value = '';
    document.getElementById('loginCard').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    document.getElementById('logoutBtn').classList.remove('hidden');
    const pairedName = res.data.deviceName || devName;
    showMsg('Paired as ' + pairedName + ' (' + (res.data.role || 'MANAGE') + ').', true);
    if (fromQr) {
      const quickInp = document.getElementById('quickPhoneNameInput');
      if (quickInp) quickInp.value = pairedName;
      const banner = document.getElementById('renamePhoneBanner');
      if (banner) banner.classList.remove('hidden');
    }
    await loadAll();
    await loadAuthMe();
  } else {
    showMsg(res.data.message || 'Pairing failed.', false);
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
    localStorage.setItem('salatsign_token', token);
    document.getElementById('pinInput').value = '';
    document.getElementById('loginCard').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    document.getElementById('logoutBtn').classList.remove('hidden');
    showMsg('Connected to SalatSign TV.', true);
    await loadAll();
    await loadAuthMe();
  } else {
    showMsg(res.data.message || 'Authentication failed.', false);
  }
}
async function logout() {
  await api('/api/v1/logout', { method: 'POST' });
  token = '';
  localStorage.removeItem('salatsign_token');
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
      (d.deviceName || d.mosqueName || 'SalatSign TV') + (d.cityName ? ' • ' + d.cityName : '');
    const tvNameEl = document.getElementById('tvDeviceName');
    const tvRoomEl = document.getElementById('tvRoomGroup');
    if (tvNameEl) tvNameEl.value = d.deviceName || '';
    if (tvRoomEl) tvRoomEl.value = d.roomGroup || '';
    document.getElementById('mosqueName').value = d.mosqueName || '';
    document.getElementById('mosqueSubtitle').value = d.mosqueSubtitle || '';
    document.getElementById('mosqueIconPreset').value = d.mosqueIconPreset || 'DOME_CRESCENT';
    document.getElementById('mosqueFontPreset').value = d.mosqueFontPreset || 'MODERN_SANS';
    document.getElementById('appLanguage').value = (d.appLanguage === 'ID') ? 'ID' : 'EN';
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
  await loadNoticeAndPeers();
}

async function loadNoticeAndPeers() {
  const n = await api('/api/v1/notices');
  if (n.ok) {
    const pill = document.getElementById('noticeStatusPill');
    if (n.data.active && n.data.notice) {
      pill.textContent = 'Active • ' + (n.data.remainingMinutes || 1) + 'm left';
      pill.className = 'pill warn';
    } else {
      pill.textContent = 'None active';
      pill.className = 'pill idle';
    }
  }
  const p = await api('/api/v1/peers');
  if (p.ok) {
    const peers = p.data.peers || [];
    const total = 1 + peers.length;
    document.getElementById('peerCountPill').textContent = total + (total === 1 ? ' TV' : ' TVs');
    const sel = document.getElementById('noticeTargetScope');
    sel.innerHTML = '<option value="ALL">All TVs (' + total + ')</option>';
    (p.data.roomGroups || []).forEach(g => {
      sel.innerHTML += '<option value="ROOM:' + escapeHtml(g) + '">Room: ' + escapeHtml(g) + '</option>';
    });
    const listEl = document.getElementById('peersList');
    const healthById = {};
    (p.data.peerHealth || []).forEach(h => { healthById[h.deviceId] = h; });
    const mismatchById = {};
    (p.data.keyMismatches || []).forEach(m => { mismatchById[m.deviceId] = m; });
    const rotationById = {};
    (p.data.rotationStatuses || []).forEach(r => { rotationById[r.deviceId] = r; });
    const localVer = p.data.configVersion || 1;
    const leaveBtn = document.getElementById('leaveGroupBtn');
    if (leaveBtn) leaveBtn.classList.toggle('hidden', !p.data.groupLinked);

    listEl.innerHTML = '';
    const selfBox = document.createElement('div');
    selfBox.className = 'slide-item';
    selfBox.innerHTML = '<strong>' + escapeHtml(p.data.self.deviceName || 'This TV') + ' (This TV)</strong>' +
      '<div class="slide-meta">' + escapeHtml(p.data.self.lastKnownHost || '') +
      (p.data.self.roomGroup ? ' • ' + escapeHtml(p.data.self.roomGroup) : '') +
      ' • v' + localVer + '</div>';
    listEl.appendChild(selfBox);

    peers.forEach(peer => {
      const h = healthById[peer.deviceId];
      const mismatch = mismatchById[peer.deviceId];
      const rot = rotationById[peer.deviceId];
      let statusText = '';
      if (h) {
        if (!h.reachable) {
          statusText = ' • Offline';
        } else {
          statusText = ' • v' + (h.configVersion || '?') + (h.inSync ? ' (In sync)' : ' (Drift — push to sync)');
          if (typeof h.clockSkewSeconds === 'number' && Math.abs(h.clockSkewSeconds) > 2) {
            statusText += ' • Clock skew ' + h.clockSkewSeconds + 's';
          }
        }
      }
      if (rot) {
        if (rot.state === 'pending') {
          statusText += ' • Key rotation pending (' + rot.attempts + '/' + (rot.maxAttempts || 6) + ')';
        } else if (rot.state === 'gave_up') {
          statusText += ' • Key rotation failed — re-join this TV manually (retry notice clears on restart)';
        }
      }
      if (mismatch) {
        statusText += ' • Key changed (New key: ' + (mismatch.candidateFingerprint || '?') + ')';
      }
      const box = document.createElement('div');
      box.className = 'slide-item flex-between';
      const info = document.createElement('div');
      info.innerHTML = '<strong>' + escapeHtml(peer.deviceName || peer.deviceId) + '</strong>' +
        '<div class="slide-meta">' + escapeHtml(peer.lastKnownHost + ':' + peer.port) +
        (peer.roomGroup ? ' • ' + escapeHtml(peer.roomGroup) : '') + escapeHtml(statusText) + '</div>';
      const actions = document.createElement('div');
      actions.className = 'row gap-sm';
      if (mismatch) {
        const acceptBtn = document.createElement('button');
        acceptBtn.className = 'sm secondary';
        acceptBtn.textContent = 'Accept New Key';
        acceptBtn.onclick = () => acceptPeerKey(peer.deviceId, peer.deviceName || peer.deviceId, mismatch.candidateFingerprint || '');
        actions.appendChild(acceptBtn);
      }
      const rmBtn = document.createElement('button');
      rmBtn.className = 'sm danger';
      rmBtn.textContent = 'Remove';
      rmBtn.onclick = () => removePeerAndRotate(peer.deviceId, peer.deviceName || peer.deviceId);
      actions.appendChild(rmBtn);
      box.appendChild(info);
      box.appendChild(actions);
      listEl.appendChild(box);
    });
  }
}

function useNoticePreset(title, msg) {
  document.getElementById('noticeTitle').value = title;
  document.getElementById('noticeMessage').value = msg;
}

function formatDeliveries(deliveries) {
  if (!deliveries || !deliveries.length) return '';
  return deliveries.map(d => (d.deviceName || d.deviceId) + ': ' + d.status + (d.message ? ' (' + d.message + ')' : '')).join(' | ');
}

async function sendUrgentNotice() {
  const title = document.getElementById('noticeTitle').value.trim() || 'Urgent Notice';
  const message = document.getElementById('noticeMessage').value.trim();
  if (!message) { showMsg('Enter a notice message first.', false); return; }
  const durationMinutes = parseInt(document.getElementById('noticeDuration').value, 10) || 10;
  const scopeVal = document.getElementById('noticeTargetScope').value;
  const payload = { title, message, durationMinutes, targetScope: scopeVal.startsWith('ROOM:') ? 'ROOM' : 'ALL' };
  if (scopeVal.startsWith('ROOM:')) payload.targetRoomGroup = scopeVal.slice(5);
  const res = await api('/api/v1/notices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  showMsg(res.ok ? 'Urgent notice sent.' : (res.data.message || 'Send failed.'), res.ok);
  document.getElementById('noticeDeliveries').textContent = res.ok ? formatDeliveries(res.data.deliveries) : '';
  if (res.ok) await loadNoticeAndPeers();
}

async function clearUrgentNotice() {
  const res = await api('/api/v1/notices', { method: 'DELETE' });
  showMsg(res.ok ? 'Urgent notice cleared.' : (res.data.message || 'Clear failed.'), res.ok);
  document.getElementById('noticeDeliveries').textContent = res.ok ? formatDeliveries(res.data.deliveries) : '';
  if (res.ok) await loadNoticeAndPeers();
}

async function pushConfigToPeers() {
  const res = await api('/api/v1/peers/push', { method: 'POST' });
  showMsg(res.ok ? 'Config pushed to linked TVs.' : (res.data.message || 'Push failed.'), res.ok);
  document.getElementById('pushDeliveries').textContent = res.ok ? (formatDeliveries(res.data.deliveries) || 'No peer TVs linked yet.') : '';
}

async function generateGroupJoinCode() {
  const res = await api('/api/v1/peers/code', { method: 'POST' });
  const box = document.getElementById('groupJoinCodeBox');
  if (res.ok && res.data.code) {
    box.textContent = 'Group Join Code (valid 3 min): ' + res.data.code + ' — enter this on another TV along with this TV\'s IP.';
    box.classList.remove('hidden');
  } else {
    showMsg(res.data.message || 'Could not generate join code.', false);
  }
}

async function joinPeerGroup() {
  const host = document.getElementById('joinHostInput').value.trim();
  const code = document.getElementById('joinCodeInput').value.toUpperCase().replace(/[^A-Z0-9]+/g, '');
  if (!host || code.length !== 8) { showMsg('Enter the other TV IP and 8-character join code.', false); return; }
  const res = await api('/api/v1/peers/join', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ host, port: 8088, code })
  });
  showMsg(res.ok ? 'Joined TV group!' : (res.data.message || 'Join failed.'), res.ok);
  if (res.ok) {
    document.getElementById('joinCodeInput').value = '';
    await loadAll();
  }
}

async function acceptPeerKey(deviceId, label, expectedFingerprint) {
  const msg = 'Accept new identity key for "' + label + '"?\n\n' +
    'New Key Fingerprint: ' + (expectedFingerprint || '(unknown)') + '\n\n' +
    'Verify this matches Settings → PIN & Remote on "' + label + '" before continuing. ' +
    'Note: you will also need to confirm this key change on any other linked TVs in the group.';
  if (!confirm(msg)) return;
  const res = await api('/api/v1/peers/accept-key', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId, expectedFingerprint })
  });
  showMsg(res.ok ? 'Accepted new key for ' + label + '.' : (res.data.message || 'Accept key failed.'), res.ok);
  if (res.ok) await loadNoticeAndPeers();
}

async function removePeerAndRotate(deviceId, label) {
  if (!confirm('Remove "' + label + '" from this TV group and rotate the group secret?')) return;
  const res = await api('/api/v1/peers/remove', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceId })
  });
  showMsg(res.ok ? 'Removed TV and rotated group secret.' : (res.data.message || 'Remove failed.'), res.ok);
  const delEl = document.getElementById('pushDeliveries');
  if (delEl && res.ok && Array.isArray(res.data.deliveries) && res.data.deliveries.length) {
    const anyFailed = res.data.deliveries.some(d => d.status !== 'delivered');
    delEl.textContent = 'Key rotation: ' + formatDeliveries(res.data.deliveries) +
      (anyFailed ? ' — Unreachable TVs will be retried automatically, or re-join them manually.' : '');
  }
  if (res.ok) await loadNoticeAndPeers();
}

async function leavePeerGroup() {
  if (!confirm('Disconnect this TV from its TV group?')) return;
  const res = await api('/api/v1/peers/leave', { method: 'POST' });
  showMsg(res.ok ? 'This TV left the group.' : (res.data.message || 'Leave failed.'), res.ok);
  if (res.ok) await loadNoticeAndPeers();
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

function insertBullet() {
  const ta = document.getElementById('slideBody');
  const prefix = (!ta.value || ta.value.endsWith('\n')) ? '• ' : '\n• ';
  ta.value += prefix;
  enforceBodyLines();
  ta.focus();
}
function insertNumbered() {
  const ta = document.getElementById('slideBody');
  const nums = ta.value.split('\n').map(l => { const m = l.match(/^\s*(\d+)\./); return m ? parseInt(m[1], 10) : 0; });
  const nextNum = Math.max(0, ...nums) + 1;
  const prefix = (!ta.value || ta.value.endsWith('\n')) ? (nextNum + '. ') : ('\n' + nextNum + '. ');
  ta.value += prefix;
  enforceBodyLines();
  ta.focus();
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

let textRows = [{ text: '', align: 'CENTER', fontSize: 'MEDIUM' }];

const ALIGN_CYCLE = ['LEFT', 'CENTER', 'RIGHT'];
const ALIGN_ICON = {
  LEFT: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 5h18v2H3V5zm0 4h12v2H3V9zm0 4h18v2H3v-2zm0 4h12v2H3v-2z"/></svg>',
  CENTER: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 5h18v2H3V5zm3 4h12v2H6V9zm-3 4h18v2H3v-2zm3 4h12v2H6v-2z"/></svg>',
  RIGHT: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 5h18v2H3V5zm6 4h12v2H9V9zm-6 4h18v2H3v-2zm6 4h12v2H9v-2z"/></svg>'
};
const ALIGN_INPUT_CLASS = { LEFT: 'align-left', CENTER: 'align-center', RIGHT: 'align-right' };
const ALIGN_TITLE = { LEFT: 'Align: Left (tap to cycle)', CENTER: 'Align: Center (tap to cycle)', RIGHT: 'Align: Right (tap to cycle)' };
const SIZE_CYCLE = ['SMALL', 'MEDIUM', 'LARGE'];
const SIZE_CLASS = { SMALL: 'row-sz-sm', MEDIUM: 'row-sz-md', LARGE: 'row-sz-lg' };
const SIZE_TITLE = { SMALL: 'Size: Small (tap to cycle)', MEDIUM: 'Size: Medium (tap to cycle)', LARGE: 'Size: Large (tap to cycle)' };
const TRASH_SVG = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 7h12v13a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V7zm3 3v8h2v-8H9zm4 0v8h2v-8h-2zM9 4l1-1h4l1 1h4v2H5V4h4z"/></svg>';

function rowWeight(row) {
  if (!/^\s*qrc:/i.test(row.text || '')) return 1;
  const sz = row.fontSize || 'MEDIUM';
  if (sz === 'SMALL') return 3;
  if (sz === 'LARGE') return 5;
  return 4;
}
function usedTextRows() {
  return textRows.reduce((acc, r) => acc + rowWeight(r), 0);
}
function updateTextRowToolbar() {
  const used = usedTextRows();
  const rem = Math.max(0, 10 - used);
  const lastBlank = textRows.length > 0 && !(textRows[textRows.length - 1].text || '').trim();
  const lbl = document.getElementById('textRowsLabel');
  if (lbl) lbl.textContent = 'Message (' + used + ' out of 10 rows)';
  const rowBtn = document.querySelector('[data-evt-click="12"]');
  const bulBtn = document.querySelector('[data-evt-click="13"]');
  const numBtn = document.querySelector('[data-evt-click="14"]');
  const qrBtn = document.querySelector('[data-evt-click="42"]');
  if (rowBtn) rowBtn.disabled = rem < 1;
  if (bulBtn) bulBtn.disabled = rem < 1 && !lastBlank;
  if (numBtn) numBtn.disabled = rem < 1 && !lastBlank;
  if (qrBtn) qrBtn.disabled = (rem + (lastBlank ? 1 : 0)) < 3;
}

function renderTextRows() {
  updateTextRowToolbar();
  const c = document.getElementById('textRowsContainer');
  if (!c) return;
  c.innerHTML = '';
  textRows.forEach((row, idx) => {
    const wrap = document.createElement('div');
    wrap.className = 'text-row-grid';

    const alignVal = ALIGN_CYCLE.includes(row.align) ? row.align : 'CENTER';
    const isQr = /^\s*qrc:/i.test(row.text || '');
    const inp = document.createElement('input');
    inp.type = 'text';
    inp.maxLength = 240;
    inp.className = 'text-row-input ' + ALIGN_INPUT_CLASS[alignVal] + (isQr ? ' qr-row-input' : '');
    inp.placeholder = isQr ? 'qrc:https://...' : ('Row ' + (idx + 1) + ' (or qrc:URL)...');
    inp.value = row.text;
    inp.oninput = (e) => {
      textRows[idx].text = e.target.value;
      const nowQr = /^\s*qrc:/i.test(e.target.value || '');
      inp.classList.toggle('qr-row-input', nowQr);
      updateTextRowToolbar();
    };

    const alignBtn = document.createElement('button');
    alignBtn.type = 'button';
    alignBtn.className = 'row-toggle-btn';
    alignBtn.innerHTML = ALIGN_ICON[alignVal];
    alignBtn.title = ALIGN_TITLE[alignVal];
    alignBtn.onclick = () => {
      const next = ALIGN_CYCLE[(ALIGN_CYCLE.indexOf(alignVal) + 1) % ALIGN_CYCLE.length];
      textRows[idx].align = next;
      renderTextRows();
    };

    const sizeVal = SIZE_CYCLE.includes(row.fontSize) ? row.fontSize : 'MEDIUM';
    const sizeBtn = document.createElement('button');
    sizeBtn.type = 'button';
    sizeBtn.className = 'row-toggle-btn ' + SIZE_CLASS[sizeVal];
    sizeBtn.textContent = 'Aa';
    sizeBtn.title = SIZE_TITLE[sizeVal];
    sizeBtn.onclick = () => {
      const rem = Math.max(0, 10 - usedTextRows());
      const curW = rowWeight(textRows[idx]);
      let chosen = sizeVal;
      for (let step = 1; step <= SIZE_CYCLE.length; step++) {
        const cand = SIZE_CYCLE[(SIZE_CYCLE.indexOf(sizeVal) + step) % SIZE_CYCLE.length];
        const candW = isQr ? (cand === 'SMALL' ? 3 : cand === 'LARGE' ? 5 : 4) : 1;
        if (candW - curW <= rem) { chosen = cand; break; }
      }
      textRows[idx].fontSize = chosen;
      renderTextRows();
    };

    const del = document.createElement('button');
    del.type = 'button';
    del.className = 'row-del-btn';
    del.innerHTML = TRASH_SVG;
    del.title = 'Delete row';
    del.disabled = textRows.length <= 1;
    del.onclick = () => {
      if (textRows.length > 1) { textRows.splice(idx, 1); renderTextRows(); }
    };

    wrap.appendChild(inp);
    wrap.appendChild(alignBtn);
    wrap.appendChild(sizeBtn);
    wrap.appendChild(del);
    c.appendChild(wrap);
  });
}

function addTextRow(prefix, align) {
  const targetAlign = align || (textRows.length ? textRows[textRows.length - 1].align : 'CENTER');
  const isQr = /^\s*qrc:/i.test(prefix || '');
  const rem = Math.max(0, 10 - usedTextRows());
  const lastBlank = textRows.length > 0 && !(textRows[textRows.length - 1].text || '').trim();
  const avail = rem + (lastBlank ? 1 : 0);
  if (isQr) {
    if (avail < 3) return;
    const qrSize = avail >= 4 ? 'MEDIUM' : 'SMALL';
    if (lastBlank) {
      textRows[textRows.length - 1] = { text: prefix, align: targetAlign, fontSize: qrSize };
    } else {
      textRows.push({ text: prefix, align: targetAlign, fontSize: qrSize });
    }
    renderTextRows();
    return;
  }
  if (prefix && lastBlank) {
    textRows[textRows.length - 1] = { text: prefix, align: targetAlign, fontSize: textRows[textRows.length - 1].fontSize || 'MEDIUM' };
    renderTextRows();
    return;
  }
  if (rem < 1) return;
  textRows.push({ text: prefix || '', align: targetAlign, fontSize: 'MEDIUM' });
  renderTextRows();
}

function addNumberedRow() {
  let maxN = 0;
  textRows.forEach(r => {
    const m = r.text.match(/^\s*(\d+)\./);
    if (m) maxN = Math.max(maxN, parseInt(m[1], 10));
  });
  addTextRow((maxN + 1) + '. ', 'LEFT');
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
  if (Array.isArray(s.lines) && s.lines.length) {
    textRows = s.lines.slice(0, 10).map(l => ({ text: l.text || '', align: l.align || 'CENTER', fontSize: l.fontSize || 'MEDIUM' }));
  } else if (s.body) {
    textRows = s.body.split('\n').slice(0, 10).map(t => ({ text: t, align: s.textAlign || 'CENTER', fontSize: 'MEDIUM' }));
  } else {
    textRows = [{ text: '', align: 'CENTER', fontSize: 'MEDIUM' }];
  }
  renderTextRows();
  document.getElementById('slideDuration').value = String([10,15,20,30,45,60].includes(s.durationSeconds) ? s.durationSeconds : 15);
  document.getElementById('slideTitle').scrollIntoView({ behavior: 'smooth' });
}

function cancelEditSlide() {
  document.getElementById('editingSlideId').value = '';
  document.getElementById('textFormHeading').textContent = 'Add Text Announcement Slide';
  document.getElementById('saveTextSlideBtn').textContent = 'Add Text Slide';
  document.getElementById('cancelEditBtn').classList.add('hidden');
  document.getElementById('slideTitle').value = '';
  textRows = [{ text: '', align: 'CENTER', fontSize: 'MEDIUM' }];
  renderTextRows();
  document.getElementById('slideDuration').value = '15';
}

async function saveTextSlide() {
  const editId = document.getElementById('editingSlideId').value;
  const title = document.getElementById('slideTitle').value.trim();
  const cleanLines = textRows
    .map(r => ({ text: (r.text || '').replace(/\n/g, ' ').trimEnd(), align: r.align || 'CENTER', fontSize: r.fontSize || 'MEDIUM' }))
    .filter(r => r.text.trim().length > 0);
  const body = cleanLines.map(r => r.text).join('\n');
  const textAlign = cleanLines.length ? cleanLines[0].align : 'CENTER';
  const durationSeconds = parseInt(document.getElementById('slideDuration').value, 10) || 15;
  if (!title && !cleanLines.length) { showMsg('Enter a title or message first.', false); return; }
  if (editId) {
    await patchSlide(editId, { title, body, textAlign, lines: cleanLines, durationSeconds });
    cancelEditSlide();
    return;
  }
  const res = await api('/api/v1/slides/text', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, body, textAlign, lines: cleanLines, durationSeconds })
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
  const rawTitle = document.getElementById('imgSlideTitle').value.trim();
  if (!rawTitle) { showMsg('Please enter a slide title before uploading.', false); return; }
  const dur = parseInt(document.getElementById('imgSlideDuration').value, 10) || 15;
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

// --- INTUITIVE IQAMAH DELAYS (-5 / +5, 0..30m) ---
function renderIqamah() {
  const container = document.getElementById('iqamahContainer');
  container.innerHTML = '';
  PRAYERS.forEach(p => {
    const val = Math.max(0, Math.min(30, iqamahValues[p.key] || 0));
    const row = document.createElement('div');
    row.className = 'iq-row';
    const left = document.createElement('div');
    left.innerHTML = '<strong>' + p.label + '</strong>';
    const controls = document.createElement('div');
    controls.className = 'iq-chips';
    controls.style.alignItems = 'center';
    const minus = document.createElement('button');
    minus.className = 'chip';
    minus.textContent = '-5';
    minus.onclick = () => { iqamahValues[p.key] = Math.max(0, (iqamahValues[p.key] || 0) - 5); renderIqamah(); };
    const valLabel = document.createElement('span');
    valLabel.style.minWidth = '96px';
    valLabel.style.textAlign = 'center';
    valLabel.style.fontSize = '13px';
    valLabel.style.fontWeight = '700';
    valLabel.style.color = val > 0 ? '#10b981' : '#9ca3af';
    valLabel.textContent = val === 0 ? 'Disabled (0m)' : '+' + val + ' min';
    const plus = document.createElement('button');
    plus.className = 'chip';
    plus.textContent = '+5';
    plus.onclick = () => { iqamahValues[p.key] = Math.min(30, (iqamahValues[p.key] || 0) + 5); renderIqamah(); };
    controls.appendChild(minus);
    controls.appendChild(valLabel);
    controls.appendChild(plus);
    row.appendChild(left);
    row.appendChild(controls);
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
  const mosqueName = document.getElementById('mosqueName').value.trim();
  if (!mosqueName) { showMsg('Please enter your mosque name before saving.', false); return; }
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      mosqueName,
      mosqueSubtitle: document.getElementById('mosqueSubtitle').value.trim(),
      mosqueIconPreset: document.getElementById('mosqueIconPreset').value,
      mosqueFontPreset: document.getElementById('mosqueFontPreset').value,
      appLanguage: document.getElementById('appLanguage').value,
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

async function loadAuthMe() {
  const me = await api('/api/v1/auth/me');
  const sec = document.getElementById('pairedPhoneNameSection');
  const inp = document.getElementById('pairedPhoneNameInput');
  if (me.ok && me.data.authType === 'PAIRED') {
    if (sec) sec.classList.remove('hidden');
    if (inp && me.data.deviceName) inp.value = me.data.deviceName;
  } else if (sec) {
    sec.classList.add('hidden');
  }
}

async function saveTvIdentity() {
  const deviceName = (document.getElementById('tvDeviceName').value || '').trim();
  const roomGroup = (document.getElementById('tvRoomGroup').value || '').trim();
  if (!deviceName) { showMsg('Enter a TV name first.', false); return; }
  const res = await api('/api/v1/settings', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceName, roomGroup })
  });
  showMsg(res.ok ? 'TV name saved as "' + deviceName + '".' : (res.data.message || 'Save failed.'), res.ok);
  if (res.ok) await loadAll();
}

async function renameThisPhone(inputId) {
  const inp = document.getElementById(inputId);
  const name = (inp ? inp.value : '').trim();
  if (!name) { showMsg('Enter a name for this phone.', false); return; }
  const res = await api('/api/v1/auth/me', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ deviceName: name })
  });
  showMsg(res.ok ? 'Phone renamed to "' + name + '".' : (res.data.message || 'Rename failed.'), res.ok);
  if (res.ok) {
    const banner = document.getElementById('renamePhoneBanner');
    if (banner) banner.classList.add('hidden');
    const mainInp = document.getElementById('pairedPhoneNameInput');
    if (mainInp) mainInp.value = name;
  }
}

function dismissRenamePhoneBanner() {
  const banner = document.getElementById('renamePhoneBanner');
  if (banner) banner.classList.add('hidden');
}

renderTextRows();
document.getElementById('deviceNameInput').placeholder = defaultDeviceName();
loadStatus();
// Auto-pair from QR code hash (#pair=123456) and immediately clear hash from browser URL
const hashMatch = (location.hash || '').match(/pair=(\d{6})/);
if (hashMatch) {
  const qrCode = hashMatch[1];
  try { history.replaceState(null, '', location.pathname); } catch (_) {}
  document.getElementById('pairCodeInput').value = qrCode;
  pairWithCode(qrCode, true);
} else if (token) {
  document.getElementById('loginCard').classList.add('hidden');
  document.getElementById('app').classList.remove('hidden');
  document.getElementById('logoutBtn').classList.remove('hidden');
  loadAll();
  loadAuthMe();
}

// Review 4 L1: Strict CSP event delegation (zero inline handlers)
const __clickHandlers = {
  "0": () => { logout(); },
  "1": () => { pairWithCode(); },
  "2": () => { login(); },
  "3": () => { openTab('slides'); },
  "4": () => { openTab('schedule'); },
  "5": () => { openTab('silent'); },
  "6": () => { openTab('notice'); },
  "7": () => { openTab('branding'); },
  "8": () => { setSchedDur(30); },
  "9": () => { setSchedDur(60); },
  "10": () => { setSchedDur(90); },
  "11": () => { setSchedDur(120); },
  "12": () => { addTextRow('', ''); },
  "13": () => { addTextRow('• ', 'LEFT'); },
  "14": () => { addNumberedRow(); },
  "15": () => { saveTextSlide(); },
  "16": () => { cancelEditSlide(); },
  "17": () => { uploadImageSlide(); },
  "18": () => { saveBuiltInSlides(); },
  "19": () => { addSitting(); },
  "20": () => { saveSchedule(); },
  "21": () => { startSilent(15); },
  "22": () => { startSilent(30); },
  "23": () => { startSilent(45); },
  "24": () => { stopSilent(); },
  "25": () => { saveSilentSettings(); },
  "26": () => { saveBranding(); },
  "27": () => { uploadLogo(); },
  "28": () => { useNoticePreset('Parking Notice','Please move the vehicle blocking the driveway.'); },
  "29": () => { useNoticePreset('Facility Notice','Wudu area is temporarily closed for cleaning.'); },
  "30": () => { useNoticePreset('Safety Notice','Please keep all exits and walkways clear.'); },
  "31": () => { sendUrgentNotice(); },
  "32": () => { clearUrgentNotice(); },
  "33": () => { pushConfigToPeers(); },
  "34": () => { leavePeerGroup(); },
  "35": () => { generateGroupJoinCode(); },
  "36": () => { joinPeerGroup(); },
  "38": () => { renameThisPhone('quickPhoneNameInput'); },
  "39": () => { dismissRenamePhoneBanner(); },
  "40": () => { saveTvIdentity(); },
  "41": () => { renameThisPhone('pairedPhoneNameInput'); },
  "42": () => { addTextRow('qrc:', 'CENTER'); },
};
const __changeHandlers = {
  "37": () => { onJumuahModeChange(); },
};
document.addEventListener('click', (e) => {
  const el = e.target.closest('[data-evt-click]');
  if (!el) return;
  const fn = __clickHandlers[el.getAttribute('data-evt-click')];
  if (fn) fn();
});
document.addEventListener('change', (e) => {
  const el = e.target.closest('[data-evt-change]');
  if (!el) return;
  const fn = __changeHandlers[el.getAttribute('data-evt-change')];
  if (fn) fn();
});
