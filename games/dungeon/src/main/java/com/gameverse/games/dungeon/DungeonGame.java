package com.gameverse.games.dungeon;

import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dungeon Escape — explore hand-crafted dungeon levels, grab the key, unlock
 * the locked door with E, defeat or avoid the patrolling enemies, watch for
 * spikes and reach the exit to advance. Level 3 holds a boss.
 *
 * Space attacks in the facing direction. Keys are picked up automatically;
 * stand next to a locked door and press E to open it (consumes one key).
 * Difficulty scales enemy speed / HP / damage, trap damage and attack power.
 */
public class DungeonGame extends BaseGame {

    public static final int TILE = 32;
    public static final int COLS = 21;
    public static final int ROWS = 12;
    public static final int MAX_LEVEL = 3;
    public static final int MAX_HP = 100;

    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final java.util.Random random = new java.util.Random();

    private char[][] map;
    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();

    private int level;
    private int keys;
    private int treasure;
    private int kills;
    private float attackCooldown;
    private float attackTimer;
    private float trapTimer;

    // ── Level layouts (21 x 12; '#' wall, '.' floor, 'P' start, 'K' key,
    //    'D' locked door, 'X' exit, 'T' spikes, 'E' enemy, 'B' boss, '$' treasure) ──
    private static final String[] LEVEL_1 = {
        "#####################",
        "#.........#.........#",
        "#P........#....E....#",
        "#....K....#.........#",
        "#.........#....$....#",
        "#....$....#.........#",
        "#.........#...T.....#",
        "#.........#.........#",
        "#..E......D.........#",
        "#.........#.........#",
        "#.........#...X.....#",
        "#####################"
    };

    private static final String[] LEVEL_2 = {
        "#####################",
        "#.........#.........#",
        "#P........#....E....#",
        "#....K....#....$....#",
        "#.........#.........#",
        "#....$....#.........#",
        "#.........#...E.....#",
        "#........D..........#",
        "#...................#",
        "#...T.....$......T..#",
        "#...E..........X....#",
        "#####################"
    };

    private static final String[] LEVEL_3 = {
        "#####################",
        "#.........#.........#",
        "#P........#....B....#",
        "#....K....#.........#",
        "#.........#....$....#",
        "#....$....#.........#",
        "#.........#...T.....#",
        "#........D..........#",
        "#...................#",
        "#..T......$.........#",
        "#..E..........X.....#",
        "#####################"
    };

    public DungeonGame() {
        super("Dungeon Escape");
        player = new Player(0, 0);
    }

    @Override
    public void initialize() {
        resetGame();
    }

    @Override
    public void start() {
        super.start();
        resetGame();
    }

    @Override
    public void restart() {
        super.restart();
        resetGame();
    }

    private void resetGame() {
        level = 1;
        score = 0;
        kills = 0;
        keys = 0;
        treasure = 0;
        attackCooldown = 0;
        attackTimer = 0;
        trapTimer = 0;
        player.hp = MAX_HP;
        result = null;
        loadLevel(1);
    }

