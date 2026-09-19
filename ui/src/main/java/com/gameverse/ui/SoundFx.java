package com.gameverse.ui;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.function.Supplier;

/**
 * Tiny synthesized sound-effects engine for GameVerse.
 *
 * Sounds are generated on the fly (sine/noise sweeps, envelopes) and played
 * through the Java Sound API — no audio files needed. Playback happens on a
 * background thread pool so the game loop never stalls. The mute state is
 * shared across all screens via {@link #setMuted(boolean)}.
 */
public final class SoundFx {

    public enum Sfx {
        MOVE,        // soft wooden "tick" — a piece lands
        CAPTURE,     // lower thunk with a snap
        CASTLE,      // two quick ticks (king + rook)
        CHECK,       // rising alarm ping
        PROMOTE,     // bright ascending arpeggio
        WIN,         // happy major arpeggio
        LOSE,        // descending minor arpeggio
        DRAW,        // neutral two-tone
        UI_CLICK,    // soft click for buttons
        UI_HOVER     // very quiet tick for hovers
    }

    private static volatile boolean muted = false;
    private static final java.util.concurrent.ExecutorService POOL =
        java.util.concurrent.Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "soundfx");
            t.setDaemon(true);
            return t;
        });

    private SoundFx() {}

    /** Mute or unmute all sounds globally. */
    public static void setMuted(boolean m) { muted = m; }

    /** True when sounds are muted. */
    public static boolean isMuted() { return muted; }

    /**
     * Fire a sound effect. Supplies are only evaluated when sound will
     * actually play, and playback is off-thread.
     */
    public static void play(Supplier<byte[]> supplier) {
        if (muted || supplier == null) return;
        byte[] data;
        try {
            data = supplier.get();
        } catch (Throwable t) {
            return; // never let sound break gameplay
        }
        if (data == null || data.length == 0) return;
        POOL.submit(() -> playData(data));
    }

    /** Convenience one-liners for the common game events. */
    public static void move()    { play(SoundFx::moveClip); }
    public static void capture() { play(SoundFx::captureClip); }
    public static void castle()  { play(SoundFx::castleClip); }
    public static void check()   { play(SoundFx::checkClip); }
    public static void promote() { play(SoundFx::promoteClip); }
    public static void win()     { play(SoundFx::winClip); }
    public static void lose()    { play(SoundFx::loseClip); }
    public static void draw()    { play(SoundFx::drawClip); }
    public static void uiClick() { play(SoundFx::uiClickClip); }

    /* ═══════════════ Synthesis ═══════════════ */

    private static final float SAMPLE_RATE = 22050f;

    /** Play raw 16-bit mono PCM through a short-lived output line. */
    private static void playData(byte[] data) {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(new AudioFormat(SAMPLE_RATE, 16, 1, true, false))) {
            line.open(new AudioFormat(SAMPLE_RATE, 16, 1, true, false), 4096);
            line.start();
            line.write(data, 0, data.length);
            line.drain();
        } catch (LineUnavailableException | SecurityException ignored) {
            // no audio hardware — stay silent
        }
    }

    /** Callback that produces one sample (−1..1) for frame i. */
    private interface SampleGen {
        double sample(int i);
    }

    /** Render n samples with a callback, then convert to 16-bit PCM bytes. */
    private static byte[] render(int n, SampleGen gen) {
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double s = Math.max(-1.0, Math.min(1.0, gen.sample(i)));
            short v = (short) (s * Short.MAX_VALUE);
            out[i * 2] = (byte) (v & 0xFF);
            out[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
        }
        return out;
    }

    /** Simple AR envelope: fast attack, exponential decay, optional release. */
    private static double env(int i, int total, double attack, double decay) {
        double a = Math.min(1.0, i / (total * attack));
        double d = Math.exp(-3.0 * i / (total * decay));
        return a * d;
    }

    private interface Mixer {
        void mix(SampleGen gen);
    }

    /** Mix several generators sample-by-sample into one clip. */
    private static byte[] mix(int ms, java.util.function.Consumer<Mixer> spec) {
        int n = (int) (SAMPLE_RATE * ms / 1000.0);
        java.util.List<SampleGen> gens = new java.util.ArrayList<>();
        spec.accept(gens::add);
        return render(n, i -> {
            double sum = 0;
            for (SampleGen g : gens) sum += g.sample(i);
            return sum;
        });
    }

    /* ─────────── Sound recipes ─────────── */

    private static byte[] moveClip() {
        int n = (int) (SAMPLE_RATE * 0.07);
        return render(n, i -> {
            double t = i / SAMPLE_RATE;
            return Math.sin(2 * Math.PI * 340 * t) * 0.35 * env(i, n, 0.02, 0.5);
        });
    }

    private static byte[] captureClip() {
        return mix(140, m -> {
            int n = (int) (SAMPLE_RATE * 0.14);
            m.mix(i -> {
                double t = i / SAMPLE_RATE;
                return Math.sin(2 * Math.PI * 190 * t) * 0.5 * env(i, n, 0.01, 0.6);
            });
            m.mix(i -> {
                double frac = (Math.sin(i * 12.9898) * 43758.5453) % 1.0;
                return frac * 0.3 * Math.exp(-9.0 * i / n);
            });
        });
    }

    private static byte[] castleClip() {
        return mix(200, m -> {
            int n1 = (int) (SAMPLE_RATE * 0.07);
            int n2 = (int) (SAMPLE_RATE * 0.09);
            m.mix(i -> {
                if (i < n1) {
                    double t = i / SAMPLE_RATE;
                    return Math.sin(2 * Math.PI * 300 * t) * 0.3 * env(i, n1, 0.02, 0.5);
                }
                int j = i - n1 / 2;
                if (j >= 0 && j < n2) {
                    double t = j / SAMPLE_RATE;
                    return Math.sin(2 * Math.PI * 360 * t) * 0.3 * env(j, n2, 0.02, 0.5);
                }
                return 0;
            });
        });
    }

    private static byte[] checkClip() {
        int n = (int) (SAMPLE_RATE * 0.18);
        return render(n, i -> {
            double t = i / SAMPLE_RATE;
            double f = 620 + 380 * (i / (double) n);
            return Math.sin(2 * Math.PI * f * t) * 0.4 * env(i, n, 0.01, 0.9);
        });
    }

    private static byte[] promoteClip() {
        return mix(320, m -> {
            double[] notes = {523.25, 659.25, 783.99, 1046.5};
            for (int k = 0; k < notes.length; k++) {
                final int idx = k;
                final double f = notes[k];
                m.mix(i -> {
                    int start = (int) (SAMPLE_RATE * idx * 0.06);
                    int len = (int) (SAMPLE_RATE * 0.14);
                    int j = i - start;
                    if (j < 0 || j >= len) return 0;
                    double t = j / SAMPLE_RATE;
                    return Math.sin(2 * Math.PI * f * t) * 0.22 * env(j, len, 0.02, 0.8);
                });
            }
        });
    }

    private static byte[] winClip() {
        return mix(600, m -> {
            double[] notes = {523.25, 659.25, 783.99, 1046.5};
            for (int k = 0; k < notes.length; k++) {
                final int idx = k;
                final double f = notes[k];
                m.mix(i -> {
                    int start = (int) (SAMPLE_RATE * idx * 0.09);
                    int len = (int) (SAMPLE_RATE * 0.22);
                    int j = i - start;
                    if (j < 0 || j >= len) return 0;
                    double t = j / SAMPLE_RATE;
                    return Math.sin(2 * Math.PI * f * t) * 0.25 * env(j, len, 0.01, 0.9);
                });
            }
        });
    }

    private static byte[] loseClip() {
        return mix(650, m -> {
            double[] notes = {392, 329.63, 261.63, 196};
            for (int k = 0; k < notes.length; k++) {
                final int idx = k;
                final double f = notes[k];
                m.mix(i -> {
                    int start = (int) (SAMPLE_RATE * idx * 0.1);
                    int len = (int) (SAMPLE_RATE * 0.25);
                    int j = i - start;
                    if (j < 0 || j >= len) return 0;
                    double t = j / SAMPLE_RATE;
                    return Math.sin(2 * Math.PI * f * t) * 0.25 * env(j, len, 0.01, 0.9);
                });
            }
        });
    }

    private static byte[] drawClip() {
        return mix(400, m -> {
            double[] notes = {440, 554.37};
            for (int k = 0; k < notes.length; k++) {
                final int idx = k;
                final double f = notes[k];
                m.mix(i -> {
                    int start = (int) (SAMPLE_RATE * idx * 0.12);
                    int len = (int) (SAMPLE_RATE * 0.2);
                    int j = i - start;
                    if (j < 0 || j >= len) return 0;
                    double t = j / SAMPLE_RATE;
                    return Math.sin(2 * Math.PI * f * t) * 0.22 * env(j, len, 0.01, 0.9);
            });
            }
        });
    }

    private static byte[] uiClickClip() {
        int n = (int) (SAMPLE_RATE * 0.05);
        return render(n, i -> {
            double t = i / SAMPLE_RATE;
            return Math.sin(2 * Math.PI * 800 * t) * 0.2 * env(i, n, 0.05, 0.7);
        });
    }

    private static byte[] uiHoverClip() {
        int n = (int) (SAMPLE_RATE * 0.03);
        return render(n, i -> {
            double t = i / SAMPLE_RATE;
            return Math.sin(2 * Math.PI * 1200 * t) * 0.08 * env(i, n, 0.1, 0.7);
        });
    }
}
