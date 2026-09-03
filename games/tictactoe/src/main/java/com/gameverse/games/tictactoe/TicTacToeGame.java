package com.gameverse.games.tictactoe;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe Tic-Tac-Toe game implementation with difficulty-based AI.
 *
 * Synchronization design (perfect synchronization):
 *   - A single intrinsic monitor ({@code lock}) guards ALL mutable game state
 *     (board, currentPlayer, humanPlayer, moves, and the inherited lifecycle
 *     fields score/result/isRunning/isPaused/difficulty via synchronized
 *     overrides), so the entire class behaves as one monitor object.
 *   - Every public mutating and state-reading method acquires {@code lock};
 *     the JVM guarantees mutual exclusion and a happens-before edge between
 *     any two critical sections, so no race conditions are possible.
 *   - {@link #makeMove(int, int)} validates and applies a human move
 *     atomically under the monitor, then signals waiters via notifyAll.
 *   - {@link #update(float)} performs the AI turn under the same monitor and
 *     hands the turn back to the human, so the AI can never move twice in a
 *     row and no other thread can observe a partially-updated board.
 *   - {@link #getBoard()} returns a defensive copy so external readers
 *     cannot mutate shared state.
 *   - AI helpers (minimax) operate on a private snapshot, so they never
 *     observe a partially-updated board.
 *
 * Easy: random moves
 * Medium: smart but makes mistakes
 * Hard: perfect minimax AI
 */
public class TicTacToeGame extends BaseGame {

    private static final int BOARD_SIZE = 3;
    private static final char EMPTY = ' ';
    private static final char PLAYER_X = 'X';
    private static final char PLAYER_O = 'O';

    private final Object lock = new Object();

    private char[][] board;
    private char currentPlayer;
    private char humanPlayer;
    private int moves;

    public TicTacToeGame() {
        super("Tic-Tac-Toe");
        synchronized (lock) {
            this.board = new char[BOARD_SIZE][BOARD_SIZE];
            this.currentPlayer = PLAYER_X;
            this.humanPlayer = PLAYER_X;
            this.moves = 0;
        }
    }

    @Override
    public void initialize() {
        synchronized (lock) {
            resetBoardLocked();
        }
    }

    @Override
    public void start() {
        synchronized (lock) {
            super.start();
            resetBoardLocked();
            this.moves = 0;
            lock.notifyAll();
        }
    }

    @Override
    public void restart() {
        synchronized (lock) {
            super.restart();
            resetBoardLocked();
            lock.notifyAll();
        }
    }

    @Override
    public void pause() {
        synchronized (lock) {
            super.pause();
            lock.notifyAll();
        }
    }

    @Override
    public void resume() {
        synchronized (lock) {
            super.resume();
            lock.notifyAll();
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (lock) {
            return super.isRunning();
        }
    }

    @Override
    public int getScore() {
        synchronized (lock) {
            return score;
        }
    }

    @Override
    public GameResult getResult() {
        synchronized (lock) {
            return result;
        }
    }

    @Override
    public void setDifficulty(Difficulty difficulty) {
        synchronized (lock) {
            this.difficulty = difficulty;
        }
    }

    @Override
    public Difficulty getDifficulty() {
        synchronized (lock) {
            return difficulty;
        }
    }

    @Override
    public void update(float deltaTime) {
        synchronized (lock) {
            if (!isRunning()) {
                return;
            }

            if (currentPlayer == PLAYER_O && humanPlayer == PLAYER_X) {
                // Perform AI turn under the same monitor so no concurrent
                // thread can mutate the board between checks and writes.
                makeAIMoveLocked();
                if (!checkGameEndLocked()) {
                    // Hand the turn back to the human so the AI does not keep
                    // moving on subsequent update() frames.
                    currentPlayer = humanPlayer;
                }
            }
        }
    }

    private void resetBoardLocked() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
        moves = 0;
        currentPlayer = PLAYER_X;
    }

    /**
     * Make a player move. Thread-safe: acquires the monitor, which also
     * serializes against any in-flight AI computation, validates the move,
     * applies it atomically, then signals any waiters.
     *
     * @param row the row index (0-2)
     * @param col the column index (0-2)
     * @return true if move was valid, false otherwise
     */
    public boolean makeMove(int row, int col) {
        synchronized (lock) {
            if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
                return false;
            }
            if (!isRunning()) {
                return false;
            }
            if (board[row][col] != EMPTY) {
                return false; // Cell already occupied
            }

            board[row][col] = currentPlayer;
            moves++;

            boolean ended = checkGameEndLocked();
            if (!ended) {
                currentPlayer = (currentPlayer == PLAYER_X) ? PLAYER_O : PLAYER_X;
            }
            lock.notifyAll();
            return true;
        }
    }

    /** Public accessor for tests: waits for the game to end or be paused. */
    public void awaitGameEnd() throws InterruptedException {
        synchronized (lock) {
            while (isRunning() && !isPaused) {
                lock.wait();
            }
        }
    }

    private void makeAIMoveLocked() {
        switch (difficulty) {
            case EASY -> makeEasyMoveLocked();
            case MEDIUM -> makeMediumMoveLocked();
            case HARD -> makeHardMoveLocked();
        }
    }

    /** Easy AI: random moves. */
    private void makeEasyMoveLocked() {
        List<int[]> available = new ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    available.add(new int[]{i, j});
                }
            }
        }
        if (available.isEmpty()) return;
        int[] move = available.get((int) (Math.random() * available.size()));
        board[move[0]][move[1]] = PLAYER_O;
        moves++;
        checkGameEndLocked();
    }

    /** Medium AI: sometimes smart, sometimes random. */
    private void makeMediumMoveLocked() {
        if (Math.random() < 0.6) {
            if (tryWinOrBlockLocked(PLAYER_O)) return;
            if (tryWinOrBlockLocked(PLAYER_X)) return;
            if (board[1][1] == EMPTY) {
                board[1][1] = PLAYER_O;
                moves++;
                checkGameEndLocked();
                return;
            }
        }
        makeEasyMoveLocked();
    }

    /** Hard AI: perfect minimax on a snapshot. */
    private void makeHardMoveLocked() {
        char[][] snapshot = snapshotBoardLocked();
        int bestScore = Integer.MIN_VALUE;
        int bestRow = -1, bestCol = -1;

        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (snapshot[i][j] == EMPTY) {
                    snapshot[i][j] = PLAYER_O;
                    int score = minimax(snapshot, 0, false);
                    snapshot[i][j] = EMPTY;
                    if (score > bestScore) {
                        bestScore = score;
                        bestRow = i;
                        bestCol = j;
                    }
                }
            }
        }

        if (bestRow != -1) {
            board[bestRow][bestCol] = PLAYER_O;
            moves++;
            checkGameEndLocked();
        }
    }

    /** Count occupied cells on a board (used by snapshot-based AI). */
    private int countMoves(char[][] b) {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++)
            for (int j = 0; j < BOARD_SIZE; j++)
                if (b[i][j] != EMPTY) count++;
        return count;
    }

    private char[][] snapshotBoardLocked() {
        char[][] copy = new char[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            System.arraycopy(board[i], 0, copy[i], 0, BOARD_SIZE);
        }
        return copy;
    }

    /** Minimax algorithm for perfect Tic-Tac-Toe play on the given snapshot. */
    private int minimax(char[][] b, int depth, boolean isMaximizing) {
        if (winsOn(b, PLAYER_O)) return 10 - depth;
        if (winsOn(b, PLAYER_X)) return depth - 10;
        if (countMoves(b) >= BOARD_SIZE * BOARD_SIZE) return 0;

        if (isMaximizing) {
            int best = Integer.MIN_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (b[i][j] == EMPTY) {
                        b[i][j] = PLAYER_O;
                        best = Math.max(best, minimax(b, depth + 1, false));
                        b[i][j] = EMPTY;
                    }
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (b[i][j] == EMPTY) {
                        b[i][j] = PLAYER_X;
                        best = Math.min(best, minimax(b, depth + 1, true));
                        b[i][j] = EMPTY;
                    }
                }
            }
            return best;
        }
    }

    private boolean tryWinOrBlockLocked(char player) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    board[i][j] = player;
                    if (winsOn(board, player)) {
                        if (player == PLAYER_O) {
                            currentPlayer = PLAYER_O;
                            moves++;
                            checkGameEndLocked();
                            return true;
                        }
                        // blocking move, undo and report failure so caller continues
                        board[i][j] = EMPTY;
                        return false;
                    }
                    board[i][j] = EMPTY;
                }
            }
        }
        return false;
    }

    private boolean checkGameEndLocked() {
        if (winsOn(board, PLAYER_X)) {
            result = new GameResult(name, GameResult.Status.WON, 100, getElapsedTime());
            isRunning = false;
            score = 100;
            lock.notifyAll();
            return true;
        }

        if (winsOn(board, PLAYER_O)) {
            result = new GameResult(name, GameResult.Status.LOST, 0, getElapsedTime());
            isRunning = false;
            score = 0;
            lock.notifyAll();
            return true;
        }

        if (moves == BOARD_SIZE * BOARD_SIZE) {
            result = new GameResult(name, GameResult.Status.DRAWN, 50, getElapsedTime());
            isRunning = false;
            score = 50;
            lock.notifyAll();
            return true;
        }

        return false;
    }

    private boolean winsOn(char[][] b, char player) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (b[i][0] == player && b[i][1] == player && b[i][2] == player) return true;
        }
        for (int j = 0; j < BOARD_SIZE; j++) {
            if (b[0][j] == player && b[1][j] == player && b[2][j] == player) return true;
        }
        if (b[0][0] == player && b[1][1] == player && b[2][2] == player) return true;
        if (b[0][2] == player && b[1][1] == player && b[2][0] == player) return true;
        return false;
    }

    /** Defensive copy to keep callers from mutating internal state. */
    public char[][] getBoard() {
        synchronized (lock) {
            char[][] copy = new char[BOARD_SIZE][BOARD_SIZE];
            for (int i = 0; i < BOARD_SIZE; i++) {
                System.arraycopy(board[i], 0, copy[i], 0, BOARD_SIZE);
            }
            return copy;
        }
    }

    public char getCurrentPlayer() {
        synchronized (lock) {
            return currentPlayer;
        }
    }

    public boolean isBoardFull() {
        synchronized (lock) {
            return moves == BOARD_SIZE * BOARD_SIZE;
        }
    }
}