package com.gameverse.player;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a player in the GameVerse platform.
 * Contains player information and statistics.
 */
public class Player {
    
    private String username;
    private int level;
    private int xp;
    private int coins;
    private int gamesPlayed;
    private int wins;
    private int losses;
    private int draws;
    private Map<String, Integer> gameHighScores;
    private Map<String, Integer> gameWins;
    private long joinDate;
    private long lastPlayedDate;
    
    public Player(String username) {
        this.username = username;
        this.level = 1;
        this.xp = 0;
        this.coins = 0;
        this.gamesPlayed = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.gameHighScores = new HashMap<>();
        this.gameWins = new HashMap<>();
        this.joinDate = System.currentTimeMillis();
        this.lastPlayedDate = joinDate;
    }
    
    // Getters
    
    public String getUsername() {
        return username;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getXp() {
        return xp;
    }
    
    public int getCoins() {
        return coins;
    }
    
    public int getGamesPlayed() {
        return gamesPlayed;
    }
    
    public int getWins() {
        return wins;
    }
    
    public int getLosses() {
        return losses;
    }
    
    public int getDraws() {
        return draws;
    }
    
    public long getJoinDate() {
        return joinDate;
    }
    
    public long getLastPlayedDate() {
        return lastPlayedDate;
    }
    
    public double getWinRate() {
        if (gamesPlayed == 0) {
            return 0.0;
        }
        return (double) wins / gamesPlayed * 100;
    }
    
    // Setters for XP and Coins
    
    public void addXp(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("XP amount must be positive");
        }
        this.xp += amount;
    }
    
    public void addCoins(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Coin amount cannot be negative");
        }
        this.coins += amount;
    }
    
    public void removeCoins(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Coin amount cannot be negative");
        }
        if (this.coins < amount) {
            throw new IllegalArgumentException("Insufficient coins");
        }
        this.coins -= amount;
    }
    
    public void setLevel(int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Level must be at least 1");
        }
        this.level = level;
    }
    
    // Game Statistics
    
    public void recordGamePlay(int score, boolean won, boolean drawn) {
        this.gamesPlayed++;
        this.lastPlayedDate = System.currentTimeMillis();
        
        if (drawn) {
            this.draws++;
        } else if (won) {
            this.wins++;
        } else {
            this.losses++;
        }
    }
    
    public void recordGameWin(String gameName) {
        gameWins.put(gameName, gameWins.getOrDefault(gameName, 0) + 1);
    }
    
    public void recordHighScore(String gameName, int score) {
        int currentHigh = gameHighScores.getOrDefault(gameName, 0);
        if (score > currentHigh) {
            gameHighScores.put(gameName, score);
        }
    }
    
    public int getGameHighScore(String gameName) {
        return gameHighScores.getOrDefault(gameName, 0);
    }
    
    public int getGameWins(String gameName) {
        return gameWins.getOrDefault(gameName, 0);
    }
    
    public Map<String, Integer> getAllGameHighScores() {
        return new HashMap<>(gameHighScores);
    }
    
    public Map<String, Integer> getAllGameWins() {
        return new HashMap<>(gameWins);
    }
    
    @Override
    public String toString() {
        return String.format(
            "Player{username='%s', level=%d, xp=%d, coins=%d, gamesPlayed=%d, wins=%d, losses=%d, winRate=%.1f%%}",
            username, level, xp, coins, gamesPlayed, wins, losses, getWinRate()
        );
    }
}
