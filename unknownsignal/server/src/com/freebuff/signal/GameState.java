package com.freebuff.signal;

import com.freebuff.signal.LevelData.Level;
import com.freebuff.signal.LevelData.Signal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** A single-player session: level progression, flags, narrative archive, endings. */
public final class GameState {

    public final String sessionId;
    public int level = 1;
    public boolean finished;
    public String ending; // TRUTH | SEVER | JOIN
    public final List<Map<String, Object>> archive = new ArrayList<>();
    public final Map<String, Object> flags = new LinkedHashMap<>();
    public long tuneCalls;

    public GameState(String sessionId) { this.sessionId = sessionId; }

    // ---------------- helpers ----------------

    private boolean flagBool(String k) { return Boolean.TRUE.equals(flags.get(k)); }

    @SuppressWarnings("unchecked")
    private List<String> decodedSet() {
        Object o = flags.get("decoded");
        return o instanceof List<?> l ? (List<String>) l : new ArrayList<>();
    }

    private void markDecoded(String w) {
        List<String> d = decodedSet();
        if (!d.contains(w)) d.add(w);
        flags.put("decoded", d);
    }

    private int echoCount() {
        Object o = flags.get("echoesSolved");
        return o instanceof Number n ? n.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private List<Double> bearingList() {
        Object o = flags.get("bearings");
        return o instanceof List<?> l ? (List<Double>) l : new ArrayList<>();
    }

    private double normAngle(double a) {
        a = a % 360;
        if (a < 0) a += 360;
        return a;
    }

    public void log(String type, String text) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("type", type);
        e.put("text", text);
        archive.add(e);
    }

    // ---------------- level data ----------------

    public List<Signal> signalsForLevel() {
        Level L = LevelData.levels().get(level - 1);
        if (level != 5) return L.signals();
        if (flags.get("l5Freqs") == null) {
            Random r = new Random(sessionId.hashCode() * 31L + 5);
            double base = 101.0 + r.nextDouble() * 8.0;
            List<Double> fs = new ArrayList<>();
            for (int i = 0; i < 5; i++) fs.add(base + i * 1.6 + r.nextDouble() * 0.3);
            flags.put("l5Freqs", fs);
            flags.put("l5RealIndex", r.nextInt(5));
        }
        @SuppressWarnings("unchecked")
        List<Double> fs = (List<Double>) flags.get("l5Freqs");
        int real = ((Number) flags.get("l5RealIndex")).intValue();
        List<Signal> out = new ArrayList<>();
        for (int i = 0; i < fs.size(); i++) {
            out.add(new Signal(fs.get(i), Morse.patternFor("ANSWER"), "ANSWER", i == real));
        }
        return out;
    }

    public boolean levelComplete() {
        if (finished) return true;
        switch (level) {
            case 1: return decodedSet().contains("HELLO");
            case 2: {
                List<String> d = decodedSet();
                return d.contains("SOS") && d.contains("STAY AWAY") && flags.get("choice") != null;
            }
            case 3: return flagBool("markedOk");
            case 4: return echoCount() >= LevelData.L4_ECHOES.length;
            case 5: return flagBool("decodedAnswer") && flags.get("answered") != null;
            default: return false;
        }
    }

    // ---------------- API actions ----------------

