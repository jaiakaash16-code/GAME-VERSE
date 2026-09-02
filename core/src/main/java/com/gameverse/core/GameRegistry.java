package com.gameverse.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for all available games in the GameVerse platform.
 * Manages game registration and retrieval.
 */
public class GameRegistry {
    
    private static GameRegistry instance;
    private Map<String, Class<? extends Game>> gameRegistry;
    
    private GameRegistry() {
        this.gameRegistry = new HashMap<>();
    }
    
    /**
     * Get the singleton instance of GameRegistry
     * @return the GameRegistry instance
     */
    public static synchronized GameRegistry getInstance() {
        if (instance == null) {
            instance = new GameRegistry();
        }
        return instance;
    }
    
    /**
     * Register a game class
     * @param gameName the name identifier for the game
     * @param gameClass the class implementing the Game interface
     */
    public void registerGame(String gameName, Class<? extends Game> gameClass) {
        if (gameName == null || gameName.isEmpty()) {
            throw new IllegalArgumentException("Game name cannot be null or empty");
        }
        if (gameClass == null) {
            throw new IllegalArgumentException("Game class cannot be null");
        }
        gameRegistry.put(gameName.toLowerCase(), gameClass);
    }
    
    /**
     * Get a registered game class by name
     * @param gameName the name of the game
     * @return the game class, or null if not registered
     */
    public Class<? extends Game> getGame(String gameName) {
        return gameRegistry.get(gameName.toLowerCase());
    }
    
    /**
     * Check if a game is registered
     * @param gameName the name of the game
     * @return true if the game is registered
     */
    public boolean isGameRegistered(String gameName) {
        return gameRegistry.containsKey(gameName.toLowerCase());
    }
    
    /**
     * Get all registered game names
     * @return a set of all registered game names
     */
    public Set<String> getRegisteredGames() {
        return gameRegistry.keySet();
    }
    
    /**
     * Unregister a game
     * @param gameName the name of the game to unregister
     */
    public void unregisterGame(String gameName) {
        gameRegistry.remove(gameName.toLowerCase());
    }
    
    /**
     * Get the total number of registered games
     * @return the number of registered games
     */
    public int getGameCount() {
        return gameRegistry.size();
    }
    
    /**
     * Clear all registered games
     */
    public void clear() {
        gameRegistry.clear();
    }
}
