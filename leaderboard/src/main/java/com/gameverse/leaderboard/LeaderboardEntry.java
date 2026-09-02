package com.gameverse.leaderboard;

/**
 * Represents an entry in a leaderboard.
 */
public class LeaderboardEntry implements Comparable<LeaderboardEntry> {
    
    private String username;
    private int score;
    private int rank;
    private long timestamp;
    
    public LeaderboardEntry(String username, int score) {
        this.username = username;
        this.score = score;
        this.timestamp = System.currentTimeMillis();
        this.rank = 0;
    }
    
    public LeaderboardEntry(String username, int score, int rank) {
        this(username, score);
        this.rank = rank;
    }
    
    // Getters
    
    public String getUsername() {
        return username;
    }
    
    public int getScore() {
        return score;
    }
    
    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public int compareTo(LeaderboardEntry other) {
        // Compare by score in descending order (higher score first)
        if (this.score != other.score) {
            return Integer.compare(other.score, this.score);
        }
        // If scores are equal, earlier submission wins
        return Long.compare(this.timestamp, other.timestamp);
    }
    
    @Override
    public String toString() {
        return String.format(
            "#%d - %s: %d points",
            rank, username, score
        );
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeaderboardEntry)) return false;
        LeaderboardEntry that = (LeaderboardEntry) o;
        return username.equalsIgnoreCase(that.username);
    }
    
    @Override
    public int hashCode() {
        return username.toLowerCase().hashCode();
    }
}
