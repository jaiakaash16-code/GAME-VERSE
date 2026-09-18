package com.gameverse.games.connectfour;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Connect Four implementation with AI difficulty support.
 * Human plays as red. Computer plays as yellow.
 */
public class ConnectFourGame extends BaseGame {

    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static final char EMPTY = ' ';
    private static final char RED = 'R';
    private static final char YELLOW = 'Y';

    private char[][] board;
    private char currentPlayer;
    private final char humanPlayer = RED;
    private final char aiPlayer = YELLOW;
    private boolean gameOver;
    private int moves;
    private int lastAiColumn = -1;
    private final Random random = new Random();

    public ConnectFourGame() {
        super("Connect Four");
        board = new char[ROWS][COLS];
        initialize();
    }

    @Override
    public void initialize() {
        resetBoard();
        currentPlayer = humanPlayer;
        gameOver = false;
        moves = 0;
        lastAiColumn = -1;
        result = null;
    }

    @Override
    public void start() {
        super.start();
        initialize();
    }

    @Override
    public void restart() {
        super.restart();
        initialize();
    }

    @Override
    public void update(float deltaTime) {
        if (!isRunning() || gameOver || currentPlayer != aiPlayer) {
            return;
        }

        int move = chooseAIMove();
        if (move == -1) {
            return;
        }
        lastAiColumn = move;
        makeMove(move);
    }

