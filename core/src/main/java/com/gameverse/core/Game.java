package com.gameverse.core;

/**
 * Core interface that all games must implement.
 * This defines the contract for modular games in the GameVerse platform.
 */
public interface Game {
    
    /**
     * Get the name of the game
     * @return the game name
     */
    String getName();
    
    /**
     * Initialize the game (setup resources, etc.)
     */
    void initialize();
    
    /**
     * Start or restart the game
     */
    void start();
    
    /**
     * Pause the game
     */
    void pause();
    
    /**
     * Resume the game
     */
    void resume();
    
    /**
     * Restart the game
     */
    void restart();
    
    /**
     * Get the result of the current game session
     * @return the game result
     */
    GameResult getResult();
    
    /**
     * Update game state (called every frame)
     * @param deltaTime time elapsed since last update in seconds
     */
    void update(float deltaTime);
    
    /**
     * Check if the game is running
     * @return true if game is currently running
     */
    boolean isRunning();

    /**
     * Get the current score
     * @return the current score
     */
    int getScore();
}
