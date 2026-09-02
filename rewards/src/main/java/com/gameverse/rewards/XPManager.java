package com.gameverse.rewards;

import com.gameverse.player.Player;

/**
 * Manager for XP rewards and level progression.
 * Handles XP calculation and level ups.
 */
public class XPManager {
    
    private static XPManager instance;
    private int xpPerLevel;
    private int maxLevel;
    
    private XPManager() {
        this.xpPerLevel = 1000; // XP required to level up
        this.maxLevel = 100;
    }
    
    /**
     * Get the singleton instance of XPManager
     * @return the XPManager instance
     */
    public static synchronized XPManager getInstance() {
        if (instance == null) {
            instance = new XPManager();
        }
        return instance;
    }
    
    /**
     * Calculate XP reward based on game score
     * Formula: score / 10 (minimum 10 XP)
     * @param score the game score
     * @return the XP reward amount
     */
    public int calculateXpReward(int score) {
        return Math.max(10, score / 10);
    }
    
    /**
     * Calculate XP reward based on game result and duration
     * @param score the game score
     * @param won whether the player won
     * @param durationSeconds the game duration in seconds
     * @return the XP reward amount
     */
    public int calculateXpReward(int score, boolean won, long durationSeconds) {
        int baseXp = calculateXpReward(score);
        
        if (won) {
            baseXp = (int) (baseXp * 1.5); // 50% bonus for winning
        }
        
        // Bonus for game duration (at least 2 minutes)
        if (durationSeconds >= 120) {
            baseXp += 50;
        }
        
        return baseXp;
    }
    
    /**
     * Award XP to a player and check for level up
     * @param player the player to award XP to
     * @param xpAmount the amount of XP to award
     * @return true if the player leveled up, false otherwise
     */
    public boolean awardXp(Player player, int xpAmount) {
        if (player == null || xpAmount <= 0) {
            throw new IllegalArgumentException("Invalid parameters");
        }
        
        player.addXp(xpAmount);
        return checkLevelUp(player);
    }
    
    /**
     * Check if a player should level up and update their level
     * @param player the player to check
     * @return true if the player leveled up, false otherwise
     */
    public boolean checkLevelUp(Player player) {
        if (player.getLevel() >= maxLevel) {
            return false;
        }
        
        int requiredXp = player.getLevel() * xpPerLevel;
        
        if (player.getXp() >= requiredXp) {
            player.setLevel(player.getLevel() + 1);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get the XP required to reach the next level
     * @param player the player
     * @return the XP required to level up
     */
    public int getXpToNextLevel(Player player) {
        if (player.getLevel() >= maxLevel) {
            return -1; // Max level reached
        }
        
        int requiredXp = player.getLevel() * xpPerLevel;
        return Math.max(0, requiredXp - player.getXp());
    }
    
    /**
     * Get the total XP required to reach a specific level
     * @param level the target level
     * @return the total XP required
     */
    public int getTotalXpForLevel(int level) {
        if (level < 1 || level > maxLevel) {
            throw new IllegalArgumentException("Invalid level");
        }
        
        int totalXp = 0;
        for (int i = 1; i < level; i++) {
            totalXp += i * xpPerLevel;
        }
        return totalXp;
    }
    
    /**
     * Set the XP required per level
     * @param xpPerLevel the new XP per level value
     */
    public void setXpPerLevel(int xpPerLevel) {
        if (xpPerLevel <= 0) {
            throw new IllegalArgumentException("XP per level must be positive");
        }
        this.xpPerLevel = xpPerLevel;
    }
    
    /**
     * Get the XP required per level
     * @return the XP per level value
     */
    public int getXpPerLevel() {
        return xpPerLevel;
    }
    
    /**
     * Get the maximum level
     * @return the maximum level
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Set the maximum level
     * @param maxLevel the new maximum level
     */
    public void setMaxLevel(int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Max level must be at least 1");
        }
        this.maxLevel = maxLevel;
    }
}
