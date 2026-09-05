package com.onebullet.controller;

import com.onebullet.model.Player;
import com.onebullet.service.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping
    public ResponseEntity<?> createPlayer(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }
        try {
            Player player = playerService.getOrCreatePlayer(username.trim());
            return ResponseEntity.ok(player);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlayer(@PathVariable Long id) {
        return playerService.getPlayer(id)
                .map(player -> ResponseEntity.ok((Object) player))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getPlayerByUsername(@PathVariable String username) {
        return playerService.getPlayerByUsername(username)
                .map(player -> ResponseEntity.ok((Object) player))
                .orElse(ResponseEntity.notFound().build());
    }
}
