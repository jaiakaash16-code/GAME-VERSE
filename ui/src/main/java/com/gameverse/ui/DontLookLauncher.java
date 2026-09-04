package com.gameverse.ui;

import javax.swing.*;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Launcher for the bundled external game "DON'T LOOK" (see repo {@code dontlook/} folder).
 *
 * The game is a self-contained web app: a zero-dependency Java HTTP server serves a
 * canvas frontend and runs the authoritative simulation. On the first click this class
 * compiles that server with the JDK's own {@code javac}, starts it on a free port,
 * waits until it answers HTTP, and opens it in the default browser. Later clicks reuse
 * the already-running server.
 */
public final class DontLookLauncher {

    private static final int MIN_PORT = 8080;
    private static final int MAX_PORT = 8090;
    private static final long STARTUP_TIMEOUT_MS = 20_000;

    private static volatile Process serverProcess;
    private static volatile int serverPort = -1;

    private DontLookLauncher() {}

    /**
     * Launch (or re-focus) the DON'T LOOK game. Runs the blocking startup work on a
     * background thread so the game hub never freezes; failures surface as a dialog.
     */
    public static void launch() {
        Thread t = new Thread(() -> {
            try {
                launchBlocking();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Couldn't launch DON'T LOOK:\n" + ex.getMessage(),
                        "Launch Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "dontlook-launcher");
        t.setDaemon(true);
        t.start();
    }

    /** Port the game server is currently listening on, or -1 if it isn't up. */
    public static synchronized int currentPort() {
        return serverPort;
    }

    private static synchronized void launchBlocking() throws Exception {
        Process p = serverProcess;
        if (p != null && p.isAlive() && serverPort > 0 && isUp(serverPort)) {
            openBrowser(serverPort);
            return;
        }

        // A server left over from a previous session (or started manually) already
        // answers /api/state — just re-focus it instead of starting a second one.
        int existing = findRunningServer();
        if (existing > 0) {
            serverPort = existing;
            openBrowser(existing);
            return;
        }

        File javaServerDir = new File(findGameDir(), "java-server");
        File outDir = new File(javaServerDir, "out");
        compileServer(javaServerDir, outDir);

        int port = findFreePort();
        startServer(javaServerDir, outDir, port);
        openBrowser(port);
    }

    /** Scan the usual port range for a game server already answering /api/state. */
    private static int findRunningServer() {
        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            try {
                HttpURLConnection c = (HttpURLConnection)
                        new URL("http://127.0.0.1:" + port + "/api/state").openConnection();
                c.setConnectTimeout(300);
                c.setReadTimeout(300);
                int code = c.getResponseCode();
                c.disconnect();
                if (code == 200) return port;
            } catch (Exception ignored) {
                // not this port — keep scanning
            }
        }
        return -1;
    }

    // ── locating the bundled game ────────────────────────────────────────────

    private static File findGameDir() throws IOException {
        String prop = System.getProperty("dontlook.dir");
        if (prop != null) {
            File f = new File(prop);
            if (new File(f, "java-server").isDirectory()) return f;
        }
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
        List<File> candidates = new ArrayList<>();
        candidates.add(new File(cwd, "dontlook"));
        File parent = cwd;
        for (int i = 0; i < 4 && parent != null; i++) {
            parent = parent.getParentFile();
            if (parent != null) candidates.add(new File(parent, "dontlook"));
        }
        for (File c : candidates) {
            if (new File(c, "java-server").isDirectory()) return c;
        }
        throw new FileNotFoundException(
                "Could not find the dontlook/ game folder. Looked in: " + candidates);
    }

    // ── one-time compile of the bundled server ───────────────────────────────

    private static void compileServer(File javaServerDir, File outDir) throws Exception {
        File srcDir = new File(javaServerDir, "src");
        File[] srcs = srcDir.listFiles((d, n) -> n.endsWith(".java"));
        if (srcs == null || srcs.length == 0) {
            throw new IOException("No server sources found in " + srcDir);
        }
        Arrays.sort(srcs);

        File marker = new File(outDir, "GameServer.class");
        boolean needCompile = !marker.isFile();
        if (!needCompile) {
            long newestSrc = 0;
            for (File s : srcs) newestSrc = Math.max(newestSrc, s.lastModified());
            needCompile = newestSrc > marker.lastModified();
        }
        if (!needCompile) return;

        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            throw new IOException("Cannot create " + outDir);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(javacExecutable());
        cmd.add("-encoding");
        cmd.add("UTF-8");
        cmd.add("-d");
        cmd.add(outDir.getAbsolutePath());
        for (File s : srcs) cmd.add(s.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String log = readAll(p.getInputStream());
        if (!p.waitFor(120, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("javac timed out compiling the game server");
        }
        if (p.exitValue() != 0) {
            throw new IOException("Failed to compile the game server:\n" + log);
        }
        System.out.println("[dontlook] compiled server: " + srcs.length + " sources -> " + outDir);
    }

    private static String javacExecutable() {
        File javaHome = new File(System.getProperty("java.home"));
        String exe = isWindows() ? "javac.exe" : "javac";
        File j = new File(new File(javaHome, "bin"), exe);
        return j.isFile() ? j.getAbsolutePath() : "javac";
    }

    // ── free port selection ──────────────────────────────────────────────────

    private static int findFreePort() throws IOException {
        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            try (ServerSocket probe = new ServerSocket(port)) {
                return probe.getLocalPort();
            } catch (IOException ignored) {
                // taken — try next
            }
        }
        throw new IOException("No free port between " + MIN_PORT + " and " + MAX_PORT);
    }

    // ── start + health-check the game server ─────────────────────────────────

    private static void startServer(File javaServerDir, File outDir, int port) throws Exception {
        File log = new File(javaServerDir, "gameverse-server.log");
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExecutable());
        cmd.add("-Dport=" + port);
        cmd.add("-cp");
        cmd.add(outDir.getAbsolutePath());
        cmd.add("GameServer");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(javaServerDir);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));

        Process p = pb.start();
        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!p.isAlive()) {
                p.destroyForcibly();
                throw new IOException("DON'T LOOK server exited during startup; see " + log);
            }
            if (isUp(port)) {
                serverProcess = p;
                serverPort = port;
                System.out.println("[dontlook] server up at http://localhost:" + port);
                return;
            }
            Thread.sleep(200);
        }
        p.destroyForcibly();
        throw new IOException("DON'T LOOK server did not answer on port " + port + "; see " + log);
    }

    private static boolean isUp(int port) {
        try {
            HttpURLConnection c = (HttpURLConnection)
                    new URL("http://127.0.0.1:" + port + "/").openConnection();
            c.setConnectTimeout(500);
            c.setReadTimeout(500);
            c.setRequestMethod("GET");
            int code = c.getResponseCode();
            c.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ── open in the default browser ──────────────────────────────────────────

    private static void openBrowser(int port) throws Exception {
        URI uri = URI.create("http://localhost:" + port + "/");
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.BROWSE)) {
                d.browse(uri);
                return;
            }
        }
        String url = uri.toString();
        if (isWindows()) {
            Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
        } else {
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        }
    }

    private static String javaExecutable() {
        File javaHome = new File(System.getProperty("java.home"));
        String exe = isWindows() ? "java.exe" : "java";
        File j = new File(new File(javaHome, "bin"), exe);
        return j.isFile() ? j.getAbsolutePath() : "java";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String readAll(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        return sb.toString();
    }
}