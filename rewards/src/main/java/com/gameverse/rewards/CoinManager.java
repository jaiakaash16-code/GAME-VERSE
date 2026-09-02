package com.gameverse.rewards;

import com.gameverse.player.Player;

/**
 * Manager for virtual coin rewards and transactions.
 * Handles coin rewards and player coin balance.
 */
public class CoinManager {
    
    private static CoinManager instance;
    private int winReward;
    private int achievementReward;
    private int highScoreReward;
    private int dailyMissionReward;
    
    private CoinManager() {
        this.winReward = 50;
        this.achievementReward = 100;
        this.highScoreReward = 75;
        this.dailyMissionReward = 25;
    }
    
    /**
     * Get the singleton instance of CoinManager
     * @return the CoinManager instance
     */
    public static synchronized CoinManager getInstance() {
        if (instance == null) {
            instance = new CoinManager();
        }
        return instance;
    }
    
    /**
     * Calculate coin reward based on game result
     * @param won whether the player won the game
     * @param newHighScore whether it's a new high score
     * @return the coin reward amount
     */
    public int calculateCoinReward(boolean won, boolean newHighScore) {
        int coins = 0;
        
        if (won) {
            coins += winReward;
        }
        
        if (newHighScore) {
            coins += highScoreReward;
        }
        
        return coins;
    }
    
    /**
     * Award coins to a player for winning
     * @param player the player to award coins to
     * @param won whether the player won
     * @param newHighScore whether it's a new high score
     */
    public void awardGameReward(Player player, boolean won, boolean newHighScore) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        
        int coins = calculateCoinReward(won, newHighScore);
        if (coins > 0) {
            player.addCoins(coins);
        }
    }
    
    /**
     * Award coins for achievement unlock
     * @param player the player to award coins to
     */
    public void awardAchievementReward(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        player.addCoins(achievementReward);
    }
    
    /**
     * Award coins for daily mission completion
     * @param player the player to award coins to
     */
    public void awardDailyMissionReward(Player player) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        player.addCoins(dailyMissionReward);
    }
    
    /**
     * Set the win reward amount
     * @param amount the new win reward
     */
    public void setWinReward(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Win reward cannot be negative");
        }
        this.winReward = amount;
    }
    
    /**
     * Set the achievement reward amount
     * @param amount the new achievement reward
     */
    public void setAchievementReward(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Achievement reward cannot be negative");
        }
        this.achievementReward = amount;
    }
    
    /**
     * Set the high score reward amount
     * @param amount the new high score reward
     */
    public void setHighScoreReward(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("High score reward cannot be negative");
        }
        this.highScoreReward = amount;
    }
    
    /**
     * Set the daily mission reward amount
     * @param amount the new daily mission reward
     */
    public void setDailyMissionReward(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Daily mission reward cannot be negative");
        }
        this.dailyMissionReward = amount;
    }
    
    // Getters
    
    public int getWinReward() {
        return winReward;
    }
    
    public int getAchievementReward() {
        return achievementReward;
    }
    
    public int getHighScoreReward() {
        return highScoreReward;
    }
    
    public int getDailyMissionReward() {
        return dailyMissionReward;
    }
}
