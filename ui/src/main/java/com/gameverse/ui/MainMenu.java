package com.gameverse.ui;

import com.gameverse.achievements.AchievementManager;
import com.gameverse.leaderboard.LeaderboardManager;
import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;
import com.gameverse.rewards.XPManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * Main menu / game hub for GameVerse.
 * Shows player profile, game selection grid, achievements, and leaderboard.
 */
public class MainMenu extends JFrame {

    private Player player;
    private JPanel gamesPanel;
    private Runnable onLogout;
    private MainMenuCallback callback;

    // ── Palette ──
    private static final Color BG = new Color(15, 16, 26);
    private static final Color PANEL_BG = new Color(23, 24, 38);
    private static final Color SIDE_BG = new Color(26, 28, 46);
    private static final Color CARD_BG = new Color(31, 33, 52);
    private static final Color CARD_HOVER = new Color(39, 42, 64);
    private static final Color CARD_LINE = new Color(52, 55, 80);
    private static final Color ACCENT = new Color(100, 150, 255);
    private static final Color ACCENT_DEEP = new Color(70, 115, 235);
    private static final Color GREEN = new Color(80, 200, 120);
    private static final Color GOLD = new Color(255, 200, 60);
    private static final Color TEXT = new Color(225, 226, 240);
    private static final Color TEXT_DIM = new Color(148, 150, 170);

    // Game names, icons and descriptions
    private static final String[][] GAMES = {
        {"Chess", "♟️", "Classic chess with AI opponent"},
        {"Snake", "🐍", "Classic snake — eat, grow, survive"},
        {"Pong", "🏓", "Paddle ball with AI"},
        {"Tic-Tac-Toe", "❌", "Turn-based with smart AI"},
        {"Memory Game", "🧠", "Match the hidden pairs"},
        {"Mini Racing", "🏎️", "Race to the finish line"},
        {"DON'T LOOK", "👁️", "It only moves when you're not looking"},
        {"UNKNOWN SIGNAL", "📡", "A radio-mystery — it starts answering back"},
        {"Mirror World", "🪞", "Flip between worlds to solve the puzzle"},
        {"One Bullet", "🔫", "One bullet, infinite puzzles"}
    };

    // One accent per game card
    private static final Color[] GAME_ACCENTS = {
        new Color(130, 165, 255), // Chess      — blue
        new Color(95, 215, 145),  // Snake      — green
        new Color(95, 200, 225),  // Pong       — cyan
        new Color(255, 185, 95),  // TTT        — orange
        new Color(200, 150, 255), // Memory     — purple
        new Color(255, 125, 120), // Racing     — red
        new Color(205, 205, 220), // DON'T LOOK — ghost white
        new Color(255, 110, 140), // UNKNOWN SIGNAL — signal red
        new Color(150, 220, 255), // Mirror World — mirror cyan
        new Color(255, 200, 50)   // One Bullet — bullet gold
    };

    public interface MainMenuCallback {
        void onPlayGame(String gameName);
        void onLogout();
    }

    public MainMenu(Player player, MainMenuCallback callback) {
        this.player = player;
        this.callback = callback;
        initUI();
        setVisible(true);
    }

    private void initUI() {
        setTitle("GameVerse — Game Hub");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1060, 720);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        setContentPane(root);

        root.add(createHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel leftSide = new JPanel(new BorderLayout(0, 14));
        leftSide.setOpaque(false);
        leftSide.add(createHero(), BorderLayout.NORTH);

        gamesPanel = createGamesPanel();
        leftSide.add(gamesPanel, BorderLayout.CENTER);
        center.add(leftSide, BorderLayout.CENTER);

        center.add(createProfilePanel(), BorderLayout.EAST);

        root.add(center, BorderLayout.CENTER);
    }

    /* ═══════════════════ HEADER ═══════════════════ */

