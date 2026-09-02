package com.gameverse.games.core;

import com.gameverse.core.Difficulty;
import com.gameverse.core.Game;
import com.gameverse.core.GameResult;

/**
 * Abstract base class for games.
 * Provides common game functionality and state management.
 */
public abstract class BaseGame implements Game {
    
    protected String name;
    protected GameResult result;
    protected boolean isRunning;
    protected boolean isPaused;
    protected long startTime;
    protected int score;
    protected Difficulty difficulty;
    
    protected BaseGame(String name) {
        this.name = name;
        this.isRunning = false;
        this.isPaused = false;
        this.score = 0;
        this.startTime = 0;
        this.difficulty = Difficulty.MEDIUM;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void initialize() {
        // Override in subclasses
    }
    
    @Override
    public void start() {
        isRunning = true;
        isPaused = false;
        startTime = System.currentTimeMillis();
    }
    
    @Override
    public void pause() {
        if (isRunning && !isPaused) {
            isPaused = true;
        }
    }
    
    @Override
    public void resume() {
        if (isRunning && isPaused) {
            isPaused = false;
        }
    }
    
    @Override
    public void restart() {
        isRunning = false;
        isPaused = false;
        score = 0;
        startTime = 0;
    }
    
    @Override
    public boolean isRunning() {
        return isRunning && !isPaused;
    }
    
    @Override
    public GameResult getResult() {
        return result;
    }
    
    /**
     * Set the difficulty level for this game.
     * @param difficulty the difficulty level
     */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }
    
    /**
     * Get the current difficulty level.
     * @return the difficulty level
     */
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    /**
     * Get the elapsed time since the game started
     * @return the elapsed time in milliseconds
     */
    protected long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * Set the current score
     * @param score the score value
     */
    protected void setScore(int score) {
        this.score = score;
    }
    
    /**
     * Get the current score
     * @return the current score
     */
    public int getScore() {
        return score;
    }
}
