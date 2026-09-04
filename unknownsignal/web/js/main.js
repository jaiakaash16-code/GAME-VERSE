'use strict';

/* UNKNOWN SIGNAL — game controller. */

const $ = id => document.getElementById(id);

const App = {
  session: null,
  level: null,
  tune: null,
  freq: 100.0,
  bw: 3.0,
  lockedPrev: false,
  renderedLogCount: 0,
  lastMark: null
};

async function api(path, body) {
  const opts = { method: body ? 'POST' : 'GET' };
  if (body) {
    opts.headers = { 'Content-Type': 'application/json' };
    opts.body = JSON.stringify(body);
  }
  const res = await fetch(path, opts);
  if (!res.ok) throw new Error(path + ' -> ' + res.status);
  return res.json();
}

const levelIs = n => App.level && App.level.level === n;

// ---------------- boot ----------------

window.addEventListener('DOMContentLoaded', async () => {
  Spectrum.init();
  Fx.init();
  Radio.init();
  Radio.onFreq = f => { App.freq = f; scheduleTune(); };
  Radio.onBw = b => { App.bw = b; scheduleTune(); };
  Radio.onAngle = a => { setFeedback('DIRECTION: ' + Math.round(a) + '°'); };

  $('playBtn').addEventListener('click', playMorse);
  $('decodeBtn').addEventListener('click', submitDecode);
  $('decodeInput').addEventListener('keydown', e => { if (e.key === 'Enter') submitDecode(); });
  $('bearingBtn').addEventListener('click', takeBearing);
  $('mapBtn').addEventListener('click', () => openMap());
  $('soundBtn').addEventListener('click', () => {
    MorseAudio.ensure();
    const on = MorseAudio.toggle();
    $('soundBtn').textContent = on ? 'SND ON' : 'SND OFF';
    $('homeSound').textContent = 'SOUND: ' + (on ? 'ON' : 'OFF');
  });
  $('homeBtn').addEventListener('click', showHome);
  $('homePower').addEventListener('click', async () => {
    MorseAudio.ensure();
    MorseAudio.transmitBeep();
    Fx.staticBurst(0.2);
    if (App.session.finished) {
      const resp = await api('/api/reset', {});
      App.session = resp.session;
      await refreshAll();
    }
    $('homeScreen').hidden = true;
    startLevel();
  });
  $('homeChart').addEventListener('click', () => { buildChart(); $('helpModal').hidden = false; });
  $('homeSound').addEventListener('click', () => {
    MorseAudio.ensure();
    const on = MorseAudio.toggle();
    $('homeSound').textContent = 'SOUND: ' + (on ? 'ON' : 'OFF');
    $('soundBtn').textContent = on ? 'SND ON' : 'SND OFF';
  });
  $('homeReset').addEventListener('click', async () => {
    if (!confirm('ERASE ALL TRANSMISSIONS AND START OVER?')) return;
    await api('/api/reset', {});
    location.reload();
  });
  $('completeLevelBtn').addEventListener('click', advanceLevel);
  $('autoFilterBtn').addEventListener('click', () => {
    Radio.setBw(1.0);
    MorseAudio.click();
  });
  $('helpBtn').addEventListener('click', () => { $('helpModal').hidden = false; buildChart(); });
  $('helpClose').addEventListener('click', () => { $('helpModal').hidden = true; });
  $('resetBtn').addEventListener('click', async () => {
    if (!confirm('ERASE ALL TRANSMISSIONS AND START OVER?')) return;
    await api('/api/reset', {});
    location.reload();
  });

  try {
    App.session = await api('/api/session');
  } catch (e) {
    console.error(e);
    setOverlay(ovBox('LINK LOST', 'CANNOT REACH STATION 7', 'CHECK THAT THE JAVA SERVER IS RUNNING.', { label: 'RETRY', fn: () => location.reload() }));
    return;
  }
  renderHud();
  renderLog(true);
  showHome();
  requestAnimationFrame(loop);
});

function loop(t) {
  requestAnimationFrame(loop);
  const bands = App.level ? App.level.bands : [];
  Spectrum.frame(t, bands, App.freq, App.tune ? App.tune.strength : 0, App.tune ? !!App.tune.locked : false);
}

