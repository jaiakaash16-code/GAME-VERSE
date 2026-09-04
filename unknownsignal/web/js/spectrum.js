'use strict';

/* Waterfall spectrum + oscilloscope renderers. */
const Spectrum = {
  wf: null,
  sc: null,
  wctx: null,
  sctx: null,
  W: 320,
  H: 170,
  rows: [],
  img: null,
  flashUntil: 0,
  minF: 80,
  maxF: 120,

  init() {
    this.wf = document.getElementById('waterfall');
    this.sc = document.getElementById('scope');
    const parent = this.wf.parentElement;
    this.W = Math.max(280, parent.clientWidth - 26);
    this.H = 170;
    this.wf.width = this.W;
    this.wf.height = this.H;
    this.sc.width = this.W;
    this.sc.height = 84;
    this.wctx = this.wf.getContext('2d');
    this.sctx = this.sc.getContext('2d');
    this.img = this.wctx.createImageData(this.W, this.H);
    this.rows = [];
    for (let i = 0; i < this.H; i++) this.rows.push(new Float32Array(this.W));
  },

  flash() {
    this.flashUntil = performance.now() + 320;
  },

  frame(t, bands, freq, strength, locked) {
    const W = this.W, H = this.H;
    const row = new Float32Array(W);
    const now = performance.now();
    const flashing = now < this.flashUntil;
    for (let x = 0; x < W; x++) {
      const f = this.minF + (x / W) * (this.maxF - this.minF);
      let v = 0.028 + Math.random() * 0.05;
      for (const b of bands) {
        const d = (f - b.freq) / 0.9;
        v += 0.55 * Math.exp(-d * d * 1.6);
        if (flashing) v += 0.5 * Math.exp(-d * d * 3);
      }
      const cur = (freq - f) / 0.5;
      v += 0.16 * Math.exp(-cur * cur * 6);
      row[x] = Math.min(1, v);
    }
    this.rows.pop();
    this.rows.unshift(row);

    const data = this.img.data;
    for (let y = 0; y < H; y++) {
      const r = this.rows[y];
      const fade = 1 - (y / H) * 0.55;
      for (let x = 0; x < W; x++) {
        const a = r[x] * fade;
        const i = (y * W + x) * 4;
        data[i] = 0;
        data[i + 1] = Math.min(255, a * 255);
        data[i + 2] = Math.min(255, a * 90);
        data[i + 3] = 255;
      }
    }
    this.wctx.putImageData(this.img, 0, 0);

    // oscilloscope
    const sctx = this.sctx, sw = this.sc.width, sh = this.sc.height;
    sctx.fillStyle = 'rgba(0,10,4,0.16)';
    sctx.fillRect(0, 0, sw, sh);
    sctx.strokeStyle = 'rgba(120,255,160,0.9)';
    sctx.lineWidth = 1.2;
    sctx.beginPath();
    const amp = strength || 0.06;
    for (let x = 0; x < sw; x += 2) {
      const y = sh / 2
        + Math.sin(x * 0.12 + t * 0.006 + Math.sin(t * 0.0013) * 3) * sh * 0.32 * amp
        + (Math.random() - 0.5) * 6 * (1 - amp);
      if (x === 0) sctx.moveTo(x, y);
      else sctx.lineTo(x, y);
    }
    sctx.stroke();
    if (locked) {
      sctx.fillStyle = 'rgba(140,255,170,0.45)';
      sctx.fillRect(sw - 26, 4, 22, 8);
    }
  }
};