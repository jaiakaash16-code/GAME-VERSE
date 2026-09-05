package com.onebullet.service;

import com.onebullet.model.Score;
import com.onebullet.repository.ScoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final PlayerService playerService;

    public ScoreService(ScoreRepository scoreRepository, PlayerService playerService) {
        this.scoreRepository = scoreRepository;
        this.playerService = playerService;
    }

    public Score submitScore(Long playerId, int levelNumber, int score, long timeMs, int bulletsUsed) {
        playerService.getPlayer(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found"));

        var player = playerService.getPlayer(playerId).get();
        Score newScore = new Score(player, levelNumber, score, timeMs, bulletsUsed);
        
        Score saved = scoreRepository.save(newScore);
        
        // Update player total score
        playerService.updateScore(playerId, score);
        
        return saved;
    }

    public List<Score> getLeaderboard() {
        return scoreRepository.findTop10ByOrderByScoreDesc(PageRequest.of(0, 10));
    }

    public List<Score> getPlayerScores(Long playerId) {
        return scoreRepository.findByPlayerIdOrderByScoreDesc(playerId);
    }

    public List<Score> getLevelScores(int levelNumber) {
        return scoreRepository.findByLevelNumberOrderByScoreDesc(levelNumber);
    }
}
