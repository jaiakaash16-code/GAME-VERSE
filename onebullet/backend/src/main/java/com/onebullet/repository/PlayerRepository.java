package com.onebullet.repository;

import com.onebullet.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUsername(String username);
    boolean existsByUsername(String username);
}