    private JPanel createHeader() {
        JPanel strip = new JPanel(new BorderLayout(0, 0));
        strip.setBackground(PANEL_BG);
        strip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(45, 48, 70)),
            BorderFactory.createEmptyBorder(10, 22, 10, 22)
        ));

        // ── Brand (left) ──
        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);

        JLabel logo = new JLabel("🎮", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        logo.setOpaque(true);
        logo.setBackground(ACCENT_DEEP);
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        logo.setPreferredSize(new Dimension(44, 40));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("GameVerse");
        title.setFont(new Font("Segoe UI", Font.BOLD, 21));
        title.setForeground(new Color(235, 240, 255));

        JLabel sub = new JLabel("Your arcade hub — play, earn XP, climb the ranks");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_DIM);

        brandText.add(title);
        brandText.add(sub);
        brand.add(logo, BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);

        // ── Right nav buttons ──
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton achievementsBtn = makeNavButton("🏆 Achievements", CARD_BG, ACCENT_DEEP);
        achievementsBtn.addActionListener(e -> showAchievements());

        JButton leaderboardBtn = makeNavButton("👑 Leaderboard", CARD_BG, new Color(120, 95, 40));
        leaderboardBtn.addActionListener(e -> showLeaderboard());

        JButton logoutBtn = makeNavButton("🚪  Logout", new Color(52, 30, 34), new Color(180, 70, 70));
        logoutBtn.addActionListener(e -> {
            PlayerManager.getInstance().setCurrentPlayer(null);
            dispose();
            if (callback != null) callback.onLogout();
        });

        right.add(achievementsBtn);
        right.add(leaderboardBtn);
        right.add(logoutBtn);

        strip.add(brand, BorderLayout.WEST);
        strip.add(right, BorderLayout.EAST);
        return strip;
    }

    private JButton makeNavButton(String text, Color bg, Color hoverBg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setBackground(bg);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 64, 90)),
            BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(hoverBg);
                b.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(bg);
                b.setForeground(TEXT);
            }
        });
        return b;
    }

    /* ═══════════════════ HERO BANNER ═══════════════════ */

    private JPanel createHero() {
        RoundedPanel hero = new RoundedPanel(new BorderLayout(0, 0),
            new Color(28, 32, 60), new Color(52, 58, 100), 18, true);
        hero.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Greeting (left)
        JPanel greet = new JPanel();
        greet.setOpaque(false);
        greet.setLayout(new BoxLayout(greet, BoxLayout.Y_AXIS));

        JLabel hi = new JLabel("Welcome back, " + displayName(player.getUsername()) + " 👋");
        hi.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hi.setForeground(new Color(240, 243, 255));

        JLabel tag = new JLabel("Pick a game below — every win earns XP, coins and achievements.");
        tag.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tag.setForeground(new Color(170, 175, 200));

        greet.add(hi);
        greet.add(Box.createVerticalStrut(3));
        greet.add(tag);

        // Stat chips (right)
        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        chipRow.setOpaque(false);
        chipRow.add(makeChip("Level", String.valueOf(player.getLevel()), ACCENT));
        chipRow.add(makeChip("XP", String.valueOf(player.getXp()), GOLD));
        chipRow.add(makeChip("Coins", String.valueOf(player.getCoins()), GREEN));

        JPanel chipWrap = new JPanel();
        chipWrap.setOpaque(false);
        chipWrap.setLayout(new BoxLayout(chipWrap, BoxLayout.Y_AXIS));
        chipWrap.add(Box.createVerticalGlue());
        chipWrap.add(chipRow);
        chipWrap.add(Box.createVerticalGlue());

        hero.add(greet, BorderLayout.WEST);
        hero.add(chipWrap, BorderLayout.EAST);
        return hero;
    }

    private JPanel makeChip(String caption, String value, Color valueColor) {
        RoundedPanel chip = new RoundedPanel(null, new Color(20, 22, 40), new Color(58, 64, 96), 12, false);
        chip.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setFont(new Font("Consolas", Font.BOLD, 17));
        val.setForeground(valueColor);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel cap = new JLabel(caption, SwingConstants.CENTER);
        cap.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cap.setForeground(TEXT_DIM);
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(val);
        inner.add(cap);
        chip.setLayout(new BorderLayout());
        chip.add(inner, BorderLayout.CENTER);
        return chip;
    }

    /* ═══════════════════ GAMES GRID ═══════════════════ */

    private JPanel createGamesPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 3, 14, 14));
        panel.setOpaque(false);
        for (int i = 0; i < GAMES.length; i++) {
            panel.add(createGameCard(i));
        }
        return panel;
    }

    /** Visual state of one game card so hover listeners can restore it. */
    private static class CardState {
        RoundedPanel card;
        JLabel play;
        JLabel record;
        Color line;
        Color playColor;
        boolean hovering;
    }

    private JPanel createGameCard(int index) {
        String name = GAMES[index][0];
        String icon = GAMES[index][1];
        String desc = GAMES[index][2];
        Color accent = GAME_ACCENTS[index];

        RoundedPanel card = new RoundedPanel(null, CARD_BG, CARD_LINE, 16, false);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 12, 16));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        // Icon tile + record pill (top row)
        JPanel topRow = new JPanel(new BorderLayout(0, 0));
        topRow.setOpaque(false);

        RoundedPanel tile = new RoundedPanel(new BorderLayout(),
            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 40),
            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110), 10, false);
        tile.setPreferredSize(new Dimension(42, 42));
        tile.setMaximumSize(new Dimension(42, 42));

        JLabel iconLabel = new JLabel(icon, SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 21));
        tile.add(iconLabel, BorderLayout.CENTER);
        topRow.add(tile, BorderLayout.WEST);

        int best = player.getGameHighScore(name);
        String recText = best > 0 ? "🏅 Best " + best : "No record yet";
        JLabel record = new JLabel(recText);
        record.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        record.setForeground(best > 0 ? GOLD : TEXT_DIM);
        record.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(best > 0 ? new Color(120, 100, 40) : CARD_LINE),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        topRow.add(record, BorderLayout.EAST);

        inner.add(topRow);
        inner.add(Box.createVerticalStrut(12));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLabel.setForeground(TEXT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(nameLabel);
        inner.add(Box.createVerticalStrut(2));

        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(TEXT_DIM);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inner.add(descLabel);
        inner.add(Box.createVerticalStrut(10));

        // Bottom row: "Play" affordance
        JPanel bottomRow = new JPanel(new BorderLayout(0, 0));
        bottomRow.setOpaque(false);
        JLabel play = new JLabel("Play  ▶");
        play.setFont(new Font("Segoe UI", Font.BOLD, 11));
        play.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 160));
        bottomRow.add(play, BorderLayout.EAST);
        inner.add(bottomRow);
        inner.add(Box.createVerticalGlue());

        card.setLayout(new BorderLayout());
        card.add(inner, BorderLayout.CENTER);

        // ── Hover + click (attached to every child so any click works) ──
        CardState state = new CardState();
        state.card = card;
        state.play = play;
        state.record = record;
        state.line = CARD_LINE;
        state.playColor = play.getForeground();

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (state.hovering) return;
                state.hovering = true;
                state.card.setFill(CARD_HOVER);
                state.card.setLine(accent);
                play.setForeground(accent);
                record.setForeground(accent);
                state.card.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if (!state.hovering) return;
                state.hovering = false;
                state.card.setFill(CARD_BG);
                state.card.setLine(state.line);
                play.setForeground(state.playColor);
                record.setForeground(best > 0 ? GOLD : TEXT_DIM);
                state.card.repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if ("DON'T LOOK".equals(name)) {
                    DontLookLauncher.launch();
                    return; // external web game — keep the hub open
                }
                if ("UNKNOWN SIGNAL".equals(name)) {
                    UnknownSignalLauncher.launch();
                    return; // external web game — keep the hub open
                }
                if ("Mirror World".equals(name)) {
                    MirrorWorldLauncher.launch();
                    return; // external web game — keep the hub open
                }
                if ("One Bullet".equals(name)) {
                    OneBulletLauncher.launch();
                    return; // external web game — keep the hub open
                }
                dispose();
                if (callback != null) callback.onPlayGame(name);
            }
        };

        wireMouse(card, adapter);
        return card;
    }

    /** Attach one mouse adapter to a component and every child of it. */
    private void wireMouse(JComponent root, MouseAdapter adapter) {
        root.addMouseListener(adapter);
        if (root instanceof Container) {
            for (Component c : ((Container) root).getComponents()) {
                if (c instanceof JComponent) wireMouse((JComponent) c, adapter);
            }
        }
    }

    /* ═══════════════════ PROFILE SIDEBAR ═══════════════════ */

    private JPanel createProfilePanel() {
        RoundedPanel panel = new RoundedPanel(null, SIDE_BG, new Color(46, 50, 76), 18, false);
        panel.setPreferredSize(new Dimension(262, 0));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(22, 18, 14, 18));

        // Avatar
        Avatar avatar = new Avatar(player.getUsername());
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(avatar);
        body.add(Box.createVerticalStrut(10));

        JLabel nameLbl = new JLabel(player.getUsername(), SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLbl.setForeground(ACCENT);
        nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(nameLbl);

        JLabel roleLbl = new JLabel("Member", SwingConstants.CENTER);
        roleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleLbl.setForeground(TEXT_DIM);
        roleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(roleLbl);

        body.add(Box.createVerticalStrut(16));

        // Stat rows
        addStatRow(body, "Level", String.valueOf(player.getLevel()), ACCENT);
        addStatRow(body, "XP", String.valueOf(player.getXp()), GOLD);
        addStatRow(body, "Coins", String.valueOf(player.getCoins()), GREEN);

        body.add(Box.createVerticalStrut(10));
        addDivider(body);
        body.add(Box.createVerticalStrut(10));

        addStatRow(body, "Games Played", String.valueOf(player.getGamesPlayed()), TEXT);
        addStatRow(body, "Wins", String.valueOf(player.getWins()), GREEN);
        addStatRow(body, "Win Rate", String.format("%.0f%%", player.getWinRate()), TEXT);

        body.add(Box.createVerticalStrut(10));
        addDivider(body);
        body.add(Box.createVerticalStrut(12));

        // XP progress
        XPManager xpMgr = XPManager.getInstance();
        int xpToNext = xpMgr.getXpToNextLevel(player);
        if (xpToNext >= 0) {
            JLabel xpLabel = new JLabel("XP to next level: " + xpToNext);
            xpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            xpLabel.setForeground(TEXT_DIM);
            xpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(xpLabel);
            body.add(Box.createVerticalStrut(6));

            int currentLevelXp = player.getLevel() * xpMgr.getXpPerLevel();
            int prevLevelXp = (player.getLevel() - 1) * xpMgr.getXpPerLevel();
            float progress = (float) (player.getXp() - prevLevelXp) / Math.max(1, currentLevelXp - prevLevelXp);

            XpBar bar = new XpBar(Math.round(progress * 100));
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            bar.setPreferredSize(new Dimension(220, 16));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
            body.add(bar);
        } else {
            JLabel maxLbl = new JLabel("🏆 Max Level Reached!");
            maxLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            maxLbl.setForeground(GOLD);
            maxLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(maxLbl);
        }

        body.add(Box.createVerticalGlue());

        // Footer note
        JLabel foot = new JLabel("💡 Tip: hover a game card, then click to play", SwingConstants.CENTER);
        foot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        foot.setForeground(TEXT_DIM);
        foot.setAlignmentX(Component.CENTER_ALIGNMENT);
        foot.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        body.add(foot);

        panel.setLayout(new BorderLayout());
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private void addStatRow(JPanel panel, String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_DIM);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 13));
        val.setForeground(valueColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        panel.add(row);
        panel.add(Box.createVerticalStrut(5));
    }

    private void addDivider(JPanel panel) {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(52, 56, 82));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sep);
    }

    /** Turn "ashaz@gmail.com" into "ashaz" for the greeting. */
    private static String displayName(String username) {
        if (username == null) return "player";
        int at = username.indexOf('@');
        String base = at > 0 ? username.substring(0, at) : username;
        if (base.isEmpty()) return "player";
        return base.substring(0, 1).toUpperCase() + base.substring(1);
    }

    /* ═══════════════════ SMALL CUSTOM COMPONENTS ═══════════════════ */

    /** Rounded panel with optional vertical gradient, painted by hand. */
    private static class RoundedPanel extends JPanel {
        private Color fill;
        private Color line;
        private final int arc;
        private final boolean gradient;

        RoundedPanel(LayoutManager layout, Color fill, Color line, int arc, boolean gradient) {
            super(layout);
            setOpaque(false);
            this.fill = fill;
            this.line = line;
            this.arc = arc;
            this.gradient = gradient;
        }

        void setFill(Color fill) {
            this.fill = fill;
            repaint();
        }

        void setLine(Color line) {
            this.line = line;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (fill != null) {
                if (gradient && h > 0) {
                    g2.setPaint(new GradientPaint(0, 0, fill, 0, h, new Color(20, 22, 42)));
                } else {
                    g2.setPaint(fill);
                }
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }
            if (line != null) {
                g2.setColor(line);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Circular avatar with the player's initial and an online dot. */
    private static class Avatar extends JComponent {
        private final String initial;
        private final Color tone;

        Avatar(String username) {
            String display = displayName(username);
            this.initial = display.substring(0, 1);
            this.tone = new Color(100, 150, 255);
            setPreferredSize(new Dimension(84, 84));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int s = 72;
            int x = (getWidth() - s) / 2;
            int y = (getHeight() - s) / 2;

            // soft shadow
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillOval(x + 2, y + 4, s, s);

            // gradient disc
            g2.setPaint(new GradientPaint(x, y, new Color(120, 160, 255), x, y + s, new Color(60, 95, 210)));
            g2.fillOval(x, y, s, s);

            // rim
            g2.setColor(new Color(190, 210, 255, 160));
            g2.drawOval(x, y, s - 1, s - 1);

            // initial
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 34));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial,
                x + (s - fm.stringWidth(initial)) / 2,
                y + (s + fm.getAscent() - fm.getDescent()) / 2);

            // online dot
            g2.setColor(new Color(20, 22, 42));
            g2.fillOval(x + s - 16, y + s - 16, 18, 18);
            g2.setColor(new Color(80, 200, 120));
            g2.fillOval(x + s - 13, y + s - 13, 12, 12);
            g2.dispose();
        }
    }

    /** Rounded XP progress bar with percentage text. */
    private static class XpBar extends JComponent {
        private final int percent;

        XpBar(int percent) {
            this.percent = Math.max(0, Math.min(100, percent));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(new Color(38, 40, 62));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 8, 8);

            int fillW = (int) ((w - 2) * percent / 100f);
            if (fillW > 0) {
                g2.setPaint(new GradientPaint(0, 0, new Color(100, 160, 255), w, 0, new Color(60, 105, 230)));
                g2.fillRoundRect(1, 1, fillW, h - 2, 7, 7);
            }

            g2.setColor(TEXT_DIM);
            g2.setFont(new Font("Consolas", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            String label = percent + "%";
            g2.drawString(label, (w - fm.stringWidth(label)) / 2,
                (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    /* ═══════════════════ ACHIEVEMENTS DIALOG ═══════════════════ */

    private void showAchievements() {
        AchievementManager mgr = AchievementManager.getInstance();
        var allAchievements = mgr.getAllAchievements();
        Set<String> owned = mgr.getPlayerAchievements(player.getUsername());

        JDialog dlg = new JDialog(this, "🏆 Achievements", true);
        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dlg.setSize(640, 500);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JLabel head = new JLabel("Achievements  —  " + owned.size() + " / " + allAchievements.size() + " unlocked",
            SwingConstants.CENTER);
        head.setFont(new Font("Segoe UI", Font.BOLD, 17));
        head.setForeground(ACCENT);
        head.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));
        root.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
        grid.setBackground(BG);
        grid.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        for (var entry : allAchievements.values()) {
            grid.add(createAchievementCard(entry.getName(), entry.getDescription(),
                owned.contains(entry.getId()), entry.getRewardXp(), entry.getRewardCoins()));
        }

        JScrollPane sp = new JScrollPane(grid);
        sp.getViewport().setBackground(BG);
        sp.setBorder(null);
        root.add(sp, BorderLayout.CENTER);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private JPanel createAchievementCard(String name, String desc, boolean owned, int xp, int coins) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(owned ? new Color(28, 55, 40) : new Color(35, 35, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(owned ? GREEN : new Color(50, 50, 70), 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel((owned ? "✅ " : "🔒 ") + name);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(owned ? GREEN : TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(5));
        JLabel descLbl = new JLabel("<html><body style='width: 220px'>" + desc + "</body></html>");
        descLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLbl.setForeground(TEXT_DIM);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(descLbl);
        if (!owned) {
            card.add(Box.createVerticalStrut(6));
            JLabel reward = new JLabel("Reward: +" + xp + " XP  +" + coins + " 🪙");
            reward.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            reward.setForeground(GOLD);
            reward.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(reward);
        }
        return card;
    }

    /* ═══════════════════ LEADERBOARD DIALOG ═══════════════════ */

    private void showLeaderboard() {
        LeaderboardManager mgr = LeaderboardManager.getInstance();
        int myRank = mgr.getPlayerGlobalRank(player.getUsername());

        JDialog dlg = new JDialog(this, "📊 Leaderboard", true);
        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dlg.setSize(540, 480);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JLabel head = new JLabel("GLOBAL LEADERBOARD", SwingConstants.CENTER);
        head.setFont(new Font("Segoe UI", Font.BOLD, 17));
        head.setForeground(ACCENT);
        head.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));
        root.add(head, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BG);
        list.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        var global = mgr.getGlobalLeaderboard(10);
        if (global.isEmpty()) {
            JLabel empty = new JLabel("No scores yet — be the first!", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(TEXT_DIM);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(empty);
        } else {
            for (var entry : global) {
                boolean me = entry.getUsername().equals(player.getUsername());
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(me);
                row.setBackground(me ? new Color(45, 60, 105) : new Color(35, 35, 55));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                String medal = switch (entry.getRank()) {
                    case 1 -> "🥇";
                    case 2 -> "🥈";
                    case 3 -> "🥉";
                    default -> "#" + entry.getRank();
                };
                JLabel r = new JLabel((me ? "⭐ " : "") + medal + "  " + entry.getUsername());
                r.setFont(new Font("Segoe UI", Font.BOLD, 13));
                r.setForeground(me ? GOLD : TEXT);
                JLabel s = new JLabel(entry.getScore() + " pts");
                s.setFont(new Font("Consolas", Font.BOLD, 13));
                s.setForeground(me ? GOLD : ACCENT);
                row.add(r, BorderLayout.WEST);
                row.add(s, BorderLayout.EAST);
                list.add(row);
                list.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane sp = new JScrollPane(list);
        sp.getViewport().setBackground(BG);
        sp.setBorder(null);
        root.add(sp, BorderLayout.CENTER);

        JLabel footer = new JLabel("Your Global Rank: " + (myRank > 0 ? "#" + myRank : "Unranked"),
            SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        footer.setForeground(GOLD);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 0, 14, 0));
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }
}