// ---------------- overlays ----------------

function ovBox(title, sub, text, button, extraClass) {
  const el = document.createElement('div');
  el.className = 'ov-box' + (extraClass ? ' ' + extraClass : '');
  if (title) { const t = document.createElement('div'); t.className = 'ov-title'; t.textContent = title; el.appendChild(t); }
  if (sub) { const s = document.createElement('div'); s.className = 'ov-sub'; s.textContent = sub; el.appendChild(s); }
  if (text) { const x = document.createElement('div'); x.className = 'ov-text'; x.textContent = text; el.appendChild(x); }
  if (button) {
    const b = document.createElement('button');
    b.className = 'ov-btn';
    if (typeof button === 'function') {
      b.textContent = 'CONTINUE';
      b.addEventListener('click', button);
    } else {
      b.textContent = button.label || 'CONTINUE';
      b.addEventListener('click', button.fn);
    }
    el.appendChild(b);
  }
  return el;
}

function setOverlay(el) {
  const ov = $('overlay');
  ov.innerHTML = '';
  ov.appendChild(el);
  ov.classList.remove('hidden');
}

function hideOverlay() { $('overlay').classList.add('hidden'); }

function showHome() {
  hideOverlay();
  const status = $('homeStatus');
  if (App.session.finished) {
    status.textContent = 'SIGNAL ENDED — ' + (App.session.ending || '') + ' · THE FREQUENCIES REMEMBER YOU';
  } else if (App.session.level > 1 || App.session.archive.length > 0) {
    status.textContent = 'SESSION RESUMES AT LEVEL ' + App.session.level + ' — ' + App.session.title;
  } else {
    status.textContent = 'STANDBY — NO ACTIVE TRANSMISSION';
  }
  $('homePower').textContent = App.session.finished ? 'LISTEN AGAIN' : 'POWER ON';
  $('homeSound').textContent = 'SOUND: ' + (MorseAudio.enabled ? 'ON' : 'OFF');
  $('homeScreen').hidden = false;
}

// ---------------- levels ----------------

async function startLevel() {
  App.level = await api('/api/level');
  renderHud();
  $('homeScreen').hidden = true;
  if (App.session.finished) { showEndingFromSession(); return; }
  const st = App.level.state;
  const el = ovBox('LEVEL ' + App.level.level + ' — ' + App.level.title,
    'TRANSMISSION BRIEFING',
    App.level.brief.join('\n'),
    { label: 'BEGIN TRANSMISSION', fn: () => { hideOverlay(); enterPlay(); } });
  const goal = document.createElement('div');
  goal.className = 'ov-goal';
  goal.textContent = 'GOAL: ' + App.level.goal;
  el.appendChild(goal);
  setOverlay(el);
  renderLevelUI();
}

function enterPlay() {
  MorseAudio.ensure();
  renderLevelUI();
  doTune();
}

function renderHud() {
  const t = App.session ? App.session.title : '';
  $('levelTag').textContent = App.session && App.session.finished
    ? 'SIGNAL ENDED — ' + (App.session.ending || '')
    : 'LEVEL ' + (App.session ? App.session.level : '?') + ' · ' + t;
}

// ---------------- tuning ----------------

let tuneTimer = null;
function scheduleTune() {
  clearTimeout(tuneTimer);
  tuneTimer = setTimeout(doTune, 120);
}

async function doTune() {
  try {
    const r = await api('/api/tune', { frequency: App.freq, bandwidth: App.bw });
    App.tune = r;
    renderTune(r);
  } catch (e) {
    console.error(e);
  }
}

