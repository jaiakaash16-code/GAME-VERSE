package com.gameverse.core;

/**
 * Represents the result of a game session.
 * Contains score, result status, and duration information.
 */
public class GameResult {
    
    public enum Status {
        WON,
        LOST,
        DRAWN,
        COMPLETED,
        ABANDONED
    }
    
    private String gameName;
    private Status status;
    private int score;
    private long duration; // in milliseconds
    private boolean isNewHighScore;
    private long timestamp;
    
    public GameResult(String gameName, Status status, int score, long duration) {
        this.gameName = gameName;
        this.status = status;
        this.score = score;
        this.duration = duration;
        this.timestamp = System.currentTimeMillis();
        this.isNewHighScore = false;
    }
    
    // Getters and Setters
    
    public String getGameName() {
        return gameName;
    }
    
    public void setGameName(String gameName) {
        this.gameName = gameName;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void setStatus(Status status) {
        this.status = status;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public boolean isNewHighScore() {
        return isNewHighScore;
    }
    
    public void setNewHighScore(boolean newHighScore) {
        isNewHighScore = newHighScore;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return String.format(
            "GameResult{game='%s', status=%s, score=%d, duration=%dms, newHighScore=%b}",
            gameName, status, score, duration, isNewHighScore
        );
    }
}
