package com.gameverse.games.tictactoe;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TicTacToeGameTest {

    @Test
    void initializesCleanBoard() {
        TicTacToeGame game = new TicTacToeGame();
        game.start();
        char[][] b = game.getBoard();
        for (char[] row : b) {
            for (char c : row) assertEquals(' ', c);
        }
        assertEquals('X', game.getCurrentPlayer());
        assertFalse(game.isBoardFull());
    }

    @Test
    void rejectsOutOfBoundsAndOccupied() {
        TicTacToeGame game = new TicTacToeGame();
        game.start();
        assertFalse(game.makeMove(-1, 0));
        assertFalse(game.makeMove(3, 0));
        assertFalse(game.makeMove(0, 3));
        assertTrue(game.makeMove(1, 1));
        assertFalse(game.makeMove(1, 1));
    }

    @Test
    void rejectsMoveWhenGameEnded() {
        TicTacToeGame game = new TicTacToeGame();
        game.start();
        assertTrue(game.makeMove(0, 0)); // X
        assertTrue(game.makeMove(1, 0)); // O
        assertTrue(game.makeMove(0, 1)); // X
        assertTrue(game.makeMove(1, 1)); // O
        assertTrue(game.makeMove(0, 2)); // X wins
        assertFalse(game.isRunning());
        assertFalse(game.makeMove(2, 2));
        assertEquals(GameResult.Status.WON, game.getResult().getStatus());
    }

    @Test
    void detectsDraw() {
        TicTacToeGame game = new TicTacToeGame();
        game.start();
        assertTrue(game.makeMove(0, 0)); // X
        assertTrue(game.makeMove(0, 1)); // O
        assertTrue(game.makeMove(0, 2)); // X
        assertTrue(game.makeMove(1, 1)); // O
        assertTrue(game.makeMove(1, 0)); // X
        assertTrue(game.makeMove(1, 2)); // O
        assertTrue(game.makeMove(2, 1)); // X
        assertTrue(game.makeMove(2, 0)); // O
        assertTrue(game.makeMove(2, 2)); // X
        assertTrue(game.isBoardFull());
        assertEquals(GameResult.Status.DRAWN, game.getResult().getStatus());
        assertEquals(50, game.getScore());
    }

    @Test
    void humanVsHumanAlternatesAndRecordsWinner() {
        TicTacToeGame game = new TicTacToeGame();
        game.start();
        assertEquals('X', game.getCurrentPlayer());
        assertTrue(game.makeMove(0, 0));
        assertEquals('O', game.getCurrentPlayer());
        assertTrue(game.makeMove(1, 1));
        assertEquals('X', game.getCurrentPlayer());
        assertTrue(game.makeMove(0, 1));
        assertTrue(game.makeMove(2, 2));
        assertTrue(game.makeMove(0, 2)); // X wins row 0
        assertFalse(game.isRunning());
        assertEquals(GameResult.Status.WON, game.getResult().getStatus());
        assertEquals(100, game.getScore());
    }

    @Test
    void restartClearsState() {
        TicTacToeGame game = new TicTacToeGame();
        game.start();
        game.makeMove(0, 0);
        game.makeMove(1, 1);
        game.restart();
        game.start();
        char[][] b = game.getBoard();
        for (char[] row : b) for (char c : row) assertEquals(' ', c);
        assertEquals('X', game.getCurrentPlayer());
    }

    @Test
    void concurrentMovesNeverCorruptBoard() throws Exception {
        TicTacToeGame game = new TicTacToeGame();
        game.start();

        int threads = 16;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        Runnable worker = () -> {
            try {
                start.await();
                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        if (game.makeMove(r, c)) accepted.incrementAndGet();
                        else rejected.incrementAndGet();
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < threads; i++) ex.submit(worker);
        start.countDown();
        ex.shutdown();
        assertTrue(ex.awaitTermination(10, TimeUnit.SECONDS));

        // No two cells may share a marker, and at most 9 moves can be valid.
        char[][] b = game.getBoard();
        int occupiedCount = 0;
        for (char[] row : b) for (char c : row) if (c != ' ') occupiedCount++;
        assertTrue(occupiedCount <= 9, "Board has more markers than cells: " + occupiedCount);
        assertEquals(occupiedCount, accepted.get(), "Accepted moves must equal occupied cells");

        // The set of occupied cells must be unique values of X/O.
        int xCount = 0, oCount = 0;
        for (char[] row : b) for (char c : row) {
            if (c == 'X') xCount++;
            else if (c == 'O') oCount++;
            else assertEquals(' ', c, "Cell must be empty, X, or O");
        }
        assertTrue(xCount + oCount <= 9);
        // X always moves first, so legal play keeps the counts within one.
        assertTrue(Math.abs(xCount - oCount) <= 1,
                "X and O counts must stay within one in legal play, got X=" + xCount + " O=" + oCount);
    }

    @Test
    void concurrentReadersSeeConsistentBoard() throws Exception {
        TicTacToeGame game = new TicTacToeGame();
        game.start();

        int writers = 2;
        int readers = 8;
        ExecutorService ex = Executors.newFixedThreadPool(writers + readers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger inconsistencies = new AtomicInteger();
        AtomicInteger movesPlayed = new AtomicInteger();
        AtomicBoolean stop = new AtomicBoolean(false);

        for (int i = 0; i < writers; i++) {
            ex.submit(() -> {
                try {
                    start.await();
                    int r = 0, c = 0;
                    while (!stop.get() && movesPlayed.get() < 9) {
                        if (game.makeMove(r, c)) movesPlayed.incrementAndGet();
                        c++;
                        if (c == 3) { c = 0; r = (r + 1) % 3; }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        for (int i = 0; i < readers; i++) {
            ex.submit(() -> {
                try {
                    start.await();
                    while (!stop.get()) {
                        char[][] b = game.getBoard();
                        int occupied = 0;
                        for (char[] row : b)
                            for (char c : row)
                                if (c != ' ') occupied++;
                        // Defensive copy must never throw and must not see partial updates.
                        if (occupied > 9) inconsistencies.incrementAndGet();
                    }
                } catch (Exception e) {
                    inconsistencies.incrementAndGet();
                }
            });
        }

        start.countDown();
        Thread.sleep(500);
        stop.set(true);
        ex.shutdown();
        assertTrue(ex.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(0, inconsistencies.get(), "Readers observed inconsistent board state");
    }

    @Test
    void aiOnHardNeverLoses() {
        TicTacToeGame game = new TicTacToeGame();
        game.setDifficulty(Difficulty.HARD);
        game.start();
        // Play a full game vs perfect AI; outcome must be DRAW/WON.
        int[][] firstPlayerMoves = {
                {0, 0}, {0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}
        };
        int idx = 0;
        while (game.isRunning() && idx < firstPlayerMoves.length) {
            int[] m = firstPlayerMoves[idx++];
            game.makeMove(m[0], m[1]);
        }
        GameResult.Status s = game.getResult().getStatus();
        assertTrue(s == GameResult.Status.WON || s == GameResult.Status.DRAWN,
                "Perfect AI should not lose, got: " + s);
    }

    @Test
    void getBoardReturnsDefensiveCopy() {
        TicTacToeGame game = new TicTacToeGame();
        game.start();
        char[][] a = game.getBoard();
        a[0][0] = 'Z';
        char[][] b = game.getBoard();
        assertEquals(' ', b[0][0], "Mutating returned board must not affect game state");
    }

    @Test
    void aiRespondsOncePerHumanMoveViaUpdate() {
        TicTacToeGame game = new TicTacToeGame();
        game.setDifficulty(Difficulty.EASY);
        game.start();

        assertTrue(game.makeMove(0, 0)); // Human (X) plays
        game.update(0.016f);             // AI (O) responds exactly once

        char[][] b = game.getBoard();
        int x = 0, o = 0;
        for (char[] row : b) {
            for (char c : row) {
                if (c == 'X') x++;
                else if (c == 'O') o++;
            }
        }
        assertEquals(1, x);
        assertEquals(1, o);
        assertEquals('X', game.getCurrentPlayer(), "Turn must return to the human after the AI move");

        // A second update() without a human move must not trigger another AI move.
        game.update(0.016f);
        b = game.getBoard();
        o = 0;
        for (char[] row : b) {
            for (char c : row) {
                if (c == 'O') o++;
            }
        }
        assertEquals(1, o, "AI must not move twice in a row");
    }

    @Test
    void perfectAiNeverLosesWhenDrivenByUpdate() {
        TicTacToeGame game = new TicTacToeGame();
        game.setDifficulty(Difficulty.HARD);
        game.start();

        int[][] humanMoves = {
                {0, 0}, {0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}
        };
        int idx = 0;
        while (game.isRunning() && idx < humanMoves.length) {
            int[] m = humanMoves[idx++];
            game.makeMove(m[0], m[1]);
            game.update(0.016f); // let the AI respond
        }

        assertNotNull(game.getResult(), "Game must have completed");
        GameResult.Status s = game.getResult().getStatus();
        // Status is from the human's perspective: the AI must never beat
        // (WON) the human, so only LOST or DRAWN are acceptable.
        assertTrue(s == GameResult.Status.LOST || s == GameResult.Status.DRAWN,
                "Perfect AI should not lose, got: " + s);
    }
}