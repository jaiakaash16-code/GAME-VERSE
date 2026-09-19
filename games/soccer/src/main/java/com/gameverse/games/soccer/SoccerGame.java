package com.gameverse.games.soccer;

import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

/**
 * 2D Soccer — top-down football on a 700x380 pitch.
 *
 * The player (blue) dribbles the ball by running into it, passes with E and
 * shoots with Space at the opponent's goal (right). The AI opponent chases the
 * ball and kicks it back toward the player's goal (left) while a goalkeeper
 * guards the net. Whoever scores more goals when the match timer expires wins.
 *
 * Difficulty affects opponent / keeper speed:
 * Easy = slow AI, Medium = balanced, Hard = fast AI + aggressive keeper.
 */
public class SoccerGame extends BaseGame {

    /** Pitch dimensions (world units; the UI renders them scaled to the panel). */
    public static final float FIELD_W = 700f;
    public static final float FIELD_H = 380f;
    /** Half-height of each goal mouth (opening in the side walls). */
    public static final float GOAL_HALF = 45f;

    private static final float PLAYER_R = 12f;
    private static final float OPPONENT_R = 12f;
    private static final float KEEPER_R = 11f;
    private static final float BALL_R = 7f;

    /** Exponential ball speed decay per second — a hard shot travels ~1/3 of the pitch. */
    private static final float BALL_DECAY = 2.0f;
    private static final float KICK_POWER = 300f;   // dribble / touch
    private static final float PASS_POWER = 380f;   // E
    private static final float SHOOT_POWER = 620f;  // Space

    private final java.util.Random random = new java.util.Random();

    private Ball ball;
    private Player player;
    private Opponent opponent;
    private Keeper keeper;

    private int playerGoals;
    private int aiGoals;
    private float matchTime;
    private float matchDuration = 60f;

    public SoccerGame() {
        super("2D Soccer");
    }

    @Override
    public void initialize() {
        resetMatch();
    }

    @Override
    public void start() {
        super.start();
        resetMatch();
    }

    @Override
    public void restart() {
        super.restart();
        resetMatch();
    }

    /** Reset the whole match. Called again from start() so the applied difficulty is in effect. */
    private void resetMatch() {
        player = new Player(FIELD_W * 0.25f, FIELD_H / 2);
        opponent = new Opponent(FIELD_W * 0.78f, FIELD_H / 2);
        keeper = new Keeper(FIELD_W - 16f, FIELD_H / 2);
        ball = new Ball(FIELD_W / 2, FIELD_H / 2);
        playerGoals = 0;
        aiGoals = 0;
        matchTime = 0;
        result = null;
        score = 0;
    }

    @Override
    public void update(float deltaTime) {
        if (!isRunning()) {
            return;
        }

        matchTime += deltaTime;
        if (matchTime >= matchDuration) {
            finishMatch();
            return;
        }

        player.update(deltaTime, getPlayerSpeed());
        opponent.update(deltaTime, getOpponentSpeed());
        keeper.update(deltaTime, getKeeperSpeed());
        ball.update(deltaTime);

        // Dribble: running into the ball kicks it along the player's facing.
        if (ball.distanceTo(player.x, player.y) < PLAYER_R + BALL_R) {
            kickBall(player.facingX, player.facingY, KICK_POWER * (player.sprinting ? 1.3f : 1f));
        }
        // Opponent clears the ball toward the player's goal.
        if (ball.distanceTo(opponent.x, opponent.y) < OPPONENT_R + BALL_R) {
            float tx = 0f;
            float ty = FIELD_H / 2 + (random.nextFloat() - 0.5f) * 50f;
            float dx = tx - ball.x;
            float dy = ty - ball.y;
            float len = (float) Math.hypot(dx, dy);
            kickBall(dx / len, dy / len, SHOOT_POWER * 0.5f, opponent);
        }

        checkGoals();
        score = playerGoals * 100;
    }