    private void loadLevel(int lvl) {
        String[] rows = switch (lvl) {
            case 1 -> LEVEL_1;
            case 2 -> LEVEL_2;
            default -> LEVEL_3;
        };
        map = new char[ROWS][COLS];
        enemies.clear();
        keys = 0;
        treasure = 0;
        for (int r = 0; r < ROWS; r++) {
            String line = rows[r];
            for (int c = 0; c < COLS; c++) {
                char ch = c < line.length() ? line.charAt(c) : '#';
                if (ch == 'P') {
                    player.x = c * TILE + TILE / 2f;
                    player.y = r * TILE + TILE / 2f;
                    ch = '.';
                } else if (ch == 'E' || ch == 'B') {
                    spawnEnemy(c, r, ch == 'B');
                    ch = '.';
                }
                map[r][c] = ch;
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!isRunning()) {
            return;
        }

        if (attackCooldown > 0) attackCooldown -= deltaTime;
        if (attackTimer > 0) attackTimer -= deltaTime;
        if (player.invuln > 0) player.invuln -= deltaTime;

        player.update(deltaTime, getPlayerSpeed());
        updateEnemies(deltaTime);
        checkTraps(deltaTime);
        checkPickups();

        if (player.hp <= 0) {
            result = new GameResult(name, GameResult.Status.LOST, score, getElapsedTime());
            isRunning = false;
        }
    }

    // ── Difficulty-derived values (read live so a mid-run change applies) ──

    private float getPlayerSpeed() {
        return switch (difficulty) {
            case EASY -> 150f;
            case MEDIUM -> 170f;
            case HARD -> 190f;
        };
    }

    private float getEnemySpeed() {
        return switch (difficulty) {
            case EASY -> 45f;
            case MEDIUM -> 60f;
            case HARD -> 75f;
        };
    }

    private float getEnemyDamage() {
        return switch (difficulty) {
            case EASY -> 10f;
            case MEDIUM -> 12f;
            case HARD -> 15f;
        };
    }

    private int getEnemyHp() {
        return switch (difficulty) {
            case EASY -> 40;
            case MEDIUM -> 60;
            case HARD -> 80;
        };
    }

    private int getBossHp() {
        return switch (difficulty) {
            case EASY -> 180;
            case MEDIUM -> 260;
            case HARD -> 350;
        };
    }

    private float getTrapDamage() {
        return switch (difficulty) {
            case EASY -> 6f;
            case MEDIUM -> 8f;
            case HARD -> 10f;
        };
    }

    private float getAttackDamage() {
        return switch (difficulty) {
            case EASY -> 45f;
            case MEDIUM -> 34f;
            case HARD -> 28f;
        };
    }

    private float getDetection() {
        return switch (difficulty) {
            case EASY -> 130f;
            case MEDIUM -> 180f;
            case HARD -> 230f;
        };
    }

    // ── Gameplay ──

    private void updateEnemies(float dt) {
        float speed = getEnemySpeed();
        float detection = getDetection();
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (e.flashTimer > 0) e.flashTimer -= dt;

            if (e.hp <= 0) {
                it.remove();
                kills++;
                score += e.boss ? 100 : 25;
                continue;
            }

            float d = (float) Math.hypot(player.x - e.x, player.y - e.y);
            e.chasing = d < detection;
            float moveSpeed = e.boss ? speed * 0.85f : speed;

            if (!e.chasing) {
                // Patrol: walk in one direction, turn when blocked
                if (e.dirX == 0 && e.dirY == 0) e.pickNewDir();
                if (isBlockedAhead(e, e.dirX, e.dirY)) e.pickNewDir();
                moveEntity(e, e.dirX * moveSpeed * 0.6f * dt, e.dirY * moveSpeed * 0.6f * dt);
            } else {
                // Chase the player
                float dx = player.x - e.x;
                float dy = player.y - e.y;
                float len = (float) Math.hypot(dx, dy);
                if (len > 1) moveEntity(e, dx / len * moveSpeed * dt, dy / len * moveSpeed * dt);
            }

            // Contact damage
            if (d < e.half + player.half + 3 && player.invuln <= 0) {
                player.hp -= (int) (e.boss ? getEnemyDamage() * 1.6f : getEnemyDamage());
                player.invuln = 1f;
                e.flashTimer = 0.2f;
                if (d > 1) {
                    e.x += (e.x - player.x) / d * 10;
                    e.y += (e.y - player.y) / d * 10;
                }
            }
        }
    }

    private boolean isBlockedAhead(Enemy e, int dirX, int dirY) {
        return collides(e.x + dirX * (e.half + 2), e.y + dirY * (e.half + 2), e.half);
    }

    private void moveEntity(Enemy e, float dx, float dy) {
        e.x += dx;
        if (collides(e.x, e.y, e.half)) e.x -= dx;
        e.y += dy;
        if (collides(e.x, e.y, e.half)) e.y -= dy;
    }

    /** Melee swing in the facing direction — damages enemies in a wide arc in front. */
    public void attack() {
        if (!isRunning() || attackCooldown > 0) return;
        attackCooldown = 0.45f;
        attackTimer = 0.22f;
        float dmg = getAttackDamage();
        for (Enemy e : enemies) {
            float dx = e.x - player.x;
            float dy = e.y - player.y;
            float d = (float) Math.hypot(dx, dy);
            if (d < 56 && d > 0.01f) {
                float dot = (dx / d) * player.facingX + (dy / d) * player.facingY;
                if (dot > 0.15f) {
                    e.hp -= dmg;
                    e.flashTimer = 0.15f;
                    e.x += dx / d * 10;
                    e.y += dy / d * 10;
                }
            }
        }
    }

    /** Open an adjacent locked door if the player holds a key. */
    public void interact() {
        if (!isRunning() || keys <= 0) return;
        int pc = (int) (player.x / TILE);
        int pr = (int) (player.y / TILE);
        for (int[] d : DIRS) {
            int c = pc + d[0];
            int r = pr + d[1];
            if (inBounds(c, r) && map[r][c] == 'D') {
                map[r][c] = '.';
                keys--;
                score += 25;
                return;
            }
        }
    }

    private void checkTraps(float dt) {
        int pc = (int) (player.x / TILE);
        int pr = (int) (player.y / TILE);
        if (inBounds(pc, pr) && map[pr][pc] == 'T') {
            trapTimer -= dt;
            if (trapTimer <= 0) {
                player.hp -= (int) getTrapDamage();
                trapTimer = 0.8f;
            }
        } else {
            trapTimer = 0;
        }
    }

