(() => {
'use strict';

const canvas = document.getElementById('game');
const ctx = canvas.getContext('2d');
const W = canvas.width, H = canvas.height;

const $ = (id) => document.getElementById(id);
const roomEl = $('room'), timeEl = $('time'), statusEl = $('status'), bestEl = $('best');
const vignetteEl = $('vignette'), hintEl = $('hint');
const menuOv = $('menu'), levelsOv = $('levels'), deadOv = $('dead'), wonOv = $('won');
const levelListEl = $('levelList');
const deadTimeEl = $('deadTime'), wonTimeEl = $('wonTime');
const playBtn = $('playBtn'), levelsBtn = $('levelsBtn'), backBtn = $('backBtn');
const retryBtn = $('retryBtn'), nextBtn = $('nextBtn');
const pauseOv = $('pause'), settingsOv = $('settings');
const resumeBtn = $('resumeBtn'), pauseSettingsBtn = $('pauseSettingsBtn'), pauseMenuBtn = $('pauseMenuBtn');
const menuSettingsBtn = $('menuSettingsBtn'), settingsBackBtn = $('settingsBackBtn'), resetBtn = $('resetBtn');
const volSlider = $('volSlider'), volVal = $('volVal'), resetNote = $('resetNote');
const deadMenuBtn = $('deadMenuBtn'), wonMenuBtn = $('wonMenuBtn');

let state = null;
let prev = null, next = null;           // interpolation pair {state, t}
let keys = {};
let mouse = { x: W / 2, y: H / 2 };
let started = false;
let lastStatus = null;
let menuOpen = true;                    // home page shows on load
let inLevels = false;                   // levels panel inside the menu
let pauseOpen = false;                  // pause panel during a run
let inSettings = false;                 // settings panel
let settingsFrom = 'menu';              // where settings was opened from
let savedVol = 0.8;
try { const v = parseFloat(localStorage.getItem('dl_volume')); if (isFinite(v)) savedVol = v; } catch (e) {}
let firstFrame = true;
let eyePos = null;
let prox = 0, hunting = false, danger = false;
let AC = null, master = null;

// ---------------- server stream ----------------
const es = new EventSource('/api/stream');
es.onmessage = (e) => {
  let s;
  try { s = JSON.parse(e.data); } catch { return; }
  state = s;
  prev = next; next = { state: s, t: performance.now() };
  if (!prev) prev = next;

  if (firstFrame) {
    firstFrame = false;
    // Arrive paused behind the home menu if a run was already in progress.
    if (s.status === 'PLAYING') post('/api/pause');
  }

  if (lastStatus !== s.status) {
    if (s.status === 'LOST') { deadTimeEl.textContent = 'You survived ' + s.time.toFixed(1) + 's'; jumpscare(); }
    else if (s.status === 'WON') { wonTimeEl.textContent = 'Escaped in ' + s.time.toFixed(1) + 's'; winChime(); }
    lastStatus = s.status;
  }
  refreshOverlays(s);
  refreshHud(s);
};

function refreshOverlays(s) {
  if (!s) return;
  const anyMenu = menuOpen || pauseOpen;
  deadOv.classList.toggle('hidden', anyMenu || s.status !== 'LOST');
  wonOv.classList.toggle('hidden', anyMenu || s.status !== 'WON');
  menuOv.classList.toggle('hidden', !(menuOpen && !inLevels && !inSettings));
  levelsOv.classList.toggle('hidden', !(menuOpen && inLevels));
  settingsOv.classList.toggle('hidden', !inSettings);
  pauseOv.classList.toggle('hidden', !pauseOpen || inSettings);
  hintEl.classList.toggle('hidden', s.status !== 'PLAYING' || anyMenu);
}

function refreshHud(s) {
  roomEl.textContent = s.room.name;
  timeEl.textContent = s.time.toFixed(1) + 's';
  bestEl.textContent = s.best != null ? 'BEST ' + s.best.toFixed(1) + 's' : '';
  const st = s.status;
  if (st === 'WAITING' || st === 'PAUSED') {
    statusEl.textContent = st === 'PAUSED' ? 'PAUSED' : 'AWAITING YOUR MOVE';
    statusEl.className = '';
  } else if (st === 'LOST') {
    statusEl.textContent = 'IT GOT YOU';
    statusEl.className = '';
  } else if (st === 'WON') {
    statusEl.textContent = 'YOU ESCAPED';
    statusEl.className = '';
  } else {
    const huntingNow = s.monster.state === 'HUNTING';
    statusEl.textContent = huntingNow ? 'IT IS HUNTING' : 'IT IS WATCHING';
    statusEl.className = huntingNow ? 'hunting' : 'watching';
  }
}

// ---------------- input ----------------
function post(url, body) {
  fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body || {})
  }).catch(() => {});
}

