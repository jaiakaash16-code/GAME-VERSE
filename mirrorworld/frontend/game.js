/* =====================================================================
 * MIRROR WORLD — Game Engine
 * Frontend: HTML + CSS + Canvas + JavaScript
 * Talks to: Java backend at the same origin
 * ===================================================================== */

(() => {
  'use strict';

  // ---------------- API ----------------
  const API = {
    levels: () => fetch('/api/levels').then(r => r.json()),
    level: id => fetch('/api/level/' + id).then(r => r.json()),
    save: body => fetch('/api/save', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}).then(r=>r.json()),
    load: pid => fetch('/api/load/' + encodeURIComponent(pid)).then(r=>r.json()),
    score: body => fetch('/api/score', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}).then(r=>r.json()),
    leaderboard: () => fetch('/api/leaderboard').then(r=>r.json()),
    stats: () => fetch('/api/stats').then(r=>r.json()),
  };

  const $ = id => document.getElementById(id);
  const playerId = () => ($('playerId').value || 'guest').trim();

  // ---------------- Constants ----------------
  const TILE = 32;
  const GRAVITY = 0.8;
  const MOVE_ACCEL = 0.9;
  const MAX_VX = 4.2;
  const MAX_VY = 12;
  const JUMP_V = -13;
  const FLIP_COOLDOWN_MS = 350;

  // Tile codes (single shared map — same layout, two interpretations)
  const T = {
    EMPTY: '.', SOLID: '#',
    SPAWN_A: 'S', SPAWN_B: 'T',
    GOAL: 'G',
    BTN_NORMAL: 'A', BTN_MIRROR: 'B',
    DOOR_NORMAL: 'D', DOOR_MIRROR: 'F',
    HAZARD_A: '1', HAZARD_B: '2',
  };

  // ---------------- Game state ----------------
  const State = {
    view: 'play',
    level: null,
    rows: [],
    width: 0, height: 0,
    // A single shared map, but obstacles interpreted per world:
    doors: {},   // 'x,y' -> {open:bool, side:'A'|'B'}
    buttons: {}, // 'x,y' -> {pressed:bool, side:'A'|'B'}
    hazards: {}, // 'x,y' -> 'A' or 'B' which world it can kill in
    player: { x:0, y:0, vx:0, vy:0, w:24, h:30, onGround:false, world:'A' },
    spawnA: null, spawnB: null,
    startedAt: 0,
    deaths: 0,
    flips: 0,
    lastFlip: 0,
    won: false,
    raf: 0,
    keys: {},
    particles: [],
    shake: 0,
    camX: 0, camY: 0,
  };

  // ---------------- Routing ----------------
  function setView(v){
    State.view = v;
    document.querySelectorAll('.view').forEach(el => el.classList.add('hidden'));
    $('view-' + v).classList.remove('hidden');
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.route === v));
    if (v === 'levels') refreshLevelList();
    if (v === 'score') refreshLeaderboard();
    if (v === 'home') refreshHomeStats();
    if (v === 'play') startLoop();
  }

  document.querySelectorAll('.nav-btn').forEach(b => {
    b.addEventListener('click', () => setView(b.dataset.route));
  });

  // Home page interactions
  async function refreshHomeStats(){
    try {
      const s = await API.stats();
      $('statLevels').textContent = s.levels;
      $('statRuns').textContent = s.runs;
      $('statBest').textContent = s.bestScore > 0 ? s.bestScore.toLocaleString() : '—';
    } catch(e) { /* stats optional */ }
  }

  function wireHome(){
    const goPlay = () => setView('play');
    const goLevels = () => setView('levels');
    const goHow = () => setView('how');
    const goScore = () => setView('score');
    const goSettings = () => {
      const id = prompt('Enter your Player ID:', playerId() || '');
      if (id !== null) {
        $('playerId').value = id.trim();
        localStorage.setItem('mw_player', $('playerId').value);
        refreshHomeStats();
      }
    };

    ['btnPlayNow','btnPlayNow2'].forEach(id => {
      const el = $(id); if (el) el.addEventListener('click', goPlay);
    });
    const lvl = $('btnLevels'); if (lvl) lvl.addEventListener('click', goLevels);
    const how = $('btnHow'); if (how) how.addEventListener('click', goHow);
    const set = $('btnSettings'); if (set) set.addEventListener('click', goSettings);

    document.querySelectorAll('[data-route]').forEach(el => {
      if (el.classList.contains('nav-btn')) return; // already wired
      el.addEventListener('click', () => setView(el.dataset.route));
    });
  }

  // ---------------- Levels ----------------
  async function refreshLevelList(){
    const list = await API.levels();
    const saves = await API.load(playerId()).catch(() => ({saves:[]}));
    const prog = {};
    saves.saves.forEach(s => prog[s.levelId] = s.progress);
    const root = $('levelsList');
    root.innerHTML = '';
    list.forEach(lvl => {
      const card = document.createElement('div');
      card.className = 'level-card';
      const p = prog[lvl.id] || 0;
      card.innerHTML = `<div class="num">LEVEL ${lvl.order}</div>
        <h3>${lvl.name}</h3>
        <div class="prog"><span style="width:${p}%"></span></div>`;
      card.addEventListener('click', () => { loadLevel(lvl.id); setView('play'); });
      root.appendChild(card);
    });
  }

  async function loadLevel(id){
    const data = await API.level(id);
    State.level = data;
    State.width = data.width;
    State.height = data.height;
    State.rows = data.rows.split('|');
    parseMap();
    resetPlayer();
    $('levelLabel').textContent = data.name;
    $('hintText').textContent = data.hint || '';
    State.startedAt = performance.now();
    State.deaths = 0; State.flips = 0; State.won = false;
    updateHud();
    API.save({ playerId: playerId(), levelId: id, progress: 5 });
  }

  function parseMap(){
    State.doors = {}; State.buttons = {}; State.hazards = {};
    State.spawnA = State.spawnB = null;
    for (let y = 0; y < State.rows.length; y++){
      const row = State.rows[y];
      for (let x = 0; x < row.length; x++){
        const c = row[x];
        const k = x + ',' + y;
        if (c === T.SPAWN_A) State.spawnA = {x, y};
        if (c === T.SPAWN_B) State.spawnB = {x, y};
        if (c === T.DOOR_NORMAL) State.doors[k] = {open:false, side:'A'};
        if (c === T.DOOR_MIRROR) State.doors[k] = {open:false, side:'B'};
        if (c === T.BTN_NORMAL) State.buttons[k] = {pressed:false, side:'A'};
        if (c === T.BTN_MIRROR) State.buttons[k] = {pressed:false, side:'B'};
        if (c === T.HAZARD_A) State.hazards[k] = 'A';
        if (c === T.HAZARD_B) State.hazards[k] = 'B';
      }
    }
  }

  function resetPlayer(){
    const sp = State.player.world === 'B' ? (State.spawnB||State.spawnA) : (State.spawnA||State.spawnB);
    State.player.x = sp.x * TILE + (TILE - State.player.w)/2;
    State.player.y = sp.y * TILE + (TILE - State.player.h);
    State.player.vx = 0; State.player.vy = 0;
    State.player.onGround = false;
  }

  // ---------------- Input handling ----------------
  function tryJump(){
    if (State.player.onGround && !State.won){
      State.player.vy = JUMP_V;
      State.player.onGround = false;
    }
  }

  window.addEventListener('keydown', e => {
    State.keys[e.code] = true;
    // SPACE and ArrowUp = jump
    if (e.code === 'Space' || e.code === 'ArrowUp' || e.code === 'KeyJ' || e.code === 'KeyZ'){
      if (!e.repeat) tryJump();
      e.preventDefault();
    }
    // W = flip to mirror world; S = flip back to normal
    if (e.code === 'KeyW'){ if (!e.repeat) tryFlip('up'); e.preventDefault(); }
    if (e.code === 'KeyS' || e.code === 'ArrowDown'){ if (!e.repeat) tryFlip('down'); e.preventDefault(); }
    if (e.code === 'KeyR'){ restartState(); }
  });
  window.addEventListener('keyup', e => { State.keys[e.code] = false; });

  function tryFlip(dir){
    if (!State.level || State.won) return;
    const now = performance.now();
    if (now - State.lastFlip < FLIP_COOLDOWN_MS) return;
    State.lastFlip = now;
    State.player.world = State.player.world === 'A' ? 'B' : 'A';
    State.flips++;
    State.player.vy = 0;
    resetPlayer();
    flashFlip(State.player.world);
    spawnFlipParticles();
    State.shake = 6;
    updateHud();
  }

  $('btnRestart').addEventListener('click', restartState);
  $('btnMenu').addEventListener('click', () => setView('levels'));

  function restartState(){
    if (!State.level) return;
    resetPlayer();
    Object.values(State.buttons).forEach(b => b.pressed = false);
    Object.values(State.doors).forEach(d => d.open = false);
    State.deaths = 0; State.flips = 0; State.won = false;
    updateHud();
  }

  // ---------------- Update loop ----------------
  function startLoop(){
    cancelAnimationFrame(State.raf);
    const loop = () => {
      update();
      render();
      State.raf = requestAnimationFrame(loop);
    };
    State.raf = requestAnimationFrame(loop);
  }

  function update(){
    if (!State.level || State.won) return;

    const p = State.player;
    let ax = 0;
    if (State.keys['ArrowLeft'] || State.keys['KeyA']) ax -= 1;
    if (State.keys['ArrowRight'] || State.keys['KeyD']) ax += 1;

    p.vx += ax * MOVE_ACCEL;
    p.vx *= 0.82;
    if (p.vx > MAX_VX) p.vx = MAX_VX;
    if (p.vx < -MAX_VX) p.vx = -MAX_VX;
    if (ax === 0 && Math.abs(p.vx) < 0.5) p.vx = 0;

    p.vy += GRAVITY;
    if (p.vy > MAX_VY) p.vy = MAX_VY;

    moveAxis('x', p.vx);
    moveAxis('y', p.vy);

    // buttons: any A-side button is pressed by A-side player only, etc.
    checkButtons();

    // hazards
    if (checkHazards()) {
      State.deaths++;
      spawnDeathParticles();
      State.shake = 12;
      resetPlayer();
      updateHud();
    }

    // goal
    if (checkGoal()) {
      State.won = true;
      showWin();
      updateHud();
    }

    // particles
    State.particles.forEach(pt => {
      pt.x += pt.vx; pt.y += pt.vy;
      pt.vy += 0.08;
      pt.life--;
    });
    State.particles = State.particles.filter(pt => pt.life > 0);

    if (State.shake > 0) State.shake *= 0.85;

    // time
    const t = Math.floor((performance.now() - State.startedAt) / 1000);
    $('timeLabel').textContent = String(Math.floor(t/60)).padStart(2,'0') + ':' + String(t%60).padStart(2,'0');
  }

  function moveAxis(axis, v){
    const p = State.player;
    if (axis === 'x'){
      p.x += v;
      resolveCollisions('x');
    } else {
      p.y += v;
      resolveCollisions('y');
    }
  }

  function tileAt(x, y){
    if (x < 0 || y < 0 || x >= State.width || y >= State.height) return T.SOLID;
    return State.rows[y][x];
  }

  function isSolid(x, y){
    const c = tileAt(x, y);
    if (c === T.SOLID) return true;
    // closed door blocks
    if (c === T.DOOR_NORMAL || c === T.DOOR_MIRROR){
      const k = x + ',' + y;
      return !State.doors[k] || !State.doors[k].open;
    }
    return false;
  }

  function resolveCollisions(axis){
    const p = State.player;
    const left = Math.floor(p.x / TILE);
    const right = Math.floor((p.x + p.w - 1) / TILE);
    const top = Math.floor(p.y / TILE);
    const bottom = Math.floor((p.y + p.h - 1) / TILE);

    for (let ty = top; ty <= bottom; ty++){
      for (let tx = left; tx <= right; tx++){
        if (isSolid(tx, ty)){
          if (axis === 'x'){
            if (p.vx > 0) p.x = tx * TILE - p.w - 0.01;
            else if (p.vx < 0) p.x = (tx + 1) * TILE + 0.01;
            p.vx = 0;
          } else {
            if (p.vy > 0){ p.y = ty * TILE - p.h - 0.01; p.onGround = true; }
            else if (p.vy < 0){ p.y = (ty + 1) * TILE + 0.01; }
            p.vy = 0;
          }
        }
      }
    }
    p.onGround = p.onGround || (axis === 'y' && p.vy === 0);
  }

  function checkButtons(){
    const p = State.player;
    const left = Math.floor((p.x + 4) / TILE);
    const right = Math.floor((p.x + p.w - 4) / TILE);
    const top = Math.floor((p.y + 4) / TILE);
    const bottom = Math.floor((p.y + p.h - 4) / TILE);

    // Latching: button stays pressed once player has stood on it.
    // But only if player is in the world the button belongs to.
    Object.keys(State.buttons).forEach(k => {
      const [bx, by] = k.split(',').map(Number);
      const btn = State.buttons[k];
      const standingOn = (bx >= left && bx <= right && by >= top && by <= bottom);
      if (standingOn && btn.side === State.player.world) btn.pressed = true;
    });

    // Open doors based on combined button states.
    Object.keys(State.doors).forEach(k => {
      const [dx, dy] = k.split(',').map(Number);
      const door = State.doors[k];
      let open = false;
      Object.keys(State.buttons).forEach(bk => {
        const btn = State.buttons[bk];
        if (btn.pressed && btn.side === door.side) open = true;
      });
      door.open = open;
    });
  }

  function checkHazards(){
    const p = State.player;
    const cx = Math.floor((p.x + p.w/2) / TILE);
    const cy = Math.floor((p.y + p.h/2) / TILE);
    const k = cx + ',' + cy;
    const side = State.hazards[k];
    return side === State.player.world;
  }

  function checkGoal(){
    const p = State.player;
    const cx = Math.floor((p.x + p.w/2) / TILE);
    const cy = Math.floor((p.y + p.h/2) / TILE);
    return tileAt(cx, cy) === T.GOAL;
  }

  // ---------------- Render ----------------
  const canvas = $('game');
  const ctx = canvas.getContext('2d');

  function render(){
    const W = canvas.width, H = canvas.height;
    ctx.clearRect(0,0,W,H);

    // world background tint
    const tintA = 'rgba(78,201,255,0.06)';
    const tintB = 'rgba(255,122,217,0.08)';
    ctx.fillStyle = State.player.world === 'A' ? tintA : tintB;
    ctx.fillRect(0,0,W,H);

    // -------- WORLD SEPARATOR BAR --------
    // A neon stripe down the middle of the canvas splits Normal (top) from Mirror (bottom).
    // Tinted to match the CURRENT world. Pulses gently so it always reads as "active world."
    const sepX = W / 2;
    const sepPulse = 0.5 + 0.5 * Math.sin(performance.now() / 600);
    const colTop = State.player.world === 'A' ? 'rgba(78,201,255,' : 'rgba(255,122,217,';
    // soft glow
    ctx.save();
    const grad = ctx.createLinearGradient(sepX - 32, 0, sepX + 32, 0);
    grad.addColorStop(0, 'rgba(0,0,0,0)');
    grad.addColorStop(0.5, colTop + (0.25 + 0.15 * sepPulse) + ')');
    grad.addColorStop(1, 'rgba(0,0,0,0)');
    ctx.fillStyle = grad;
    ctx.fillRect(sepX - 32, 0, 64, H);
    // hard core line
    ctx.fillStyle = colTop + (0.7 + 0.3 * sepPulse) + ')';
    ctx.fillRect(sepX - 1, 0, 2, H);
    // tiny diamond markers
    for (let y = 30; y < H; y += 90){
      ctx.fillStyle = colTop + '0.6)';
      ctx.beginPath();
      ctx.moveTo(sepX, y);
      ctx.lineTo(sepX + 5, y + 5);
      ctx.lineTo(sepX, y + 10);
      ctx.lineTo(sepX - 5, y + 5);
      ctx.closePath();
      ctx.fill();
    }
    // labels
    ctx.font = 'bold 10px monospace';
    ctx.fillStyle = colTop + '0.7)';
    ctx.textAlign = 'center';
    ctx.fillText('◀ NORMAL', sepX - 12, 16);
    ctx.fillText('MIRROR ▶', sepX + 12, 16);
    ctx.restore();
    // -------- /SEPARATOR --------

    // top tint stripe (subtle)
    ctx.save();
    ctx.globalAlpha = 0.18;
    ctx.fillStyle = State.player.world === 'A' ? '#4ec9ff' : '#ff7ad9';
    ctx.fillRect(0, 0, W, 4);
    ctx.restore();

    // shake
    ctx.save();
    if (State.shake > 0.2){
      ctx.translate((Math.random()-0.5)*State.shake, (Math.random()-0.5)*State.shake);
    }

    // tile rendering
    const ox = (W - State.width * TILE) / 2;
    const oy = (H - State.height * TILE) / 2;
    for (let y = 0; y < State.height; y++){
      for (let x = 0; x < State.width; x++){
        const c = State.rows[y][x];
        drawTile(c, x, y, ox, oy);
      }
    }

    // doors (animated)
    Object.keys(State.doors).forEach(k => {
      const [x,y] = k.split(',').map(Number);
      const d = State.doors[k];
      drawDoor(x, y, ox, oy, d.open, d.side);
    });

    // buttons pulse
    Object.keys(State.buttons).forEach(k => {
      const [x,y] = k.split(',').map(Number);
      const b = State.buttons[k];
      drawButton(x, y, ox, oy, b.pressed, b.side);
    });

    // hazards flicker
    Object.keys(State.hazards).forEach(k => {
      const [x,y] = k.split(',').map(Number);
      const side = State.hazards[k];
      drawHazard(x, y, ox, oy, side);
    });

    // goal pulse
    for (let y = 0; y < State.height; y++){
      for (let x = 0; x < State.width; x++){
        if (State.rows[y][x] === T.GOAL) drawGoal(x, y, ox, oy);
      }
    }

    // particles
    State.particles.forEach(pt => {
      ctx.fillStyle = pt.color;
      ctx.globalAlpha = pt.life / pt.maxLife;
      ctx.fillRect(pt.x + ox, pt.y + oy, pt.size, pt.size);
    });
    ctx.globalAlpha = 1;

    // player
    drawPlayer(ox, oy);

    ctx.restore();
  }

  function drawTile(c, x, y, ox, oy){
    const px = x*TILE + ox, py = y*TILE + oy;
    if (c === T.SOLID){
      // Brick
      ctx.fillStyle = '#1d1d2e';
      ctx.fillRect(px, py, TILE, TILE);
      ctx.strokeStyle = 'rgba(155,140,255,0.25)';
      ctx.lineWidth = 1;
      ctx.strokeRect(px+0.5, py+0.5, TILE-1, TILE-1);
      // highlight
      ctx.fillStyle = 'rgba(155,140,255,0.10)';
      ctx.fillRect(px, py, TILE, 3);
      ctx.fillRect(px, py, 3, TILE);
    } else if (c === T.EMPTY || c === T.SPAWN_A || c === T.SPAWN_B || c === T.GOAL){
      // dot grid
      ctx.fillStyle = 'rgba(255,255,255,0.04)';
      ctx.fillRect(px + TILE/2 - 1, py + TILE/2 - 1, 2, 2);
    }
  }

  function drawDoor(x, y, ox, oy, open, side){
    const px = x*TILE + ox, py = y*TILE + oy;
    const color = side === 'A' ? '#4ec9ff' : '#ff7ad9';
    if (open){
      ctx.fillStyle = 'rgba(0,0,0,0.5)';
      ctx.fillRect(px+4, py+TILE-6, TILE-8, 2);
      ctx.fillStyle = color;
      ctx.fillRect(px+TILE/2-3, py+TILE-8, 6, 2);
    } else {
      ctx.fillStyle = '#0d0d18';
      ctx.fillRect(px, py, TILE, TILE);
      ctx.strokeStyle = color;
      ctx.lineWidth = 2;
      ctx.strokeRect(px+2, py+2, TILE-4, TILE-4);
      // inner lock
      ctx.fillStyle = color;
      ctx.fillRect(px+TILE/2-4, py+TILE/2-4, 8, 8);
    }
  }

  function drawButton(x, y, ox, oy, pressed, side){
    const px = x*TILE + ox, py = y*TILE + oy;
    const color = side === 'A' ? '#4ec9ff' : '#ff7ad9';
    const t = performance.now() / 200;
    const pulse = pressed ? 1 : (0.5 + 0.5*Math.sin(t + x + y));
    ctx.fillStyle = pressed ? color : 'rgba(0,0,0,0.5)';
    ctx.beginPath();
    ctx.arc(px + TILE/2, py + TILE/2, 6 + pulse*2, 0, Math.PI*2);
    ctx.fill();
    if (pressed){
      ctx.fillStyle = '#fff';
      ctx.beginPath();
      ctx.arc(px + TILE/2, py + TILE/2, 2, 0, Math.PI*2);
      ctx.fill();
    }
  }

  function drawHazard(x, y, ox, oy, side){
    const px = x*TILE + ox, py = y*TILE + oy;
    const color = side === 'A' ? '#4ec9ff' : '#ff7ad9';
    const t = performance.now() / 100;
    ctx.fillStyle = color;
    const h = 12 + Math.sin(t + x - y)*2;
    // spikes
    for (let i = 0; i < 4; i++){
      const sx = px + i*8 + 4;
      ctx.beginPath();
      ctx.moveTo(sx - 4, py + TILE);
      ctx.lineTo(sx, py + TILE - h);
      ctx.lineTo(sx + 4, py + TILE);
      ctx.closePath();
      ctx.fill();
    }
  }

  function drawGoal(x, y, ox, oy){
    const px = x*TILE + ox, py = y*TILE + oy;
    const t = performance.now() / 300;
    const r = 12 + Math.sin(t)*2;
    const grd = ctx.createRadialGradient(px+TILE/2, py+TILE/2, 2, px+TILE/2, py+TILE/2, r);
    grd.addColorStop(0, 'rgba(255,214,107,1)');
    grd.addColorStop(1, 'rgba(255,214,107,0)');
    ctx.fillStyle = grd;
    ctx.fillRect(px-8, py-8, TILE+16, TILE+16);
    ctx.fillStyle = '#ffd66b';
    ctx.beginPath();
    ctx.arc(px+TILE/2, py+TILE/2, 4, 0, Math.PI*2);
    ctx.fill();
  }

  function drawPlayer(ox, oy){
    const p = State.player;
    const px = p.x + ox, py = p.y + oy;
    const color = p.world === 'A' ? '#4ec9ff' : '#ff7ad9';
    const glow = p.world === 'A' ? 'rgba(78,201,255,0.5)' : 'rgba(255,122,217,0.5)';

    // shadow
    ctx.fillStyle = 'rgba(0,0,0,0.4)';
    ctx.beginPath();
    ctx.ellipse(px + p.w/2, py + p.h + 2, p.w/2, 3, 0, 0, Math.PI*2);
    ctx.fill();

    // body
    ctx.fillStyle = color;
    ctx.fillRect(px + 2, py + 4, p.w - 4, p.h - 4);

    // outline
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 2;
    ctx.strokeRect(px + 2, py + 4, p.w - 4, p.h - 4);

    // glow
    ctx.shadowColor = color;
    ctx.shadowBlur = 16;
    ctx.fillStyle = glow;
    ctx.fillRect(px + 2, py + 4, p.w - 4, p.h - 4);
    ctx.shadowBlur = 0;

    // eyes
    ctx.fillStyle = '#fff';
    const eyeY = py + 10;
    const look = p.vx > 0.05 ? 2 : (p.vx < -0.05 ? -2 : 0);
    ctx.fillRect(px + 7 + look, eyeY, 2, 3);
    ctx.fillRect(px + 15 + look, eyeY, 2, 3);

    // mirror marker (small arrow)
    ctx.fillStyle = '#fff';
    const arrow = p.world === 'A' ? '▲' : '▼';
    ctx.font = 'bold 10px monospace';
    ctx.fillText(arrow, px + p.w/2 - 4, py - 2);
  }

  // ---------------- Effects ----------------
  function flashFlip(world){
    const f = $('flip-flash');
    f.style.background = world === 'A'
      ? 'radial-gradient(circle, rgba(78,201,255,0.7), transparent)'
      : 'radial-gradient(circle, rgba(255,122,217,0.7), transparent)';
    f.classList.remove('active'); void f.offsetWidth; f.classList.add('active');
    setTimeout(() => f.classList.remove('active'), 600);
  }

  function spawnFlipParticles(){
    const p = State.player;
    const color = State.player.world === 'A' ? '#4ec9ff' : '#ff7ad9';
    for (let i = 0; i < 24; i++){
      const a = Math.random()*Math.PI*2;
      const s = Math.random()*3 + 1;
      State.particles.push({
        x: p.x + p.w/2 - 2,
        y: p.y + p.h/2 - 2,
        vx: Math.cos(a)*s*0.4, vy: Math.sin(a)*s*0.4,
        size: 3, life: 40, maxLife: 40, color
      });
    }
  }

  function spawnDeathParticles(){
    const p = State.player;
    for (let i = 0; i < 36; i++){
      const a = Math.random()*Math.PI*2;
      const s = Math.random()*4 + 1;
      State.particles.push({
        x: p.x + p.w/2 - 2,
        y: p.y + p.h/2 - 2,
        vx: Math.cos(a)*s*0.4, vy: Math.sin(a)*s*0.4,
        size: 3, life: 50, maxLife: 50, color: '#ff6b6b'
      });
    }
  }

  // ---------------- Win / Messages ----------------
  function showWin(){
    const elapsed = Math.floor(performance.now() - State.startedAt);
    const msg = $('message');
    msg.classList.remove('hidden','die');
    msg.classList.add('win');
    msg.innerHTML = `<h2 style="margin:0 0 8px;color:#ffd66b">✦ LEVEL CLEAR ✦</h2>
      <p>Time: ${(elapsed/1000).toFixed(1)}s · Deaths: ${State.deaths} · Flips: ${State.flips}</p>
      <p style="margin-top:8px;color:var(--ink-dim)">Submit your score?</p>
      <button id="submitScore" style="margin-top:10px">SUBMIT SCORE</button>
      <button id="nextLevel" style="margin-top:10px;margin-left:6px">NEXT LEVEL</button>`;
    msg.querySelector('#submitScore').addEventListener('click', async () => {
      const r = await API.score({
        playerId: playerId(),
        levelId: State.level.id,
        timeMs: elapsed,
        deaths: State.deaths,
        switches: State.flips
      });
      msg.querySelector('#submitScore').textContent = 'SUBMITTED (' + r.score + ' pts)';
    });
    msg.querySelector('#nextLevel').addEventListener('click', async () => {
      const all = await API.levels();
      const idx = all.findIndex(l => l.id === State.level.id);
      if (idx >= 0 && idx + 1 < all.length){
        loadLevel(all[idx+1].id);
      } else {
        msg.classList.add('hidden');
      }
    });
    API.save({ playerId: playerId(), levelId: State.level.id, progress: 100 });
  }

  // ---------------- HUD ----------------
  function updateHud(){
    $('worldLabel').textContent = State.player.world === 'A' ? 'NORMAL' : 'MIRROR';
    $('worldLabel').className = 'hud-value ' + (State.player.world === 'A' ? 'world-a' : 'world-b');
    $('deathLabel').textContent = State.deaths;
    $('flipLabel').textContent = State.flips;
  }

  // ---------------- Leaderboard ----------------
  async function refreshLeaderboard(){
    const rows = await API.leaderboard();
    const tbody = document.querySelector('#leaderboard tbody');
    tbody.innerHTML = '';
    rows.forEach((r, i) => {
      const tr = document.createElement('tr');
      const t = (r.timeMs/1000).toFixed(1) + 's';
      tr.innerHTML = `<td>${i+1}</td><td>${r.playerId}</td><td>${r.levelId}</td><td>${r.score}</td><td>${t}</td><td>${r.deaths}</td>`;
      tbody.appendChild(tr);
    });
    if (!rows.length){
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:var(--ink-dim)">No scores yet — finish a level!</td></tr>';
    }
  }

  // ---------------- Boot ----------------
  (async function init(){
    $('playerId').value = localStorage.getItem('mw_player') || '';
    $('playerId').addEventListener('change', () => localStorage.setItem('mw_player', $('playerId').value));
    wireHome();
    const levels = await API.levels();
    if (levels.length) loadLevel(levels[0].id);
    setView('home');
  })();

})();