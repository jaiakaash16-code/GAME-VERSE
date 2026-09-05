package com.onebullet.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scores")
public class Score {

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
    private int score;

    @Column(nullable = false)
    private long timeMs;

    @Column(nullable = false)
    private int bulletsUsed;

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    public Score() {}

    public Score(Player player, int levelNumber, int score, long timeMs, int bulletsUsed) {
        this.player = player;
        this.levelNumber = levelNumber;
        this.score = score;
        this.timeMs = timeMs;
        this.bulletsUsed = bulletsUsed;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public int getLevelNumber() { return levelNumber; }
    public void setLevelNumber(int levelNumber) { this.levelNumber = levelNumber; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public long getTimeMs() { return timeMs; }
    public void setTimeMs(long timeMs) { this.timeMs = timeMs; }

    public int getBulletsUsed() { return bulletsUsed; }
    public void setBulletsUsed(int bulletsUsed) { this.bulletsUsed = bulletsUsed; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
