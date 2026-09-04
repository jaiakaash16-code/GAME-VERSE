/** Vision cone checks: distance, angle and line-of-sight raycast against solid tiles. */
public final class Vision {
    private Vision() {}

    /** True when the monster is inside the player's vision cone with clear line of sight. */
    public static boolean isObserved(Room room, double px, double py, double aim,
                                     double mx, double my, double range, double coneHalf) {
        double dx = mx - px, dy = my - py;
        double dist = Math.hypot(dx, dy);
        if (dist > range) return false;
        if (angleDiff(aim, Math.atan2(dy, dx)) > coneHalf) return false;
        return !losBlocked(room, px, py, mx, my);
    }

    public static double angleDiff(double a, double b) {
        double d = a - b;
        while (d > Math.PI) d -= 2 * Math.PI;
        while (d < -Math.PI) d += 2 * Math.PI;
        return Math.abs(d);
    }

    /** DDA traversal of the tile grid between two world points. */
    private static boolean losBlocked(Room room, double x0, double y0, double x1, double y1) {
        double tile = room.tileSize;
        int tx0 = (int) Math.floor(x0 / tile), ty0 = (int) Math.floor(y0 / tile);
        int tx1 = (int) Math.floor(x1 / tile), ty1 = (int) Math.floor(y1 / tile);
        if (tx0 == tx1 && ty0 == ty1) return false;
        double dx = x1 - x0, dy = y1 - y0;
        if (dx == 0 && dy == 0) return false;
        int stepX = Integer.compare(tx1, tx0), stepY = Integer.compare(ty1, ty0);
        double tDeltaX = dx != 0 ? Math.abs(tile / dx) : Double.POSITIVE_INFINITY;
        double tDeltaY = dy != 0 ? Math.abs(tile / dy) : Double.POSITIVE_INFINITY;
        double tMaxX = Double.POSITIVE_INFINITY, tMaxY = Double.POSITIVE_INFINITY;
        if (stepX != 0) {
            double fx = stepX > 0 ? (tx0 + 1) * tile - x0 : tx0 * tile - x0;
            tMaxX = Math.abs(fx / dx);
        }
        if (stepY != 0) {
            double fy = stepY > 0 ? (ty0 + 1) * tile - y0 : ty0 * tile - y0;
            tMaxY = Math.abs(fy / dy);
        }
        int tx = tx0, ty = ty0;
        while (tx != tx1 || ty != ty1) {
            if (tMaxX < tMaxY) { tx += stepX; tMaxX += tDeltaX; }
            else { ty += stepY; tMaxY += tDeltaY; }
            if (tx == tx1 && ty == ty1) break;
            if (room.isSolidTile(tx, ty)) return true;
        }
        return false;
    }
}