    private void checkPickups() {
        int pc = (int) (player.x / TILE);
        int pr = (int) (player.y / TILE);
        if (!inBounds(pc, pr)) return;
        char t = map[pr][pc];
        switch (t) {
            case 'K' -> {
                keys++;
                score += 25;
                map[pr][pc] = '.';
            }
            case '$' -> {
                treasure++;
                score += 50;
                map[pr][pc] = '.';
            }
            case 'X' -> levelCleared();
            default -> { }
        }
    }

    private void levelCleared() {
        if (level >= MAX_LEVEL) {
            score += 500;
            result = new GameResult(name, GameResult.Status.WON, score, getElapsedTime());
            isRunning = false;
            return;
        }
        score += 200 + level * 100;
        player.hp = Math.min(MAX_HP, player.hp + 40);
        level++;
        loadLevel(level);
    }

    // ── Collision helpers ──

    private boolean inBounds(int c, int r) {
        return c >= 0 && c < COLS && r >= 0 && r < ROWS;
    }

    private boolean isSolid(int c, int r) {
        if (c < 0 || c >= COLS || r < 0 || r >= ROWS) return true;
        char t = map[r][c];
        return t == '#' || t == 'D';
    }

    private boolean collides(float px, float py, float half) {
        int c0 = (int) ((px - half) / TILE);
        int c1 = (int) ((px + half - 0.01f) / TILE);
        int r0 = (int) ((py - half) / TILE);
        int r1 = (int) ((py + half - 0.01f) / TILE);
        return isSolid(c0, r0) || isSolid(c1, r0) || isSolid(c0, r1) || isSolid(c1, r1);
    }

    private void spawnEnemy(int col, int row, boolean boss) {
        enemies.add(new Enemy(col * TILE + TILE / 2f, row * TILE + TILE / 2f, boss));
    }

    // ── Public input / rendering API ──

    public void setMove(boolean up, boolean down, boolean left, boolean right) {
        player.up = up;
        player.down = down;
        player.left = left;
        player.right = right;
    }

    public char[][] getMap() { return map; }
    public List<Enemy> getEnemies() { return enemies; }
    public float getPlayerX() { return player.x; }
    public float getPlayerY() { return player.y; }
    public float getPlayerFacingX() { return player.facingX; }
    public float getPlayerFacingY() { return player.facingY; }
    public int getPlayerHp() { return player.hp; }
    public int getMaxHp() { return MAX_HP; }
    public float getPlayerInvuln() { return player.invuln; }
    public int getLevel() { return level; }
    public int getKeyCount() { return keys; }
    public int getTreasureCount() { return treasure; }
    public int getKills() { return kills; }
    public float getAttackCooldown() { return attackCooldown; }
    public float getAttackTimer() { return attackTimer; }

    // ── Entities ──

    private class Player {
        float x, y;
        float facingX = 1f, facingY = 0f;
        boolean up, down, left, right;
        int hp = MAX_HP;
        float invuln;
        final float half = 11f;

        Player(float x, float y) {
            this.x = x;
            this.y = y;
        }

        void update(float dt, float speed) {
            float mx = (right ? 1 : 0) - (left ? 1 : 0);
            float my = (down ? 1 : 0) - (up ? 1 : 0);
            if (mx != 0 || my != 0) {
                float len = (float) Math.hypot(mx, my);
                facingX = mx / len;
                facingY = my / len;
                moveBy(facingX * speed * dt, facingY * speed * dt);
            }
        }

        void moveBy(float dx, float dy) {
            x += dx;
            if (collides(x, y, half)) x -= dx;
            y += dy;
            if (collides(x, y, half)) y -= dy;
        }
    }

    /** A dungeon enemy (patrol / chase); boss = bigger, tougher. */
    public class Enemy {
        public float x, y;
        public int hp, maxHp;
        public boolean boss;
        public float flashTimer;
        public boolean chasing;
        int dirX = 1, dirY = 0;
        public final float half;

        Enemy(float x, float y, boolean boss) {
            this.x = x;
            this.y = y;
            this.boss = boss;
            this.half = boss ? 17f : 12f;
            this.maxHp = boss ? getBossHp() : getEnemyHp();
            this.hp = maxHp;
        }

        /** Choose a new patrol direction that is not blocked. */
        void pickNewDir() {
            int start = random.nextInt(4);
            for (int i = 0; i < 4; i++) {
                int[] d = DIRS[(start + i) % 4];
                if (!isBlockedAhead(this, d[0], d[1])) {
                    dirX = d[0];
                    dirY = d[1];
                    return;
                }
            }
            dirX = 0;
            dirY = 0;
        }
    }
}