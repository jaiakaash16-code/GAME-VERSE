package com.onebullet.service;

import com.onebullet.model.Player;
import com.onebullet.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public Player createPlayer(String username) {
        if (playerRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        Player player = new Player(username);
        return playerRepository.save(player);
    }

    public Optional<Player> getPlayer(Long id) {
        return playerRepository.findById(id);
    }

    public Optional<Player> getPlayerByUsername(String username) {
        return playerRepository.findByUsername(username);
    }

    public Player getOrCreatePlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseGet(() -> createPlayer(username));
    }

    public Player updateScore(Long playerId, int additionalScore) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));
        player.setTotalScore(player.getTotalScore() + additionalScore);
        return playerRepository.save(player);
    }
}