function renderTune(r) {
  const lamp = $('lockLamp');
  if (r.garbled) { lamp.textContent = 'GARBLED'; lamp.className = 'lock-lamp garbled'; }
  else if (r.locked) { lamp.textContent = 'LOCKED'; lamp.className = 'lock-lamp locked'; }
  else { lamp.textContent = '\u2014'; lamp.className = 'lock-lamp'; }

  if (r.locked && !App.lockedPrev) {
    Fx.staticBurst(0.22);
    Fx.shake(4);
    MorseAudio.lockChime();
    if (r.stable !== undefined && r.stable >= 0.8) MorseAudio.presence();
  }
  App.lockedPrev = r.locked;

  setMeter('strengthFill', r.strength || 0);
  if (r.stable !== undefined) setMeter('stableFill', r.stable);
  else if (levelIs(5)) setMeter('stableFill', 0);

  const mo = $('morseOut');
  if (r.locked && r.morse) {
    mo.textContent = r.morse;
    $('playBtn').disabled = false;
    $('decodeInput').disabled = false;
    $('decodeBtn').disabled = false;
    $('autoFilterBtn').hidden = true;
  } else if (r.locked && r.bearingMode) {
    mo.textContent = 'SIGNAL ACQUIRED — BEARING MODE';
    $('playBtn').disabled = true;
    $('decodeInput').disabled = true;
    $('decodeBtn').disabled = true;
    $('autoFilterBtn').hidden = true;
  } else if (r.garbled) {
    mo.textContent = 'GARBLED — FILTER TOO WIDE · NARROW IT BELOW 2.0';
    $('playBtn').disabled = true;
    $('decodeInput').disabled = true;
    $('decodeBtn').disabled = true;
    $('autoFilterBtn').hidden = false;
  } else {
    mo.textContent = '\u00b7 \u00a0\u00b7 \u00a0\u00b7';
    $('playBtn').disabled = true;
    $('decodeInput').disabled = true;
    $('decodeBtn').disabled = true;
    $('autoFilterBtn').hidden = true;
  }

  $('antennaRow').hidden = !r.bearingMode;
  if (levelIs(3) && App.level.state.readyToMark) $('mapBtn').hidden = false;
}

function setMeter(id, v) {
  const fill = $(id);
  if (fill) fill.style.width = Math.max(0, Math.min(100, v * 100)).toFixed(0) + '%';
}

// ---------------- morse playback ----------------

function playMorse() {
  if (!App.tune || !App.tune.morse) return;
  const pattern = App.tune.morse;
  const mo = $('morseOut');
  let ord = 0;
  const html = pattern.split('').map(ch => {
    if (ch === '.') { ord++; return `<span class="sym" data-o="${ord - 1}">\u00b7</span>`; }
    if (ch === '-') { ord++; return `<span class="sym" data-o="${ord - 1}">-</span>`; }
    return ch === '/' ? ' / ' : ' ';
  }).join('');
  mo.innerHTML = html;
  const spans = mo.querySelectorAll('.sym');

  // per-symbol start offsets, same timing as the audio
  const unit = 0.09;
  const offsets = [];
  let t = 0.08;
  for (const ch of pattern) {
    if (ch === '.') { offsets.push(t); t += unit + unit; }
    else if (ch === '-') { offsets.push(t); t += unit * 3 + unit; }
    else if (ch === ' ') t += unit * 2;
    else if (ch === '/') t += unit * 4;
  }
  MorseAudio.playPattern(pattern, () => Spectrum.flash());
  offsets.forEach((off, i) => {
    setTimeout(() => {
      spans.forEach(s => s.classList.remove('active'));
      if (spans[i]) spans[i].classList.add('active');
    }, off * 1000);
  });
}

// ---------------- decode ----------------

async function submitDecode() {
  const input = $('decodeInput');
  const text = input.value.trim();
  if (!text) return;
  let r;
  try { r = await api('/api/decode', { text }); } catch (e) { console.error(e); return; }
  if (r.ok) {
    MorseAudio.success();
    Fx.staticBurst(0.12);
    input.value = '';
    setFeedbackOk(r.plain ? 'DECODED: ' + r.plain : 'ECHO CORRECTED');
    await refreshAll();
    if (r.complete) { showComplete(); return; }
    if (levelIs(4)) { await doTune(); }
    if (r.endingReady) renderLevelUI();
  } else {
    setFeedback(r.reason || 'NO MATCH — LISTEN AGAIN');
    Fx.shake(2);
    MorseAudio.deny();
  }
}

// ---------------- transmit / choices ----------------

