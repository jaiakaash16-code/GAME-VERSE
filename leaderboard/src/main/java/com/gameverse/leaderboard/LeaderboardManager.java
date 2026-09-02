package com.gameverse.leaderboard;

import java.util.*;

/**
 * Manager for game leaderboards.
 * Maintains global and game-specific leaderboards.
 */
public class LeaderboardManager {
    
    private static LeaderboardManager instance;
    private Map<String, PriorityQueue<LeaderboardEntry>> gameLeaderboards;
    private PriorityQueue<LeaderboardEntry> globalLeaderboard;
    private int maxEntries;
    
    private LeaderboardManager() {
        this.gameLeaderboards = new HashMap<>();
        this.globalLeaderboard = new PriorityQueue<>();
        this.maxEntries = 100;
    }
    
    /**
     * Get the singleton instance of LeaderboardManager
     * @return the LeaderboardManager instance
     */
    public static synchronized LeaderboardManager getInstance() {
        if (instance == null) {
            instance = new LeaderboardManager();
        }
        return instance;
    }
    
    /**
     * Submit a score to a game-specific leaderboard
     * @param gameName the name of the game
     * @param username the username of the player
     * @param score the score to submit
     */
    public void submitScore(String gameName, String username, int score) {
        if (gameName == null || gameName.isEmpty() || username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        
        // Add to game-specific leaderboard
        PriorityQueue<LeaderboardEntry> gameLeaderboard = gameLeaderboards
            .computeIfAbsent(gameName, k -> new PriorityQueue<>());
        
        LeaderboardEntry entry = new LeaderboardEntry(username, score);
        gameLeaderboard.add(entry);
        
        // Limit to max entries
        if (gameLeaderboard.size() > maxEntries) {
            List<LeaderboardEntry> entries = new ArrayList<>(gameLeaderboard);
            entries.sort(Comparator.naturalOrder());
            gameLeaderboard.clear();
            gameLeaderboard.addAll(entries.subList(0, maxEntries));
        }
        
        // Add to global leaderboard
        globalLeaderboard.add(entry);
        if (globalLeaderboard.size() > maxEntries) {
            List<LeaderboardEntry> entries = new ArrayList<>(globalLeaderboard);
            entries.sort(Comparator.naturalOrder());
            globalLeaderboard.clear();
            globalLeaderboard.addAll(entries.subList(0, maxEntries));
        }
    }
    
    /**
     * Get the top entries from a game-specific leaderboard
     * @param gameName the name of the game
     * @param limit the number of entries to return
     * @return a list of top entries, sorted by rank
     */
    public List<LeaderboardEntry> getGameLeaderboard(String gameName, int limit) {
        PriorityQueue<LeaderboardEntry> gameLeaderboard = gameLeaderboards.get(gameName);
        
        if (gameLeaderboard == null || gameLeaderboard.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<LeaderboardEntry> entries = new ArrayList<>(gameLeaderboard);
        entries.sort(Comparator.naturalOrder());
        
        // Assign ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
        
        return entries.subList(0, Math.min(limit, entries.size()));
    }
    
    /**
     * Get the global leaderboard
     * @param limit the number of entries to return
     * @return a list of top global entries, sorted by rank
     */
    public List<LeaderboardEntry> getGlobalLeaderboard(int limit) {
        if (globalLeaderboard.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<LeaderboardEntry> entries = new ArrayList<>(globalLeaderboard);
        entries.sort(Comparator.naturalOrder());
        
        // Assign ranks
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }
        
        return entries.subList(0, Math.min(limit, entries.size()));
    }
    
    /**
     * Get a player's rank in a game-specific leaderboard
     * @param gameName the name of the game
     * @param username the username of the player
     * @return the player's rank, or -1 if not found
     */
    public int getPlayerRankInGame(String gameName, String username) {
        List<LeaderboardEntry> leaderboard = getGameLeaderboard(gameName, maxEntries);
        
        for (LeaderboardEntry entry : leaderboard) {
            if (entry.getUsername().equalsIgnoreCase(username)) {
                return entry.getRank();
            }
        }
        
        return -1;
    }
    
    /**
     * Get a player's rank in the global leaderboard
     * @param username the username of the player
     * @return the player's rank, or -1 if not found
     */
    public int getPlayerGlobalRank(String username) {
        List<LeaderboardEntry> leaderboard = getGlobalLeaderboard(maxEntries);
        
        for (LeaderboardEntry entry : leaderboard) {
            if (entry.getUsername().equalsIgnoreCase(username)) {
                return entry.getRank();
            }
        }
        
        return -1;
    }
    
    /**
     * Get all game names with leaderboards
     * @return a set of game names
     */
    public Set<String> getAllGameLeaderboards() {
        return new HashSet<>(gameLeaderboards.keySet());
    }
    
    /**
     * Clear all leaderboards
     */
    public void clearAllLeaderboards() {
        gameLeaderboards.clear();
        globalLeaderboard.clear();
    }
    
    /**
     * Set the maximum number of entries per leaderboard
     * @param maxEntries the maximum entries
     */
    public void setMaxEntries(int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("Max entries must be at least 1");
        }
        this.maxEntries = maxEntries;
    }
    
    /**
     * Get the maximum number of entries per leaderboard
     * @return the maximum entries
     */
    public int getMaxEntries() {
        return maxEntries;
    }
}
