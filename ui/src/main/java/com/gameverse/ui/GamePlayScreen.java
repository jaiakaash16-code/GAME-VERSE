package com.gameverse.ui;

import com.gameverse.achievements.AchievementManager;
import com.gameverse.core.Difficulty;
import com.gameverse.core.Game;
import com.gameverse.core.GameManager;
import com.gameverse.core.GameResult;
import com.gameverse.games.chess.ChessGame;
import com.gameverse.games.memory.MemoryGame;
import com.gameverse.games.snake.SnakeGame;
import com.gameverse.leaderboard.LeaderboardManager;
import com.gameverse.player.Player;
import com.gameverse.rewards.CoinManager;
import com.gameverse.rewards.XPManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Fully interactive game screen — all 6 games playable with keyboard and mouse.
 * Includes difficulty selection (Easy, Medium, Hard) for all games.
 */
public class GamePlayScreen extends JFrame {

    private Player player;
    private String gameName;
    private GameManager gameManager;
    private Game game;
    private Timer gameLoop;
    private JLabel statusLabel;
    private JLabel scoreLabel;
    private JLabel timerLabel;
    private JLabel difficultyLabel;
    private JPanel boardPanel;
    private long startTime;
    private Runnable onGameFinished;
    private boolean gameEnding = false;
    private Difficulty currentDifficulty = Difficulty.MEDIUM;
    private int hoverRow = -1, hoverCol = -1;
    private boolean upHeld, downHeld; // held-key state for continuous Pong paddle movement
    private final Map<Difficulty, JButton> diffButtons = new HashMap<>();

    // Colors
    private static final Color BG = new Color(18, 18, 28);
    private static final Color PANEL_BG = new Color(28, 28, 42);
    private static final Color ACCENT = new Color(100, 150, 255);
    private static final Color GREEN = new Color(80, 200, 120);
    private static final Color RED = new Color(220, 80, 80);
    private static final Color GOLD = new Color(255, 200, 60);
    private static final Color TEXT = new Color(210, 210, 225);
    private static final Color TEXT_DIM = new Color(140, 140, 160);
    private static final Color CARD_BG = new Color(35, 35, 55);
    private static final Color EASY_COLOR = new Color(80, 200, 120);
    private static final Color MEDIUM_COLOR = new Color(255, 200, 60);
    private static final Color HARD_COLOR = new Color(220, 80, 80);