async function transmit(msg) {
  let r;
  try { r = await api('/api/transmit', { message: msg }); } catch (e) { console.error(e); return; }
  if (r.ok) {
    MorseAudio.transmitBeep();
    Fx.staticBurst(0.15);
    if (r.ending) {
      App.session = { ...App.session, finished: true, ending: r.ending };
      showEnding(r);
      return;
    }
    await refreshAll();
    if (r.complete) { showComplete(); return; }
  } else {
    setFeedback(r.reason || 'NO RESPONSE');
    MorseAudio.deny();
  }
}

// ---------------- bearing / map ----------------

async function takeBearing() {
  let r;
  try { r = await api('/api/bearing', { angle: Radio.angle }); } catch (e) { console.error(e); return; }
  if (r.ok) {
    MorseAudio.click();
    setFeedbackOk('BEARING ' + r.angle + '\u00b0 — STRENGTH ' + Math.round(r.strength * 100) + '%');
    await refreshAll();
    if (r.readyToMark) setFeedback('THREE BEARINGS TAKEN — OPEN THE MAP AND MARK THE SOURCE');
    if (r.complete) { showComplete(); return; }
  } else {
    setFeedback(r.reason || 'NO READING');
    MorseAudio.deny();
  }
}

function openMap() {
  MapView.open(App.level.state.bearings, App.level.state.marked, App.lastMark, async (x, y) => {
    App.lastMark = { x, y };
    let r;
    try { r = await api('/api/mark', { x, y }); } catch (e) { console.error(e); return; }
    if (r.ok) {
      MorseAudio.success();
      setFeedbackOk('SOURCE MARKED');
      await refreshAll();
      if (r.complete) { MapView.close(); showComplete(); return; }
    } else {
      setFeedback(r.reason || 'NO SOURCE THERE');
      MorseAudio.deny();
    }
  });
}

// ---------------- completion / endings ----------------

async function advanceLevel() {
  const resp = await api('/api/advance', {});
  App.session = resp.session;
  await refreshAll();
  if (App.session.finished) { showEndingFromSession(); return; }
  await startLevel();
}

async function showComplete() {
  App.level = await api('/api/level');
  const el = ovBox('LEVEL ' + App.level.level + ' COMPLETE', App.level.title, null, {
    label: 'COMPLETE LEVEL',
    fn: advanceLevel
  });
  setOverlay(el);
}

function showEndingFromSession() {
  const key = App.session.ending;
  const ending = App.level && App.level.endings ? App.level.endings.find(e => e.key === key) : null;
  if (!ending) { showEnding({ ending: key, title: key, text: '', epilogue: '' }); return; }
  showEnding(ending);
}

function showEnding(r) {
  const el = ovBox('SIGNAL ENDED — ' + r.title, 'TRANSMISSION COMPLETE', null, null);
  const txt = document.createElement('div');
  txt.className = 'ov-ending';
  txt.textContent = r.text;
  el.appendChild(txt);
  const ep = document.createElement('div');
  ep.className = 'ov-epilogue';
  ep.textContent = r.epilogue;
  el.appendChild(ep);
  const b = document.createElement('button');
  b.className = 'ov-btn';
  b.textContent = 'LISTEN AGAIN';
  b.addEventListener('click', async () => {
    await api('/api/reset', {});
    location.reload();
  });
  el.appendChild(b);
  setOverlay(el);
  MorseAudio.presence();
  Fx.staticBurst(0.3);
}

// ---------------- refresh ----------------

async function refreshAll() {
  App.session = await api('/api/session');
  App.level = await api('/api/level');
  renderHud();
  renderLog(false);
  renderLevelUI();
}

