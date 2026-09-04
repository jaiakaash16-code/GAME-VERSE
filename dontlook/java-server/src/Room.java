import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Immutable tile-based room loaded from JSON. World coords are in pixels, tile coords are grid cells. */
public final class Room {
    public final String name;
    public final int width, height;
    public final double tileSize;
    private final boolean[][] solid; // [ty][tx]
    public final List<int[]> walls = new ArrayList<>();
    public final List<int[]> furniture = new ArrayList<>();
    public final double playerX, playerY, monsterX, monsterY, exitX, exitY;
    public final boolean generated;

    public Room(String name, int width, int height, double tileSize,
                List<int[]> walls, List<int[]> furniture,
                double playerX, double playerY, double monsterX, double monsterY,
                double exitX, double exitY, boolean generated) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.tileSize = tileSize;
        this.walls.addAll(walls);
        this.furniture.addAll(furniture);
        this.playerX = playerX;
        this.playerY = playerY;
        this.monsterX = monsterX;
        this.monsterY = monsterY;
        this.exitX = exitX;
        this.exitY = exitY;
        this.generated = generated;
        this.solid = new boolean[height][width];
        for (int[] c : walls) mark(c[0], c[1]);
        for (int[] c : furniture) mark(c[0], c[1]);
        // the exit tile is always walkable
        int etx = (int) Math.floor(exitX / tileSize), ety = (int) Math.floor(exitY / tileSize);
        if (inBounds(etx, ety)) solid[ety][etx] = false;
    }

    private void mark(int tx, int ty) {
        if (inBounds(tx, ty)) solid[ty][tx] = true;
    }

    public boolean inBounds(int tx, int ty) {
        return tx >= 0 && ty >= 0 && tx < width && ty < height;
    }

    public boolean isSolidTile(int tx, int ty) {
        return !inBounds(tx, ty) || solid[ty][tx];
    }

    public boolean isSolidAt(double x, double y) {
        return isSolidTile((int) Math.floor(x / tileSize), (int) Math.floor(y / tileSize));
    }

    public static Room fromJson(Map<String, Object> m) {
        String name = str(m, "name", "Room");
        int w = int_(m, "width", 16), h = int_(m, "height", 12);
        double tile = num(m, "tile", 40);
        List<int[]> walls = pairs(m.get("walls"));
        List<int[]> furniture = pairs(m.get("furniture"));
        double[] ps = pos(m.get("playerSpawn"), tile);
        double[] ms = pos(m.get("monsterSpawn"), tile);
        double[] es = pos(m.get("exit"), tile);
        boolean gen = bool(m, "generated", false);
        return new Room(name, w, h, tile, walls, furniture, ps[0], ps[1], ms[0], ms[1], es[0], es[1], gen);
    }

    private static List<int[]> pairs(Object o) {
        List<int[]> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object e : list) {
                if (e instanceof List<?> p && p.size() == 2
                        && p.get(0) instanceof Number && p.get(1) instanceof Number) {
                    out.add(new int[]{((Number) p.get(0)).intValue(), ((Number) p.get(1)).intValue()});
                }
            }
        }
        return out;
    }

    /** Tile coords in JSON become world coords (tile center) here. */
    private static double[] pos(Object o, double tile) {
        if (o instanceof List<?> p && p.size() >= 2
                && p.get(0) instanceof Number && p.get(1) instanceof Number) {
            return new double[]{((Number) p.get(0)).doubleValue() * tile + tile / 2,
                                ((Number) p.get(1)).doubleValue() * tile + tile / 2};
        }
        return new double[]{tile * 1.5, tile * 1.5};
    }

    private static String str(Map<String, Object> m, String k, String d) {
        Object v = m.get(k);
        return v instanceof String s ? s : d;
    }

    private static int int_(Map<String, Object> m, String k, int d) {
        Object v = m.get(k);
        return v instanceof Number n ? n.intValue() : d;
    }

    private static double num(Map<String, Object> m, String k, double d) {
        Object v = m.get(k);
        return v instanceof Number n ? n.doubleValue() : d;
    }

    private static boolean bool(Map<String, Object> m, String k, boolean d) {
        Object v = m.get(k);
        return v instanceof Boolean b ? b : d;
    }
}