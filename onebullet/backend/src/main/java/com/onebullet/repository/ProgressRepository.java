package com.onebullet.repository;

import com.onebullet.model.LevelProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<LevelProgress, Long> {
    List<LevelProgress> findByPlayerId(Long playerId);
    Optional<LevelProgress> findByPlayerIdAndLevelNumber(Long playerId, int levelNumber);
}
