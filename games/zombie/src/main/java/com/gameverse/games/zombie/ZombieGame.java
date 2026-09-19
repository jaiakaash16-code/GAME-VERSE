package com.gameverse.games.zombie;

import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Zombie Survival — wave-based survival on a 700x400 arena.
 *
 * The player (green) moves with WASD, aims with the mouse and shoots with
 * left-click or Space. Zombies stream in from the arena edges in escalating
 * waves; every 5th wave is a boss wave. Power-ups (health, rapid fire, double
 * damage) drop from slain zombies. Death ends the run.
 *
 * Difficulty scales zombie count / speed and player damage:
 * Easy = 4 zombies, slow; Medium = 6, balanced; Hard = 8, fast.
 */
public class ZombieGame extends BaseGame {

    public static final float WORLD_W = 700f;
    public static final float WORLD_H = 400f;
    public static final int MAX_HP = 100;

    private static final float PLAYER_R = 14f;
    private static final float BULLET_SPEED = 480f;

    private final java.util.Random random = new java.util.Random();

    private final Player player;
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();

    private int wave;
    private boolean waveActive;
    private float intermission = 2f;
    private int kills;

    // Difficulty-derived values (refreshed every frame from `difficulty`)
    private int baseCount;
    private int increment;
    private float zombieSpeed;
    private float playerDamage;
    private float fireCooldown;

    public ZombieGame() {
        super("Zombie Survival");
        player = new Player(WORLD_W / 2, WORLD_H / 2);
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
        zombies.clear();
        bullets.clear();
        powerUps.clear();
        player.reset(WORLD_W / 2, WORLD_H / 2);
        wave = 0;
        waveActive = false;
        intermission = 2f;
        kills = 0;
        result = null;
        score = 0;
    }

    private void readDifficulty() {
        switch (difficulty) {
            case EASY -> { baseCount = 4; increment = 1; zombieSpeed = 55f; playerDamage = 34f; fireCooldown = 0.30f; }
            case MEDIUM -> { baseCount = 6; increment = 2; zombieSpeed = 75f; playerDamage = 25f; fireCooldown = 0.25f; }
            case HARD -> { baseCount = 7; increment = 2; zombieSpeed = 80f; playerDamage = 20f; fireCooldown = 0.22f; }
        }
    }

    @Override
    public void update(float deltaTime) {
        if (!isRunning()) {
            return;
        }
        readDifficulty();

        player.update(deltaTime);
        if (player.fireCooldown > 0) player.fireCooldown -= deltaTime;
        if (player.invuln > 0) player.invuln -= deltaTime;
        if (player.rapidTimer > 0) player.rapidTimer -= deltaTime;
        if (player.dmgTimer > 0) player.dmgTimer -= deltaTime;

        if (!waveActive) {
            intermission -= deltaTime;
            if (intermission <= 0) startWave();
        } else {
            updateZombies(deltaTime);
            updateBullets(deltaTime);
            updatePowerUps(deltaTime);
            if (zombies.isEmpty()) {
                // Wave cleared — small breather before the next one
                waveActive = false;
                intermission = 1.8f;
                score += 25 * wave;
            }
        }

        if (player.hp <= 0) {
            result = new GameResult(name, GameResult.Status.LOST, score, getElapsedTime());
            isRunning = false;
        }
    }

    private void startWave() {
        wave++;
        waveActive = true;
        boolean bossWave = wave % 5 == 0;
        int count = Math.min(14, baseCount + increment * (wave - 1));
        if (bossWave) {
            zombies.add(spawnZombie(true));
            for (int i = 0; i < Math.max(2, count / 2); i++) {
                zombies.add(spawnZombie(false));
            }
        } else {
            for (int i = 0; i < count; i++) {
                zombies.add(spawnZombie(false));
            }
        }
    }

