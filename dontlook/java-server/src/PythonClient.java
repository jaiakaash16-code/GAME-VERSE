import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Thin client for the Python room-generator service; returns null when unavailable. */
public final class PythonClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(800))
            .build();
    private final String base;
    private volatile boolean warned;

    public PythonClient(String base) {
        this.base = base;
    }

    public Room generate(long seed, int difficulty) {
        try {
            String body = "{\"seed\":" + seed + ",\"difficulty\":" + difficulty + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/generate-room"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return null;
            Map<String, Object> m = Json.parseObject(resp.body());
            return Room.fromJson(m);
        } catch (Exception e) {
            if (!warned) {
                warned = true;
                System.err.println("[python] generator unavailable (" + e.getClass().getSimpleName()
                        + "): using Java fallback generator");
            }
            return null;
        }
    }
}