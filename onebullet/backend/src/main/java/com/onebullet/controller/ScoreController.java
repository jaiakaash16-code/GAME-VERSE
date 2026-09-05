package com.onebullet.controller;

import com.onebullet.model.Score;
import com.onebullet.service.ScoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping
    public ResponseEntity<?> submitScore(@RequestBody Map<String, Object> request) {
        try {
            Long playerId = Long.valueOf(request.get("playerId").toString());
            int levelNumber = (int) request.get("levelNumber");
            int score = (int) request.get("score");
            long timeMs = request.get("timeMs") != null ? Long.valueOf(request.get("timeMs").toString()) : 0;
            int bulletsUsed = request.get("bulletsUsed") != null ? (int) request.get("bulletsUsed") : 1;
            
            Score newScore = scoreService.submitScore(playerId, levelNumber, score, timeMs, bulletsUsed);
            return ResponseEntity.ok(newScore);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Score>> getLeaderboard() {
        return ResponseEntity.ok(scoreService.getLeaderboard());
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Score>> getPlayerScores(@PathVariable Long playerId) {
        return ResponseEntity.ok(scoreService.getPlayerScores(playerId));
    }

    @GetMapping("/level/{levelNumber}")
    public ResponseEntity<List<Score>> getLevelScores(@PathVariable int levelNumber) {
        return ResponseEntity.ok(scoreService.getLevelScores(levelNumber));
    }
}
