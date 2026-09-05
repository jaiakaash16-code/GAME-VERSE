package com.gameverse.ui;

import javax.swing.*;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class OneBulletLauncher {
    private static final int BACKEND_MIN = 8140;
    private static final int BACKEND_MAX = 8150;
    private static final int FRONTEND_MIN = 8155;
    private static final int FRONTEND_MAX = 8165;
    private static final long BACKEND_TIMEOUT_MS = 30_000;
    private static volatile Process backendProcess;
    private static volatile Process frontendProcess;
    private static volatile int frontendPort = -1;
    private OneBulletLauncher() {}
    public static void launch() {
        Thread t = new Thread(() -> {
            try { launchBlocking(); }
            catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Could not launch One Bullet: " + ex.getMessage(),
                    "Launch Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "onebullet-launcher");
        t.setDaemon(true); t.start();
    }
    public static synchronized int currentPort() { return frontendPort; }
    private static synchronized void launchBlocking() throws Exception {
        if (frontendPort > 0 && isUp(frontendPort)) { openBrowser(frontendPort); return; }
        for (int port = FRONTEND_MIN; port <= FRONTEND_MAX; port++) {
            if (isUp(port)) { frontendPort = port; openBrowser(port); return; }
        }
        File gameDir = findGameDir();
        File frontendDir = new File(gameDir, "frontend");
        File jarFile = findBackendJar(gameDir);
        int bPort = findFreePort(BACKEND_MIN, BACKEND_MAX);
        startBackend(jarFile, gameDir, bPort);
        File serveScript = new File(gameDir, "serve-frontend.js");
        int fPort = findFreePort(FRONTEND_MIN, FRONTEND_MAX);
        startFrontend(serveScript, frontendDir, fPort);
        frontendPort = fPort; openBrowser(fPort);
    }
    private static File findGameDir() throws IOException {
        String prop = System.getProperty("onebullet.dir");
        if (prop != null) { File f = new File(prop); if (new File(f, "frontend").isDirectory()) return f; }
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
        List<File> candidates = new ArrayList<>();
        candidates.add(new File(cwd, "onebullet"));
        File parent = cwd;
        for (int i = 0; i < 4 && parent != null; i++) { parent = parent.getParentFile(); if (parent != null) candidates.add(new File(parent, "onebullet")); }
        for (File c : candidates) { if (new File(c, "frontend").isDirectory()) return c; }
        throw new FileNotFoundException("Could not find onebullet/ folder");
    }
    private static File findBackendJar(File gameDir) throws IOException {
        File target = new File(gameDir, "backend/target");
        File[] jars = target.listFiles((d, n) -> n.endsWith(".jar") && !n.contains("original"));
        if (jars != null && jars.length > 0) { File biggest = jars[0]; for (File j : jars) if (j.length() > biggest.length()) biggest = j; return biggest; }
        throw new IOException("No backend jar in " + target);
    }
    private static void startBackend(File jarFile, File gameDir, int port) throws Exception {
        File log = new File(gameDir, "backend-server.log");
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe()); cmd.add("-jar"); cmd.add(jarFile.getAbsolutePath());
        cmd.add("--server.port=" + port);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(gameDir, "backend"));
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        Process p = pb.start();
        long deadline = System.currentTimeMillis() + BACKEND_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (!p.isAlive()) { p.destroyForcibly(); throw new IOException("Backend exited; see " + log); }
            if (isBackendUp(port)) { backendProcess = p; return; }
            Thread.sleep(500);
        }
        p.destroyForcibly();
        throw new IOException("Backend did not answer on port " + port);
    }
    private static void startFrontend(File script, File frontendDir, int port) throws Exception {
        File log = script.getParentFile();
        List<String> cmd = new ArrayList<>();
        cmd.add("node"); cmd.add(script.getAbsolutePath());
        cmd.add(frontendDir.getAbsolutePath()); cmd.add(String.valueOf(port));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(log, "frontend-server.log")));
        Process p = pb.start();
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (!p.isAlive()) { p.destroyForcibly(); throw new IOException("Frontend server exited"); }
            if (isUp(port)) { frontendProcess = p; return; }
            Thread.sleep(200);
        }
        p.destroyForcibly();
        throw new IOException("Frontend did not answer on port " + port);
    }
    private static boolean isUp(int port) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/").openConnection();
            c.setConnectTimeout(500); c.setReadTimeout(500);
            c.setRequestMethod("GET");
            int code = c.getResponseCode(); c.disconnect();
            return code == 200;
        } catch (Exception e) { return false; }
    }
    private static boolean isBackendUp(int port) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/api/players").openConnection();
            c.setConnectTimeout(500); c.setReadTimeout(500);
            int code = c.getResponseCode(); c.disconnect();
            return code >= 200 && code < 500;
        } catch (Exception e) { return false; }
    }
    private static int findFreePort(int min, int max) throws IOException {
        for (int port = min; port <= max; port++) {
            try (ServerSocket probe = new ServerSocket(port)) { return probe.getLocalPort(); }
            catch (IOException ignored) {}
        }
        throw new IOException("No free port between " + min + " and " + max);
    }
    private static void openBrowser(int port) throws Exception {
        URI uri = URI.create("http://localhost:" + port + "/");
        if (Desktop.isDesktopSupported()) {
            Desktop d = Desktop.getDesktop();
            if (d.isSupported(Desktop.Action.BROWSE)) { d.browse(uri); return; }
        }
        if (isWindows()) Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", uri.toString()});
        else Runtime.getRuntime().exec(new String[]{"xdg-open", uri.toString()});
    }
    private static String javaExe() {
        File j = new File(new File(System.getProperty("java.home"), "bin"), isWindows() ? "java.exe" : "java");
        return j.isFile() ? j.getAbsolutePath() : "java";
    }
    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}