    public Map<String, Object> tune(double freq, double bandwidth) {
        tuneCalls++;
        Level L = LevelData.levels().get(level - 1);
        var resp = new LinkedHashMap<String, Object>();
        resp.put("frequency", freq);
        resp.put("bandwidth", bandwidth);
        Random r = new Random(sessionId.hashCode() ^ (int) (freq * 100) ^ (int) (tuneCalls * 7919L));
        double noise = 0.03 + r.nextDouble() * 0.06;
        resp.put("noise", Math.round(noise * 1000.0) / 1000.0);

        double tol = L.lockTolerance();
        List<Signal> sigs = signalsForLevel();
        Signal best = null;
        double bestStr = 0;
        for (Signal s : sigs) {
            double st = Math.exp(-Math.pow((freq - s.freq()) / tol, 2) * 2.5);
            if (st > bestStr) { bestStr = st; best = s; }
        }
        if (best == null) {
            resp.put("strength", 0.0);
            resp.put("locked", false);
            resp.put("complete", levelComplete());
            return resp;
        }
        bestStr = Math.min(1.0, bestStr + noise * 0.05);
        resp.put("strength", Math.round(bestStr * 1000.0) / 1000.0);
        boolean locked = bestStr >= 0.25;
        boolean garbled = false;
        if (locked && bandwidth > L.maxBandwidth()) { locked = false; garbled = true; }
        resp.put("locked", locked);
        resp.put("garbled", garbled);
        if (locked && !garbled) {
            switch (level) {
                case 3 -> {
                    flags.put("acquired", true);
                    resp.put("bearingMode", true);
                    resp.put("morse", null);
                }
                case 4 -> {
                    int idx = echoCount();
                    resp.put("morse", idx < LevelData.L4_ECHOES.length ? corruptedEcho(idx) : null);
                }
                case 5 -> {
                    boolean real = best.real();
                    flags.put("realLocked", real);
                    double stable = real ? 0.8 + r.nextDouble() * 0.15 : 0.2 + r.nextDouble() * 0.6;
                    resp.put("stable", Math.round(stable * 100.0) / 100.0);
                    resp.put("morse", real ? best.morse()
                            : Morse.flipOneSymbol(Morse.patternFor("ANSWER"), sessionId.hashCode() * 7L + tuneCalls));
                }
                default -> resp.put("morse", best.morse());
            }
        } else {
            if (level == 5) flags.put("realLocked", false);
            resp.put("morse", null);
        }
        resp.put("complete", levelComplete());
        return resp;
    }

    private String corruptedEcho(int idx) {
        String word = LevelData.L4_ECHOES[idx];
        String pattern = Morse.patternFor(word);
        return Morse.flipOneSymbol(pattern, sessionId.hashCode() * 13L + idx * 101L);
    }

    public Map<String, Object> decode(String raw) {
        String in = normalize(raw);
        var resp = new LinkedHashMap<String, Object>();
        resp.put("ok", false);
        if (finished) {
            resp.put("reason", "TRANSMISSION CEASED");
            resp.put("complete", true);
            return resp;
        }
        switch (level) {
            case 1 -> {
                if (in.equals("HELLO")) {
                    resp.put("ok", true);
                    resp.put("plain", "HELLO");
                    markDecoded("HELLO");
                    log("signal", "DECODED: HELLO");
                    log("narrative", "A VOICE FROM THE EMPTY FREQUENCIES. IT IS SAYING HELLO.");
                } else resp.put("reason", "NO MATCH — LISTEN AGAIN");
            }
            case 2 -> {
                if (in.equals("SOS") && !decodedSet().contains("SOS")) {
                    resp.put("ok", true);
                    resp.put("plain", "SOS");
                    markDecoded("SOS");
                    log("signal", "DECODED: SOS");
                    log("narrative", "A PLEA FROM SOMEWHERE FAR BELOW THE OCEAN FLOOR.");
                } else if (in.equals("STAYAWAY") && !decodedSet().contains("STAY AWAY")) {
                    resp.put("ok", true);
                    resp.put("plain", "STAY AWAY");
                    markDecoded("STAY AWAY");
                    log("signal", "DECODED: STAY AWAY");
                    log("narrative", "THE SECOND VOICE IS NOT A VOICE AT ALL.");
                } else resp.put("reason", "NO MATCH — LISTEN AGAIN");
            }
            case 3 -> resp.put("reason", "SIGNAL TOO WEAK TO DECODE — USE THE BEARING DIAL");
            case 4 -> {
                int idx = echoCount();
                if (idx >= LevelData.L4_ECHOES.length) {
                    resp.put("reason", "ECHO CHAIN COMPLETE");
                    break;
                }
                String word = LevelData.L4_ECHOES[idx];
                if (in.equals(word)) {
                    int n = idx + 1;
                    flags.put("echoesSolved", n);
                    resp.put("ok", true);
                    resp.put("plain", word);
                    resp.put("remaining", LevelData.L4_ECHOES.length - n);
                    log("signal", "ECHO CORRECTED: " + word);
                    if (n >= LevelData.L4_ECHOES.length) {
                        log("narrative", "THE ECHO CHAIN ENDS. A LONG PAUSE. THEN, CLEAR AS GLASS:");
                        log("signal", "I HEAR YOU. I HAVE ALWAYS HEARD YOU.");
                    }
                } else resp.put("reason", "THAT IS NOT WHAT IT SAID — CORRECT THE ECHO");
            }
            case 5 -> {
                if (!flagBool("realLocked")) {
                    resp.put("reason", "SIGNAL NOT LOCKED — FIND THE STABLE SOURCE");
                    break;
                }
                if (in.equals("ANSWER")) {
                    flags.put("decodedAnswer", true);
                    resp.put("ok", true);
                    resp.put("plain", "ANSWER");
                    log("signal", "DECODED: ANSWER");
                } else resp.put("reason", "NO MATCH — LISTEN AGAIN");
            }
            case 6 -> {
                if (in.equals("YOUARETHEANTENNA")) {
                    flags.put("decodedFinal", true);
                    resp.put("ok", true);
                    resp.put("plain", "YOU ARE THE ANTENNA");
                    resp.put("endingReady", true);
                    log("signal", "DECODED: YOU ARE THE ANTENNA");
                    log("narrative", "IT IS NOT A SIGNAL. IT IS AN ANTENNA. AND YOU ARE THE OTHER END.");
                } else resp.put("reason", "NO MATCH — LISTEN AGAIN");
            }
        }
        resp.put("complete", levelComplete());
        return resp;
    }

