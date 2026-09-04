'use strict';

/* Tuning dial, filter slider, and antenna compass. */
const Radio = {
  minF: 80,
  maxF: 120,
  freq: 100.0,
  bw: 3.0,
  angle: 0,
  onFreq: null,
  onBw: null,
  onAngle: null,

  init() {
    const dial = document.getElementById('dial');
    const knob = document.getElementById('knob');
    let dragging = false;
    let lastY = 0;

    dial.addEventListener('pointerdown', e => {
      dragging = true;
      lastY = e.clientY;
      dial.setPointerCapture(e.pointerId);
      MorseAudio.click();
    });
    dial.addEventListener('pointermove', e => {
      if (!dragging) return;
      const dy = lastY - e.clientY;
      lastY = e.clientY;
      this.nudgeFreq(dy * 0.5);
    });
    dial.addEventListener('pointerup', () => { dragging = false; });
    dial.addEventListener('pointercancel', () => { dragging = false; });
    // safety net: never leave the dial stuck in a drag even if a pointerup is missed
    window.addEventListener('pointerup', () => { dragging = false; });
    window.addEventListener('pointercancel', () => { dragging = false; });

    window.addEventListener('keydown', e => {
      if (e.target.tagName === 'INPUT') return;
      if (e.key === 'ArrowRight' || e.key === 'ArrowUp') { e.preventDefault(); this.nudgeFreq(0.1); }
      else if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') { e.preventDefault(); this.nudgeFreq(-0.1); }
    });

    const freqInput = document.getElementById('freqInput');
    freqInput.addEventListener('keydown', e => {
      if (e.key === 'Enter') {
        const v = parseFloat(freqInput.value);
        if (!isNaN(v)) {
          this.setFreq(v);
          freqInput.value = '';
          freqInput.blur();
        }
      }
    });

    const bwSlider = document.getElementById('bwSlider');
    bwSlider.addEventListener('input', e => this.setBw(parseFloat(e.target.value)));
    document.getElementById('bwMinus').addEventListener('click', () => this.setBw(this.bw - 0.2));
    document.getElementById('bwPlus').addEventListener('click', () => this.setBw(this.bw + 0.2));

    // compass / antenna
    const compass = document.getElementById('compass');
    const needle = document.getElementById('needle');
    let cDragging = false;
    compass.addEventListener('pointerdown', e => {
      cDragging = true;
      compass.setPointerCapture(e.pointerId);
      this.updateAngleFromEvent(e);
    });
    compass.addEventListener('pointermove', e => {
      if (cDragging) this.updateAngleFromEvent(e);
    });
    compass.addEventListener('pointerup', () => { cDragging = false; });
    compass.addEventListener('pointercancel', () => { cDragging = false; });
    window.addEventListener('pointerup', () => { cDragging = false; });
    window.addEventListener('pointercancel', () => { cDragging = false; });
    this.needle = needle;
  },

  updateAngleFromEvent(e) {
    const rect = document.getElementById('compass').getBoundingClientRect();
    const cx = rect.left + rect.width / 2;
    const cy = rect.top + rect.height / 2;
    const dx = e.clientX - cx;
    const dy = e.clientY - cy;
    let deg = Math.atan2(dx, -dy) * 180 / Math.PI;
    if (deg < 0) deg += 360;
    this.setAngle(deg);
  },

  setFreq(f) {
    this.freq = Math.round(Math.min(this.maxF, Math.max(this.minF, f)) * 10) / 10;
    this.update();
  },

  nudgeFreq(d) { this.setFreq(this.freq + d); },

  setBw(b) {
    this.bw = Math.round(Math.min(6, Math.max(0.5, b)) * 10) / 10;
    this.update();
  },

  setAngle(a) {
    this.angle = ((a % 360) + 360) % 360;
    if (this.needle) this.needle.style.transform = `rotate(${this.angle}deg)`;
    if (this.onAngle) this.onAngle(this.angle);
  },

  update() {
    document.getElementById('freqDisplay').textContent = this.freq.toFixed(1);
    document.getElementById('bwValue').textContent = this.bw.toFixed(1);
    const pct = (this.freq - this.minF) / (this.maxF - this.minF);
    document.getElementById('knob').style.transform = `rotate(${-135 + pct * 270}deg)`;
    if (this.onFreq) this.onFreq(this.freq);
    if (this.onBw) this.onBw(this.bw);
  }
};