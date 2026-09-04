package com.freebuff.signal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Loads/persists the single session to a local JSON file. */
public final class SaveStore {

    private final Path path;
    private GameState state;

    public SaveStore(String file) {
        this.path = Paths.get(file);
        load();
    }

    private void load() {
        if (Files.exists(path)) {
            try {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                Object o = Json.parse(text);
                if (o instanceof Map<?, ?> m) {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) mm.put(String.valueOf(e.getKey()), e.getValue());
                    state = GameState.fromMap(mm);
                    return;
                }
            } catch (Exception e) {
                System.err.println("Save file unreadable, starting fresh: " + e.getMessage());
            }
        }
        state = new GameState(UUID.randomUUID().toString().substring(0, 8));
    }

    public GameState state() { return state; }

    public void save() {
        try {
            Files.writeString(path, Json.write(state.toMap()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public void reset() {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
        state = new GameState(UUID.randomUUID().toString().substring(0, 8));
        save();
    }
}