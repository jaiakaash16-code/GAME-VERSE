package com.gameverse.games.chess;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

import java.util.ArrayList;
import java.util.List;

/**
 * Chess game: Player (white) vs Computer (black).
 * Uppercase = white pieces, lowercase = black pieces.
 *
 * Full standard chess rules: a side may only play moves that do not leave its
 * own king in check, kings can never move into check or next to the enemy
 * king, castling (both sides, kingside and queenside), en passant, pawn
 * promotion, and the game ends by checkmate (win/loss) or stalemate (draw).
 * AI difficulty levels: Easy = random legal move, Medium = greedy (prefers
 * captures), Hard = evaluation-based search over legal moves.
 */
public class ChessGame extends BaseGame {

    public static final int SIZE = 8;
    private char[][] board;
    private boolean whiteToMove;
    private int moveCount;
    private int selectedRow = -1;
    private int selectedCol = -1;

    // Pawn promotion: while set, the game waits for the player to choose the
    // promotion piece before the AI answers / the turn continues.
    private boolean promotionPending;
    private int promotionRow = -1;
    private int promotionCol = -1;

    // Castling rights: true while the king and the relevant rook still sit on
    // their home squares (both corners of both sides).
    private boolean whiteCastleK = true;
    private boolean whiteCastleQ = true;
    private boolean blackCastleK = true;
    private boolean blackCastleQ = true;

    // En passant target: the empty square beside a pawn that just double-
    // pushed (where an adjacent enemy pawn may capture). (-1, -1) = none.
    private int epRow = -1;
    private int epCol = -1;

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

