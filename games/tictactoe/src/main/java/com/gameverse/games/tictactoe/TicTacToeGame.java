package com.gameverse.games.tictactoe;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

/**
 * Tic-Tac-Toe game implementation with difficulty-based AI.
 * Easy: random moves
 * Medium: smart but makes mistakes
 * Hard: perfect minimax AI
 */
public class TicTacToeGame extends BaseGame {
    
    private static final int BOARD_SIZE = 3;
    private char[][] board;
    private char currentPlayer; // 'X' or 'O'
    private char humanPlayer;
    private int moves;
    
    public TicTacToeGame() {
        super("Tic-Tac-Toe");
        this.board = new char[BOARD_SIZE][BOARD_SIZE];
        this.currentPlayer = 'X';
        this.humanPlayer = 'X';
        this.moves = 0;
    }
    
    @Override
    public void initialize() {
        resetBoard();
    }
    
    @Override
    public void start() {
        super.start();
        resetBoard();
        moves = 0;
    }
    
    @Override
    public void restart() {
        super.restart();
        initialize();
    }
    
    @Override
    public void update(float deltaTime) {
        if (!isRunning()) {
            return;
        }
        
        // AI move if human is not playing
        if (currentPlayer == 'O' && humanPlayer == 'X') {
            makeAIMove();
            checkGameEnd();
        }
    }
    
    private void resetBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = ' ';
            }
        }
        moves = 0;
        currentPlayer = 'X';
    }
    
    /**
     * Make a player move
     * @param row the row index (0-2)
     * @param col the column index (0-2)
     * @return true if move was valid, false otherwise
     */
    public boolean makeMove(int row, int col) {
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return false;
        }
        
        if (board[row][col] != ' ') {
            return false; // Cell already occupied
        }
        
        board[row][col] = currentPlayer;
        moves++;
        
        if (!checkGameEnd()) {
            currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        }
        
        return true;
    }
    
    private void makeAIMove() {
        switch (difficulty) {
            case EASY -> makeEasyMove();
            case MEDIUM -> makeMediumMove();
            case HARD -> makeHardMove();
        }
    }
    
    /** Easy AI: random moves */
    private void makeEasyMove() {
        java.util.List<int[]> available = new java.util.ArrayList<>();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == ' ') {
                    available.add(new int[]{i, j});
                }
            }
        }
        if (available.isEmpty()) return;
        int[] move = available.get((int)(Math.random() * available.size()));
        board[move[0]][move[1]] = 'O';
        moves++;
        checkGameEnd();
    }
    
    /** Medium AI: sometimes smart, sometimes random */
    private void makeMediumMove() {
        // 60% chance of making a smart move, 40% random
        if (Math.random() < 0.6) {
            // Try to win
            if (tryMove('O')) return;
            // Block human
            if (tryMove('X')) return;
            // Take center
            if (board[1][1] == ' ') {
                board[1][1] = 'O';
                moves++;
                checkGameEnd();
                return;
            }
        }
        // Fall through to random move
        makeEasyMove();
    }
    
    /** Hard AI: perfect minimax */
    private void makeHardMove() {
        int bestScore = Integer.MIN_VALUE;
        int bestRow = -1, bestCol = -1;
        
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == ' ') {
                    board[i][j] = 'O';
                    int score = minimax(board, 0, false);
                    board[i][j] = ' ';
                    if (score > bestScore) {
                        bestScore = score;
                        bestRow = i;
                        bestCol = j;
                    }
                }
            }
        }
        
        if (bestRow != -1) {
            board[bestRow][bestCol] = 'O';
            moves++;
            checkGameEnd();
        }
    }
    
    /** Count occupied cells on the board */
    private int countMoves() {
        int count = 0;
        for (int i = 0; i < BOARD_SIZE; i++)
            for (int j = 0; j < BOARD_SIZE; j++)
                if (board[i][j] != ' ') count++;
        return count;
    }

    /** Minimax algorithm for perfect Tic-Tac-Toe play */
    private int minimax(char[][] board, int depth, boolean isMaximizing) {
        if (checkWin('O')) return 10 - depth;
        if (checkWin('X')) return depth - 10;
        if (countMoves() >= BOARD_SIZE * BOARD_SIZE) return 0;
        
        if (isMaximizing) {
            int best = Integer.MIN_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == ' ') {
                        board[i][j] = 'O';
                        best = Math.max(best, minimax(board, depth + 1, false));
                        board[i][j] = ' ';
                    }
                }
            }
            return best;
        } else {
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < BOARD_SIZE; i++) {
                for (int j = 0; j < BOARD_SIZE; j++) {
                    if (board[i][j] == ' ') {
                        board[i][j] = 'X';
                        best = Math.min(best, minimax(board, depth + 1, true));
                        board[i][j] = ' ';
                    }
                }
            }
            return best;
        }
    }
    
    private boolean tryMove(char player) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j] == ' ') {
                    board[i][j] = player;
                    if (checkWin(player)) {
                        if (player == 'O') {
                            currentPlayer = 'O';
                            moves++;
                            return true;
                        }
                    }
                    board[i][j] = ' ';
                }
            }
        }
        return false;
    }
    
    private boolean checkGameEnd() {
        if (checkWin('X')) {
            result = new GameResult(name, GameResult.Status.WON, 100, getElapsedTime());
            isRunning = false;
            score = 100;
            return true;
        }
        
        if (checkWin('O')) {
            result = new GameResult(name, GameResult.Status.LOST, 0, getElapsedTime());
            isRunning = false;
            score = 0;
            return true;
        }
        
        if (moves == BOARD_SIZE * BOARD_SIZE) {
            result = new GameResult(name, GameResult.Status.DRAWN, 50, getElapsedTime());
            isRunning = false;
            score = 50;
            return true;
        }
        
        return false;
    }
    
    private boolean checkWin(char player) {
        // Check rows
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) {
                return true;
            }
        }
        
        // Check columns
        for (int j = 0; j < BOARD_SIZE; j++) {
            if (board[0][j] == player && board[1][j] == player && board[2][j] == player) {
                return true;
            }
        }
        
        // Check diagonals
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            return true;
        }
        
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            return true;
        }
        
        return false;
    }
    
    public char[][] getBoard() {
        return board;
    }
    
    public char getCurrentPlayer() {
        return currentPlayer;
    }
    
    public boolean isBoardFull() {
        return moves == BOARD_SIZE * BOARD_SIZE;
    }
}
