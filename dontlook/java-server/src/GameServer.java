import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** DON'T LOOK game server: serves the frontend, runs the simulation, streams state over SSE. */
public final class GameServer {
    private static final int TICK_HZ = 30;
    private static final int PORT = Integer.getInteger("port", 8080);
    private static final Path STATIC_DIR = Paths.get(System.getProperty("static.dir", "../frontend"));
    private static final Path ROOM_DIR = Paths.get(System.getProperty("room.dir", "../rooms"));
    private static final String PYTHON_URL = System.getProperty("python.url", "http://127.0.0.1:8001");

    private final List<Room> storyRooms = new ArrayList<>();
    private final List<HttpExchange> clients = new CopyOnWriteArrayList<>();
    private final Simulation sim;
    private final PythonClient python = new PythonClient(PYTHON_URL);
    private final Random rng = new Random();
    private volatile int roomIndex = 0;
    private volatile boolean endless = false;
    private volatile int difficulty = 0;

    private GameServer() throws IOException {
        loadStoryRooms();
        Room first = storyRooms.isEmpty()
                ? FallbackGenerator.generate(rng.nextLong(), 1)
                : storyRooms.get(0);
        sim = new Simulation(first);
    }

    public static void main(String[] args) throws Exception {
        new GameServer().run();
    }

    private void loadStoryRooms() throws IOException {
        if (!Files.isDirectory(ROOM_DIR)) {
            System.out.println("[server] room dir not found: " + ROOM_DIR.toAbsolutePath());
            return;
        }
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(ROOM_DIR)) {
            stream.filter(p -> p.toString().endsWith(".json")).sorted().forEach(files::add);
        }
        for (Path f : files) {
            try {
                Room r = Room.fromJson(Json.parseObject(Files.readString(f)));
                storyRooms.add(r);
                System.out.println("[server] loaded room: " + r.name + " (" + f.getFileName() + ")");
            } catch (Exception e) {
                System.err.println("[server] failed to load " + f + ": " + e.getMessage());
            }
        }
        if (storyRooms.isEmpty()) System.out.println("[server] no story rooms loaded; starting in endless mode");
    }

    private void run() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/api/stream", this::handleStream);
        server.createContext("/api/state", this::handleState);
        server.createContext("/api/input", this::handleInput);
        server.createContext("/api/restart", this::handleRestart);
        server.createContext("/api/next", this::handleNext);
        server.createContext("/api/pause", this::handlePause);
        server.createContext("/api/levels", this::handleLevels);
        server.createContext("/api/select", this::handleSelect);
        server.createContext("/api/reset", this::handleReset);
        server.createContext("/api/resume", this::handleResume);
        server.createContext("/", this::handleStatic);
        server.start();
        System.out.println("[server] DON'T LOOK serving at http://localhost:" + PORT);
        System.out.println("[server] story rooms: " + storyRooms.size()
                + ", python generator: " + PYTHON_URL);

        ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "game-loop");
            t.setDaemon(true);
            return t;
        });
        loop.scheduleAtFixedRate(this::tick, 0, 1000L / TICK_HZ, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        sim.tick(1.0 / TICK_HZ);
        String json = sim.stateJson();
        for (HttpExchange ex : clients) {
            try {
                OutputStream os = ex.getResponseBody();
                os.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                clients.remove(ex);
                ex.close();
            }
        }
    }

    private void handleStream(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "text/event-stream");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.getResponseHeaders().set("Connection", "keep-alive");
        ex.sendResponseHeaders(200, 0);
        OutputStream os = ex.getResponseBody();
        os.write(("data: " + sim.stateJson() + "\n\n").getBytes(StandardCharsets.UTF_8));
        os.flush();
        clients.add(ex);
    }

    private void handleState(HttpExchange ex) throws IOException {
        respondJson(ex, sim.stateJson());
    }

    private void handleInput(HttpExchange ex) throws IOException {
        Map<String, Object> m = readJson(ex);
        sim.setInput(num(m.get("vx")), num(m.get("vy")), num(m.get("aim")));
        respond204(ex);
    }

    private void handleRestart(HttpExchange ex) throws IOException {
        sim.reset();
        respond204(ex);
    }

    private void handlePause(HttpExchange ex) throws IOException {
        sim.pause();
        respond204(ex);
    }

    private void handleNext(HttpExchange ex) throws IOException {
        synchronized (sim) {
            if (!endless && roomIndex + 1 < storyRooms.size()) {
                roomIndex++;
                sim.loadRoom(storyRooms.get(roomIndex));
            } else {
                endless = true;
                difficulty++;
                long seed = rng.nextLong();
                Room r = python.generate(seed, difficulty);
                if (r == null) r = FallbackGenerator.generate(seed, difficulty);
                sim.loadRoom(r);
            }
        }
        respond204(ex);
    }

    private void handleLevels(HttpExchange ex) throws IOException {
        StringBuilder sb = new StringBuilder("{\"story\":[");
        boolean first = true;
        for (int i = 0; i < storyRooms.size(); i++) {
            Room r = storyRooms.get(i);
            double b = sim.bestFor(r.name);
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"index\":").append(i)
              .append(",\"name\":").append(Json.quote(r.name))
              .append(",\"best\":").append(b < 0 ? "null" : Json.num(b))
              .append(",\"completed\":").append(b >= 0).append("}");
        }
        sb.append("]}");
        respondJson(ex, sb.toString());
    }

    private void handleResume(HttpExchange ex) throws IOException {
        sim.resume();
        respond204(ex);
    }

    private void handleReset(HttpExchange ex) throws IOException {
        sim.resetProgress();
        respond204(ex);
    }

    private void handleSelect(HttpExchange ex) throws IOException {
        Map<String, Object> m = readJson(ex);
        synchronized (sim) {
            if (Boolean.TRUE.equals(m.get("endless"))) {
                endless = true;
                difficulty++;
                long seed = rng.nextLong();
                Room r = python.generate(seed, difficulty);
                if (r == null) r = FallbackGenerator.generate(seed, difficulty);
                sim.loadRoom(r);
            } else if (m.get("index") instanceof Number n && !storyRooms.isEmpty()) {
                int idx = Math.max(0, Math.min(storyRooms.size() - 1, n.intValue()));
                endless = false;
                difficulty = 0;
                roomIndex = idx;
                sim.loadRoom(storyRooms.get(idx));
            }
        }
        respond204(ex);
    }

    private static Map<String, Object> readJson(HttpExchange ex) {
        try {
            byte[] body = ex.getRequestBody().readAllBytes();
            if (body.length == 0) return Map.of();
            return Json.parseObject(new String(body, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Map.of();
        }
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..")) {
            respondCode(ex, 400, "bad path");
            return;
        }
        Path base = STATIC_DIR.toAbsolutePath().normalize();
        Path file = base.resolve(path.substring(1)).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            respondCode(ex, 404, "not found");
            return;
        }
        byte[] data = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType(path));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }

    private static void respondJson(HttpExchange ex, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private static void respond204(HttpExchange ex) throws IOException {
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    private static void respondCode(HttpExchange ex, int code, String text) throws IOException {
        byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }

    private static String contentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
