import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Java-side procedural room generator used as a fallback when the Python service is down. */
public final class FallbackGenerator {
    private FallbackGenerator() {}

    public static Room generate(long seed, int difficulty) {
        Random r = new Random(seed ^ (long) difficulty * 0x9E3779B97F4A7C15L);
        int w = 16 + difficulty * 2, h = 11 + difficulty;
        for (int attempt = 0; attempt < 80; attempt++) {
            Room room = tryBuild(r, w, h, difficulty, attempt);
            if (room != null) return room;
        }
        return tryBuild(r, w, h, 0, 99);
    }

    private static Room tryBuild(Random r, int w, int h, int difficulty, int attempt) {
        boolean[][] solid = new boolean[h][w];
        List<int[]> walls = new ArrayList<>();
        List<int[]> furniture = new ArrayList<>();
        for (int x = 0; x < w; x++) {
            solid[0][x] = true; walls.add(new int[]{x, 0});
            solid[h - 1][x] = true; walls.add(new int[]{x, h - 1});
        }
        for (int y = 0; y < h; y++) {
            solid[y][0] = true; walls.add(new int[]{0, y});
            solid[y][w - 1] = true; walls.add(new int[]{w - 1, y});
        }
        int nFurn = 3 + difficulty * 2;
        for (int i = 0; i < nFurn; i++) {
            int fx = 2 + r.nextInt(Math.max(1, w - 4));
            int fy = 2 + r.nextInt(Math.max(1, h - 4));
            solid[fy][fx] = true;
            furniture.add(new int[]{fx, fy});
            if (r.nextInt(3) == 0 && fx + 1 < w - 1 && !solid[fy][fx + 1]) {
                solid[fy][fx + 1] = true;
                furniture.add(new int[]{fx + 1, fy});
            }
        }
        int ptx = 1, pty = h - 2;
        int etx = w - 2, ety = 1;
        solid[pty][ptx] = false;
        solid[ety][etx] = false;

        List<int[]> candidates = new ArrayList<>();
        for (int y = 2; y < h - 2; y++) {
            for (int x = 2; x < w - 2; x++) {
                if (solid[y][x] || (x == ptx && y == pty) || (x == etx && y == ety)) continue;
                double d = (x - ptx) * (x - ptx) + (y - pty) * (y - pty);
                double maxD = (double) (w - 3) * (w - 3) + (double) (h - 3) * (h - 3);
                if (d > maxD * 0.45) candidates.add(new int[]{x, y});
            }
        }
        if (candidates.isEmpty()) return null;
        int[] m = candidates.get(r.nextInt(candidates.size()));
        solid[m[1]][m[0]] = false;

        if (!reachable(solid, w, h, ptx, pty, etx, ety)) return null;

        double t = 40;
        return new Room("Cellar " + (100 + attempt), w, h, t, walls, furniture,
                ptx * t + t / 2, pty * t + t / 2,
                m[0] * t + t / 2, m[1] * t + t / 2,
                etx * t + t / 2, ety * t + t / 2, true);
    }

    private static boolean reachable(boolean[][] solid, int w, int h, int sx, int sy, int tx, int ty) {
        if (solid[sy][sx] || solid[ty][tx]) return false;
        boolean[][] seen = new boolean[h][w];
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{sx, sy});
        seen[sy][sx] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!stack.isEmpty()) {
            int[] c = stack.remove(stack.size() - 1);
            if (c[0] == tx && c[1] == ty) return true;
            for (int[] d : dirs) {
                int nx = c[0] + d[0], ny = c[1] + d[1];
                if (nx >= 0 && ny >= 0 && nx < w && ny < h && !solid[ny][nx] && !seen[ny][nx]) {
                    seen[ny][nx] = true;
                    stack.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }
}