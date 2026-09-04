package com.freebuff.signal;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/** Serves the game frontend and the JSON API on a local port. */
public final class HttpServer {

    private final int port;
    private final SaveStore store;

    public HttpServer(int port, SaveStore store) {
        this.port = port;
        this.store = store;
    }

    public void start() {
        try {
            com.sun.net.httpserver.HttpServer srv = com.sun.net.httpserver.HttpServer.create(
                    new InetSocketAddress("127.0.0.1", port), 0);
            srv.createContext("/", this::handle);
            srv.setExecutor(Executors.newFixedThreadPool(4));
            srv.start();
            System.out.println("UNKNOWN SIGNAL — LISTENING ON http://127.0.0.1:" + port);
        } catch (IOException e) {
            System.err.println("Cannot bind port " + port + ": " + e.getMessage());
            System.err.println("Pick another port, e.g.  SIGNAL_PORT=8091 ./run.sh");
            System.exit(1);
        }
    }

    private void handle(HttpExchange ex) {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.startsWith("/api/")) handleApi(ex, path);
            else serveStatic(ex, path);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                send(ex, 500, "application/json", Json.write(Map.of("error", String.valueOf(e.getMessage()))));
            } catch (IOException ignored) {
            }
        } finally {
            ex.close();
        }
    }

    private void handleApi(HttpExchange ex, String path) throws IOException {
        GameState st = store.state();
        switch (path) {
            case "/api/session" -> {
                requireGet(ex);
                sendJson(ex, 200, st.sessionJson());
            }
            case "/api/level" -> {
                requireGet(ex);
                sendJson(ex, 200, st.levelJson());
            }
            case "/api/tune" -> {
                requirePost(ex);
                Map<String, Object> b = body(ex);
                sendJson(ex, 200, st.tune(num(b.get("frequency"), 100.0), num(b.get("bandwidth"), 3.0)));
            }
            case "/api/decode" -> {
                requirePost(ex);
                Map<String, Object> b = body(ex);
                sendJson(ex, 200, st.decode(str(b.get("text"))));
                store.save();
            }
            case "/api/transmit" -> {
                requirePost(ex);
                Map<String, Object> b = body(ex);
                sendJson(ex, 200, st.transmit(str(b.get("message"))));
                store.save();
            }
            case "/api/bearing" -> {
                requirePost(ex);
                Map<String, Object> b = body(ex);
                sendJson(ex, 200, st.bearing(num(b.get("angle"), 0.0)));
                store.save();
            }
            case "/api/mark" -> {
                requirePost(ex);
                Map<String, Object> b = body(ex);
                sendJson(ex, 200, st.mark(num(b.get("x"), -1.0), num(b.get("y"), -1.0)));
                store.save();
            }
            case "/api/advance" -> {
                requirePost(ex);
                sendJson(ex, 200, st.advance());
                store.save();
            }
            case "/api/reset" -> {
                requirePost(ex);
                store.reset();
                sendJson(ex, 200, Map.of("ok", true, "session", store.state().sessionJson()));
            }
            default -> send(ex, 404, "application/json", Json.write(Map.of("error", "no such endpoint")));
        }
    }

    private void serveStatic(HttpExchange ex, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        Path base = Paths.get("web").toAbsolutePath().normalize();
        Path file = base.resolve(path.substring(1)).normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) {
            send(ex, 404, "text/plain", "NOT FOUND");
            return;
        }
        byte[] data = Files.readAllBytes(file);
        sendBytes(ex, 200, contentType(file), data);
    }

    private static String contentType(Path file) {
        String n = file.getFileName().toString().toLowerCase();
        if (n.endsWith(".html")) return "text/html; charset=utf-8";
        if (n.endsWith(".css")) return "text/css; charset=utf-8";
        if (n.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (n.endsWith(".json")) return "application/json";
        if (n.endsWith(".svg")) return "image/svg+xml";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".ico")) return "image/x-icon";
        if (n.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    // ---------------- helpers ----------------

    private static void requireGet(HttpExchange ex) throws IOException {
        if (!"GET".equals(ex.getRequestMethod())) {
            send(ex, 405, "application/json", Json.write(Map.of("error", "GET only")));
        }
    }

    private static void requirePost(HttpExchange ex) throws IOException {
        if (!"POST".equals(ex.getRequestMethod())) {
            send(ex, 405, "application/json", Json.write(Map.of("error", "POST only")));
        }
    }

    private static Map<String, Object> body(HttpExchange ex) throws IOException {
        String raw = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (raw.isBlank()) return new LinkedHashMap<>();
        Object o = Json.parse(raw);
        Map<String, Object> m = new LinkedHashMap<>();
        if (o instanceof Map<?, ?> mm) {
            for (Map.Entry<?, ?> e : mm.entrySet()) m.put(String.valueOf(e.getKey()), e.getValue());
        }
        return m;
    }

    private static double num(Object o, double dflt) {
        return o instanceof Number n ? n.doubleValue() : dflt;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static void sendJson(HttpExchange ex, int code, Object payload) throws IOException {
        send(ex, code, "application/json", Json.write(payload));
    }

    private static void send(HttpExchange ex, int code, String contentType, String text) throws IOException {
        sendBytes(ex, code, contentType, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange ex, int code, String contentType, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, data.length);
        OutputStream os = ex.getResponseBody();
        os.write(data);
        os.close();
    }
}