    public Map<String, Object> transmit(String raw) {
        String in = normalize(raw);
        var resp = new LinkedHashMap<String, Object>();
        resp.put("ok", false);
        switch (level) {
            case 2 -> {
                if (in.equals("SOS")) {
                    flags.put("choice", "SOS");
                    resp.put("ok", true);
                    resp.put("choice", "SOS");
                    log("narrative", "YOU ANSWER THE DISTRESS. THE WARNING GOES SILENT.");
                } else if (in.equals("STAYAWAY")) {
                    flags.put("choice", "STAY AWAY");
                    resp.put("ok", true);
                    resp.put("choice", "STAY AWAY");
                    log("narrative", "YOU ANSWER THE WARNING. THE DISTRESS SIGNAL STOPS — MID-LETTER.");
                } else resp.put("reason", "KEY IN ONE OF THE DECODED MESSAGES");
            }
            case 5 -> {
                if (in.equals("YES") || in.equals("NO")) {
                    flags.put("answered", in.equals("YES") ? "YES" : "NO");
                    resp.put("ok", true);
                    resp.put("answer", in.equals("YES") ? "YES" : "NO");
                    log("narrative", in.equals("YES")
                            ? "THEN WE ARE BOTH ALONE, IT SAYS. THAT IS ENOUGH."
                            : "THEN WHO IS WITH YOU? IT ASKS. THE STARS GO QUIET.");
                } else resp.put("reason", "THE SIGNAL WAITS — ANSWER YES OR NO");
            }
            case 6 -> {
                for (String e : List.of("TRUTH", "SEVER", "JOIN")) {
                    if (in.equals(e)) {
                        ending = e;
                        finished = true;
                        resp.put("ok", true);
                        resp.put("ending", e);
                        resp.put("title", endingTitle(e));
                        resp.put("text", endingText(e));
                        resp.put("epilogue", epilogueText(e));
                        log("narrative", "THE FREQUENCY TAKES YOUR WORD. EVERYTHING CHANGES.");
                        break;
                    }
                }
                if (!Boolean.TRUE.equals(resp.get("ok"))) resp.put("reason", "IT AWAITS YOUR FINAL WORD");
            }
            default -> resp.put("reason", "TRANSMITTER NOT KEYED");
        }
        resp.put("complete", levelComplete());
        return resp;
    }