    private float getPlayerSpeed() {
        return switch (difficulty) {
            case EASY -> 190f;
            case MEDIUM -> 220f;
            case HARD -> 250f;
        };
    }

    private float getOpponentSpeed() {
        return switch (difficulty) {
            case EASY -> 130f;
            case MEDIUM -> 160f;
            case HARD -> 195f;
        };
    }

    private float getKeeperSpeed() {
        return switch (difficulty) {
            case EASY -> 120f;
            case MEDIUM -> 150f;
            case HARD -> 190f;
        };
    }

    /** Apply a kick impulse from the player; push the ball out of collision so it won't re-trigger. */
    private void kickBall(float dx, float dy, float power) {
        ball.vx = dx * power;
        ball.vy = dy * power;
        ball.x = player.x + dx * (PLAYER_R + BALL_R + 2);
        ball.y = player.y + dy * (PLAYER_R + BALL_R + 2);
        clampBall();
    }

    /** Same, but the kick originates from the opponent. */
    private void kickBall(float dx, float dy, float power, Opponent from) {
        ball.vx = dx * power;
        ball.vy = dy * power;
        ball.x = from.x + dx * (OPPONENT_R + BALL_R + 2);
        ball.y = from.y + dy * (OPPONENT_R + BALL_R + 2);
        clampBall();
    }

    private void clampBall() {
        ball.x = Math.max(BALL_R, Math.min(FIELD_W - BALL_R, ball.x));
        ball.y = Math.max(BALL_R, Math.min(FIELD_H - BALL_R, ball.y));
    }

    /** Kick the ball hard along the player's facing (with aim assist toward the opponent goal). */
    public void shoot() {
        if (!isRunning()) return;
        if (ball.distanceTo(player.x, player.y) > 120f) return;

        float aimX = player.facingX;
        float aimY = player.facingY;
        float dx = FIELD_W - player.x;
        float dy = FIELD_H / 2 - player.y;
        float len = (float) Math.hypot(dx, dy);
        if (len > 1) {
            float dot = player.facingX * dx / len + player.facingY * dy / len;
            if (dot > 0.55f) {
                aimX = dx / len;
                aimY = dy / len;
            }
        }
        kickBall(aimX, aimY, SHOOT_POWER);
    }

    /** Soft kick along the facing — useful for control / setting up a shot. */
    public void pass() {
        if (!isRunning()) return;
        if (ball.distanceTo(player.x, player.y) > 120f) return;
        kickBall(player.facingX, player.facingY, PASS_POWER);
    }

    private void checkGoals() {
        if (ball.x < -BALL_R || ball.x > FIELD_W + BALL_R) {
            boolean inMouth = Math.abs(ball.y - FIELD_H / 2) < GOAL_HALF;
            if (ball.x < -BALL_R) {
                if (inMouth) aiGoals++;
                else { ball.x = BALL_R; ball.vx = -ball.vx * 0.5f; }
            } else {
                if (inMouth) playerGoals++;
                else { ball.x = FIELD_W - BALL_R; ball.vx = -ball.vx * 0.5f; }
            }
            if (inMouth) kickoff();
        }
    }

    private void kickoff() {
        ball.x = FIELD_W / 2;
        ball.y = FIELD_H / 2;
        ball.vx = (random.nextBoolean() ? 60f : -60f) + (random.nextFloat() - 0.5f) * 40f;
        ball.vy = (random.nextFloat() - 0.5f) * 60f;
    }

    private void finishMatch() {
        isRunning = false;
        if (playerGoals > aiGoals) {
            score = playerGoals * 100 + 500;
            result = new GameResult(name, GameResult.Status.WON, score, getElapsedTime());
        } else if (playerGoals == aiGoals) {
            score = playerGoals * 100 + 100;
            result = new GameResult(name, GameResult.Status.DRAWN, score, getElapsedTime());
        } else {
            score = playerGoals * 100;
            result = new GameResult(name, GameResult.Status.LOST, score, getElapsedTime());
        }
    }

    // ── Public input / rendering API ──