    private static final int[][] ROOK_DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private static final int[][] BISHOP_DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
    private static final int[][] KNIGHT_OFFSETS = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}
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
        promotionPending = false;
        promotionRow = -1;
        promotionCol = -1;
        whiteCastleK = true;
        whiteCastleQ = true;
        blackCastleK = true;
        blackCastleQ = true;
        epRow = -1;
        epCol = -1;
        whiteToMove = true;
        moveCount = 0;
        score = 0;
        result = null;
    }

    /**
     * Handle click: first click selects, second click moves.
     * After white moves, the AI answers; checkmate/stalemate ends the game.
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

        // Try move (full legality: shape, path and king safety)
        boolean moved = tryMove(selectedRow, selectedCol, row, col);
        selectedRow = -1;
        selectedCol = -1;

        if (moved) {
            score += 10;
            moveCount++;

            // A white pawn that reached the last rank (row 0) freezes the game
            // until the player picks its promotion piece via promote().
            if (board[row][col] == 'P' && row == 0) {
                whiteToMove = false;
                promotionPending = true;
                promotionRow = row;
                promotionCol = col;
                return true;
            }

            whiteToMove = false;
            completeWhiteTurn();
            return true;
        }
        return false;
    }

    /**
     * Run everything that happens after white has made a move: black's reply
     * (if any) and the mate/stalemate check for both sides. Ends with the
     * board back on white's turn, or with the game finished.
     */
    private void completeWhiteTurn() {
        if (!isRunning()) return;

        // Does black still have a reply? If not: checkmate (white wins)
        // or stalemate (draw).
        if (!hasAnyLegalMove(false)) {
            finishWithoutMoves(false);
            return;
        }

        // AI responds. Guard it so a bug can never leave the game stuck in
        // the AI's turn (whiteToMove=false forever => board shows
        // "AI thinking..." and every click is rejected).
        try {
            aiMove();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally {
            whiteToMove = true;
        }

        // After the AI's move: does white still have a legal move?
        if (isRunning() && !hasAnyLegalMove(true)) {
            finishWithoutMoves(true);
        }
    }

    /** Complete a move for white; rejects anything that leaves white in check. */
    private boolean tryMove(int fr, int fc, int tr, int tc) {
        char piece = board[fr][fc];
        if (piece == ' ') return false;
        char captured = board[tr][tc];
        if (!canPieceMove(piece, fr, fc, tr, tc)) return false;
        if (leavesKingInCheck(fr, fc, tr, tc, true)) return false;
        applyMove(fr, fc, tr, tc);
        updateCastleRights(fr, fc, tr, tc, piece, captured, true);
        updateEnPassant(fr, fc, tr, tc, piece);
        return true;
    }

    /**
     * End the game because the side about to move has no legal moves.
     * @param sideToMoveNoMoves true = white has no moves, false = black
     */
    private void finishWithoutMoves(boolean sideToMoveNoMoves) {
        if (isInCheck(sideToMoveNoMoves)) {
            // Checkmate
            if (sideToMoveNoMoves) {
                result = new GameResult(name, GameResult.Status.LOST, score, getElapsedTime());
            } else {
                score += 100;
                result = new GameResult(name, GameResult.Status.WON, score, getElapsedTime());
            }
        } else {
            // Stalemate
            result = new GameResult(name, GameResult.Status.DRAWN, score, getElapsedTime());
        }
        isRunning = false;
    }

    // ═══════════════ AI ═══════════════

    private void aiMove() {
        switch (difficulty) {
            case EASY -> aiMoveEasy();
            case MEDIUM -> aiMoveMedium();
            case HARD -> aiMoveHard();
        }
    }

    /** Easy AI: picks a random legal move. */
    private void aiMoveEasy() {
        List<int[]> allMoves = getAllLegalMoves(false);
        if (allMoves.isEmpty()) return;
        int[] move = allMoves.get((int) (Math.random() * allMoves.size()));
        executeMove(move);
    }

    /** Medium AI: prefers captures of higher-value pieces, then random. */
    private void aiMoveMedium() {
        List<int[]> allMoves = getAllLegalMoves(false);
        if (allMoves.isEmpty()) return;

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
            int[] move = allMoves.get((int) (Math.random() * allMoves.size()));
            executeMove(move);
        }
    }

    /** Hard AI: one-ply evaluation over legal moves (minimizing for black). */
    private void aiMoveHard() {
        List<int[]> allMoves = getAllLegalMoves(false);
        if (allMoves.isEmpty()) return;

        int bestScore = Integer.MAX_VALUE; // Minimizing for black
        int[] bestMove = null;

        for (int[] m : allMoves) {
            char[][] sim = copyBoard();
            applyBoardMove(sim, m[0], m[1], m[2], m[3]);
            int score = evaluateBoard(sim);

            if (score < bestScore) {
                bestScore = score;
                bestMove = m;
            }
        }

        if (bestMove != null) {
            executeMove(bestMove);
        }
    }

    /** Evaluate a board position (positive = good for white, negative = good for black). */
    private int evaluateBoard(char[][] b) {
        int score = 0;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                char piece = b[r][c];
                if (piece != ' ') {
                    int val = getPieceValue(piece);
                    boolean white = Character.isUpperCase(piece);
                    if (white) {
                        score += val;
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

    // ═══════════════ Move generation & legality ═══════════════

    /**
     * All LEGAL moves for one side: pseudo-legal moves that also keep the
     * mover's own king out of check (pins are respected, escapes from check
     * are the only options while in check). Includes castling and en passant.
     */
    private List<int[]> getAllLegalMoves(boolean white) {
        List<int[]> moves = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                char piece = board[r][c];
                if (piece == ' ' || Character.isUpperCase(piece) != white) continue;
                for (int tr = 0; tr < SIZE; tr++) {
                    for (int tc = 0; tc < SIZE; tc++) {
                        if (tr == r && tc == c) continue;
                        if (!canPieceMove(piece, r, c, tr, tc)) continue;
                        if (leavesKingInCheck(r, c, tr, tc, white)) continue;
                        moves.add(new int[]{r, c, tr, tc});
                    }
                }
            }
        }
        return moves;
    }

    private boolean hasAnyLegalMove(boolean white) {
        return !getAllLegalMoves(white).isEmpty();
    }

    /**
     * Full move-shape legality of a piece move, ignoring king safety only.
     * Enforces each piece's movement pattern, slider path clearance, pawn
     * pushes/captures/en passant/double-push, and castling availability.
     */
    private boolean canPieceMove(char piece, int fr, int fc, int tr, int tc) {
        if (tr < 0 || tr >= SIZE || tc < 0 || tc >= SIZE) return false;
        int dr = tr - fr, dc = tc - fc;
        int adr = Math.abs(dr), adc = Math.abs(dc);
        char lower = Character.toLowerCase(piece);
        boolean white = Character.isUpperCase(piece);
        char target = board[tr][tc];
        // Can never capture your own color
        if (target != ' ' && Character.isUpperCase(target) == white) return false;

        switch (lower) {
            case 'p': {
                int dir = white ? -1 : 1;
                int startRow = white ? 6 : 1;
                // Single push
                if (dc == 0 && dr == dir && target == ' ') return true;
                // Double push from the home row (middle square must be empty)
                if (dc == 0 && dr == dir * 2 && fr == startRow && target == ' '
                        && board[(fr + tr) / 2][fc] == ' ') return true;
                if (adc == 1 && dr == dir) {
                    // Normal diagonal capture
                    if (target != ' ') return true;
                    // En passant: onto the empty square the enemy pawn just skipped
                    if (tr == epRow && tc == epCol) {
                        char beside = board[fr][tc];
                        return beside != ' ' && Character.isUpperCase(beside) != white
                            && Character.toLowerCase(beside) == 'p';
                    }
                }
                return false;
            }
            case 'r': {
                if ((dr == 0 || dc == 0) && (adr + adc > 0)) {
                    return isPathClear(fr, fc, tr, tc);
                }
                return false;
            }
            case 'n':
                return (adr == 2 && adc == 1) || (adr == 1 && adc == 2);
            case 'b': {
                if (adr == adc && adr > 0) {
                    return isPathClear(fr, fc, tr, tc);
                }
                return false;
            }
            case 'q': {
                if (((dr == 0 || dc == 0) || (adr == adc)) && (adr + adc > 0)) {
                    return isPathClear(fr, fc, tr, tc);
                }
                return false;
            }
            case 'k':
                // One square in any direction
                if (adr <= 1 && adc <= 1) return adr + adc > 0;
                // Castling: two squares sideways from the home square
                if (adr == 0 && adc == 2 && fr == (white ? 7 : 0) && fc == 4) {
                    return target == ' ' && canCastle(white, tc > fc);
                }
                return false;
            default:
                return false;
        }
    }

    /** True if the side may castle toward the given side. */
    private boolean canCastle(boolean white, boolean kingside) {
        int row = white ? 7 : 0;
        boolean right = white
            ? (kingside ? whiteCastleK : whiteCastleQ)
            : (kingside ? blackCastleK : blackCastleQ);
        if (!right) return false;
        // King and rook must still be on their home squares.
        if (board[row][4] != (white ? 'K' : 'k')) return false;
        if (board[row][kingside ? 7 : 0] != (white ? 'R' : 'r')) return false;
        // Squares between king and rook must be empty.
        int rookCol = kingside ? 7 : 0;
        int step = kingside ? 1 : -1;
        for (int c = 4 + step; c != rookCol; c += step) {
            if (board[row][c] != ' ') return false;
        }
        // The king may not castle out of, through, or into check.
        int[] path = kingside ? new int[]{4, 5, 6} : new int[]{4, 3, 2};
        for (int c : path) {
            if (isSquareAttacked(row, c, !white)) return false;
        }
        return true;
    }

    /** Check if path is clear (for rook/bishop/queen). Only called on aligned moves. */
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

    /** Simulate the move on a copy; true if the mover's own king would be in check. */
    private boolean leavesKingInCheck(int fr, int fc, int tr, int tc, boolean movingWhite) {
        char[][] sim = copyBoard();
        applyBoardMove(sim, fr, fc, tr, tc);
        int[] king = findKing(sim, movingWhite);
        return king == null || isSquareAttacked(sim, king[0], king[1], !movingWhite);
    }

    /** Apply a move to this board (piece motion, castling rook, en passant removal). */
    private void applyMove(int fr, int fc, int tr, int tc) {
        applyBoardMove(board, fr, fc, tr, tc);
    }

    /**
     * Move a piece on the given board, handling en passant removal (the pawn
     * is captured beside the landing square) and castling (the rook jumps
     * over the king). Used for real moves and legality simulations alike.
     */
    private static void applyBoardMove(char[][] b, int fr, int fc, int tr, int tc) {
        char piece = b[fr][fc];
        char lower = Character.toLowerCase(piece);
        // En passant: diagonal pawn move onto an empty square removes the
        // enemy pawn beside the landing square.
        if (lower == 'p' && fc != tc && b[tr][tc] == ' ') {
            b[fr][tc] = ' ';
        }
        b[tr][tc] = piece;
        b[fr][fc] = ' ';
        // Castling: the king moves two squares and the rook jumps to the
        // square the king crossed.
        if (lower == 'k' && Math.abs(tc - fc) == 2) {
            boolean kingside = tc > fc;
            b[fr][kingside ? 5 : 3] = b[fr][kingside ? 7 : 0];
            b[fr][kingside ? 7 : 0] = ' ';
        }
    }

    private char[][] copyBoard() {
        char[][] copy = new char[SIZE][];
        for (int r = 0; r < SIZE; r++) copy[r] = board[r].clone();
        return copy;
    }

    /**
     * Update castling rights after a real move (never re-granted once lost).
     * Called only from tryMove / executeMove — never from simulations.
     */
    private void updateCastleRights(int fr, int fc, int tr, int tc,
                                    char piece, char captured, boolean moverWhite) {
        // Moving the king (including castling) forfeits both of that side's rights.
        if (Character.toLowerCase(piece) == 'k') {
            setCastleRights(moverWhite, false, false);
        }
        // Moving a rook off its home corner forfeits that side's right there.
        if (Character.toLowerCase(piece) == 'r') {
            int homeRow = moverWhite ? 7 : 0;
            if (fr == homeRow && fc == 0) setCastleRight(moverWhite, false, false);
            else if (fr == homeRow && fc == 7) setCastleRight(moverWhite, true, false);
        }
        // Capturing the enemy rook on its home corner forfeits that side's right.
        if (captured != ' ' && Character.toLowerCase(captured) == 'r') {
            int homeRow = moverWhite ? 0 : 7;
            if (tr == homeRow && tc == 0) setCastleRight(!moverWhite, false, false);
            else if (tr == homeRow && tc == 7) setCastleRight(!moverWhite, true, false);
        }
    }

    private void setCastleRights(boolean white, boolean kingside, boolean queenside) {
        if (white) { whiteCastleK = kingside; whiteCastleQ = queenside; }
        else { blackCastleK = kingside; blackCastleQ = queenside; }
    }

    private void setCastleRight(boolean white, boolean kingside, boolean value) {
        if (white) { if (kingside) whiteCastleK = value; else whiteCastleQ = value; }
        else { if (kingside) blackCastleK = value; else blackCastleQ = value; }
    }

    /**
     * Clear any stale en passant target, then record the new one if this move
     * was a pawn double-push. Called from tryMove / executeMove only.
     */
    private void updateEnPassant(int fr, int fc, int tr, int tc, char piece) {
        epRow = -1;
        epCol = -1;
        if (Character.toLowerCase(piece) == 'p' && Math.abs(tr - fr) == 2) {
            epRow = (fr + tr) / 2;
            epCol = fc;
        }
    }

    /** Execute a (pre-validated) move on the board — the AI's path. */
    private void executeMove(int[] move) {
        int fr = move[0], fc = move[1], tr = move[2], tc = move[3];
        char piece = board[fr][fc];
        char captured = board[tr][tc];
        applyMove(fr, fc, tr, tc);
        // Black pawns that reach the last rank promote automatically to a
        // queen (no dialog is shown for the AI's move).
        if (piece == 'p' && tr == SIZE - 1) {
            board[tr][tc] = 'q';
        }
        updateCastleRights(fr, fc, tr, tc, piece, captured, false);
        updateEnPassant(fr, fc, tr, tc, piece);
    }

    // ═══════════════ Attack scans / check ═══════════════

    /** True if the white king is currently attacked. */
    public boolean isWhiteInCheck() {
        return isInCheck(true);
    }

    /** True if the black king is currently attacked. */
    public boolean isBlackInCheck() {
        return isInCheck(false);
    }

    private boolean isInCheck(boolean white) {
        int[] king = findKing(board, white);
        return king != null && isSquareAttacked(board, king[0], king[1], !white);
    }

    private boolean isSquareAttacked(int r, int c, boolean byWhite) {
        return isSquareAttacked(board, r, c, byWhite);
    }

    /** True if the square at (r,c) on the given board is attacked by any piece of the given color. */
    private static boolean isSquareAttacked(char[][] b, int r, int c, boolean byWhite) {
        // Pawns
        if (byWhite) {
            if (r + 1 < SIZE) {
                if (c - 1 >= 0 && b[r + 1][c - 1] == 'P') return true;
                if (c + 1 < SIZE && b[r + 1][c + 1] == 'P') return true;
            }
        } else {
            if (r - 1 >= 0) {
                if (c - 1 >= 0 && b[r - 1][c - 1] == 'p') return true;
                if (c + 1 < SIZE && b[r - 1][c + 1] == 'p') return true;
            }
        }

        // Knights
        char knight = byWhite ? 'N' : 'n';
        for (int[] off : KNIGHT_OFFSETS) {
            int nr = r + off[0], nc = c + off[1];
            if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && b[nr][nc] == knight) {
                return true;
            }
        }

        // Sliding pieces: scan rays until the first piece.
        char rook = byWhite ? 'R' : 'r';
        char bishop = byWhite ? 'B' : 'b';
        char queen = byWhite ? 'Q' : 'q';

        for (int[] dir : ROOK_DIRS) {
            int nr = r + dir[0], nc = c + dir[1];
            while (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                char p = b[nr][nc];
                if (p != ' ') {
                    if (p == rook || p == queen) return true;
                    break; // blocked
                }
                nr += dir[0];
                nc += dir[1];
            }
        }
        for (int[] dir : BISHOP_DIRS) {
            int nr = r + dir[0], nc = c + dir[1];
            while (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                char p = b[nr][nc];
                if (p != ' ') {
                    if (p == bishop || p == queen) return true;
                    break; // blocked
                }
                nr += dir[0];
                nc += dir[1];
            }
        }

        // Enemy king (kings can never sit next to each other)
        char king = byWhite ? 'K' : 'k';
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && b[nr][nc] == king) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int[] findKing(char[][] b, boolean white) {
        char target = white ? 'K' : 'k';
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (b[r][c] == target) return new int[]{r, c};
            }
        }
        return null;
    }

    // ═══════════════ Public API ═══════════════

    public char getPiece(int row, int col) { return board[row][col]; }
    public boolean isWhiteToMove() { return whiteToMove; }

    /** True while the game waits for the player to choose a promotion piece. */
    public boolean isPromotionPending() {
        return promotionPending && promotionRow >= 0 && promotionCol >= 0
            && board[promotionRow][promotionCol] == 'P';
    }

    /**
     * Finish a pending white pawn promotion with the chosen piece and then
     * continue the game normally (AI reply, mate/stalemate checks).
     * @param piece one of 'Q', 'R', 'B', 'N'
     * @return true if the promotion was applied
     */
    public boolean promote(char piece) {
        if (!isPromotionPending() || !isRunning()) return false;
        char up = Character.toUpperCase(piece);
        if (up != 'Q' && up != 'R' && up != 'B' && up != 'N') return false;

        board[promotionRow][promotionCol] = up;
        promotionPending = false;
        promotionRow = -1;
        promotionCol = -1;
        completeWhiteTurn();
        return true;
    }

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
