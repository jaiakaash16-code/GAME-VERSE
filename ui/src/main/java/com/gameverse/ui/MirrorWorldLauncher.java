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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Files;

/**
 * Launcher for the bundled external game "Mirror World" (see repo {@code mirrorworld/} folder).
 *
 * Mirror World is a self-contained web app: a Java HTTP server serves a canvas/CSS frontend
 * and runs the authoritative simulation. On the first click this class compiles that server
 * with the JDK's own {@code javac}, starts it on a free port (8120-8135), waits until it
 * answers HTTP, and opens it in the default browser. Later clicks reuse the running server.
 *
 * The backend's PORT constant is overridden via the MIRROR_PORT environment variable so
 * GameVerse can pick a non-conflicting port even though the upstream source defaults to 8080.
 */
public final class MirrorWorldLauncher {

    private static final int MIN_PORT = 8120;
    private static final int MAX_PORT = 8135;
    private static final long STARTUP_TIMEOUT_MS = 20_000;

    private static volatile Process serverProcess;
    private static volatile int serverPort = -1;

    private MirrorWorldLauncher() {}

    public static void launch() {
        Thread t = new Thread(() -> {
            try {
                launchBlocking();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Couldn't launch Mirror World:\n" + ex.getMessage(),
                        "Launch Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "mirrorworld-launcher");
        t.setDaemon(true);
        t.start();
    }

    public static synchronized int currentPort() {
        return serverPort;
    }

    private static synchronized void launchBlocking() throws Exception {
        Process p = serverProcess;
        if (p != null && p.isAlive() && serverPort > 0 && isUp(serverPort)) {
            openBrowser(serverPort);
            return;
        }

        int existing = findRunningServer();
        if (existing > 0) {
            serverPort = existing;
            openBrowser(existing);
            return;
        }

        File gameDir = findGameDir();
        File outDir = new File(gameDir, "out");
        compileServer(gameDir, outDir);

        int port = findFreePort();
        startServer(gameDir, outDir, port);
        openBrowser(port);
    }

    private static int findRunningServer() {
        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            try {
                HttpURLConnection c = (HttpURLConnection)
                        new URL("http://127.0.0.1:" + port + "/").openConnection();
                c.setConnectTimeout(300);
                c.setReadTimeout(300);
                int code = c.getResponseCode();
                c.disconnect();
                if (code == 200) return port;
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private static File findGameDir() throws IOException {
        String prop = System.getProperty("mirrorworld.dir");
        if (prop != null) {
            File f = new File(prop);
            if (new File(f, "backend").isDirectory()) return f;
        }
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
        List<File> candidates = new ArrayList<>();
        candidates.add(new File(cwd, "mirrorworld"));
        File parent = cwd;
        for (int i = 0; i < 4 && parent != null; i++) {
            parent = parent.getParentFile();
            if (parent != null) candidates.add(new File(parent, "mirrorworld"));
        }
        for (File c : candidates) {
            if (new File(c, "backend").isDirectory()) return c;
        }
        throw new FileNotFoundException(
                "Could not find the mirrorworld/ game folder. Looked in: " + candidates);
    }

    private static void compileServer(File gameDir, File outDir) throws Exception {
        File srcRoot = new File(gameDir, "backend");
        List<File> srcs;
        try (Stream<java.nio.file.Path> walk = Files.walk(srcRoot.toPath())) {
            srcs = walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(java.nio.file.Path::toFile)
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (srcs.isEmpty()) {
            throw new IOException("No server sources found under " + srcRoot);
        }

        File marker = new File(outDir, "MirrorWorldServer.class");
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
        System.out.println("[mirrorworld] compiled server: " + srcs.size() + " sources -> " + outDir);
    }

    private static String javacExecutable() {
        File javaHome = new File(System.getProperty("java.home"));
        String exe = isWindows() ? "javac.exe" : "javac";
        File j = new File(new File(javaHome, "bin"), exe);
        return j.isFile() ? j.getAbsolutePath() : "javac";
    }

    private static int findFreePort() throws IOException {
        for (int port = MIN_PORT; port <= MAX_PORT; port++) {
            try (ServerSocket probe = new ServerSocket(port)) {
                return probe.getLocalPort();
            } catch (IOException ignored) {
            }
        }
        throw new IOException("No free port between " + MIN_PORT + " and " + MAX_PORT);
    }

    private static void startServer(File gameDir, File outDir, int port) throws Exception {
        File log = new File(gameDir, "gameverse-server.log");
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExecutable());
        cmd.add("-cp");
        cmd.add(outDir.getAbsolutePath());
        cmd.add("MirrorWorldServer");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(gameDir, "backend")); // server reads frontend via ../frontend
        pb.environment().put("MIRROR_PORT", String.valueOf(port));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));

        Process p = pb.start();
        long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!p.isAlive()) {
                p.destroyForcibly();
                throw new IOException("Mirror World server exited during startup; see " + log);
            }
            if (isUp(port)) {
                serverProcess = p;
                serverPort = port;
                System.out.println("[mirrorworld] server up at http://localhost:" + port);
                return;
            }
            Thread.sleep(200);
        }
        p.destroyForcibly();
        throw new IOException("Mirror World server did not answer on port " + port + "; see " + log);
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