    public void setMove(boolean up, boolean down, boolean left, boolean right, boolean sprint) {
        player.up = up;
        player.down = down;
        player.left = left;
        player.right = right;
        player.sprinting = sprint;
    }

    public int getPlayerGoals() { return playerGoals; }
    public int getAiGoals() { return aiGoals; }
    public float getMatchTimeLeft() { return Math.max(0, matchDuration - matchTime); }

    public float getPlayerX() { return player.x; }
    public float getPlayerY() { return player.y; }
    public float getPlayerFacingX() { return player.facingX; }
    public float getPlayerFacingY() { return player.facingY; }
    public float getOpponentX() { return opponent.x; }
    public float getOpponentY() { return opponent.y; }
    public float getKeeperX() { return keeper.x; }
    public float getKeeperY() { return keeper.y; }
    public float getBallX() { return ball.x; }
    public float getBallY() { return ball.y; }

    // ── Entities ──

    private class Player {
        float x, y;
        float facingX = 1f, facingY = 0f;
        boolean up, down, left, right, sprinting;

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
                float spd = speed * (sprinting ? 1.5f : 1f);
                x += facingX * spd * dt;
                y += facingY * spd * dt;
            }
            x = Math.max(PLAYER_R, Math.min(FIELD_W - PLAYER_R, x));
            y = Math.max(PLAYER_R, Math.min(FIELD_H - PLAYER_R, y));
        }
    }

    private class Opponent {
        float x, y;

        Opponent(float x, float y) {
            this.x = x;
            this.y = y;
        }

        void update(float dt, float speed) {
            float tx, ty;
            if (ball.x > FIELD_W / 2) {
                // Attack: chase the ball
                tx = ball.x;
                ty = ball.y;
            } else {
                // Defend: sit between the ball and our own goal (left)
                float dx = ball.x - 10;
                float dy = ball.y - FIELD_H / 2;
                float len = (float) Math.hypot(dx, dy);
                if (len < 1) { tx = FIELD_W / 2; ty = FIELD_H / 2; }
                else {
                    tx = ball.x - dx / len * 70;
                    ty = ball.y - dy / len * 70;
                }
            }
            tx = Math.max(OPPONENT_R, Math.min(FIELD_W - OPPONENT_R, tx));
            ty = Math.max(OPPONENT_R, Math.min(FIELD_H - OPPONENT_R, ty));
            float dx = tx - x;
            float dy = ty - y;
            float len = (float) Math.hypot(dx, dy);
            if (len > 2) {
                x += dx / len * speed * dt;
                y += dy / len * speed * dt;
            }
        }
    }

    private class Keeper {
        float x, y;

        Keeper(float x, float y) {
            this.x = x;
            this.y = y;
        }

        void update(float dt, float speed) {
            float targetY = FIELD_H / 2;
            if (ball.x > FIELD_W * 0.4f) {
                targetY = ball.y;
            }
            targetY = Math.max(FIELD_H / 2 - GOAL_HALF + 10,
                Math.min(FIELD_H / 2 + GOAL_HALF - 10, targetY));
            float dy = targetY - y;
            float step = Math.signum(dy) * Math.min(Math.abs(dy), speed * dt);
            y += step;
            x = FIELD_W - 16f;
        }
    }

    private static class Ball {
        float x, y, vx, vy;

        Ball(float x, float y) {
            this.x = x;
            this.y = y;
        }

        void update(float dt) {
            float decay = (float) Math.exp(-BALL_DECAY * dt);
            vx *= decay;
            vy *= decay;
            x += vx * dt;
            y += vy * dt;
            if (y < BALL_R) { y = BALL_R; vy = -vy * 0.6f; }
            if (y > FIELD_H - BALL_R) { y = FIELD_H - BALL_R; vy = -vy * 0.6f; }
        }

        float distanceTo(float px, float py) {
            float dx = x - px;
            float dy = y - py;
            return (float) Math.hypot(dx, dy);
        }
    }
}