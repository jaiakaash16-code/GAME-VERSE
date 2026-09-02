package com.gameverse.core;

/**
 * Manager for controlling game lifecycle and transitions.
 * Handles starting, pausing, resuming, and stopping games.
 */
public class GameManager {
    
    private Game currentGame;
    private GameRegistry registry;
    private boolean isInitialized;
    
    public GameManager() {
        this.registry = GameRegistry.getInstance();
        this.currentGame = null;
        this.isInitialized = false;
    }
    
    /**
     * Load and instantiate a game by name
     * @param gameName the name of the game to load
     * @return true if the game was successfully loaded, false otherwise
     */
    public boolean loadGame(String gameName) {
        if (!registry.isGameRegistered(gameName)) {
            System.err.println("Game not registered: " + gameName);
            return false;
        }
        
        try {
            Class<? extends Game> gameClass = registry.getGame(gameName);
            currentGame = gameClass.getDeclaredConstructor().newInstance();
            currentGame.initialize();
            isInitialized = true;
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load game: " + gameName);
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Start the current game
     */
    public void startGame() {
        if (currentGame == null) {
            throw new IllegalStateException("No game loaded");
        }
        currentGame.start();
    }
    
    /**
     * Pause the current game
     */
    public void pauseGame() {
        if (currentGame == null) {
            throw new IllegalStateException("No game loaded");
        }
        currentGame.pause();
    }
    
    /**
     * Resume the current game
     */
    public void resumeGame() {
        if (currentGame == null) {
            throw new IllegalStateException("No game loaded");
        }
        currentGame.resume();
    }
    
    /**
     * Restart the current game
     */
    public void restartGame() {
        if (currentGame == null) {
            throw new IllegalStateException("No game loaded");
        }
        currentGame.restart();
    }
    
    /**
     * End the current game and get its result
     * @return the game result
     */
    public GameResult endGame() {
        if (currentGame == null) {
            throw new IllegalStateException("No game loaded");
        }
        GameResult result = currentGame.getResult();
        currentGame = null;
        return result;
    }
    
    /**
     * Update the current game
     * @param deltaTime time elapsed since last update in seconds
     */
    public void update(float deltaTime) {
        if (currentGame != null && currentGame.isRunning()) {
            currentGame.update(deltaTime);
        }
    }
    
    /**
     * Get the currently loaded game
     * @return the current game, or null if no game is loaded
     */
    public Game getCurrentGame() {
        return currentGame;
    }
    
    /**
     * Check if a game is currently loaded
     * @return true if a game is loaded
     */
    public boolean hasGameLoaded() {
        return currentGame != null;
    }
    
    /**
     * Check if a game is initialized
     * @return true if the game is initialized
     */
    public boolean isGameInitialized() {
        return isInitialized;
    }
}
