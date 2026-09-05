package com.onebullet.repository;

import com.onebullet.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByPlayerIdOrderByScoreDesc(Long playerId);
    
    @Query("SELECT s FROM Score s ORDER BY s.score DESC")
    List<Score> findTop10ByOrderByScoreDesc(org.springframework.data.domain.Pageable pageable);
    
    List<Score> findByLevelNumberOrderByScoreDesc(int levelNumber);
}
