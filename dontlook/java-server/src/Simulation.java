import java.util.List;

/** Authoritative game simulation: player, monster AI, vision, win/lose. Runs on the server. */
public final class Simulation {
    public static final double PLAYER_SPEED = 130, MONSTER_SPEED = 168;
    public static final double PLAYER_R = 9, MONSTER_R = 11;
    public static final double VISION_RANGE = 300, CONE_HALF = 0.60, GRACE = 0.30;

    public enum Status { WAITING, PLAYING, PAUSED, WON, LOST }
    public enum MonsterState { FROZEN, HUNTING }

    // written by HTTP handler threads, read by the game loop thread
    public volatile double inVx, inVy, inAim;
    public volatile boolean inputSeen;

    private final Object lock = new Object();
    private final BestTimes best = new BestTimes();

    private Room room;
    private double px, py, aim, mx, my, grace, time;
    private Status status = Status.WAITING;
    private MonsterState mstate = MonsterState.FROZEN;
    private boolean observed;
    private String roomJson;

    public Simulation(Room initial) {
        loadRoom(initial);
    }

    public void setInput(double vx, double vy, double aim) {
        if (Double.isFinite(vx) && Double.isFinite(vy) && Double.isFinite(aim)) {
            inVx = clamp(vx, -1, 1);
            inVy = clamp(vy, -1, 1);
            inAim = aim;
            inputSeen = true;
        }
    }

    public void loadRoom(Room r) {
        synchronized (lock) {
            room = r;
            buildRoomJson();
            resetLocked();
        }
    }

    public void reset() {
        synchronized (lock) {
            resetLocked();
        }
    }

    /** Freeze the simulation while the menu is open; no-op unless a run is in progress. */
    public void pause() {
        synchronized (lock) {
            if (status == Status.PLAYING) {
                status = Status.PAUSED;
                inputSeen = false;
            }
        }
    }

    /** Explicitly resume from the pause menu (PAUSED ignores input packets). */
    public void resume() {
        synchronized (lock) {
            if (status == Status.PAUSED) status = Status.PLAYING;
        }
    }

    /** Best recorded time for a named room (story levels), or -1 when none exists. */
    public double bestFor(String name) {
        return best.get(name);
    }

    /** Clears all recorded best times (used by settings > reset progress). */
    public void resetProgress() {
        best.clear();
    }

    private void resetLocked() {
        px = room.playerX; py = room.playerY;
        mx = room.monsterX; my = room.monsterY;
        aim = 0;
        grace = GRACE;
        time = 0;
        status = Status.WAITING;
        mstate = MonsterState.FROZEN;
        observed = false;
        inputSeen = false;
    }

    public void tick(double dt) {
        synchronized (lock) {
            if (status == Status.WAITING && inputSeen) {
                status = Status.PLAYING;
            }
            if (status != Status.PLAYING) return;
            time += dt;

            double vx = inVx, vy = inVy;
            aim = inAim;
            double len = Math.hypot(vx, vy);
            if (len > 1) { vx /= len; vy /= len; }
            movePlayer(vx * PLAYER_SPEED * dt, vy * PLAYER_SPEED * dt);

            observed = Vision.isObserved(room, px, py, aim, mx, my, VISION_RANGE, CONE_HALF);
            if (observed) {
                mstate = MonsterState.FROZEN;
                grace = GRACE;
            } else {
                grace -= dt;
                if (grace <= 0) mstate = MonsterState.HUNTING;
            }
            if (mstate == MonsterState.HUNTING) {
                double dx = px - mx, dy = py - my;
                double d = Math.hypot(dx, dy);
                if (d > 1e-3) {
                    moveMonster(dx / d * MONSTER_SPEED * dt, dy / d * MONSTER_SPEED * dt);
                }
            }

            if (Math.hypot(px - mx, py - my) < PLAYER_R + MONSTER_R) {
                status = Status.LOST;
                return;
            }
            int ptx = (int) Math.floor(px / room.tileSize);
            int pty = (int) Math.floor(py / room.tileSize);
            int etx = (int) Math.floor(room.exitX / room.tileSize);
            int ety = (int) Math.floor(room.exitY / room.tileSize);
            if (ptx == etx && pty == ety) {
                status = Status.WON;
                best.record(room.name, time);
            }
        }
    }

    private void movePlayer(double dx, double dy) {
        px += dx;
        if (collides(px, py, PLAYER_R)) px -= dx;
        py += dy;
        if (collides(px, py, PLAYER_R)) py -= dy;
    }

    private void moveMonster(double dx, double dy) {
        mx += dx;
        if (collides(mx, my, MONSTER_R)) mx -= dx;
        my += dy;
        if (collides(mx, my, MONSTER_R)) my -= dy;
    }

    private boolean collides(double x, double y, double r) {
        int tx0 = (int) Math.floor((x - r) / room.tileSize);
        int tx1 = (int) Math.floor((x + r) / room.tileSize);
        int ty0 = (int) Math.floor((y - r) / room.tileSize);
        int ty1 = (int) Math.floor((y + r) / room.tileSize);
        for (int ty = ty0; ty <= ty1; ty++)
            for (int tx = tx0; tx <= tx1; tx++)
                if (room.isSolidTile(tx, ty)) return true;
        return false;
    }

    public String stateJson() {
        synchronized (lock) {
            double b = best.get(room.name);
            return "{"
                + "\"room\":" + roomJson + ","
                + "\"player\":{\"x\":" + Json.num(px) + ",\"y\":" + Json.num(py) + ",\"aim\":" + Json.num(aim) + "},"
                + "\"monster\":{\"x\":" + Json.num(mx) + ",\"y\":" + Json.num(my)
                + ",\"state\":\"" + mstate.name() + "\",\"observed\":" + observed + "},"
                + "\"status\":\"" + status.name() + "\","
                + "\"time\":" + Json.num(time) + ","
                + "\"best\":" + (b < 0 ? "null" : Json.num(b))
                + "}";
        }
    }

    private void buildRoomJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"name\":").append(Json.quote(room.name))
          .append(",\"width\":").append(room.width)
          .append(",\"height\":").append(room.height)
          .append(",\"tile\":").append(Json.num(room.tileSize))
          .append(",\"walls\":");
        appendPairs(sb, room.walls);
        sb.append(",\"furniture\":");
        appendPairs(sb, room.furniture);
        sb.append(",\"exit\":[").append(Json.num(room.exitX)).append(",").append(Json.num(room.exitY)).append("]")
          .append(",\"generated\":").append(room.generated).append("}");
        roomJson = sb.toString();
    }

    private static void appendPairs(StringBuilder sb, List<int[]> tiles) {
        sb.append("[");
        for (int i = 0; i < tiles.size(); i++) {
            if (i > 0) sb.append(",");
            int[] t = tiles.get(i);
            sb.append("[").append(t[0]).append(",").append(t[1]).append("]");
        }
        sb.append("]");
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
