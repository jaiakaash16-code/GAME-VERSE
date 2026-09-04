'use strict';

/* Triangulation map modal. */
const MapView = {
  bearings: [],
  marked: false,
  lastMark: null,
  onMark: null,
  canvas: null,

  open(bearings, marked, lastMark, onMark) {
    this.bearings = bearings || [];
    this.marked = marked;
    this.lastMark = lastMark;
    this.onMark = onMark;
    const wrap = document.getElementById('mapWrap');
    wrap.hidden = false;
    this.canvas = document.getElementById('mapCanvas');
    const S = Math.min(360, Math.max(280, window.innerWidth - 80));
    this.canvas.width = S;
    this.canvas.height = S;
    this.draw();
    this.canvas.onclick = e => {
      if (!this.onMark) return;
      const r = this.canvas.getBoundingClientRect();
      const x = (e.clientX - r.left) / S;
      const y = (e.clientY - r.top) / S;
      this.lastMark = { x, y };
      this.onMark(x, y);
      this.draw();
    };
    document.getElementById('mapClose').onclick = () => this.close();
  },

  close() {
    document.getElementById('mapWrap').hidden = true;
    this.onMark = null;
  },

  draw() {
    const c = this.canvas.getContext('2d');
    const S = this.canvas.width;
    c.fillStyle = '#02100a';
    c.fillRect(0, 0, S, S);

    // grid
    c.strokeStyle = 'rgba(60,140,90,0.22)';
    c.lineWidth = 1;
    for (let i = 1; i < 8; i++) {
      const p = i / 8 * S;
      c.beginPath(); c.moveTo(p, 0); c.lineTo(p, S); c.stroke();
      c.beginPath(); c.moveTo(0, p); c.lineTo(S, p); c.stroke();
    }

    // station
    c.fillStyle = '#9dffb8';
    c.beginPath(); c.arc(S / 2, S / 2, 4, 0, Math.PI * 2); c.fill();
    c.fillStyle = 'rgba(120,255,160,0.5)';
    c.font = '11px monospace';
    c.textAlign = 'center';
    c.fillText('STATION 7', S / 2, S / 2 + 18);

    // compass ring
    c.strokeStyle = 'rgba(160,255,190,0.45)';
    c.beginPath(); c.arc(S / 2, S / 2, S * 0.42, 0, Math.PI * 2); c.stroke();
    c.fillStyle = '#9dffb8';
    c.fillText('N', S / 2, S / 2 - S * 0.42 - 5);

    // bearing lines
    c.strokeStyle = 'rgba(120,255,160,0.85)';
    c.lineWidth = 1.5;
    for (const a of this.bearings) {
      const rad = a * Math.PI / 180;
      const ex = S / 2 + Math.sin(rad) * S * 0.42;
      const ey = S / 2 - Math.cos(rad) * S * 0.42;
      c.beginPath(); c.moveTo(S / 2, S / 2); c.lineTo(ex, ey); c.stroke();
    }

    // marked source
    if (this.lastMark) {
      const px = this.lastMark.x * S, py = this.lastMark.y * S;
      c.fillStyle = '#ffd24d';
      c.beginPath(); c.arc(px, py, 6, 0, Math.PI * 2); c.fill();
      c.strokeStyle = 'rgba(255,210,77,0.7)';
      c.beginPath(); c.arc(px, py, 12, 0, Math.PI * 2); c.stroke();
    }

    c.fillStyle = '#6fbf8d';
    c.font = '10px monospace';
    c.textAlign = 'left';
    c.fillText('CLICK TO MARK THE SOURCE', 10, S - 12);
  }
};