    private void resetBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = EMPTY;
            }
        }
    }

    public boolean makeMove(int column) {
        if (!isRunning() || gameOver || column < 0 || column >= COLS) {
            return false;
        }
        if (currentPlayer != humanPlayer && currentPlayer != aiPlayer) {
            return false;
        }

        int row = getDropRow(column);
        if (row == -1) {
            return false;
        }
        if (currentPlayer == humanPlayer) {
            lastAiColumn = -1;
        }
        return place(row, column);
    }

    /**
     * Places the current player's disc in an exact cell — no gravity.
     * Used by the UI so the disc lands precisely where the user clicks.
     */
    public boolean makeMoveAt(int row, int col) {
        if (!isRunning() || gameOver || row < 0 || row >= ROWS || col < 0 || col >= COLS) {
            return false;
        }
        if (currentPlayer != humanPlayer && currentPlayer != aiPlayer) {
            return false;
        }
        if (board[row][col] != EMPTY) {
            return false;
        }
        if (currentPlayer == humanPlayer) {
            lastAiColumn = -1;
        }
        return place(row, col);
    }

    /** Shared placement logic: mark cell, check win/draw, switch player. */
    private boolean place(int row, int col) {
        board[row][col] = currentPlayer;
        moves++;

        if (hasConnectFour(row, col, currentPlayer)) {
            finishGame(currentPlayer == humanPlayer ? GameResult.Status.WON : GameResult.Status.LOST, 200);
            return true;
        }

        if (moves >= ROWS * COLS) {
            finishGame(GameResult.Status.DRAWN, 100);
            return true;
        }

        currentPlayer = (currentPlayer == humanPlayer) ? aiPlayer : humanPlayer;
        return true;
    }

    public char[][] getBoard() {
        char[][] copy = new char[ROWS][COLS];
        for (int row = 0; row < ROWS; row++) {
            System.arraycopy(board[row], 0, copy[row], 0, COLS);
        }
        return copy;
    }

    public char getCurrentPlayer() {
        return currentPlayer;
    }

    /** Column of the AI's most recent reply, or -1 if it hasn't answered yet. */
    public int getLastAiColumn() {
        return lastAiColumn;
    }

    public boolean isBoardFull() {
        return moves >= ROWS * COLS;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getRows() {
        return ROWS;
    }

    public int getColumns() {
        return COLS;
    }

    private void finishGame(GameResult.Status status, int amount) {
        gameOver = true;
        isRunning = false;
        score = amount;
        result = new GameResult(getName(), status, score, 0L);
    }

    private boolean hasConnectFour(int row, int col, char player) {
        int[][] directions = {
            {0, 1},
            {1, 0},
            {1, 1},
            {1, -1}
        };

        for (int[] direction : directions) {
            int dx = direction[0];
            int dy = direction[1];
            int count = 1;

            count += countDirection(row, col, dx, dy, player);
            count += countDirection(row, col, -dx, -dy, player);
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }

    private int countDirection(int row, int col, int rowStep, int colStep, char player) {
        int count = 0;
        int r = row + rowStep;
        int c = col + colStep;

        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && board[r][c] == player) {
            count++;
            r += rowStep;
            c += colStep;
        }
        return count;
    }

    private int chooseAIMove() {
        if (difficulty == Difficulty.EASY) {
            return chooseRandomMove();
        }
        if (difficulty == Difficulty.MEDIUM) {
            int winningMove = findImmediateWin(aiPlayer);
            if (winningMove != -1) {
                return winningMove;
            }
            int blockingMove = findImmediateWin(humanPlayer);
            if (blockingMove != -1) {
                return blockingMove;
            }
            int center = 3;
            if (isValidColumn(center)) {
                return center;
            }
            return chooseRandomMove();
        }

        int bestMove = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int col = 0; col < COLS; col++) {
            if (!isValidColumn(col)) {
                continue;
            }
            int row = getDropRow(col);
            board[row][col] = aiPlayer;
            int score = minimax(board, 5, Integer.MIN_VALUE, Integer.MAX_VALUE, false, aiPlayer, humanPlayer);
            board[row][col] = EMPTY;
            if (score > bestScore) {
                bestScore = score;
                bestMove = col;
            }
        }
        return bestMove == -1 ? chooseRandomMove() : bestMove;
    }

    private int minimax(char[][] state, int depth, int alpha, int beta, boolean maximizing, char ai, char human) {
        if (depth == 0 || hasWinnerOnBoard(state, ai) || hasWinnerOnBoard(state, human) || isBoardFullState(state)) {
            return evaluateBoard(state, ai, human);
        }

        if (maximizing) {
            int best = Integer.MIN_VALUE;
            for (int col = 0; col < COLS; col++) {
                if (!isValidColumnState(state, col)) {
                    continue;
                }
                int row = getDropRowState(state, col);
                state[row][col] = ai;
                int score = minimax(state, depth - 1, alpha, beta, false, ai, human);
                state[row][col] = EMPTY;
                best = Math.max(best, score);
                alpha = Math.max(alpha, score);
                if (beta <= alpha) {
                    break;
                }
            }
            return best;
        }

        int best = Integer.MAX_VALUE;
        for (int col = 0; col < COLS; col++) {
            if (!isValidColumnState(state, col)) {
                continue;
            }
            int row = getDropRowState(state, col);
            state[row][col] = human;
            int score = minimax(state, depth - 1, alpha, beta, true, ai, human);
            state[row][col] = EMPTY;
            best = Math.min(best, score);
            beta = Math.min(beta, score);
            if (beta <= alpha) {
                break;
            }
        }
        return best;
    }

    private int evaluateBoard(char[][] state, char ai, char human) {
        if (hasWinnerOnBoard(state, ai)) {
            return 100000;
        }
        if (hasWinnerOnBoard(state, human)) {
            return -100000;
        }

        int score = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (state[row][col] == ai) {
                    score += 10;
                } else if (state[row][col] == human) {
                    score -= 10;
                }
            }
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS - 3; col++) {
                int[] values = {state[row][col], state[row][col + 1], state[row][col + 2], state[row][col + 3]};
                score += evaluateWindow(values, ai, human);
            }
        }
        for (int row = 0; row < ROWS - 3; row++) {
            for (int col = 0; col < COLS; col++) {
                int[] values = {state[row][col], state[row + 1][col], state[row + 2][col], state[row + 3][col]};
                score += evaluateWindow(values, ai, human);
            }
        }
        return score;
    }

    private int evaluateWindow(int[] window, char ai, char human) {
        int aiCount = 0;
        int humanCount = 0;
        int emptyCount = 0;

        for (int value : window) {
            if (value == ai) {
                aiCount++;
            } else if (value == human) {
                humanCount++;
            } else {
                emptyCount++;
            }
        }

        if (aiCount > 0 && humanCount == 0 && emptyCount == 0) {
            return 10000;
        }
        if (aiCount > 0 && humanCount == 0) {
            return 10 * aiCount;
        }
        if (humanCount > 0 && aiCount == 0) {
            return -10 * humanCount;
        }
        return 0;
    }

    private int findImmediateWin(char player) {
        for (int col = 0; col < COLS; col++) {
            if (!isValidColumn(col)) {
                continue;
            }
            int row = getDropRow(col);
            board[row][col] = player;
            boolean win = hasConnectFour(row, col, player);
            board[row][col] = EMPTY;
            if (win) {
                return col;
            }
        }
        return -1;
    }

    private int chooseRandomMove() {
        List<Integer> validColumns = new ArrayList<>();
        for (int col = 0; col < COLS; col++) {
            if (isValidColumn(col)) {
                validColumns.add(col);
            }
        }
        if (validColumns.isEmpty()) {
            return -1;
        }
        return validColumns.get(random.nextInt(validColumns.size()));
    }

    private boolean isValidColumn(int column) {
        return column >= 0 && column < COLS && board[0][column] == EMPTY;
    }

    private int getDropRow(int column) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][column] == EMPTY) {
                return row;
            }
        }
        return -1;
    }

    private boolean hasWinnerOnBoard(char[][] state, char checked) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (state[row][col] == checked && hasConnectFourOnState(state, row, col, checked)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasConnectFourOnState(char[][] state, int row, int col, char player) {
        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] direction : directions) {
            int count = 1;
            count += countDirectionOnState(state, row, col, direction[0], direction[1], player);
            count += countDirectionOnState(state, row, col, -direction[0], -direction[1], player);
            if (count >= 4) {
                return true;
            }
        }
        return false;
    }

    private int countDirectionOnState(char[][] state, int row, int col, int rowStep, int colStep, char player) {
        int count = 0;
        int r = row + rowStep;
        int c = col + colStep;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && state[r][c] == player) {
            count++;
            r += rowStep;
            c += colStep;
        }
        return count;
    }

    private boolean isBoardFullState(char[][] state) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (state[row][col] == EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidColumnState(char[][] state, int column) {
        return column >= 0 && column < COLS && state[0][column] == EMPTY;
    }

    private int getDropRowState(char[][] state, int column) {
        for (int row = ROWS - 1; row >= 0; row--) {
            if (state[row][column] == EMPTY) {
                return row;
            }
        }
        return -1;
    }
}