function sendInput() {
  const s = next && next.state;
  let aim = 0;
  if (s) {
    const cam = camera(s.room);
    aim = Math.atan2(
      (mouse.y - cam.oy) / cam.scale - s.player.y,
      (mouse.x - cam.ox) / cam.scale - s.player.x
    );
  }
  let vx = 0, vy = 0;
  if (keys['w'] || keys['arrowup']) vy -= 1;
  if (keys['s'] || keys['arrowdown']) vy += 1;
  if (keys['a'] || keys['arrowleft']) vx -= 1;
  if (keys['d'] || keys['arrowright']) vx += 1;
  post('/api/input', { vx, vy, aim });
}
setInterval(() => { if (started && !menuOpen && !pauseOpen) sendInput(); }, 33);

// ---------------- menu / game flow ----------------
function beginGame() {
  const s = state;
  if (s && (s.status === 'LOST' || s.status === 'WON')) post('/api/restart');
  else if (s && s.status === 'PAUSED') post('/api/resume');
  menuOpen = false;
  inLevels = false;
  inSettings = false;
  pauseOpen = false;
  started = true;
  refreshOverlays(s);
  refreshHud(s);
  sendInput();
}

function openMenu() {
  pauseOpen = false;
  inLevels = false;
  inSettings = false;
  menuOpen = true;
  refreshOverlays(state);
  refreshHud(state);
}

function openPause() {
  pauseOpen = true;
  menuOpen = false;
  inLevels = false;
  inSettings = false;
  if (state) post('/api/pause');
  refreshOverlays(state);
  refreshHud(state);
}

function resumeGame() {
  pauseOpen = false;
  refreshOverlays(state);
  refreshHud(state);
  post('/api/resume');
}

function openSettings(from) {
  inSettings = true;
  settingsFrom = from || 'menu';
  refreshOverlays(state);
  refreshHud(state);
}

function closeSettings() {
  inSettings = false;
  refreshOverlays(state);
  refreshHud(state);
}

function applyVolume() {
  const v = parseInt(volSlider.value, 10) / 100;
  try { localStorage.setItem('dl_volume', String(v)); } catch (e) {}
  if (master) master.gain.value = v;
  volVal.textContent = volSlider.value + '%';
}

function restart() {
  menuOpen = false;
  started = true;
  post('/api/restart');
  refreshOverlays(state);
  refreshHud(state);
  sendInput();
}

function nextRoom() {
  menuOpen = false;
  started = true;
  post('/api/next');
  refreshOverlays(state);
  refreshHud(state);
  sendInput();
}

// ---------------- levels ----------------
function selectLevel(payload) {
  post('/api/select', payload);
  menuOpen = false;
  inLevels = false;
  started = true;
  refreshOverlays(state);
  refreshHud(state);
  sendInput();
}

function openLevels() {
  inLevels = true;
  refreshOverlays(state);
  levelListEl.innerHTML = '<p class="level-note">Loading levels…</p>';
  fetch('/api/levels')
    .then((r) => r.json())
    .then(renderLevels)
    .catch(() => {
      levelListEl.innerHTML = '<p class="level-note">Could not load levels.</p>';
    });
}

function renderLevels(data) {
  levelListEl.innerHTML = '';
  if (!data || !data.story) {
    levelListEl.innerHTML = '<p class="level-note">Could not load levels.</p>';
    return;
  }
  for (const lv of data.story) {
    const row = document.createElement('button');
    row.className = 'level-row' + (lv.completed ? ' done' : '');
    const stateTxt = lv.completed ? '✓ ' + lv.best.toFixed(1) + 's' : 'not completed';
    row.innerHTML = '<span class="level-name"></span><span class="level-state"></span>';
    row.querySelector('.level-name').textContent = lv.name;
    row.querySelector('.level-state').textContent = stateTxt;
    row.addEventListener('click', () => selectLevel({ index: lv.index }));
    levelListEl.appendChild(row);
  }
  const endlessRow = document.createElement('button');
  endlessRow.className = 'level-row endless';
  endlessRow.innerHTML = '<span class="level-name">∞ Endless Mode</span><span class="level-state">procedural rooms — difficulty climbs</span>';
  endlessRow.addEventListener('click', () => selectLevel({ endless: true }));
  levelListEl.appendChild(endlessRow);
}

