package com.onebullet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "level_progress")
public class LevelProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Player player;

    @Column(nullable = false)
    private int levelNumber;

    @Column(nullable = false)
    private boolean completed = false;

    @Column(nullable = false)
    private int bestScore = 0;

    @Column(nullable = false)
    private int attempts = 0;

    private LocalDateTime completedAt;

    public LevelProgress() {}

    public LevelProgress(Player player, int levelNumber) {
        this.player = player;
        this.levelNumber = levelNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public int getLevelNumber() { return levelNumber; }
    public void setLevelNumber(int levelNumber) { this.levelNumber = levelNumber; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getBestScore() { return bestScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
