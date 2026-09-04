import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Persists best completion time per room to best_time.json in the working directory. */
public final class BestTimes {
    private final Path file = Path.of("best_time.json");
    private final Map<String, Double> times = new HashMap<>();
    private final Object lock = new Object();

    public BestTimes() {
        try {
            if (Files.exists(file)) {
                Map<String, Object> m = Json.parseObject(Files.readString(file));
                for (Map.Entry<String, Object> e : m.entrySet()) {
                    if (e.getValue() instanceof Number n) times.put(e.getKey(), n.doubleValue());
                }
            }
        } catch (Exception e) {
            // corrupt or missing file: start fresh
        }
    }

    /** Best time for a room, or -1 if none recorded. */
    public double get(String name) {
        synchronized (lock) {
            return times.getOrDefault(name, -1.0);
        }
    }

    /** Records a time if it beats the stored one; returns true when improved. */
    public boolean record(String name, double t) {
        synchronized (lock) {
            Double cur = times.get(name);
            if (cur != null && cur <= t) return false;
            times.put(name, t);
            save();
            return true;
        }
    }

    /** Clears all recorded best times and persists an empty file. */
    public void clear() {
        synchronized (lock) {
            times.clear();
            save();
        }
    }

    private void save() {
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Double> e : times.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append(Json.quote(e.getKey())).append(":").append(Json.num(e.getValue()));
            }
            sb.append("}");
            Files.writeString(file, sb.toString());
        } catch (Exception e) {
            // non-fatal: keep in memory
        }
    }
}