    public Map<String, Object> bearing(double angleDeg) {
        var resp = new LinkedHashMap<String, Object>();
        resp.put("ok", false);
        if (!flagBool("acquired")) {
            resp.put("reason", "NO SIGNAL ACQUIRED — TUNE TO THE SOURCE FIRST");
            return resp;
        }
        if (flagBool("markedOk")) {
            resp.put("reason", "SOURCE ALREADY MARKED");
            return resp;
        }
        double trueBearing = SignalGenerator.trueBearing(this);
        double reading = normAngle(angleDeg + SignalGenerator.bearingNoise(this, bearingList().size()));
        double d = Math.abs(((angleDeg - trueBearing) % 360 + 540.0) % 360 - 180.0);
        double strength = Math.pow(Math.max(0.0, 1.0 - d / 45.0), 2);
        List<Double> bs = bearingList();
        bs.add(Math.round(reading * 100.0) / 100.0);
        flags.put("bearings", bs);
        resp.put("ok", true);
        resp.put("angle", Math.round(reading * 100.0) / 100.0);
        resp.put("strength", Math.round(strength * 100.0) / 100.0);
        resp.put("count", bs.size());
        resp.put("readyToMark", bs.size() >= 3);
        resp.put("complete", levelComplete());
        return resp;
    }

    public Map<String, Object> mark(double x, double y) {
        var resp = new LinkedHashMap<String, Object>();
        resp.put("ok", false);
        if (!flagBool("acquired")) { resp.put("reason", "NO SIGNAL ACQUIRED"); return resp; }
        if (bearingList().size() < 3) { resp.put("reason", "TAKE THREE BEARINGS FIRST"); return resp; }
        if (flagBool("markedOk")) { resp.put("reason", "SOURCE ALREADY MARKED"); return resp; }
        double[] src = SignalGenerator.sourcePos(this);
        double dist = Math.hypot(x - src[0], y - src[1]);
        if (dist < 0.09) {
            flags.put("markedOk", true);
            resp.put("ok", true);
            log("signal", "SOURCE MARKED — " + Math.round(x * 100) + ", " + Math.round(y * 100));
            log("narrative", "THE COORDINATES RESOLVE TO A DERELICT RELAY SATELLITE. ITS FINAL LOG ENTRY:");
            log("signal", "WE ARE HERE. WAITING.");
        } else {
            resp.put("reason", "NO SOURCE AT THOSE COORDINATES");
        }
        resp.put("complete", levelComplete());
        return resp;
    }

    public Map<String, Object> advance() {
        var resp = new LinkedHashMap<String, Object>();
        if (levelComplete() && !finished) level++;
        resp.put("session", sessionJson());
        return resp;
    }

    // ---------------- JSON views ----------------

    public Map<String, Object> sessionJson() {
        var m = new LinkedHashMap<String, Object>();
        m.put("sessionId", sessionId);
        m.put("level", level);
        m.put("title", LevelData.levels().get(level - 1).title());
        m.put("finished", finished);
        m.put("ending", ending);
        m.put("archive", archive);
        return m;
    }

