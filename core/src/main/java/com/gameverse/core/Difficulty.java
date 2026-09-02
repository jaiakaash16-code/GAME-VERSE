package com.gameverse.core;

/**
 * Difficulty levels for games.
 * Controls AI intelligence, speed, and challenge.
 */
public enum Difficulty {
    EASY("Easy", 0.3),
    MEDIUM("Medium", 0.6),
    HARD("Hard", 1.0);

    private final String displayName;
    private final double aiStrength; // 0.0 to 1.0 — used by AI logic

    Difficulty(String displayName, double aiStrength) {
        this.displayName = displayName;
        this.aiStrength = aiStrength;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getAiStrength() {
        return aiStrength;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