function backToMenu() {
  inLevels = false;
  refreshOverlays(state);
  refreshHud(state);
}

// ---------------- keyboard ----------------
window.addEventListener('keydown', (e) => {
  initAudio();
  if (e.key === 'Escape') {
    e.preventDefault();
    if (inSettings) closeSettings();
    else if (inLevels) backToMenu();
    else if (pauseOpen) resumeGame();
    else if (menuOpen) beginGame();
    else openPause();
    return;
  }
  const k = e.key.toLowerCase();
  keys[k] = true;
  if (menuOpen && !inLevels && !inSettings && (e.key === ' ' || e.key === 'Enter')) {
    e.preventDefault();
    beginGame();
    return;
  }
  if (k === 'r' && !menuOpen && !pauseOpen && state && state.status === 'LOST') restart();
  if (k === 'n' && !menuOpen && !pauseOpen && state && state.status === 'WON') nextRoom();
  if ([' ', 'arrowup', 'arrowdown', 'arrowleft', 'arrowright'].includes(e.key)) e.preventDefault();
});
window.addEventListener('keyup', (e) => { keys[e.key.toLowerCase()] = false; });
window.addEventListener('blur', () => { keys = {}; });

window.addEventListener('mousemove', (e) => {
  const r = canvas.getBoundingClientRect();
  mouse.x = (e.clientX - r.left) * (W / r.width);
  mouse.y = (e.clientY - r.top) * (H / r.height);
});
window.addEventListener('pointerdown', () => initAudio());

playBtn.addEventListener('click', beginGame);
levelsBtn.addEventListener('click', openLevels);
menuSettingsBtn.addEventListener('click', () => openSettings('menu'));
settingsBackBtn.addEventListener('click', closeSettings);
backBtn.addEventListener('click', backToMenu);
retryBtn.addEventListener('click', restart);
deadMenuBtn.addEventListener('click', openMenu);
nextBtn.addEventListener('click', nextRoom);
wonMenuBtn.addEventListener('click', openMenu);
resumeBtn.addEventListener('click', resumeGame);
pauseSettingsBtn.addEventListener('click', () => openSettings('pause'));
pauseMenuBtn.addEventListener('click', openMenu);
volSlider.addEventListener('input', applyVolume);
resetBtn.addEventListener('click', () => {
  fetch('/api/reset', { method: 'POST' }).then(() => {
    resetNote.textContent = 'Progress reset.';
    resetNote.classList.remove('hidden');
    setTimeout(() => resetNote.classList.add('hidden'), 2500);
  }).catch(() => {});
});
volSlider.value = String(Math.round(savedVol * 100));
volVal.textContent = volSlider.value + '%';

// ---------------- camera & render ----------------
function camera(room) {
  const rw = room.width * room.tile, rh = room.height * room.tile;
  const scale = Math.min((W - 48) / rw, (H - 48) / rh);
  return { scale, ox: (W - rw * scale) / 2, oy: (H - rh * scale) / 2 };
}

const lerp2 = (a, b, t) => ({ x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t });

function rr(c, x, y, w, h, r) {
  c.beginPath();
  if (c.roundRect) c.roundRect(x, y, w, h, r);
  else c.rect(x, y, w, h);
  c.fill();
}

const dark = document.createElement('canvas');
dark.width = W; dark.height = H;
const dctx = dark.getContext('2d');

function render(now) {
  requestAnimationFrame(render);
  if (!state) return;
  let s = state;
  if (prev && next && next.t > prev.t) {
    const a = Math.max(0, Math.min(1, (now - prev.t) / (next.t - prev.t)));
    s = {
      ...state,
      player: lerp2(prev.state.player, state.player, a),
      monster: lerp2(prev.state.monster, state.monster, a),
    };
  }
  updateMood(s);
  drawScene(s, now);
}
requestAnimationFrame(render);

