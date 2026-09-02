package com.gameverse.games.chess;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

/**
 * Chess game: Player (white) vs Computer (black).
 * Uppercase = white pieces, lowercase = black pieces.
 * AI difficulty levels: Easy=random, Medium=greedy, Hard=evaluation-based.
 */
public class ChessGame extends BaseGame {

    public static final int SIZE = 8;
    private char[][] board;
    private boolean whiteToMove;
    private int moveCount;
    private int selectedRow = -1;
    private int selectedCol = -1;

    // Piece values for evaluation
    private static final int[] PIECE_VALUES = {
        0,   // ' '
        100, // 'P' pawn
        320, // 'N' knight
        330, // 'B' bishop
        500, // 'R' rook
        900, // 'Q' queen
        20000 // 'K' king
    };

    public ChessGame() {
        super("Chess");
        this.board = new char[SIZE][SIZE];
        this.whiteToMove = true;
        this.moveCount = 0;
    }

    @Override
    public void initialize() { setupBoard(); }

    @Override
    public void start() { super.start(); setupBoard(); }

    @Override
    public void restart() { super.restart(); setupBoard(); }

    @Override
    public void update(float deltaTime) { /* moves handled via click */ }

    private void setupBoard() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                board[r][c] = ' ';

        // Black pieces (top rows 0-1) — lowercase
        String back = "rnbqkbnr";
        for (int c = 0; c < SIZE; c++) {
            board[0][c] = back.charAt(c);
            board[1][c] = 'p';
        }
        // White pieces (bottom rows 6-7) — uppercase
        for (int c = 0; c < SIZE; c++) {
            board[7][c] = back.toUpperCase().charAt(c);
            board[6][c] = 'P';
        }
        selectedRow = -1;
        selectedCol = -1;
        whiteToMove = true;
        moveCount = 0;
    }

    /**
     * Handle click: first click selects, second click moves.
     * After white moves, AI automatically responds.
     */
    public boolean handleClick(int row, int col) {
        if (!isRunning() || !whiteToMove) return false;
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return false;

        if (selectedRow == -1) {
            // Select own (white) piece
            char piece = board[row][col];
            if (piece != ' ' && Character.isUpperCase(piece)) {
                selectedRow = row;
                selectedCol = col;
                return true;
            }
            return false;
        }

        // Same square = deselect
        if (row == selectedRow && col == selectedCol) {
            selectedRow = -1;
            selectedCol = -1;
            return true;
        }

        // Clicking another white piece = reselect
        char target = board[row][col];
        if (target != ' ' && Character.isUpperCase(target)) {
            selectedRow = row;
            selectedCol = col;
            return true;
        }

        // Try move
        boolean moved = tryMove(selectedRow, selectedCol, row, col);
        selectedRow = -1;
        selectedCol = -1;

        if (moved) {
            score += 10;
            moveCount++;
            whiteToMove = false;
            // AI responds
            aiMove();
            whiteToMove = true;
            return true;
        }
        return false;
    }

    /** Direct move (called from click handler) */
    private boolean tryMove(int fr, int fc, int tr, int tc) {
        char piece = board[fr][fc];
        char target = board[tr][tc];
        if (piece == ' ') return false;
        // Can't capture own color
        if (target != ' ' && Character.isUpperCase(piece) == Character.isUpperCase(target))
            return false;
        if (!isPathClear(fr, fc, tr, tc)) return false;
        if (!canPieceMove(piece, fr, fc, tr, tc)) return false;

        // Check if capturing enemy king
        if (target == 'k') {
            board[tr][tc] = piece;
            board[fr][fc] = ' ';
            result = new GameResult(name, GameResult.Status.WON, score + 100, getElapsedTime());
            isRunning = false;
            return true;
        }
        board[tr][tc] = piece;
        board[fr][fc] = ' ';
        return true;
    }

    private boolean canPieceMove(char piece, int fr, int fc, int tr, int tc) {
        int dr = tr - fr, dc = tc - fc;
        int adr = Math.abs(dr), adc = Math.abs(dc);
        char lower = Character.toLowerCase(piece);

        return switch (lower) {
            case 'p' -> {
                int dir = Character.isUpperCase(piece) ? -1 : 1;
                if (dc == 0 && dr == dir && board[tr][tc] == ' ') yield true;
                if (adr == 1 && adc == 1 && dr == dir && board[tr][tc] != ' ') yield true;
                // Double move from start
                int startRow = Character.isUpperCase(piece) ? 6 : 1;
                if (dc == 0 && dr == dir * 2 && fr == startRow && board[tr][tc] == ' ') yield true;
                yield false;
            }
            case 'r' -> (dr == 0 || dc == 0) && (adr + adc > 0);
            case 'n' -> (adr == 2 && adc == 1) || (adr == 1 && adc == 2);
            case 'b' -> adr == adc && adr > 0;
            case 'q' -> ((dr == 0 || dc == 0) || (adr == adc)) && (adr + adc > 0);
            case 'k' -> adr <= 1 && adc <= 1 && (adr + adc > 0);
            default -> false;
        };
    }

    /** Check if path is clear (for rook/bishop/queen) */
    private boolean isPathClear(int fr, int fc, int tr, int tc) {
        int dr = Integer.signum(tr - fr);
        int dc = Integer.signum(tc - fc);
        int r = fr + dr, c = fc + dc;
        while (r != tr || c != tc) {
            if (board[r][c] != ' ') return false;
            r += dr;
            c += dc;
        }
        return true;
    }

    private void aiMove() {
        switch (difficulty) {
            case EASY -> aiMoveEasy();
            case MEDIUM -> aiMoveMedium();
            case HARD -> aiMoveHard();
        }
    }

    /** Easy AI: picks a random valid move */
    private void aiMoveEasy() {
        java.util.List<int[]> allMoves = getAllBlackMoves();
        if (allMoves.isEmpty()) return;
        int[] move = allMoves.get((int)(Math.random() * allMoves.size()));
        executeMove(move);
    }

    /** Medium AI: prefers captures, then random */
    private void aiMoveMedium() {
        java.util.List<int[]> allMoves = getAllBlackMoves();
        if (allMoves.isEmpty()) return;

        // Prefer captures of higher-value pieces
        int[] bestCapture = null;
        int bestCaptureValue = 0;
        for (int[] m : allMoves) {
            char target = board[m[2]][m[3]];
            if (target != ' ' && Character.isUpperCase(target)) {
                int val = getPieceValue(target);
                if (val > bestCaptureValue) {
                    bestCaptureValue = val;
                    bestCapture = m;
                }
            }
        }
        if (bestCapture != null) {
            executeMove(bestCapture);
        } else {
            // Random move
            int[] move = allMoves.get((int)(Math.random() * allMoves.size()));
            executeMove(move);
        }
    }

    /** Hard AI: uses position evaluation with limited depth search */
    private void aiMoveHard() {
        java.util.List<int[]> allMoves = getAllBlackMoves();
        if (allMoves.isEmpty()) return;

        int bestScore = Integer.MAX_VALUE; // Minimizing for black
        int[] bestMove = null;

        for (int[] m : allMoves) {
            // Make move temporarily
            char piece = board[m[0]][m[1]];
            char target = board[m[2]][m[3]];
            board[m[2]][m[3]] = piece;
            board[m[0]][m[1]] = ' ';

            int score = evaluateBoard();

            // Undo move
            board[m[0]][m[1]] = piece;
            board[m[2]][m[3]] = target;

            if (score < bestScore) {
                bestScore = score;
                bestMove = m;
            }
        }

        if (bestMove != null) {
            executeMove(bestMove);
        }
    }

    /** Evaluate board position (positive = good for white, negative = good for black) */
    private int evaluateBoard() {
        int score = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                char piece = board[r][c];
                if (piece != ' ') {
                    int val = getPieceValue(piece);
                    if (Character.isUpperCase(piece)) {
                        score += val;
                        // Bonus for center control
                        if ((r >= 2 && r <= 5) && (c >= 2 && c <= 5)) score += 10;
                    } else {
                        score -= val;
                        if ((r >= 2 && r <= 5) && (c >= 2 && c <= 5)) score -= 10;
                    }
                }
            }
        }
        return score;
    }

    private int getPieceValue(char piece) {
        char lower = Character.toLowerCase(piece);
        return switch (lower) {
            case 'p' -> PIECE_VALUES[1];
            case 'n' -> PIECE_VALUES[2];
            case 'b' -> PIECE_VALUES[3];
            case 'r' -> PIECE_VALUES[4];
            case 'q' -> PIECE_VALUES[5];
            case 'k' -> PIECE_VALUES[6];
            default -> 0;
        };
    }

    /** Get all valid moves for black pieces */
    private java.util.List<int[]> getAllBlackMoves() {
        java.util.List<int[]> allMoves = new java.util.ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                char piece = board[r][c];
                if (piece == ' ' || Character.isUpperCase(piece)) continue;
                for (int tr = 0; tr < SIZE; tr++) {
                    for (int tc = 0; tc < SIZE; tc++) {
                        if (tr == r && tc == c) continue;
                        char target = board[tr][tc];
                        if (target != ' ' && Character.isLowerCase(target)) continue;
                        if (!isPathClear(r, c, tr, tc)) continue;
                        if (!canPieceMove(piece, r, c, tr, tc)) continue;
                        allMoves.add(new int[]{r, c, tr, tc});
                    }
                }
            }
        }
        return allMoves;
    }

    /** Execute a move on the board */
    private void executeMove(int[] move) {
        board[move[2]][move[3]] = board[move[0]][move[1]];
        board[move[0]][move[1]] = ' ';
    }

    /* ──── Public getters ──── */

    public char getPiece(int row, int col) { return board[row][col]; }
    public boolean isWhiteToMove() { return whiteToMove; }
    public int getSelectedRow() { return selectedRow; }
    public int getSelectedCol() { return selectedCol; }
    public int getMoveCount() { return moveCount; }

    /** Unicode chess symbol for a piece character */
    public static String getSymbol(char piece) {
        return switch (piece) {
            case 'K' -> "\u2654"; case 'Q' -> "\u2655"; case 'R' -> "\u2656";
            case 'B' -> "\u2657"; case 'N' -> "\u2658"; case 'P' -> "\u2659";
            case 'k' -> "\u265A"; case 'q' -> "\u265B"; case 'r' -> "\u265C";
            case 'b' -> "\u265D"; case 'n' -> "\u265E"; case 'p' -> "\u265F";
            default -> "";
        };
    }

    public static boolean isWhitePiece(char piece) {
        return piece != ' ' && Character.isUpperCase(piece);
    }
}
