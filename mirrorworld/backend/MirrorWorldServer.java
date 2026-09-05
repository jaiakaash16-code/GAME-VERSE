import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class MirrorWorldServer {

    private static final Path FRONTEND_DIR = Paths.get("..", "frontend").toAbsolutePath().normalize();
    private static int resolvePort() {
        String env = System.getenv("MIRROR_PORT");
        if (env != null) {
            try { return Integer.parseInt(env.trim()); } catch (NumberFormatException ignored) {}
        }
        return 8080;
    }

    private static final int PORT = resolvePort();

    private static final Map<String, Map<String, Object>> LEVELS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> SAVE_STORE = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        registerLevels();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/api/levels", MirrorWorldServer::handleLevels);
        server.createContext("/api/level/", MirrorWorldServer::handleLevelById);
        server.createContext("/api/save", MirrorWorldServer::handleSave);
        server.createContext("/api/load/", MirrorWorldServer::handleLoad);
        server.createContext("/api/score", MirrorWorldServer::handleScore);
        server.createContext("/api/leaderboard", MirrorWorldServer::handleLeaderboard);
        server.createContext("/api/stats", MirrorWorldServer::handleStats);
        server.createContext("/", MirrorWorldServer::handleStatic);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("=========================================");
        System.out.println("  MIRROR WORLD - Java Backend Live");
        System.out.println("  Open: http://localhost:" + PORT + "/");
        System.out.println("=========================================");
    }

    // ---------- API handlers ----------

    private static void handleLevels(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"method\"}"); return; }
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (String id : LEVELS.keySet()) {
            if (!first) sb.append(",");
            first = false;
            Map<String, Object> lvl = LEVELS.get(id);
            sb.append("{\"id\":\"").append(id)
              .append("\",\"name\":\"").append(lvl.get("name"))
              .append("\",\"order\":").append(lvl.get("order"))
              .append("}");
        }
        sb.append("]");
        sendJson(ex, sb.toString());
    }

    private static void handleLevelById(HttpExchange ex) throws IOException {
        String id = ex.getRequestURI().getPath().substring("/api/level/".length());
        Map<String, Object> lvl = LEVELS.get(id);
        if (lvl == null) { send(ex, 404, "{\"error\":\"not_found\"}"); return; }
        sendJson(ex, toJson(lvl));
    }

    private static void handleSave(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"method\"}"); return; }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String playerId = extract(body, "playerId");
        String levelId = extract(body, "levelId");
        int progress = (int) extractNumber(body, "progress", 0);
        if (playerId == null || levelId == null) { send(ex, 400, "{\"error\":\"bad_input\"}"); return; }
        String key = playerId + ":" + levelId;
        Map<String, Object> rec = new HashMap<>();
        rec.put("progress", progress);
        rec.put("updatedAt", System.currentTimeMillis());
        SAVE_STORE.put(key, rec);
        sendJson(ex, "{\"ok\":true}");
    }

    private static void handleLoad(HttpExchange ex) throws IOException {
        String playerId = ex.getRequestURI().getPath().substring("/api/load/".length());
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"playerId\":\"").append(playerId).append("\",\"saves\":[");
        boolean first = true;
        for (var e : SAVE_STORE.entrySet()) {
            if (!e.getKey().startsWith(playerId + ":")) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"levelId\":\"").append(e.getKey().substring(playerId.length() + 1))
              .append("\",\"progress\":").append(e.getValue().get("progress")).append("}");
        }
        sb.append("]}");
        sendJson(ex, sb.toString());
    }

    private static void handleScore(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"method\"}"); return; }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String playerId = extract(body, "playerId");
        String levelId = extract(body, "levelId");
        int timeMs = (int) extractNumber(body, "timeMs", 0);
        int deaths = (int) extractNumber(body, "deaths", 0);
        int switches = (int) extractNumber(body, "switches", 0);
        if (playerId == null) { send(ex, 400, "{\"error\":\"bad_input\"}"); return; }
        int score = Math.max(0, 100000 - timeMs - deaths * 500 - switches * 50);
        String key = playerId + "::" + (levelId == null ? "_" : levelId);
        Map<String, Object> rec = new HashMap<>();
        rec.put("playerId", playerId);
        rec.put("levelId", levelId);
        rec.put("timeMs", timeMs);
        rec.put("deaths", deaths);
        rec.put("switches", switches);
        rec.put("score", score);
        rec.put("at", System.currentTimeMillis());
        SAVE_STORE.put(key, rec);
        sendJson(ex, "{\"ok\":true,\"score\":" + score + "}");
    }

    private static void handleLeaderboard(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        List<Map<String, Object>> rows = new ArrayList<>();
        for (var e : SAVE_STORE.entrySet()) {
            if (!e.getKey().contains("::")) continue;
            rows.add(e.getValue());
        }
        rows.sort((a, b) -> Integer.compare((int) b.get("score"), (int) a.get("score")));
        for (Map<String, Object> r : rows) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{").append("\"playerId\":\"").append(r.get("playerId"))
              .append("\",\"levelId\":\"").append(r.get("levelId"))
              .append("\",\"score\":").append(r.get("score"))
              .append(",\"timeMs\":").append(r.get("timeMs"))
              .append(",\"deaths\":").append(r.get("deaths"))
              .append("}");
        }
        sb.append("]");
        sendJson(ex, sb.toString());
    }

    private static void handleStats(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) { send(ex, 405, "{\"error\":\"method\"}"); return; }
        int runs = 0, bestScore = 0;
        for (var e : SAVE_STORE.entrySet()) {
            if (!e.getKey().contains("::")) continue;
            runs++;
            Object s = e.getValue().get("score");
            if (s instanceof Number && ((Number)s).intValue() > bestScore) bestScore = ((Number)s).intValue();
        }
        int saves = 0;
        for (var e : SAVE_STORE.entrySet()) {
            if (e.getKey().contains("::")) continue;
            saves++;
        }
        String body = "{\"levels\":" + LEVELS.size()
                    + ",\"runs\":" + runs
                    + ",\"saves\":" + saves
                    + ",\"bestScore\":" + bestScore + "}";
        sendJson(ex, body);
    }

    // ---------- Static files ----------

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if ("/".equals(path)) path = "/index.html";
        Path file = FRONTEND_DIR.resolve(path.substring(1)).normalize();
        if (!file.startsWith(FRONTEND_DIR) || !Files.exists(file) || Files.isDirectory(file)) {
            file = FRONTEND_DIR.resolve("index.html");
        }
        String mime = mimeFor(file.toString());
        byte[] data = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    // ---------- Helpers ----------

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private static void sendJson(HttpExchange ex, String body) throws IOException {
        send(ex, 200, body);
    }

    private static String mimeFor(String p) {
        if (p.endsWith(".html")) return "text/html; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".wav")) return "audio/wav";
        if (p.endsWith(".mp3")) return "audio/mpeg";
        return "application/octet-stream";
    }

    private static String extract(String body, String key) {
        String pat = "\"" + key + "\":\"";
        int i = body.indexOf(pat);
        if (i < 0) return null;
        int j = body.indexOf("\"", i + pat.length());
        if (j < 0) return null;
        return body.substring(i + pat.length(), j);
    }

    private static String extractDefault(String body, String key, String def) {
        String v = extract(body, key);
        return v == null ? def : v;
    }

    private static double extractNumber(String body, String key, double def) {
        String pat = "\"" + key + "\":";
        int i = body.indexOf(pat);
        if (i < 0) return def;
        int j = i + pat.length();
        while (j < body.length() && (body.charAt(j) == ' ' || body.charAt(j) == '\t')) j++;
        int end = j;
        while (end < body.length() && "-0123456789.".indexOf(body.charAt(end)) >= 0) end++;
        if (end == j) return def;
        try { return Double.parseDouble(body.substring(j, end)); }
        catch (Exception e) { return def; }
    }

    private static String toJson(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else if (v instanceof List) {
                sb.append("[");
                boolean f2 = true;
                for (Object o : (List<?>) v) {
                    if (!f2) sb.append(",");
                    f2 = false;
                    sb.append(o);
                }
                sb.append("]");
            } else sb.append("\"").append(v).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

// ---------- Level data ----------
    //
    // Convention:
    //   - Single shared map; player exists in 'A' or 'B' world.
    //   - Latching buttons (once pressed, stay pressed).
    //   - A-side button opens A-side doors. B-side button opens B-side doors.
    //   - A-side hazard only kills player when in world A.
    //   - Doors (D/F) block movement when closed; passable when open.
    //   - Walls (#) are always solid; pillars act as visual dividers
    //     but have gaps the player can walk/fall through.
    //
    // Tiles:  . open   # wall   S/T spawn A/B   G goal
    //         A/B button (A or B side)
    //         D/F door   (A or B side)
    //         1 A-side hazard (kills in world A)

private static void registerLevels() {

        // =====================================================================
        // Grid: 30 cols x 19 rows.
        //   - Row 0 + row 18 = solid top/bottom borders.
        //   - Col 0 + col 29 = solid side borders.
        //   - Player (24x30) walks on row 17 (one tile above bottom border).
        //
        // DESIGN — each level teaches ONE thing:
        //   1. Tutorial     : JUMP (Space) over a wall.
        //   2. Two Doors    : BUTTONS open DOORS — press, walk through.
        //   3. Chain        : B-side buttons only work in World B; FLIP (W).
        //   4. Split Path   : HAZARDS only kill in their world.
        //   5. Mirror Heart : combines all of the above.
        //
        // All buttons are A-side except where noted; B-side buttons force flipping.
        // =====================================================================

        // ---- TUTORIAL: JUMP over wall + BUTTON/DOOR ----
        LEVELS.put("tutorial", level("Tutorial", 1,
            "##############################",  // 0
            "#............................#",  // 1
            "#............................#",  // 2
            "#............................#",  // 3
            "#............................#",  // 4
            "#............................#",  // 5
            "#............................#",  // 6
            "#............................#",  // 7
            "#............................#",  // 8
            "#............................#",  // 9
            "#............................#",  // 10
            "#............................#",  // 11
            "#............................#",  // 12
            "#............................#",  // 13
            "#............................#",  // 14
            "#............................#",  // 15
            "#............................#",  // 16
            "#S.###......#......A....D...G#",  // 17  jump wall, jump wall, press A, walk through D
            "##############################"   // 18
        ));

        // ---- TWO DOORS: A-door + B-door, MUST flip to press B ----
        // A-button (col 6) opens A-door (col 15). Player crosses, then hits
        // B-door (col 26) which only opens via B-button (col 23, B-side).
        // Spawn B at col 19 — flip teleports player to col 21 area. They
        // walk back to col 23 (B-button), press, walk through B-door to goal.
        LEVELS.put("switch_intro", level("Two Doors", 2,
            "##############################",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#S...A..#.....D..T...B#F....G#",
            "##############################"
        ));

        // ---- CHAIN REACTION: A-door + B-door (must flip) ----
        // A-button (col 4) opens A-door (col 9). B-button (col 15) opens B-door (col 22).
        // Spawn B at col 19 between the two doors so a flip teleports player
        // exactly where they need to be to press B.
        LEVELS.put("chained_doors", level("Chain Reaction", 3,
            "##############################",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#S..A.#..D...T.B#....F......G#",
            "##############################"
        ));

        // ---- HAZARD SPLIT: A-side spikes block player at row 17 ----
        // Player walks on row 17. Mid-floor spike pits at cols 9-11 row 17
        // force player to JUMP. The spikes only kill in World A — so flipping
        // to World B allows walking safely through. Spawn B at col 28 lets
        // player flip past and walk back.
        LEVELS.put("hazard_split", level("Split Path", 4,
            "##############################",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#1111111111111111111111111111#",  // 6
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#S.1111.................T...G#",  // 17  spike pit on floor (A-side)
            "##############################"
        ));

        // ---- FINAL MIRROR: A-door + spike pit + B-door ----
        // Spike pit (A-side) at cols 4-6 row 17. Player in A-world must jump
        // over OR flip to B to walk through. B-door (col 25) blocks World B
        // path past the pit. A-door (col 10) blocks World A return path.
        LEVELS.put("final_mirror", level("The Mirror Heart", 5,
            "##############################",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#1111111111111111111111111111#",  // 6
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#............................#",
            "#S.111..A..#D..T..B...F.....G#",  // 17
            "##############################"
        ));
    }

    private static Map<String, Object> level(String name, int order, String... rows) {
        // Normalize: pad/truncate every row to the maximum row width found.
        int max = 0;
        for (String r : rows) max = Math.max(max, r.length());
        String[] fixed = new String[rows.length];
        for (int i = 0; i < rows.length; i++){
          StringBuilder sb = new StringBuilder(rows[i]);
          while (sb.length() < max) sb.append('.');
          if (sb.length() > max) sb.setLength(max);
          fixed[i] = sb.toString();
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("order", order);
        m.put("width", max);
        m.put("height", fixed.length);
        m.put("rows", String.join("|", fixed));
        m.put("hint", hintFor(name));
        return m;
    }

    private static String hintFor(String name) {
        return switch (name) {
            case "Tutorial" -> "JUMP (Space) over the wall, then press the A-button to open the door.";
            case "Two Doors" -> "A-door blocks World A, B-door blocks World B. Press [W] to flip worlds.";
            case "Chain Reaction" -> "Three gates. Press A, then flip to B, press B, flip back, press A again.";
            case "Split Path" -> "Spikes only kill in World A. Flip to B to cross safely.";
            default -> "The Mirror Heart. Combine jumps, button presses, and flips. Plan your path.";
        };
    }
}