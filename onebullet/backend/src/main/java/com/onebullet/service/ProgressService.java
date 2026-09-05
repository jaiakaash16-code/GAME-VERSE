package com.onebullet.service;

import com.onebullet.model.LevelProgress;
import com.onebullet.model.Player;
import com.onebullet.repository.ProgressRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProgressService {

    private final ProgressRepository progressRepository;
    private final PlayerService playerService;

    public ProgressService(ProgressRepository progressRepository, PlayerService playerService) {
        this.progressRepository = progressRepository;
        this.playerService = playerService;
    }

    public List<LevelProgress> getPlayerProgress(Long playerId) {
        return progressRepository.findByPlayerId(playerId);
    }

    public LevelProgress saveProgress(Long playerId, int levelNumber, boolean completed, int score) {
        Player player = playerService.getPlayer(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        LevelProgress progress = progressRepository
                .findByPlayerIdAndLevelNumber(playerId, levelNumber)
                .orElse(new LevelProgress(player, levelNumber));

        progress.setAttempts(progress.getAttempts() + 1);
        
        if (completed) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            if (score > progress.getBestScore()) {
                progress.setBestScore(score);
            }
        }

        return progressRepository.save(progress);
    }

    public Optional<LevelProgress> getLevelProgress(Long playerId, int levelNumber) {
        return progressRepository.findByPlayerIdAndLevelNumber(playerId, levelNumber);
    }
}
