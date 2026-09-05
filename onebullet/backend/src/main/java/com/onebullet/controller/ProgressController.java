package com.onebullet.controller;

import com.onebullet.model.LevelProgress;
import com.onebullet.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/players/{playerId}/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping
    public ResponseEntity<List<LevelProgress>> getProgress(@PathVariable Long playerId) {
        return ResponseEntity.ok(progressService.getPlayerProgress(playerId));
    }

    @PostMapping
    public ResponseEntity<?> saveProgress(@PathVariable Long playerId, @RequestBody Map<String, Object> request) {
        try {
            int levelNumber = (int) request.get("levelNumber");
            boolean completed = (boolean) request.get("completed");
            int score = request.get("score") != null ? (int) request.get("score") : 0;
            
            LevelProgress progress = progressService.saveProgress(playerId, levelNumber, completed, score);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{levelNumber}")
    public ResponseEntity<?> getLevelProgress(@PathVariable Long playerId, @PathVariable int levelNumber) {
        return progressService.getLevelProgress(playerId, levelNumber)
                .map(progress -> ResponseEntity.ok((Object) progress))
                .orElse(ResponseEntity.notFound().build());
    }
}