    /** Spawn a zombie on a random arena edge, never too close to the player. */
    private Zombie spawnZombie(boolean boss) {
        float x = 20f;
        float y = 20f;
        for (int attempt = 0; attempt < 30; attempt++) {
            switch (random.nextInt(4)) {
                case 0 -> { x = random.nextFloat() * WORLD_W; y = 20f; }
                case 1 -> { x = random.nextFloat() * WORLD_W; y = WORLD_H - 20f; }
                case 2 -> { x = 20f; y = random.nextFloat() * WORLD_H; }
                default -> { x = WORLD_W - 20f; y = random.nextFloat() * WORLD_H; }
            }
            if (Math.hypot(x - player.x, y - player.y) > 160) break;
        }
        return new Zombie(x, y, boss);
    }

    private void updateZombies(float dt) {
        Iterator<Zombie> it = zombies.iterator();
        while (it.hasNext()) {
            Zombie z = it.next();
            if (z.flashTimer > 0) z.flashTimer -= dt;

            float dx = player.x - z.x;
            float dy = player.y - z.y;
            float d = (float) Math.hypot(dx, dy);
            if (d > 1) {
                float step = z.getSpeed() * dt;
                z.x += dx / d * step;
                z.y += dy / d * step;
            }

            if (d < z.radius() + PLAYER_R && player.invuln <= 0) {
                player.hp -= z.getDamage();
                player.invuln = 1f;
                if (d > 1) { z.x += dx / d * 12; z.y += dy / d * 12; }
            }

            if (z.hp <= 0) {
                it.remove();
                kills++;
                score += z.boss ? 100 : 10;
                // Power-up drop chance
                if (random.nextFloat() < 0.18f) {
                    powerUps.add(new PowerUp(z.x, z.y, random.nextInt(3)));
                }
            }
        }
    }