    public GamePlayScreen(Player player, String gameName, Runnable onGameFinished) {
        this.player = player;
        this.gameName = gameName;
        this.onGameFinished = onGameFinished;
        this.gameManager = new GameManager();
        initGame();
        initUI();
        setupInput();
        startGameLoop();
        setVisible(true);
        // The window is not displayable yet inside setupInput(), so a focus request
        // made there is silently ignored. Grab keyboard focus once the frame is
        // actually shown, and re-grab it whenever the window regains activation
        // (after Alt-Tab, dialogs, etc.) so arrow keys always reach the board.
        SwingUtilities.invokeLater(() -> boardPanel.requestFocusInWindow());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                if (boardPanel != null) boardPanel.requestFocusInWindow();
            }
            @Override
            public void windowDeactivated(WindowEvent e) {
                upHeld = false;
                downHeld = false;
            }
        });
    }

    /* ═══════════════ INIT ═══════════════ */

    private void initGame() {
        if (!gameManager.loadGame(gameName)) {
            JOptionPane.showMessageDialog(null, "Could not load: " + gameName);
            dispose();
            if (onGameFinished != null) onGameFinished.run();
            return;
        }
        game = gameManager.getCurrentGame();
        // Apply difficulty to game
        if (game instanceof com.gameverse.games.core.BaseGame) {
            ((com.gameverse.games.core.BaseGame) game).setDifficulty(currentDifficulty);
        }
        gameManager.startGame();
        startTime = System.currentTimeMillis();
    }

    private void initUI() {
        setTitle("GameVerse — " + gameName);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        setContentPane(root);

        root.add(createTopBar(), BorderLayout.NORTH);
        boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame((Graphics2D) g);
            }
        };
        boardPanel.setBackground(new Color(12, 12, 20));
        boardPanel.setPreferredSize(new Dimension(700, 400));
        root.add(boardPanel, BorderLayout.CENTER);
        root.add(createControls(), BorderLayout.SOUTH);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(PANEL_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 70)),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        
        // Left side: title + difficulty selector
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        
        JLabel title = new JLabel("\uD83C\uDFAE " + gameName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(ACCENT);
        
        // Difficulty buttons
        JPanel diffPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        diffPanel.setOpaque(false);
        diffPanel.add(createDiffButton("Easy", Difficulty.EASY));
        diffPanel.add(createDiffButton("Medium", Difficulty.MEDIUM));
        diffPanel.add(createDiffButton("Hard", Difficulty.HARD));
        
        left.add(title);
        left.add(diffPanel);
        
        // Right side: score + timer
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        right.setOpaque(false);
        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        scoreLabel.setForeground(GOLD);
        timerLabel = new JLabel("Time: 0s");
        timerLabel.setFont(new Font("Consolas", Font.PLAIN, 13));
        timerLabel.setForeground(TEXT_DIM);
        right.add(scoreLabel);
        right.add(timerLabel);
        
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JButton createDiffButton(String label, Difficulty diff) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80)),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        
        btn.addActionListener(e -> {
            if (currentDifficulty == diff) return;
            currentDifficulty = diff;
            // Refresh the button highlights so the selection is visible.
            updateDiffButtons();
            restartGame();
        });
        
        diffButtons.put(diff, btn);
        applyDiffStyle(btn, diff);
        return btn;
    }

    private void updateDiffButtons() {
        diffButtons.forEach((diff, btn) -> applyDiffStyle(btn, diff));
    }

    private void applyDiffStyle(JButton btn, Difficulty diff) {
        Color bgColor = switch (diff) {
            case EASY -> EASY_COLOR;
            case MEDIUM -> MEDIUM_COLOR;
            case HARD -> HARD_COLOR;
        };
        if (currentDifficulty == diff) {
            btn.setBackground(bgColor);
            btn.setForeground(Color.WHITE);
        } else {
            btn.setBackground(CARD_BG);
            btn.setForeground(TEXT_DIM);
        }
        btn.repaint();
    }

    private JPanel createControls() {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(PANEL_BG);
        c.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 70)),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        statusLabel = new JLabel(getHint());
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_DIM);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton restart = makeBtn("\uD83D\uDD04 Restart", ACCENT);
        restart.addActionListener(e -> restartGame());
        JButton end = makeBtn("\uD83C\uDFC1 End", RED);
        end.addActionListener(e -> endGame(true));
        btns.add(restart);
        btns.add(end);
        c.add(statusLabel, BorderLayout.WEST);
        c.add(btns, BorderLayout.EAST);
        return c;
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private String getHint() {
        String diffStr = " [" + currentDifficulty.getDisplayName() + "]";
        return switch (gameName) {
            case "Snake" -> "\uD83C\uDFAE Arrow keys / WASD \u2190\u2191\u2192\u2193 to move | Eat food to grow" + diffStr;
            case "Pong" -> "\uD83C\uDFAE \u2191/\u2193 or W/S to move paddle | First to 5 wins" + diffStr;
            case "Tic-Tac-Toe" -> "\uD83C\uDFAE Click cell to place X | Beat the AI!" + diffStr;
            case "Memory Game" -> "\uD83C\uDFAE Click cards to flip | Beat the AI opponent!" + diffStr;
            case "Mini Racing" -> "\uD83C\uDFAE \u2191 accelerate, \u2193 brake | Race to the finish!" + diffStr;
            case "Chess" -> "\uD83C\uDFAE Click white piece \u2192 click destination | Capture the black king!" + diffStr;
            default -> "\uD83C\uDFAE Play!" + diffStr;
        };
    }

    /* ═══════════════ INPUT ═══════════════ */

    private void setupInput() {
        boardPanel.setFocusable(true);
        boardPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_R) restartGame();
                else if (code == KeyEvent.VK_ESCAPE) endGame(true);
                else if ("Pong".equals(gameName)
                        && (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN
                            || code == KeyEvent.VK_W || code == KeyEvent.VK_S)) {
                    // Track held state — the game loop moves the paddle each frame
                    // while a key is down, so holding \u2191/\u2193 glides smoothly.
                    if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) upHeld = true;
                    else downHeld = true;
                } else {
                    handleKey(code);
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) upHeld = false;
                else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) downHeld = false;
            }
        });
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { boardPanel.requestFocusInWindow(); }
            @Override
            public void mouseClicked(MouseEvent e) { handleClick(e.getX(), e.getY()); }
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverRow != -1 || hoverCol != -1) { hoverRow = -1; hoverCol = -1; boardPanel.repaint(); }
            }
        });
        boardPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) { updateHover(e.getX(), e.getY()); }
        });
        boardPanel.requestFocusInWindow();
    }

    /** Track the cell under the mouse for hover highlights (Tic-Tac-Toe). */
    private void updateHover(int mx, int my) {
        if (!"Tic-Tac-Toe".equals(gameName)) {
            if (hoverRow != -1 || hoverCol != -1) { hoverRow = -1; hoverCol = -1; boardPanel.repaint(); }
            return;
        }
        int bs = Math.min(boardPanel.getWidth(), boardPanel.getHeight()) - 80;
        int cs = bs / 3;
        int sx = (boardPanel.getWidth() - bs) / 2;
        int sy = (boardPanel.getHeight() - bs) / 2;
        int col = (mx - sx) / cs, row = (my - sy) / cs;
        boolean in = row >= 0 && row < 3 && col >= 0 && col < 3;
        int nr = in ? row : -1, nc = in ? col : -1;
        if (nr != hoverRow || nc != hoverCol) { hoverRow = nr; hoverCol = nc; boardPanel.repaint(); }
    }

    private void handleKey(int code) {
        if (game == null || !game.isRunning()) return;
        try {
            switch (gameName) {
                case "Snake" -> {
                    String d = switch (code) {
                        case KeyEvent.VK_UP, KeyEvent.VK_W -> "up";
                        case KeyEvent.VK_DOWN, KeyEvent.VK_S -> "down";
                        case KeyEvent.VK_LEFT, KeyEvent.VK_A -> "left";
                        case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> "right";
                        default -> null;
                    };
                    if (d != null) game.getClass().getMethod("moveSnake", String.class).invoke(game, d);
                }
                // Pong movement is handled by the game loop while \u2191/\u2193/W/S are held.
                case "Mini Racing" -> {
                    if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W || code == KeyEvent.VK_SPACE)
                        game.getClass().getMethod("accelerate").invoke(game);
                    else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S)
                        game.getClass().getMethod("decelerate").invoke(game);
                }
            }
        } catch (Exception ignored) {}
        boardPanel.repaint();
    }

    private void handleClick(int mx, int my) {
        if (game == null || !game.isRunning()) return;
        try {
            switch (gameName) {
                case "Tic-Tac-Toe" -> {
                    int bs = Math.min(boardPanel.getWidth(), boardPanel.getHeight()) - 80;
                    int cs = bs / 3;
                    int sx = (boardPanel.getWidth() - bs) / 2;
                    int sy = (boardPanel.getHeight() - bs) / 2;
                    int col = (mx - sx) / cs, row = (my - sy) / cs;
                    if (row >= 0 && row < 3 && col >= 0 && col < 3) {
                        boolean ok = (boolean) game.getClass().getMethod("makeMove", int.class, int.class).invoke(game, row, col);
                        if (ok) {
                            game.update(0.1f);
                            statusLabel.setText("\u274C Move made! Score: " + game.getScore());
                        } else {
                            statusLabel.setText("\u274C Cell occupied!");
                        }
                    }
                }
                case "Memory Game" -> {
                    MemoryGame mg = (MemoryGame) game;
                    if (!mg.isPlayerTurn()) {
                        statusLabel.setText("\u23F3 AI is thinking...");
                        return;
                    }
                    int gs = mg.getGridSize();
                    int cs = Math.min(boardPanel.getWidth(), boardPanel.getHeight()) / (gs + 2);
                    int sx = (boardPanel.getWidth() - gs * cs) / 2;
                    int sy = (boardPanel.getHeight() - gs * cs) / 2;
                    int col = (mx - sx) / cs, row = (my - sy) / cs;
                    if (row >= 0 && row < gs && col >= 0 && col < gs) {
                        boolean ok = mg.flipCard(row, col);
                        if (ok) statusLabel.setText("\uD83E\uDDE0 Score: " + game.getScore() + " | You: " + mg.getMatchesFound() + " | AI: " + mg.getAiMatchesFound() + "/" + mg.getTotalPairs());
                    }
                }
                case "Chess" -> {
                    ChessGame cg = (ChessGame) game;
                    int bs = Math.min(boardPanel.getWidth(), boardPanel.getHeight()) - 40;
                    int cs = bs / 8;
                    int sx = (boardPanel.getWidth() - bs) / 2;
                    int sy = (boardPanel.getHeight() - bs) / 2;
                    int col = (mx - sx) / cs, row = (my - sy) / cs;
                    if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                        boolean ok = cg.handleClick(row, col);
                        statusLabel.setText(ok ? "\u265F\uFE0F Move made! Score: " + game.getScore() : "\u265F\uFE0F Invalid move");
                    }
                }
            }
        } catch (Exception e) { statusLabel.setText("Error: " + e.getMessage()); }
        boardPanel.repaint();
    }

    /* ═══════════════ GAME LOOP ═══════════════ */

    private void startGameLoop() {
        int ms = getGameLoopInterval();
        gameLoop = new Timer(ms, e -> {
            if (game == null || gameEnding) return;
            if (!game.isRunning() && game.getResult() != null) { endGame(false); return; }
            timerLabel.setText("Time: " + (System.currentTimeMillis() - startTime) / 1000 + "s");
            scoreLabel.setText("Score: " + game.getScore());
            // Continuous Pong paddle movement while a direction key is held.
            if ("Pong".equals(gameName) && game.isRunning() && (upHeld || downHeld)) {
                try {
                    if (upHeld) game.getClass().getMethod("movePaddleUp").invoke(game);
                    if (downHeld) game.getClass().getMethod("movePaddleDown").invoke(game);
                } catch (Exception ignored) {}
            }
            game.update(ms / 1000f);
            boardPanel.repaint();
            if (game.getResult() != null) endGame(false);
        });
        gameLoop.start();
    }

    private int getGameLoopInterval() {
        int base = switch (gameName) {
            case "Snake" -> 150;
            case "Pong" -> 16;
            case "Memory Game", "Chess" -> 50;
            default -> 33;
        };
        // Adjust for difficulty
        if ("Snake".equals(gameName)) {
            return switch (currentDifficulty) {
                case EASY -> 200;
                case MEDIUM -> 150;
                case HARD -> 100;
            };
        }
        return base;
    }

    /* ═══════════════ DRAWING ═══════════════ */

    private void drawGame(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = boardPanel.getWidth(), h = boardPanel.getHeight();
        if (game == null) { drawCenter(g2, "Loading...", w, h); return; }
        if (!game.isRunning() && game.getResult() == null) { drawCenter(g2, "Press \uD83D\uDD04 Restart to play!", w, h); return; }

        switch (gameName) {
            case "Tic-Tac-Toe" -> drawTicTacToe(g2, w, h);
            case "Snake" -> drawSnake(g2, w, h);
            case "Pong" -> drawPong(g2, w, h);
            case "Memory Game" -> drawMemory(g2, w, h);
            case "Mini Racing" -> drawRacing(g2, w, h);
            case "Chess" -> drawChess(g2, w, h);
            default -> drawCenter(g2, gameName, w, h);
        }
    }

    /* ──── TIC-TAC-TOE ──── */
    private void drawTicTacToe(Graphics2D g2, int w, int h) {
        try {
            char[][] board = (char[][]) game.getClass().getMethod("getBoard").invoke(game);
            int bs = Math.min(w, h) - 80;
            int cs = bs / 3;
            int sx = (w - bs) / 2, sy = (h - bs) / 2;

            // Board background
            g2.setColor(new Color(25, 25, 40));
            g2.fillRoundRect(sx - 10, sy - 10, bs + 20, bs + 20, 16, 16);

            // Grid
            g2.setColor(new Color(70, 70, 100));
            g2.setStroke(new BasicStroke(3));
            for (int i = 1; i < 3; i++) {
                g2.drawLine(sx + i * cs, sy, sx + i * cs, sy + bs);
                g2.drawLine(sx, sy + i * cs, sx + bs, sy + i * cs);
            }

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    int x = sx + c * cs, y = sy + r * cs;
                    // Cell background
                    g2.setColor(new Color(35, 35, 55));
                    g2.fillRoundRect(x + 5, y + 5, cs - 10, cs - 10, 8, 8);

                    // Hover highlight on empty cell
                    if (r == hoverRow && c == hoverCol && board[r][c] == ' ') {
                        g2.setColor(new Color(100, 150, 255, 45));
                        g2.fillRoundRect(x + 5, y + 5, cs - 10, cs - 10, 8, 8);
                        g2.setColor(new Color(100, 150, 255, 120));
                        g2.drawRoundRect(x + 5, y + 5, cs - 10, cs - 10, 8, 8);
                    }

                    char ch = board[r][c];
                    if (ch != ' ') {
                        g2.setFont(new Font("Segoe UI", Font.BOLD, cs / 2));
                        g2.setColor(ch == 'X' ? ACCENT : RED);
                        String s = String.valueOf(ch);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(s, x + (cs - fm.stringWidth(s)) / 2,
                            y + (cs + fm.getAscent() - fm.getDescent()) / 2);
                    }
                }
            }
            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2.drawString("You = X (blue)  |  AI = O (red)  |  [" + currentDifficulty.getDisplayName() + "]", sx, sy - 15);
        } catch (Exception e) { drawCenter(g2, "Tic-Tac-Toe", w, h); }
    }

    /* ──── SNAKE ──── */
    private void drawSnake(Graphics2D g2, int w, int h) {
        try {
            SnakeGame sg = (SnakeGame) game;
            Field snakeF = game.getClass().getDeclaredField("snake");
            snakeF.setAccessible(true);
            Object snake = snakeF.get(game);
            Object head = snake.getClass().getDeclaredMethod("getHead").invoke(snake);
            Field xf = head.getClass().getDeclaredField("x"); xf.setAccessible(true);
            Field yf = head.getClass().getDeclaredField("y"); yf.setAccessible(true);
            int hx = xf.getInt(head), hy = yf.getInt(head);

            Field foodF = game.getClass().getDeclaredField("food");
            foodF.setAccessible(true);
            Object food = foodF.get(game);
            Object fpos = food.getClass().getMethod("getPosition").invoke(food);
            int fx = xf.getInt(fpos), fy = yf.getInt(fpos);

            Field bWF = game.getClass().getDeclaredField("boardWidth"); bWF.setAccessible(true);
            Field bHF = game.getClass().getDeclaredField("boardHeight"); bHF.setAccessible(true);
            int bw = bWF.getInt(game), bh = bHF.getInt(game);
            int cs = Math.min(w / bw, h / bh);
            int ox = (w - bw * cs) / 2, oy = (h - bh * cs) / 2;

            // Grid
            for (int x = 0; x < bw; x++)
                for (int y = 0; y < bh; y++) {
                    g2.setColor((x + y) % 2 == 0 ? new Color(18, 22, 32) : new Color(22, 28, 40));
                    g2.fillRect(ox + x * cs, oy + y * cs, cs - 1, cs - 1);
                }

            // AI snake (if present)
            if (sg.hasAiSnake()) {
                // Get Position class from SnakeGame via reflection
                Class<?> posClass = null;
                for (Class<?> inner : SnakeGame.class.getDeclaredClasses()) {
                    if (inner.getSimpleName().equals("Position")) {
                        posClass = inner;
                        break;
                    }
                }
                if (posClass != null) {
                    Field pxField = posClass.getDeclaredField("x"); pxField.setAccessible(true);
                    Field pyField = posClass.getDeclaredField("y"); pyField.setAccessible(true);

                    Object aiHead = sg.getAiSnakeHead();
                    if (aiHead != null) {
                        int ahx = pxField.getInt(aiHead), ahy = pyField.getInt(aiHead);
                        g2.setColor(RED);
                        g2.fillRoundRect(ox + ahx * cs + 1, oy + ahy * cs + 1, cs - 2, cs - 2, 6, 6);
                        g2.setColor(new Color(255, 120, 120));
                        g2.fillOval(ox + ahx * cs + cs/3, oy + ahy * cs + cs/4, 5, 5);
                    }
                    Object aiFoodPos = sg.getAiFoodPosition();
                    if (aiFoodPos != null) {
                        int afx = pxField.getInt(aiFoodPos), afy = pyField.getInt(aiFoodPos);
                        g2.setColor(GOLD);
                        g2.fillOval(ox + afx * cs + 3, oy + afy * cs + 3, cs - 6, cs - 6);
                    }
                }
            }

            // Food (red apple)
            g2.setColor(RED);
            g2.fillOval(ox + fx * cs + 3, oy + fy * cs + 3, cs - 6, cs - 6);
            g2.setColor(new Color(255, 120, 120));
            g2.fillOval(ox + fx * cs + cs/3, oy + fy * cs + cs/4, 4, 4);

            // Snake head
            g2.setColor(GREEN);
            g2.fillRoundRect(ox + hx * cs + 1, oy + hy * cs + 1, cs - 2, cs - 2, 6, 6);
            g2.setColor(new Color(140, 255, 180));
            g2.fillOval(ox + hx * cs + cs/3, oy + hy * cs + cs/4, 5, 5);

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            String hint = "Arrow keys to move  |  Eat the red food!  |  [" + currentDifficulty.getDisplayName() + "]";
            if (sg.hasAiSnake()) hint += "  |  AI snake active!";
            g2.drawString(hint, 15, h - 10);
        } catch (Exception e) { drawCenter(g2, "Snake — Use arrow keys!", w, h); }
    }

    /* ──── PONG ──── */
    private void drawPong(Graphics2D g2, int w, int h) {
        try {
            Field ballF = game.getClass().getDeclaredField("ball"); ballF.setAccessible(true);
            Object ball = ballF.get(game);
            Field bxf = ball.getClass().getDeclaredField("x"); bxf.setAccessible(true);
            Field byf = ball.getClass().getDeclaredField("y"); byf.setAccessible(true);
            double bx = bxf.getDouble(ball), by = byf.getDouble(ball);

            Field ppF = game.getClass().getDeclaredField("playerPaddle"); ppF.setAccessible(true);
            Object pp = ppF.get(game);
            Field ppxF = pp.getClass().getDeclaredField("x"); ppxF.setAccessible(true);
            Field ppyF = pp.getClass().getDeclaredField("y"); ppyF.setAccessible(true);
            Field ppwF = pp.getClass().getDeclaredField("width"); ppwF.setAccessible(true);
            Field pphF = pp.getClass().getDeclaredField("height"); pphF.setAccessible(true);
            int ppx = ppxF.getInt(pp), ppy = ppyF.getInt(pp), ppw = ppwF.getInt(pp), pph = pphF.getInt(pp);

            Field apF = game.getClass().getDeclaredField("aiPaddle"); apF.setAccessible(true);
            Object ap = apF.get(game);
            Field apxF = ap.getClass().getDeclaredField("x"); apxF.setAccessible(true);
            Field apyF = ap.getClass().getDeclaredField("y"); apyF.setAccessible(true);
            Field apwF = ap.getClass().getDeclaredField("width"); apwF.setAccessible(true);
            Field aphF = ap.getClass().getDeclaredField("height"); aphF.setAccessible(true);
            int apx = apxF.getInt(ap), apy = apyF.getInt(ap), apw = apwF.getInt(ap), aph = aphF.getInt(ap);

            Field psF = game.getClass().getDeclaredField("playerScore"); psF.setAccessible(true);
            Field asF = game.getClass().getDeclaredField("aiScore"); asF.setAccessible(true);
            int pScore = psF.getInt(game), aScore = asF.getInt(game);

            double sx = w / 800.0, sy = h / 600.0;

            // Center dashed line
            g2.setColor(new Color(50, 50, 70));
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 6}, 0));
            g2.drawLine(w / 2, 0, w / 2, h);

            // Scores
            g2.setFont(new Font("Segoe UI", Font.BOLD, 48));
            g2.setColor(ACCENT);
            g2.drawString(String.valueOf(pScore), w / 4 - 15, 70);
            g2.setColor(RED);
            g2.drawString(String.valueOf(aScore), 3 * w / 4 - 15, 70);

            // Paddles
            g2.setColor(ACCENT);
            g2.fillRoundRect((int)(ppx * sx), (int)(ppy * sy), (int)(ppw * sx), (int)(pph * sy), 6, 6);
            g2.setColor(RED);
            g2.fillRoundRect((int)(apx * sx), (int)(apy * sy), (int)(apw * sx), (int)(aph * sy), 6, 6);

            // Ball
            g2.setColor(GOLD);
            g2.fillOval((int)(bx * sx) - 6, (int)(by * sy) - 6, 12, 12);

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.drawString("You (blue left) vs AI (red right)  |  \u2191\u2193 / W/S to move  |  [" + currentDifficulty.getDisplayName() + "]", 15, h - 10);
        } catch (Exception e) { drawCenter(g2, "Pong — \u2191\u2193 to move", w, h); }
    }

    /* ──── MEMORY GAME ──── */
    private void drawMemory(Graphics2D g2, int w, int h) {
        MemoryGame mg = (MemoryGame) game;
        int gs = mg.getGridSize();
        int cs = Math.min(w, h) / (gs + 2);
        int sx = (w - gs * cs) / 2, sy = (h - gs * cs) / 2;

        for (int r = 0; r < gs; r++) {
            for (int c = 0; c < gs; c++) {
                int x = sx + c * cs + 3, y = sy + r * cs + 3;
                int cw = cs - 6, ch = cs - 6;

                boolean flipped = mg.isCardFlipped(r, c);
                boolean matched = mg.isCardMatched(r, c);
                int val = mg.getCardValue(r, c);
                String sym = MemoryGame.getSymbolForValue(val);
                Color fruitColor = MemoryGame.getColorForValue(val);

                if (matched) {
                    g2.setColor(new Color(30, 70, 30));
                    g2.fillRoundRect(x, y, cw, ch, 10, 10);
                    g2.setColor(new Color(50, 100, 50));
                    g2.drawRoundRect(x, y, cw, ch, 10, 10);
                    drawFruit(g2, sym, fruitColor, x, y, cw, ch, true);
                } else if (flipped) {
                    g2.setColor(new Color(50, 50, 80));
                    g2.fillRoundRect(x, y, cw, ch, 10, 10);
                    g2.setColor(ACCENT);
                    g2.drawRoundRect(x, y, cw, ch, 10, 10);
                    drawFruit(g2, sym, fruitColor, x, y, cw, ch, false);
                } else {
                    g2.setColor(CARD_BG);
                    g2.fillRoundRect(x, y, cw, ch, 10, 10);
                    g2.setColor(new Color(60, 60, 90));
                    g2.drawRoundRect(x, y, cw, ch, 10, 10);
                    g2.setColor(new Color(50, 50, 75));
                    g2.fillRoundRect(x + cw/4, y + ch/4, cw/2, ch/2, 6, 6);
                    g2.setColor(new Color(80, 80, 110));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, cw / 3));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("?", x + (cw - fm.stringWidth("?")) / 2,
                        y + (ch + fm.getAscent() - fm.getDescent()) / 2);
                }
            }
        }

        // Turn indicator
        Color turnColor = mg.isPlayerTurn() ? ACCENT : RED;
        String turnText = mg.isPlayerTurn() ? "YOUR TURN" : (mg.isAiThinking() ? "AI THINKING..." : "AI TURN");
        g2.setColor(turnColor);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString(turnText, sx, sy - 20);

        g2.setColor(TEXT_DIM);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("You: " + mg.getMatchesFound() + " | AI: " + mg.getAiMatchesFound() + "/" + mg.getTotalPairs() + "  |  Attempts: " + mg.getAttempts() + "  |  [" + currentDifficulty.getDisplayName() + "]", 15, h - 10);
    }

    /**
     * Draw a memory-game fruit in its own color: a soft colored glow behind the
     * card, a drop shadow, the tinted emoji glyph, and a green \u2713 badge when
     * the pair is matched.
     */
    private void drawFruit(Graphics2D g2, String sym, Color color, int x, int y, int cw, int ch, boolean matched) {
        int size = cw / 2;

        // Soft glow in the fruit's color
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
        g2.fillOval(x + cw / 2 - size / 2 - 6, y + ch / 2 - size / 2 - 6, size + 12, size + 12);

        // Drop shadow
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, size));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(0, 0, 0, 90));
        g2.drawString(sym, x + (cw - fm.stringWidth(sym)) / 2 + 2,
            y + (ch + fm.getAscent() - fm.getDescent()) / 2 + 2);

        // Fruit glyph tinted with its own color
        g2.setColor(color);
        g2.drawString(sym, x + (cw - fm.stringWidth(sym)) / 2,
            y + (ch + fm.getAscent() - fm.getDescent()) / 2);

        // Match badge
        if (matched) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            FontMetrics cfm = g2.getFontMetrics();
            g2.setColor(GREEN);
            g2.drawString("\u2713", x + cw - cfm.stringWidth("\u2713") - 4, y + cfm.getAscent());
        }
    }

    /* ──── RACING ──── */
    private void drawRacing(Graphics2D g2, int w, int h) {
        try {
            com.gameverse.games.racing.RacingGame rg = (com.gameverse.games.racing.RacingGame) game;
            int pos = rg.getPlayerPosition();
            int speed = rg.getPlayerSpeed();
            Field tlF = game.getClass().getDeclaredField("trackLength"); tlF.setAccessible(true);
            int trackLen = tlF.getInt(game);

            g2.setColor(new Color(35, 35, 45));
            g2.fillRect(0, 0, w, h);

            // Road edges
            g2.setColor(new Color(60, 60, 75));
            g2.fillRect(0, 0, 8, h);
            g2.fillRect(w - 8, 0, 8, h);

            // Scrolling lane markings
            int scroll = pos % 40;
            g2.setColor(new Color(80, 80, 100));
            for (int y = -scroll; y < h; y += 40)
                g2.fillRect(w / 2 - 2, y, 4, 20);

            // Trees scrolling
            for (int y = -(scroll * 2) % 80; y < h; y += 80) {
                g2.setColor(new Color(40, 65, 40));
                g2.fillOval(20, y, 18, 18);
                g2.fillOval(w - 38, y + 30, 18, 18);
            }

            // Draw AI cars
            if (rg.hasAICars()) {
                for (var aiCar : rg.getAICars()) {
                    int carProgress = (int)(aiCar.position / trackLen * (h - 140));
                    int carX = w / 2 + (int)((aiCar.lane - 50) * 0.8);
                    int carY = h - 100 - carProgress;
                    
                    // AI car body
                    g2.setColor(RED);
                    g2.fillRoundRect(carX - 12, carY - 25, 24, 45, 8, 8);
                    g2.setColor(new Color(255, 150, 150));
                    g2.fillRoundRect(carX - 8, carY - 20, 16, 10, 4, 4);
                }
            }

            // Player car
            int cx = w / 2 - 18, cy = h - 100;
            g2.setColor(GREEN);
            g2.fillRoundRect(cx, cy, 36, 60, 10, 10);
            g2.setColor(new Color(150, 200, 255));
            g2.fillRoundRect(cx + 4, cy + 5, 28, 14, 6, 6);
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(cx - 3, cy + 5, 5, 12);
            g2.fillRect(cx + 34, cy + 5, 5, 12);
            g2.fillRect(cx - 3, cy + 42, 5, 12);
            g2.fillRect(cx + 34, cy + 42, 5, 12);

            // Progress bar
            float progress = Math.min(1f, Math.max(0f, (float) pos / trackLen));
            g2.setColor(new Color(40, 40, 55));
            g2.fillRect(20, h - 30, w - 40, 12);
            g2.setColor(GREEN);
            g2.fillRect(20, h - 30, (int)((w - 40) * progress), 12);
            g2.setColor(TEXT);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.drawString((int)(progress * 100) + "%", w / 2 - 10, h - 20);

            g2.setColor(GOLD);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
            g2.drawString("Speed: " + speed, 20, 30);
            g2.drawString(pos + " / " + trackLen, 20, 55);

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            String hint = "\u2191 Accelerate  |  \u2193 Brake  |  [" + currentDifficulty.getDisplayName() + "]";
            if (rg.hasAICars()) hint += "  |  AI cars on track!";
            g2.drawString(hint, 15, h - 40);
        } catch (Exception e) { drawCenter(g2, "Racing — \u2191 to accelerate!", w, h); }
    }

    /* ──── CHESS ──── */
    private void drawChess(Graphics2D g2, int w, int h) {
        ChessGame cg = (ChessGame) game;
        int bs = Math.min(w, h) - 40;
        int cs = bs / 8;
        int sx = (w - bs) / 2, sy = (h - bs) / 2;

        // Column labels
        g2.setColor(TEXT_DIM);
        g2.setFont(new Font("Consolas", Font.PLAIN, 11));
        for (int c = 0; c < 8; c++) {
            g2.drawString(String.valueOf((char)('a' + c)), sx + c * cs + cs / 2 - 3, sy - 5);
        }

        for (int r = 0; r < 8; r++) {
            // Row labels
            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Consolas", Font.PLAIN, 11));
            g2.drawString(String.valueOf(8 - r), sx - 14, sy + r * cs + cs / 2 + 4);

            for (int c = 0; c < 8; c++) {
                int x = sx + c * cs, y = sy + r * cs;

                // Square colors
                boolean light = (r + c) % 2 == 0;
                g2.setColor(light ? new Color(200, 210, 190) : new Color(100, 130, 90));
                g2.fillRect(x, y, cs, cs);

                // Selected highlight
                if (r == cg.getSelectedRow() && c == cg.getSelectedCol()) {
                    g2.setColor(new Color(255, 255, 0, 120));
                    g2.fillRect(x, y, cs, cs);
                }

                // Piece
                char piece = cg.getPiece(r, c);
                if (piece != ' ') {
                    String sym = ChessGame.getSymbol(piece);
                    boolean white = ChessGame.isWhitePiece(piece);

                    // Piece shadow
                    g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, cs - 10));
                    g2.setColor(new Color(0, 0, 0, 60));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(sym, x + (cs - fm.stringWidth(sym)) / 2 + 2,
                        y + (cs + fm.getAscent() - fm.getDescent()) / 2 + 2);

                    // Piece
                    g2.setColor(white ? new Color(255, 255, 240) : new Color(40, 40, 40));
                    g2.drawString(sym, x + (cs - fm.stringWidth(sym)) / 2,
                        y + (cs + fm.getAscent() - fm.getDescent()) / 2);

                    // Piece outline for black pieces
                    if (!white) {
                        g2.setColor(new Color(100, 100, 100));
                        g2.drawOval(x + cs/4, y + cs/4, cs/2, cs/2);
                    }
                }
            }
        }

        g2.setColor(TEXT_DIM);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        String turn = cg.isWhiteToMove() ? "Your turn (white)" : "AI thinking...";
        g2.drawString(turn + "  |  Click piece \u2192 click destination  |  [" + currentDifficulty.getDisplayName() + "]", 15, h - 5);
    }

    private void drawCenter(Graphics2D g2, String msg, int w, int h) {
        g2.setColor(TEXT);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2);
    }

    /* ═══════════════ RESTART / END ═══════════════ */

    private void restartGame() {
        if (gameLoop != null) gameLoop.stop();
        gameEnding = false;
        gameManager = new GameManager();
        initGame();
        startTime = System.currentTimeMillis();
        startGameLoop();
        boardPanel.requestFocusInWindow();
        statusLabel.setText(getHint());
    }

    private void endGame(boolean abandoned) {
        if (gameEnding) return;
        gameEnding = true;
        if (gameLoop != null) gameLoop.stop();

        long elapsed = System.currentTimeMillis() - startTime;
        GameResult result;
        if (abandoned || game.getResult() == null) {
            boolean won = game.getScore() > 50;
            result = new GameResult(gameName, won ? GameResult.Status.WON : GameResult.Status.COMPLETED, game.getScore(), elapsed);
        } else {
            result = game.getResult();
            result.setDuration(elapsed);
        }

        XPManager xpMgr = XPManager.getInstance();
        int xp = xpMgr.calculateXpReward(result.getScore(), result.getStatus() == GameResult.Status.WON, elapsed / 1000);
        boolean lvlUp = xpMgr.awardXp(player, xp);

        boolean hs = result.getScore() > player.getGameHighScore(gameName);
        if (hs) player.recordHighScore(gameName, result.getScore());
        CoinManager.getInstance().awardGameReward(player, result.getStatus() == GameResult.Status.WON, hs);

        boolean won = result.getStatus() == GameResult.Status.WON;
        player.recordGamePlay(result.getScore(), won, result.getStatus() == GameResult.Status.DRAWN);
        if (won) player.recordGameWin(gameName);
        LeaderboardManager.getInstance().submitScore(gameName, player.getUsername(), result.getScore());

        AchievementManager am = AchievementManager.getInstance();
        if (won && player.getWins() == 1) am.grantAchievement(player.getUsername(), "FIRST_VICTORY");
        if (player.getAllGameWins().size() >= 5) am.grantAchievement(player.getUsername(), "GAME_EXPLORER");
        if ("Snake".equals(gameName) && game.getScore() >= 1000) am.grantAchievement(player.getUsername(), "SNAKE_MASTER");
        if ("Chess".equals(gameName) && won) am.grantAchievement(player.getUsername(), "CHESS_BEGINNER");
        if (player.getWins() >= 10 && player.getAllGameWins().size() >= 2) am.grantAchievement(player.getUsername(), "MULTI_GAME_CHAMPION");

        int c = showResultDialog(result, xp, lvlUp, hs);
        dispose();
        if (c == 0) new GamePlayScreen(player, gameName, onGameFinished);
        else if (onGameFinished != null) onGameFinished.run();
    }

    /* ─────────── RESULT DIALOG ─────────── */

    /** Polished end-of-game dialog. Returns 0 = play again, 1 = back to menu. */
    private int showResultDialog(GameResult result, int xp, boolean lvlUp, boolean hs) {
        JDialog dlg = new JDialog(this, "Result", true);
        dlg.setUndecorated(true);
        dlg.setModal(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(PANEL_BG);
        root.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 100)),
            BorderFactory.createEmptyBorder(26, 36, 22, 36)));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        String statusEmoji = switch (result.getStatus()) {
            case WON -> "\uD83C\uDFC6";
            case LOST -> "\uD83D\uDC80";
            case DRAWN -> "\uD83E\uDD1D";
            default -> "\u2705";
        };
        String statusText = switch (result.getStatus()) {
            case WON -> "YOU WIN!";
            case LOST -> "GAME OVER";
            case DRAWN -> "IT'S A DRAW";
            default -> "DONE";
        };
        Color statusColor = switch (result.getStatus()) {
            case WON -> GOLD;
            case LOST -> RED;
            case DRAWN -> ACCENT;
            default -> GREEN;
        };

        JLabel emojiLbl = new JLabel(statusEmoji, SwingConstants.CENTER);
        emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 54));
        emojiLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(emojiLbl);

        JLabel statusLbl = new JLabel(statusText, SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        statusLbl.setForeground(statusColor);
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(statusLbl);

        content.add(Box.createVerticalStrut(18));
        content.add(makeResultRow("Game", gameName));
        content.add(makeResultRow("Difficulty", currentDifficulty.getDisplayName()));
        content.add(makeResultRow("Score", String.valueOf(result.getScore())));
        content.add(makeResultRow("Time", (result.getDuration() / 1000) + "s"));
        content.add(makeResultRow("XP Gained", "+" + xp));

        if (lvlUp) {
            content.add(Box.createVerticalStrut(12));
            JLabel lvl = new JLabel("\uD83C\uDF89 LEVEL UP \u2192 Lv." + player.getLevel(), SwingConstants.CENTER);
            lvl.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lvl.setForeground(GOLD);
            lvl.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(lvl);
        }
        if (hs) {
            content.add(Box.createVerticalStrut(8));
            JLabel hsb = new JLabel("\u2B50 NEW HIGH SCORE!", SwingConstants.CENTER);
            hsb.setFont(new Font("Segoe UI", Font.BOLD, 14));
            hsb.setForeground(GREEN);
            hsb.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(hsb);
        }

        root.add(content, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createEmptyBorder(22, 0, 0, 0));

        final int[] choice = {1};
        JButton again = makeBtn("\u25B6 Play Again", ACCENT);
        again.addActionListener(e -> { choice[0] = 0; dlg.dispose(); });
        JButton menu = makeBtn("\uD83C\uDFE0 Back to Menu", CARD_BG);
        menu.setForeground(TEXT);
        menu.addActionListener(e -> { choice[0] = 1; dlg.dispose(); });
        btns.add(again);
        btns.add(menu);

        root.add(btns, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        return choice[0];
    }

    private JPanel makeResultRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(320, 26));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(TEXT_DIM);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Consolas", Font.BOLD, 13));
        v.setForeground(TEXT);
        row.add(l, BorderLayout.WEST);
        row.add(v, BorderLayout.EAST);
        return row;
    }
}
