'use strict';

/* CRT static overlay, screen shake, and burst helpers. */
const Fx = {
  staticLevel: 0.015,
  shakeAmt: 0,
  cv: null,
  ctx: null,
  off: null,
  offctx: null,
  img: null,

  init() {
    this.cv = document.getElementById('snow');
    this.ctx = this.cv.getContext('2d');
    const off = document.createElement('canvas');
    off.width = 220;
    off.height = 140;
    this.off = off;
    this.offctx = off.getContext('2d');
    this.img = this.offctx.createImageData(220, 140);

    const resize = () => {
      this.cv.width = window.innerWidth;
      this.cv.height = window.innerHeight;
    };
    resize();
    window.addEventListener('resize', resize);

    requestAnimationFrame(() => this.frame());

    setInterval(() => {
      if (this.shakeAmt > 0.25) {
        this.shakeAmt *= 0.8;
        const r = () => (Math.random() - 0.5) * 2;
        document.getElementById('crt').style.transform =
          `translate(${r() * this.shakeAmt}px, ${r() * this.shakeAmt}px)`;
      } else if (this.shakeAmt > 0) {
        this.shakeAmt = 0;
        document.getElementById('crt').style.transform = '';
      }
    }, 40);
  },

  frame() {
    this.staticLevel *= 0.985;
    const w = this.off.width, h = this.off.height;
    const data = this.img.data;
    const density = Math.min(1, this.staticLevel * 3.2);
    for (let i = 0; i < w * h; i++) {
      const v = Math.random();
      if (v > 0.55) {
        const b = 120 + Math.floor(Math.random() * 120);
        data[i * 4] = b;
        data[i * 4 + 1] = 255;
        data[i * 4 + 2] = b;
        data[i * 4 + 3] = 255;
      } else {
        data[i * 4] = 0;
        data[i * 4 + 1] = 0;
        data[i * 4 + 2] = 0;
        data[i * 4 + 3] = 0;
      }
    }
    this.offctx.putImageData(this.img, 0, 0);
    this.ctx.clearRect(0, 0, this.cv.width, this.cv.height);
    this.ctx.globalAlpha = density;
    this.ctx.drawImage(this.off, 0, 0, this.cv.width, this.cv.height);
    this.ctx.globalAlpha = 1;
    requestAnimationFrame(() => this.frame());
  },

  staticBurst(n) {
    this.staticLevel = Math.min(0.45, this.staticLevel + n);
  },

  shake(n) {
    this.shakeAmt = Math.max(this.shakeAmt, n);
  }
};