    private void updateBullets(float dt) {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.x += b.vx * dt;
            b.y += b.vy * dt;
            b.life -= dt;

            boolean dead = b.life <= 0
                || b.x < -20 || b.x > WORLD_W + 20
                || b.y < -20 || b.y > WORLD_H + 20;
            if (!dead) {
                for (Zombie z : zombies) {
                    if (Math.hypot(z.x - b.x, z.y - b.y) < z.radius() + 5) {
                        z.hp -= (int) (playerDamage * (player.dmgTimer > 0 ? 2f : 1f));
                        z.flashTimer = 0.1f;
                        dead = true;
                        break;
                    }
                }
            }
            if (dead) it.remove();
        }
    }

    private void updatePowerUps(float dt) {
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp p = it.next();
            p.life -= dt;
            if (p.life <= 0) {
                it.remove();
                continue;
            }
            if (Math.hypot(player.x - p.x, player.y - p.y) < PLAYER_R + 12) {
                switch (p.type) {
                    case 0 -> player.hp = Math.min(MAX_HP, player.hp + 35);
                    case 1 -> player.rapidTimer = 8f;
                    default -> player.dmgTimer = 8f;
                }
                it.remove();
            }
        }
    }

    /** Fire a bullet toward the aim point (or the nearest zombie when no mouse aim is set). */
    public void shoot() {
        if (!isRunning()) return;
        if (player.fireCooldown > 0) return;
        player.fireCooldown = player.rapidTimer > 0 ? fireCooldown * 0.5f : fireCooldown;

        float tx = player.aimX;
        float ty = player.aimY;
        if (!player.aimSet) {
            Zombie nearest = nearestZombie();
            if (nearest != null) {
                tx = nearest.x;
                ty = nearest.y;
            } else {
                tx = player.x + player.facingX * 200;
                ty = player.y + player.facingY * 200;
            }
        }
        float dx = tx - player.x;
        float dy = ty - player.y;
        float len = (float) Math.hypot(dx, dy);
        if (len < 1) {
            dx = player.facingX;
            dy = player.facingY;
        } else {
            dx /= len;
            dy /= len;
        }
        bullets.add(new Bullet(
            player.x + dx * (PLAYER_R + 4),
            player.y + dy * (PLAYER_R + 4),
            dx * BULLET_SPEED, dy * BULLET_SPEED));
    }

    private Zombie nearestZombie() {
        Zombie best = null;
        float bestD = Float.MAX_VALUE;
        for (Zombie z : zombies) {
            float d = (float) Math.hypot(z.x - player.x, z.y - player.y);
            if (d < bestD) {
                bestD = d;
                best = z;
            }
        }
        return best;
    }

    // ── Public input / rendering API ──

    public void setMove(boolean up, boolean down, boolean left, boolean right) {
        player.up = up;
        player.down = down;
        player.left = left;
        player.right = right;
    }

    /** Set the aim point (mouse position) and face it. */
    public void setAim(float mx, float my) {
        player.aimX = mx;
        player.aimY = my;
        player.aimSet = true;
        float dx = mx - player.x;
        float dy = my - player.y;
        float len = (float) Math.hypot(dx, dy);
        if (len > 1) {
            player.facingX = dx / len;
            player.facingY = dy / len;
        }
    }

    public float getPlayerX() { return player.x; }
    public float getPlayerY() { return player.y; }
    public int getPlayerHp() { return player.hp; }
    public int getMaxHp() { return MAX_HP; }
    public float getPlayerInvuln() { return player.invuln; }
    public float getAimX() { return player.aimX; }
    public float getAimY() { return player.aimY; }
    public boolean isAimSet() { return player.aimSet; }
    public List<Zombie> getZombies() { return zombies; }
    public List<Bullet> getBullets() { return bullets; }
    public List<PowerUp> getPowerUps() { return powerUps; }
    public int getWave() { return wave; }
    public boolean isWaveActive() { return waveActive; }
    public float getIntermission() { return intermission; }
    public int getZombiesRemaining() { return zombies.size(); }
    public int getKills() { return kills; }
    public boolean isRapidActive() { return player.rapidTimer > 0; }
    public boolean isDamageActive() { return player.dmgTimer > 0; }

    // ── Entities ──

    private class Player {
        float x, y;
        float facingX = 1f, facingY = 0f;
        float aimX, aimY;
        boolean aimSet;
        boolean up, down, left, right;
        int hp = MAX_HP;
        float invuln;
        float fireCooldown;
        float rapidTimer;
        float dmgTimer;

        Player(float x, float y) {
            reset(x, y);
        }

        void reset(float x, float y) {
            this.x = x;
            this.y = y;
            hp = MAX_HP;
            invuln = 0;
            fireCooldown = 0;
            rapidTimer = 0;
            dmgTimer = 0;
            aimSet = false;
            facingX = 1;
            facingY = 0;
        }

        void update(float dt) {
            float mx = (right ? 1 : 0) - (left ? 1 : 0);
            float my = (down ? 1 : 0) - (up ? 1 : 0);
            if (mx != 0 || my != 0) {
                float len = (float) Math.hypot(mx, my);
                facingX = mx / len;
                facingY = my / len;
                x += facingX * 200f * dt;
                y += facingY * 200f * dt;
            }
            x = Math.max(PLAYER_R, Math.min(WORLD_W - PLAYER_R, x));
            y = Math.max(PLAYER_R, Math.min(WORLD_H - PLAYER_R, y));
        }
    }

    /** A zombie (or boss) that moves toward the player. */
    public class Zombie {
        public float x, y;
        public int hp, maxHp;
        public boolean boss;
        public float flashTimer;
        private final float speed;
        private final int damage;
        private final float r;

        Zombie(float x, float y, boolean boss) {
            this.x = x;
            this.y = y;
            this.boss = boss;
            this.r = boss ? 26f : 13f;
            this.maxHp = boss ? 400 : 50;
            this.hp = maxHp;
            this.speed = boss
                ? zombieSpeed * 0.55f
                : zombieSpeed * (0.85f + random.nextFloat() * 0.3f);
            this.damage = boss ? 20 : 10;
        }

        float radius() { return r; }
        int getDamage() { return damage; }
        float getSpeed() { return speed; }
    }

    public static class Bullet {
        public float x, y;
        final float vx, vy;
        float life = 2.5f;

        Bullet(float x, float y, float vx, float vy) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
        }
    }

    /** 0 = health, 1 = rapid fire, 2 = double damage. */
    public static class PowerUp {
        public float x, y;
        public int type;
        public float life = 9f;

        PowerUp(float x, float y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }
}