function renderLevelUI() {
  if (!App.level) return;
  const st = App.level.state;
  const choice = $('choiceArea');
  choice.innerHTML = '';
  setFeedback('');

  if (levelIs(2) && st.decoded.includes('SOS') && st.decoded.includes('STAY AWAY') && !st.choiceMade) {
    const t = document.createElement('div');
    t.className = 'choice-title';
    t.textContent = 'ANSWER ONE SIGNAL:';
    const row = document.createElement('div');
    row.className = 'choice-row';
    row.innerHTML = '<button data-msg="SOS">ANSWER THE SOS</button><button data-msg="STAY AWAY">ANSWER THE WARNING</button>';
    choice.appendChild(t); choice.appendChild(row);
    row.querySelectorAll('button').forEach(b => b.addEventListener('click', () => transmit(b.dataset.msg)));
  }

  if (levelIs(5) && st.decodedAnswer && !st.answered) {
    const t = document.createElement('div');
    t.className = 'choice-title';
    t.textContent = 'THE SIGNAL ASKS: "' + (App.level.askText || 'ARE YOU ALONE?') + '"';
    const row = document.createElement('div');
    row.className = 'question-row';
    row.innerHTML = '<input id="qInput" type="text" placeholder="YES or NO" autocomplete="off"><button id="qSend">KEY</button>';
    choice.appendChild(t); choice.appendChild(row);
    $('qSend').addEventListener('click', () => { transmit($('qInput').value); $('qInput').value = ''; });
    $('qInput').addEventListener('keydown', e => { if (e.key === 'Enter') { transmit(e.target.value); e.target.value = ''; } });
  }

  if (levelIs(6) && st.endingReady && !st.finished) {
    const t = document.createElement('div');
    t.className = 'choice-title';
    t.textContent = 'MAKE YOUR FINAL TRANSMISSION:';
    const row = document.createElement('div');
    row.className = 'choice-row';
    row.innerHTML = '<button data-msg="TRUTH">BROADCAST THE TRUTH</button><button data-msg="SEVER">SEVER THE LINK</button><button data-msg="JOIN">JOIN THE ANTENNA</button>';
    choice.appendChild(t); choice.appendChild(row);
    row.querySelectorAll('button').forEach(b => b.addEventListener('click', () => transmit(b.dataset.msg)));
  }

  if (levelIs(3)) {
    $('mapBtn').hidden = !(st.readyToMark && !st.marked);
  }
  $('completeLevelBtn').hidden = !(st.complete && !st.finished);
}

// ---------------- log ----------------

function renderLog(initial) {
  const body = $('logBody');
  const lines = App.session.archive;
  if (initial) {
    body.innerHTML = '';
    for (const e of lines) body.appendChild(logEntry(e, false));
    App.renderedLogCount = lines.length;
    body.scrollTop = body.scrollHeight;
    return;
  }
  for (let i = App.renderedLogCount; i < lines.length; i++) {
    body.appendChild(logEntry(lines[i], true));
  }
  App.renderedLogCount = lines.length;
  body.scrollTop = body.scrollHeight;
}

function logEntry(e, animate) {
  const div = document.createElement('div');
  div.className = 'entry ' + (e.type || 'narrative');
  const t = document.createElement('span');
  t.className = 't';
  const now = new Date();
  t.textContent = now.toTimeString().slice(0, 5) + ' ' + (e.type === 'signal' ? 'SIG' : e.type === 'system' ? 'SYS' : 'LOG') + ' ';
  const x = document.createElement('span');
  x.className = 'text';
  div.appendChild(t);
  div.appendChild(x);
  if (animate) {
    let i = 0;
    x.textContent = '';
    const iv = setInterval(() => {
      i++;
      x.textContent = e.text.slice(0, i);
      if (i >= e.text.length) clearInterval(iv);
    }, 14);
  } else {
    x.textContent = e.text;
  }
  return div;
}

// ---------------- feedback ----------------

function setFeedback(text) {
  const f = $('decodeFeedback');
  f.textContent = text;
  f.className = 'feedback';
}

function setFeedbackOk(text) {
  const f = $('decodeFeedback');
  f.textContent = text;
  f.className = 'feedback ok';
}

// ---------------- help chart ----------------

function buildChart() {
  const box = $('morseChart');
  if (box.children.length) return;
  for (const [k, v] of Object.entries(MORSE_CHART)) {
    const s = document.createElement('span');
    const kk = document.createElement('b');
    kk.textContent = k;
    const cc = document.createElement('i');
    cc.className = 'code';
    cc.textContent = v;
    s.appendChild(kk);
    s.appendChild(cc);
    box.appendChild(s);
  }
}