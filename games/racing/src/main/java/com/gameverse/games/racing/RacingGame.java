package com.gameverse.games.racing;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;
import java.util.ArrayList;
import java.util.List;

/**
 * Racing game with difficulty-based AI opponents.
 * Easy: no AI cars, just reach the finish
 * Medium: 2 AI cars with moderate speed
 * Hard: 3 AI cars with high speed, aggressive driving
 */
public class RacingGame extends BaseGame {
    
    private int trackLength;
    private int playerPosition;
    private int playerSpeed;
    private int maxSpeed;
    private boolean raceFinished;
    
    // AI cars
    private List<AICar> aiCars;
    private boolean hasAICars;
    
    public RacingGame() {
        super("Mini Racing");
        this.trackLength = 1000;
        this.playerPosition = 0;
        this.playerSpeed = 0;
        this.maxSpeed = 10;
        this.raceFinished = false;
        this.aiCars = new ArrayList<>();
        this.hasAICars = false;
    }
    
    @Override
    public void initialize() {
        playerPosition = 0;
        playerSpeed = 0;
        raceFinished = false;
        setupDifficulty();
    }
    
    @Override
    public void start() {
        super.start();
        initialize();
    }
    
    @Override
    public void restart() {
        super.restart();
        initialize();
    }
    
    @Override
    public void update(float deltaTime) {
        if (!isRunning()) {
            return;
        }
        
        // Update player position based on speed
        playerPosition += playerSpeed * deltaTime * 100;
        score = (int) playerPosition;
        
        // Update AI cars
        if (hasAICars) {
            for (AICar car : aiCars) {
                car.update(deltaTime);
                
                // AI cars reach finish
                if (car.position >= trackLength && !raceFinished) {
                    // AI finished first — player loses
                    finishRace(false);
                    return;
                }
            }
        }
        
        // Check if player finished
        if (playerPosition >= trackLength) {
            finishRace(true);
        }
    }
    
    private void setupDifficulty() {
        aiCars.clear();
        switch (difficulty) {
            case EASY -> {
                hasAICars = false;
                maxSpeed = 10;
            }
            case MEDIUM -> {
                hasAICars = true;
                maxSpeed = 8;
                // 2 AI cars with moderate speed
                aiCars.add(new AICar(4.0 + Math.random() * 2.0, 30));  // Lane 1
                aiCars.add(new AICar(4.5 + Math.random() * 1.5, 60));  // Lane 2
            }
            case HARD -> {
                hasAICars = true;
                maxSpeed = 7;
                // 3 AI cars with high speed
                aiCars.add(new AICar(5.0 + Math.random() * 2.0, 25));  // Lane 1
                aiCars.add(new AICar(5.5 + Math.random() * 1.5, 50));  // Lane 2
                aiCars.add(new AICar(6.0 + Math.random() * 1.0, 75));  // Lane 3
            }
        }
    }
    
    /**
     * Accelerate the car
     */
    public void accelerate() {
        playerSpeed = Math.min(maxSpeed, playerSpeed + 1);
    }
    
    /**
     * Decelerate the car
     */
    public void decelerate() {
        playerSpeed = Math.max(0, playerSpeed - 1);
    }
    
    /**
     * Steer left
     */
    public void steerLeft() {
        // Steering logic would affect lane position
    }
    
    /**
     * Steer right
     */
    public void steerRight() {
        // Steering logic would affect lane position
    }
    
    private void finishRace(boolean playerWon) {
        raceFinished = true;
        isRunning = false;
        long elapsedSeconds = getElapsedTime() / 1000;
        
        if (playerWon) {
            score = (int) (100 * maxSpeed) / Math.max(1, (int) elapsedSeconds);
            // Bonus for beating AI
            if (hasAICars) score += 200;
            result = new GameResult(name, GameResult.Status.WON, score, getElapsedTime());
        } else {
            score = Math.max(0, (int) (50 * maxSpeed) / Math.max(1, (int) elapsedSeconds));
            result = new GameResult(name, GameResult.Status.LOST, score, getElapsedTime());
        }
    }
    
    public int getPlayerPosition() {
        return playerPosition;
    }
    
    public int getPlayerSpeed() {
        return playerSpeed;
    }
    
    public boolean isRaceFinished() {
        return raceFinished;
    }
    
    /** Check if AI cars are active */
    public boolean hasAICars() {
        return hasAICars;
    }
    
    /** Get list of AI cars for rendering */
    public List<AICar> getAICars() {
        return aiCars;
    }
    
    /** AI Car inner class */
    public static class AICar {
        public double position;
        public double speed;
        public int lane; // vertical position offset (0-100)
        
        AICar(double speed, int lane) {
            this.position = 0;
            this.speed = speed;
            this.lane = lane;
        }
        
        void update(float deltaTime) {
            position += speed * deltaTime * 100;
            // Add some speed variation
            speed += (Math.random() - 0.5) * 0.5;
            speed = Math.max(2.0, Math.min(8.0, speed));
        }
    }
}
