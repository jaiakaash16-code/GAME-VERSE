package com.gameverse.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manager for player accounts and profiles.
 * Handles player creation, retrieval, and updates.
 */
public class PlayerManager {
    
    private static PlayerManager instance;
    private Map<String, Player> players;
    private String currentPlayerUsername;
    
    private PlayerManager() {
        this.players = new HashMap<>();
        this.currentPlayerUsername = null;
    }
    
    /**
     * Get the singleton instance of PlayerManager
     * @return the PlayerManager instance
     */
    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }
    
    /**
     * Create a new player
     * @param username the username for the new player
     * @return the created player, or null if username already exists
     */
    public Player createPlayer(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        
        String normalizedUsername = username.toLowerCase();
        if (players.containsKey(normalizedUsername)) {
            return null; // Username already exists
        }
        
        Player player = new Player(username);
        players.put(normalizedUsername, player);
        return player;
    }
    
    /**
     * Get a player by username
     * @param username the username of the player
     * @return the player, or null if not found
     */
    public Player getPlayer(String username) {
        if (username == null) {
            return null;
        }
        return players.get(username.toLowerCase());
    }
    
    /**
     * Set the current active player
     * @param username the username of the player to set as current
     * @return true if the player was set, false if player not found
     */
    public boolean setCurrentPlayer(String username) {
        if (username == null) {
            currentPlayerUsername = null;
            return true;
        }
        
        Player player = getPlayer(username);
        if (player == null) {
            return false;
        }
        
        this.currentPlayerUsername = username.toLowerCase();
        return true;
    }
    
    /**
     * Get the current active player
     * @return the current player, or null if no player is set
     */
    public Player getCurrentPlayer() {
        if (currentPlayerUsername == null) {
            return null;
        }
        return getPlayer(currentPlayerUsername);
    }
    
    /**
     * Check if a player exists
     * @param username the username to check
     * @return true if the player exists
     */
    public boolean playerExists(String username) {
        return username != null && players.containsKey(username.toLowerCase());
    }
    
    /**
     * Delete a player
     * @param username the username of the player to delete
     * @return true if the player was deleted, false if not found
     */
    public boolean deletePlayer(String username) {
        if (username == null) {
            return false;
        }
        
        String normalizedUsername = username.toLowerCase();
        if (normalizedUsername.equals(currentPlayerUsername)) {
            currentPlayerUsername = null;
        }
        
        return players.remove(normalizedUsername) != null;
    }
    
    /**
     * Get all player usernames
     * @return a set of all player usernames
     */
    public Set<String> getAllPlayerUsernames() {
        return new HashMap<>(players).keySet();
    }
    
    /**
     * Get the total number of players
     * @return the number of players
     */
    public int getPlayerCount() {
        return players.size();
    }
    
    /**
     * Get all players sorted by level (descending)
     * @return a sorted list of players
     */
    public java.util.List<Player> getPlayersByLevel() {
        return players.values().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getLevel(), p1.getLevel()))
            .toList();
    }
    
    /**
     * Get all players sorted by XP (descending)
     * @return a sorted list of players
     */
    public java.util.List<Player> getPlayersByXp() {
        return players.values().stream()
            .sorted((p1, p2) -> Integer.compare(p2.getXp(), p1.getXp()))
            .toList();
    }
    
    /**
     * Clear all players
     */
    public void clearAllPlayers() {
        players.clear();
        currentPlayerUsername = null;
    }
}
