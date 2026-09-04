'use strict';

/* WebAudio-synthesized Morse audio + the client-side Morse chart. */
const MORSE_CHART = {
  A: '.-', B: '-...', C: '-.-.', D: '-..', E: '.', F: '..-.',
  G: '--.', H: '....', I: '..', J: '.---', K: '-.-', L: '.-..',
  M: '--', N: '-.', O: '---', P: '.--.', Q: '--.-', R: '.-.',
  S: '...', T: '-', U: '..-', V: '...-', W: '.--', X: '-..-',
  Y: '-.--', Z: '--..',
  '0': '-----', '1': '.----', '2': '..---', '3': '...--', '4': '....-',
  '5': '.....', '6': '-....', '7': '--...', '8': '---..', '9': '----.'
};

const MorseAudio = {
  ctx: null,
  master: null,
  enabled: true,

  ensure() {
    if (!this.ctx) {
      const AC = window.AudioContext || window.webkitAudioContext;
      if (!AC) return;
      this.ctx = new AC();
      this.master = this.ctx.createGain();
      this.master.gain.value = 0.42;
      this.master.connect(this.ctx.destination);
    }
    if (this.ctx.state === 'suspended') this.ctx.resume();
  },

  toggle() {
    this.enabled = !this.enabled;
    if (this.master) this.master.gain.value = this.enabled ? 0.42 : 0;
    return this.enabled;
  },

  tone(freq, start, dur, vol, type) {
    if (!this.ctx || !this.enabled) return;
    const t0 = this.ctx.currentTime + start;
    const o = this.ctx.createOscillator();
    const g = this.ctx.createGain();
    o.type = type || 'sine';
    o.frequency.value = freq;
    g.gain.setValueAtTime(0, t0);
    g.gain.linearRampToValueAtTime(vol, t0 + 0.008);
    g.gain.setValueAtTime(vol, t0 + Math.max(0.005, dur - 0.02));
    g.gain.linearRampToValueAtTime(0, t0 + dur);
    o.connect(g);
    g.connect(this.master);
    o.start(t0);
    o.stop(t0 + dur + 0.05);
  },

  /* Play a morse pattern; onSymbol(offsetSec, symbol) fires per dot/dash. */
  playPattern(pattern, onSymbol) {
    this.ensure();
    if (!this.ctx) return;
    const unit = 0.09;
    let t = 0.08;
    for (const ch of pattern) {
      if (ch === '.') {
        this.tone(880, t, unit, 0.5);
        if (onSymbol) onSymbol(t, ch);
        t += unit + unit;
      } else if (ch === '-') {
        this.tone(880, t, unit * 3, 0.5);
        if (onSymbol) onSymbol(t, ch);
        t += unit * 3 + unit;
      } else if (ch === ' ') {
        t += unit * 2;
      } else if (ch === '/') {
        t += unit * 4;
      }
    }
  },

  lockChime() { this.tone(660, 0, 0.09, 0.35); this.tone(990, 0.1, 0.12, 0.3); },
  success() { this.tone(523, 0, 0.1, 0.4); this.tone(784, 0.09, 0.14, 0.4); this.tone(1047, 0.18, 0.2, 0.35); },
  deny() { this.tone(180, 0, 0.25, 0.35, 'sawtooth'); },
  transmitBeep() { this.tone(1200, 0, 0.06, 0.3); this.tone(1200, 0.12, 0.06, 0.3); },
  click() { this.tone(2400, 0, 0.015, 0.12, 'square'); },
  presence() { this.tone(55, 0, 1.2, 0.16, 'sine'); this.tone(110, 0.05, 1.2, 0.09, 'sine'); }
};