function updateMood(s) {
  const d = Math.hypot(s.player.x - s.monster.x, s.player.y - s.monster.y);
  prox = Math.max(0, Math.min(1, 1 - d / (300 * 1.3)));
  hunting = s.status === 'PLAYING' && s.monster.state === 'HUNTING';
  danger = hunting && prox > 0.55;
  vignetteEl.classList.toggle('danger', danger);
}

function drawScene(s, now) {
  const room = s.room;
  const cam = camera(room);
  const { scale, ox, oy } = cam;
  const ts = room.tile * scale;
  const rw = room.width * ts, rh = room.height * ts;

  ctx.fillStyle = '#010209';
  ctx.fillRect(0, 0, W, H);

  // floor
  ctx.fillStyle = '#0a0d15';
  ctx.fillRect(ox, oy, rw, rh);
  ctx.fillStyle = 'rgba(255,255,255,0.016)';
  for (let ty = 0; ty < room.height; ty++)
    for (let tx = 0; tx < room.width; tx++)
      if ((tx + ty) % 2 === 0)
        ctx.fillRect(ox + tx * ts, oy + ty * ts, ts, ts);

  drawExit(room, cam, now);
  blocks(room, room.furniture, cam, '#161c2b', '#2b3550');
  blocks(room, room.walls, cam, '#0f1422', '#2c3a5c');
  drawMonster(s, cam, now);
  drawDarkness(s, cam, now);
  drawPlayer(s, cam, now);
  drawEyes(now);
}

function blocks(room, tiles, cam, fill, top) {
  const ts = room.tile * cam.scale;
  const e = Math.max(2, ts * 0.07);
  for (const t of tiles) {
    const x = cam.ox + t[0] * ts, y = cam.oy + t[1] * ts;
    ctx.fillStyle = fill;
    ctx.fillRect(x, y, ts, ts);
    ctx.fillStyle = top;
    ctx.fillRect(x, y, ts, e);
    ctx.fillRect(x, y, e, ts);
    ctx.fillStyle = 'rgba(0,0,0,0.45)';
    ctx.fillRect(x, y + ts - e, ts, e);
    ctx.fillRect(x + ts - e, y, e, ts);
  }
}

function drawExit(room, cam, now) {
  const x = cam.ox + room.exit[0] * cam.scale;
  const y = cam.oy + room.exit[1] * cam.scale;
  const ts = room.tile * cam.scale;
  const pulse = 0.5 + 0.5 * Math.sin(now / 280);
  const w = ts * 0.72, h = ts * 1.05;
  const g = ctx.createRadialGradient(x, y + h * 0.3, 2, x, y + h * 0.3, ts * 0.95);
  g.addColorStop(0, 'rgba(70,255,170,' + (0.28 + 0.12 * pulse) + ')');
  g.addColorStop(1, 'rgba(70,255,170,0)');
  ctx.fillStyle = g;
  ctx.fillRect(x - ts * 0.95, y - ts * 0.55, ts * 1.9, ts * 1.7);
  ctx.save();
  ctx.shadowColor = 'rgba(60,240,160,0.95)';
  ctx.shadowBlur = 14 + 12 * pulse;
  ctx.fillStyle = 'rgba(90,255,185,' + (0.65 + 0.3 * pulse) + ')';
  rr(ctx, x - w / 2, y + h * 0.15 - h, w, h, 4);
  ctx.restore();
}