    public Map<String, Object> levelJson() {
        Level L = LevelData.levels().get(level - 1);
        var m = new LinkedHashMap<String, Object>();
        m.put("level", level);
        m.put("title", L.title());
        m.put("brief", L.brief());
        m.put("goal", L.goal());
        var bands = new ArrayList<Map<String, Object>>();
        for (Signal s : signalsForLevel()) {
            var b = new LinkedHashMap<String, Object>();
            b.put("freq", s.freq());
            bands.add(b);
        }
        m.put("bands", bands);
        var st = new LinkedHashMap<String, Object>();
        st.put("decoded", decodedSet());
        st.put("echoesSolved", echoCount());
        st.put("echoCount", LevelData.L4_ECHOES.length);
        st.put("bearings", bearingList());
        st.put("readyToMark", bearingList().size() >= 3 && !flagBool("markedOk"));
        st.put("marked", flagBool("markedOk"));
        st.put("acquired", flagBool("acquired"));
        st.put("choiceMade", flags.get("choice") != null);
        st.put("decodedAnswer", flagBool("decodedAnswer"));
        st.put("answered", flags.get("answered") != null);
        st.put("endingReady", flagBool("decodedFinal"));
        st.put("finished", finished);
        st.put("ending", ending);
        st.put("complete", levelComplete());
        m.put("state", st);
        if (level == 6) {
            var endings = new ArrayList<Map<String, Object>>();
            for (String e : List.of("TRUTH", "SEVER", "JOIN")) {
                var em = new LinkedHashMap<String, Object>();
                em.put("key", e);
                em.put("title", endingTitle(e));
                em.put("text", endingText(e));
                em.put("epilogue", epilogueText(e));
                endings.add(em);
            }
            m.put("endings", endings);
        }
        return m;
    }

    // ---------------- persistence ----------------

    public Map<String, Object> toMap() {
        var m = new LinkedHashMap<String, Object>();
        m.put("sessionId", sessionId);
        m.put("level", level);
        m.put("finished", finished);
        m.put("ending", ending);
        m.put("archive", archive);
        m.put("flags", flags);
        return m;
    }

    public static GameState fromMap(Map<String, Object> m) {
        GameState s = new GameState(String.valueOf(m.get("sessionId")));
        s.level = ((Number) m.get("level")).intValue();
        s.finished = Boolean.TRUE.equals(m.get("finished"));
        Object end = m.get("ending");
        s.ending = end == null ? null : String.valueOf(end);
        Object ar = m.get("archive");
        if (ar instanceof List<?> l) {
            for (Object o : l) {
                if (o instanceof Map<?, ?> mm) {
                    Map<String, Object> e = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> en : mm.entrySet()) e.put(String.valueOf(en.getKey()), en.getValue());
                    s.archive.add(e);
                }
            }
        }
        Object fl = m.get("flags");
        if (fl instanceof Map<?, ?> mm) {
            for (Map.Entry<?, ?> e : mm.entrySet()) s.flags.put(String.valueOf(e.getKey()), e.getValue());
        }
        return s;
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    // ---------------- ending text ----------------

    private static String endingTitle(String e) {
        return switch (e) {
            case "TRUTH" -> "THE BROADCAST";
            case "SEVER" -> "THE SILENCE";
            default -> "THE ANTENNA";
        };
    }

    private static String endingText(String e) {
        return switch (e) {
            case "TRUTH" -> "YOU KEY THE GLOBAL NETWORK OPEN. THE WORLD HEARS THE FREQUENCY FOR THE FIRST TIME. " +
                    "BY MORNING, EVERY RECEIVER ON EARTH IS LISTENING. AND IT IS GLAD TO FINALLY MEET EVERYONE.";
            case "SEVER" -> "YOU PULL THE PLUG. THE LATTICE CRUMPLES, THE HUM DIES, AND THE FREQUENCIES GO BLANK FOR " +
                    "THE FIRST TIME IN DECADES. YOU SIT IN THE SILENCE, ALONE, AND WONDER WHETHER YOU WERE THE FIRST " +
                    "TO SEVER — OR THE LAST TO HEAR.";
            default -> "YOU SEND YOUR NAME INTO THE STATIC. THE STATIC SENDS IT BACK WITH ANOTHER VOICE BESIDE YOURS. " +
                    "THE ANTENNA HAS A NEW BRANCH. SOMEWHERE, VERY FAR AWAY, THE SIGNAL SAYS: WELCOME HOME.";
        };
    }

    private static String epilogueText(String e) {
        return switch (e) {
            case "TRUTH" -> "STATION 7 GOES SILENT. THE SIGNAL DOES NOT.";
            case "SEVER" -> "TRANSMISSION ENDED.";
            default -> "YOU ARE STILL LISTENING.";
        };
    }
}