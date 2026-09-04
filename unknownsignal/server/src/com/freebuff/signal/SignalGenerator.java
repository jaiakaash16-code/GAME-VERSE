package com.freebuff.signal;

import java.util.Random;

/** Seeded procedural helpers: triangulation source position and bearing noise. */
public final class SignalGenerator {

    private SignalGenerator() {}

    /** Normalized source position (0..1 map coords), stable per session. */
    public static double[] sourcePos(GameState s) {
        Random r = new Random(s.sessionId.hashCode() * 17L + 3);
        return new double[]{0.55 + r.nextDouble() * 0.30, 0.30 + r.nextDouble() * 0.35};
    }

    /** True bearing from station (map center) to source, degrees, 0 = north. */
    public static double trueBearing(GameState s) {
        double[] p = sourcePos(s);
        double dx = p[0] - 0.5;
        double dy = -(p[1] - 0.5);
        double deg = Math.toDegrees(Math.atan2(dx, dy));
        deg = deg % 360;
        if (deg < 0) deg += 360;
        return deg;
    }

    /** Deterministic bearing error per reading (±3 degrees). */
    public static double bearingNoise(GameState s, int readingIndex) {
        Random r = new Random(s.sessionId.hashCode() * 19L + readingIndex * 7L);
        return (r.nextDouble() - 0.5) * 6.0;
    }
}