function drawMonster(s, cam) {
  const ts = s.room.tile * cam.scale;
  const mx = cam.ox + s.monster.x * cam.scale;
  const my = cam.oy + s.monster.y * cam.scale;
  const w = ts * 0.6, h = ts * 0.85;
  const observed = s.monster.observed;
  eyePos = { x: mx, y: my - h * 0.95, spread: w * 0.3, observed };

  ctx.fillStyle = 'rgba(0,0,0,0.5)';
  ctx.beginPath();
  ctx.ellipse(mx, my + h * 0.15, w * 0.6, h * 0.12, 0, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = observed ? '#c6cbd8' : '#05070d';
  rr(ctx, mx - w / 2, my - h * 0.85, w, h * 0.85, 4);
  ctx.beginPath();
  ctx.arc(mx, my - h * 0.95, w * 0.42, 0, Math.PI * 2);
  ctx.fill();
  if (observed) {
    ctx.strokeStyle = 'rgba(20,22,30,0.6)';
    ctx.lineWidth = 2;
    ctx.stroke();
  }
}

function drawEyes(now) {
  if (!eyePos) return;
  const pulse = 0.6 + 0.4 * Math.sin(now / 160);
  const rad = Math.max(2, 3.2);
  ctx.save();
  if (eyePos.observed) {
    ctx.fillStyle = '#14161f';
    ctx.fillRect(eyePos.x - eyePos.spread - rad, eyePos.y - rad / 2, rad * 2, rad);
    ctx.fillRect(eyePos.x + eyePos.spread - rad, eyePos.y - rad / 2, rad * 2, rad);
  } else {
    ctx.globalCompositeOperation = 'lighter';
    ctx.shadowColor = '#ff2222';
    ctx.shadowBlur = 12 * pulse;
    ctx.fillStyle = 'rgba(255,40,40,' + (0.75 + 0.25 * pulse) + ')';
    ctx.fillRect(eyePos.x - eyePos.spread - rad, eyePos.y - rad / 2, rad * 2, rad);
    ctx.fillRect(eyePos.x + eyePos.spread - rad, eyePos.y - rad / 2, rad * 2, rad);
  }
  ctx.restore();
}

function drawPlayer(s, cam) {
  const ts = s.room.tile * cam.scale;
  const px = cam.ox + s.player.x * cam.scale;
  const py = cam.oy + s.player.y * cam.scale;
  const aim = s.player.aim;

  ctx.fillStyle = 'rgba(0,0,0,0.55)';
  ctx.beginPath();
  ctx.ellipse(px, py + ts * 0.14, ts * 0.2, ts * 0.07, 0, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = '#39415a';
  ctx.fillRect(px - ts * 0.16, py - ts * 0.02, ts * 0.12, ts * 0.16);
  ctx.fillRect(px + ts * 0.04, py - ts * 0.02, ts * 0.12, ts * 0.16);

  ctx.fillStyle = '#dfe6f4';
  rr(ctx, px - ts * 0.16, py - ts * 0.4, ts * 0.32, ts * 0.4, ts * 0.08);

  ctx.fillStyle = '#eceff8';
  ctx.beginPath();
  ctx.arc(px, py - ts * 0.48, ts * 0.14, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = '#ffd9a0';
  ctx.beginPath();
  ctx.arc(px + Math.cos(aim) * ts * 0.42, py + Math.sin(aim) * ts * 0.42, Math.max(2, ts * 0.05), 0, Math.PI * 2);
  ctx.fill();
}

function conePath(c, px, py, aim, half, range) {
  c.beginPath();
  c.moveTo(px, py);
  const steps = 10;
  for (let i = 0; i <= steps; i++) {
    const a = aim - half + (2 * half) * (i / steps);
    c.lineTo(px + Math.cos(a) * range, py + Math.sin(a) * range);
  }
  c.closePath();
}

function drawDarkness(s, cam) {
  const { scale, ox, oy } = cam;
  const px = ox + s.player.x * scale;
  const py = oy + s.player.y * scale;
  const aim = s.player.aim;
  const range = 300 * scale;
  const half = 0.60;

  dctx.globalCompositeOperation = 'source-over';
  dctx.clearRect(0, 0, W, H);
  dctx.fillStyle = 'rgba(1,3,12,0.955)';
  dctx.fillRect(0, 0, W, H);

  dctx.globalCompositeOperation = 'destination-out';

  const ag = dctx.createRadialGradient(px, py, 6, px, py, 90 * scale);
  ag.addColorStop(0, 'rgba(0,0,0,1)');
  ag.addColorStop(1, 'rgba(0,0,0,0)');
  dctx.fillStyle = ag;
  dctx.beginPath();
  dctx.arc(px, py, 90 * scale, 0, Math.PI * 2);
  dctx.fill();

  dctx.fillStyle = 'rgba(0,0,0,0.28)';
  conePath(dctx, px, py, aim, half * 1.35, range * 1.05);
  dctx.fill();

  const cg = dctx.createRadialGradient(px, py, 2, px, py, range);
  cg.addColorStop(0, 'rgba(0,0,0,1)');
  cg.addColorStop(0.75, 'rgba(0,0,0,0.96)');
  cg.addColorStop(1, 'rgba(0,0,0,0.78)');
  dctx.fillStyle = cg;
  conePath(dctx, px, py, aim, half, range);
  dctx.fill();

  ctx.drawImage(dark, 0, 0);

  ctx.save();
  ctx.globalCompositeOperation = 'lighter';
  const lg = ctx.createRadialGradient(px, py, 0, px, py, range);
  lg.addColorStop(0, 'rgba(255,214,150,0.10)');
  lg.addColorStop(0.6, 'rgba(255,190,120,0.05)');
  lg.addColorStop(1, 'rgba(255,190,120,0)');
  ctx.fillStyle = lg;
  ctx.beginPath();
  ctx.arc(px, py, range, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

// ---------------- audio ----------------
function initAudio() {
  if (AC) return;
  const Ctor = window.AudioContext || window.webkitAudioContext;
  if (!Ctor) return;
  AC = new Ctor();
  master = AC.createGain();
  master.gain.value = savedVol;
  master.connect(AC.destination);

  const g = AC.createGain();
  g.gain.value = 0.02;
  const o1 = AC.createOscillator(); o1.type = 'sine'; o1.frequency.value = 52;
  const o2 = AC.createOscillator(); o2.type = 'sine'; o2.frequency.value = 52.8;
  const o3 = AC.createOscillator(); o3.type = 'triangle'; o3.frequency.value = 26.2;
  const f = AC.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 120;
  o1.connect(f); o2.connect(f); o3.connect(f);
  f.connect(g); g.connect(master);
  o1.start(); o2.start(); o3.start();

  scheduleBeat();
}

function scheduleBeat() {
  if (!AC) return;
  const iv = hunting ? Math.max(0.35, 0.95 - 0.6 * prox) : 1.15;
  thump(AC.currentTime, iv);
  setTimeout(scheduleBeat, iv * 1000);
}

function thump(t, iv) {
  const o = AC.createOscillator(); o.type = 'sine';
  o.frequency.setValueAtTime(58, t);
  o.frequency.exponentialRampToValueAtTime(36, t + 0.16);
  const g = AC.createGain();
  const vol = hunting ? 0.16 + 0.5 * prox : 0.10;
  g.gain.setValueAtTime(vol, t);
  g.gain.exponentialRampToValueAtTime(0.001, t + 0.22);
  o.connect(g); g.connect(master);
  o.start(t); o.stop(t + 0.25);
}

function jumpscare() {
  if (!AC) return;
  const t = AC.currentTime;
  const len = Math.floor(AC.sampleRate * 0.5);
  const buf = AC.createBuffer(1, len, AC.sampleRate);
  const d = buf.getChannelData(0);
  for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / len);
  const src = AC.createBufferSource(); src.buffer = buf;
  const f = AC.createBiquadFilter(); f.type = 'lowpass'; f.frequency.value = 1400;
  const g = AC.createGain();
  g.gain.setValueAtTime(0.7, t);
  g.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
  src.connect(f); f.connect(g); g.connect(master);
  src.start(t);

  const o = AC.createOscillator(); o.type = 'sawtooth';
  o.frequency.setValueAtTime(130, t);
  o.frequency.exponentialRampToValueAtTime(34, t + 0.6);
  const og = AC.createGain();
  og.gain.setValueAtTime(0.4, t);
  og.gain.exponentialRampToValueAtTime(0.001, t + 0.6);
  o.connect(og); og.connect(master);
  o.start(t); o.stop(t + 0.65);
}

function winChime() {
  if (!AC) return;
  [523.25, 659.25, 783.99].forEach((freq, i) => {
    const t = AC.currentTime + i * 0.18;
    const o = AC.createOscillator(); o.type = 'sine'; o.frequency.value = freq;
    const g = AC.createGain();
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(0.18, t + 0.03);
    g.gain.exponentialRampToValueAtTime(0.001, t + 0.7);
    o.connect(g); g.connect(master);
    o.start(t); o.stop(t + 0.75);
  });
}

// ---------------- debug hooks (used by tests / devtools) ----------------
window.__dbg = {
  move: (vx, vy) => { keys['w'] = vy < 0; keys['s'] = vy > 0; keys['a'] = vx < 0; keys['d'] = vx > 0; },
  aim: (x, y) => { mouse.x = x; mouse.y = y; },
  post,
  play: beginGame,
  menu: openMenu,
  pause: openPause,
  resume: resumeGame,
  settings: () => openSettings('menu'),
  levels: openLevels,
  select: selectLevel,
  restart,
  next: nextRoom,
  state: () => state,
};
})();
