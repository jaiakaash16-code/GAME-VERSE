package com.freebuff.signal;

/** Entry point. Port from SIGNAL_PORT env, default 8090. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        int port = 8090;
        String env = System.getenv("SIGNAL_PORT");
        if (env != null && !env.isBlank()) {
            try {
                port = Integer.parseInt(env.trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid SIGNAL_PORT: " + env + " — using 8090");
            }
        }
        SaveStore store = new SaveStore("save.json");
        HttpServer server = new HttpServer(port, store);
        server.start();
    }
}