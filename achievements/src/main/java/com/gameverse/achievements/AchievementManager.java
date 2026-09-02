package com.gameverse.achievements;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manager for achievements in the GameVerse platform.
 * Handles achievement creation, granting, and tracking.
 */
public class AchievementManager {
    
    private static AchievementManager instance;
    private Map<String, Achievement> achievements;
    private Map<String, Set<String>> playerAchievements; // username -> set of achievement IDs
    
    private AchievementManager() {
        this.achievements = new HashMap<>();
        this.playerAchievements = new HashMap<>();
        initializeDefaultAchievements();
    }
    
    /**
     * Get the singleton instance of AchievementManager
     * @return the AchievementManager instance
     */
    public static synchronized AchievementManager getInstance() {
        if (instance == null) {
            instance = new AchievementManager();
        }
        return instance;
    }
    
    /**
     * Initialize default achievements
     */
    private void initializeDefaultAchievements() {
        // First Victory
        registerAchievement(new Achievement(
            "FIRST_VICTORY",
            "First Victory",
            "Win your first game",
            50,
            25
        ));
        
        // Game Explorer
        registerAchievement(new Achievement(
            "GAME_EXPLORER",
            "Game Explorer",
            "Play 5 different games",
            100,
            50
        ));
        
        // Winning Streak
        registerAchievement(new Achievement(
            "WINNING_STREAK",
            "Winning Streak",
            "Win 5 games in a row",
            150,
            75
        ));
        
        // Snake Master
        registerAchievement(new Achievement(
            "SNAKE_MASTER",
            "Snake Master",
            "Achieve 1000+ score in Snake",
            200,
            100
        ));
        
        // Chess Beginner
        registerAchievement(new Achievement(
            "CHESS_BEGINNER",
            "Chess Beginner",
            "Win your first Chess game",
            75,
            40
        ));
        
        // Multi-Game Champion
        registerAchievement(new Achievement(
            "MULTI_GAME_CHAMPION",
            "Multi-Game Champion",
            "Win 10 games across different games",
            300,
            150
        ));
    }
    
    /**
     * Register a new achievement
     * @param achievement the achievement to register
     */
    public void registerAchievement(Achievement achievement) {
        if (achievement == null || achievement.getId() == null) {
            throw new IllegalArgumentException("Achievement cannot be null");
        }
        achievements.put(achievement.getId(), achievement);
    }
    
    /**
     * Get an achievement by ID
     * @param id the achievement ID
     * @return the achievement, or null if not found
     */
    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }
    
    /**
     * Grant an achievement to a player
     * @param username the username of the player
     * @param achievementId the ID of the achievement
     * @return true if the achievement was granted (new), false if already owned
     */
    public boolean grantAchievement(String username, String achievementId) {
        if (!achievements.containsKey(achievementId)) {
            throw new IllegalArgumentException("Achievement not found: " + achievementId);
        }
        
        Set<String> playerAchievs = playerAchievements.computeIfAbsent(username, k -> new HashSet<>());
        return playerAchievs.add(achievementId); // Returns true if not already present
    }
    
    /**
     * Check if a player has an achievement
     * @param username the username of the player
     * @param achievementId the ID of the achievement
     * @return true if the player has the achievement
     */
    public boolean hasAchievement(String username, String achievementId) {
        Set<String> playerAchievs = playerAchievements.get(username);
        return playerAchievs != null && playerAchievs.contains(achievementId);
    }
    
    /**
     * Get all achievements for a player
     * @param username the username of the player
     * @return a set of achievement IDs owned by the player
     */
    public Set<String> getPlayerAchievements(String username) {
        return new HashSet<>(playerAchievements.getOrDefault(username, new HashSet<>()));
    }
    
    /**
     * Get the count of achievements for a player
     * @param username the username of the player
     * @return the number of achievements owned by the player
     */
    public int getPlayerAchievementCount(String username) {
        return playerAchievements.getOrDefault(username, new HashSet<>()).size();
    }
    
    /**
     * Get all registered achievements
     * @return a map of all achievements
     */
    public Map<String, Achievement> getAllAchievements() {
        return new HashMap<>(achievements);
    }
    
    /**
     * Get the total number of achievements
     * @return the total number of achievements
     */
    public int getTotalAchievementCount() {
        return achievements.size();
    }
    
    /**
     * Revoke an achievement from a player
     * @param username the username of the player
     * @param achievementId the ID of the achievement
     * @return true if the achievement was revoked, false if not owned
     */
    public boolean revokeAchievement(String username, String achievementId) {
        Set<String> playerAchievs = playerAchievements.get(username);
        return playerAchievs != null && playerAchievs.remove(achievementId);
    }
    
    /**
     * Clear all player achievements
     * @param username the username of the player
     */
    public void clearPlayerAchievements(String username) {
        playerAchievements.remove(username);
    }
}
