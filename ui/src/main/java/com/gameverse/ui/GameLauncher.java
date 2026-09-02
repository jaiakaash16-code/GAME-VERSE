package com.gameverse.ui;

import com.gameverse.core.GameRegistry;
import com.gameverse.games.chess.ChessGame;
import com.gameverse.games.memory.MemoryGame;
import com.gameverse.games.pong.PongGame;
import com.gameverse.games.racing.RacingGame;
import com.gameverse.games.snake.SnakeGame;
import com.gameverse.games.tictactoe.TicTacToeGame;
import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;

import javax.swing.*;

/**
 * Main launcher for GameVerse application.
 * Handles the full flow: Login → MainMenu → GamePlay → back.
 */
public class GameLauncher {

    public static void main(String[] args) {
        registerGames();
        SwingUtilities.invokeLater(GameLauncher::showLoginPage);
    }

    /**
     * Register all games in the GameRegistry
     */
    private static void registerGames() {
        GameRegistry registry = GameRegistry.getInstance();
        registry.registerGame("Chess", ChessGame.class);
        registry.registerGame("Snake", SnakeGame.class);
        registry.registerGame("Pong", PongGame.class);
        registry.registerGame("Tic-Tac-Toe", TicTacToeGame.class);
        registry.registerGame("Memory Game", MemoryGame.class);
        registry.registerGame("Mini Racing", RacingGame.class);
        System.out.println("✓ Registered " + registry.getGameCount() + " games");
    }

    private static void showLoginPage() {
        new LoginPage(new LoginPage.LoginCallback() {
            @Override
            public void onLoginSuccess(Player player) {
                System.out.println("✓ Logged in: " + player.getUsername());
                showMainMenu(player);
            }

            @Override
            public void onLoginFailed(String message) {
                System.err.println("✗ Login failed: " + message);
                JOptionPane.showMessageDialog(null,
                    "Login failed: " + message,
                    "Error", JOptionPane.ERROR_MESSAGE);
                showLoginPage();
            }

            @Override
            public void onSignUp() {
                showSignUpPage();
            }
        });
    }

    private static void showSignUpPage() {
        new SignUpPage(new SignUpPage.SignUpCallback() {
            @Override
            public void onSignUpSuccess(Player player) {
                System.out.println("✓ Account created: " + player.getUsername());
                showMainMenu(player);
            }

            @Override
            public void onSignUpFailed(String message) {
                System.err.println("✗ Sign up failed: " + message);
                JOptionPane.showMessageDialog(null,
                    "Sign up failed: " + message,
                    "Error", JOptionPane.ERROR_MESSAGE);
                showSignUpPage();
            }

            @Override
            public void onBackToLogin() {
                showLoginPage();
            }
        });
    }

    private static void showMainMenu(Player player) {
        new MainMenu(player, new MainMenu.MainMenuCallback() {
            @Override
            public void onPlayGame(String gameName) {
                showGamePlay(player, gameName);
            }

            @Override
            public void onLogout() {
                showLoginPage();
            }
        });
    }

    private static void showGamePlay(Player player, String gameName) {
        new GamePlayScreen(player, gameName, () -> showMainMenu(player));
    }
}
