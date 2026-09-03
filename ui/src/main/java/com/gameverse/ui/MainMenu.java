package com.gameverse.ui;

import com.gameverse.achievements.AchievementManager;
import com.gameverse.core.GameRegistry;
import com.gameverse.leaderboard.LeaderboardManager;
import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;
import com.gameverse.rewards.CoinManager;
import com.gameverse.rewards.XPManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Main menu / game hub for GameVerse.
 * Shows player profile, game selection grid, achievements, and leaderboard.
 */
public class MainMenu extends JFrame {

    private Player player;
    private JPanel profilePanel;
    private JPanel gamesPanel;
    private Runnable onLogout;
    private MainMenuCallback callback;

    // UI Constants
    private static final Color BG = new Color(18, 18, 28);
    private static final Color PANEL_BG = new Color(28, 28, 42);
    private static final Color CARD_BG = new Color(35, 35, 55);
    private static final Color ACCENT = new Color(100, 150, 255);
    private static final Color GREEN = new Color(80, 200, 120);
    private static final Color GOLD = new Color(255, 200, 60);
    private static final Color TEXT = new Color(210, 210, 225);
    private static final Color TEXT_DIM = new Color(140, 140, 160);

    // Game names and descriptions
    private static final String[][] GAMES = {
        {"Chess", "♟️", "Classic chess with AI opponent"},
        {"Snake", "🐍", "Classic snake — eat, grow, survive"},
        {"Pong", "🏓", "Paddle ball with AI"},
        {"Tic-Tac-Toe", "❌", "Turn-based with smart AI"},
        {"Memory Game", "🧠", "Match the hidden pairs"},
        {"Mini Racing", "🏎️", "Race to the finish line"}
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
        setSize(900, 650);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        setContentPane(root);

        // ── Header ──
        root.add(createHeader(), BorderLayout.NORTH);

        // ── Center: games grid + profile sidebar ──
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        gamesPanel = createGamesPanel();
        center.add(gamesPanel, BorderLayout.CENTER);

        profilePanel = createProfilePanel();
        center.add(profilePanel, BorderLayout.EAST);

        root.add(center, BorderLayout.CENTER);
    }

    /* ─────────────────── HEADER ─────────────────── */

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PANEL_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 50, 70)),
            BorderFactory.createEmptyBorder(12, 25, 12, 25)
        ));

        JLabel title = new JLabel("🎮 GameVerse");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(ACCENT);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton achievementsBtn = makeHeaderBtn("🏆 Achievements");
        achievementsBtn.addActionListener(e -> showAchievements());

        JButton leaderboardBtn = makeHeaderBtn("📊 Leaderboard");
        leaderboardBtn.addActionListener(e -> showLeaderboard());

        JButton logoutBtn = makeHeaderBtn("🚪 Logout");
        logoutBtn.addActionListener(e -> {
            PlayerManager.getInstance().setCurrentPlayer(null);
            dispose();
            if (callback != null) callback.onLogout();
        });

        right.add(achievementsBtn);
        right.add(leaderboardBtn);
        right.add(logoutBtn);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JButton makeHeaderBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setBackground(CARD_BG);
        b.setForeground(TEXT);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80)),
            BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(50, 50, 75));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(CARD_BG);
            }
        });
        return b;
    }

    /* ─────────────────── GAMES GRID ─────────────────── */

    private JPanel createGamesPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 12, 12));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));

        for (String[] g : GAMES) {
            panel.add(createGameCard(g[0], g[1], g[2]));
        }
        return panel;
    }

    private JPanel createGameCard(String name, String icon, String desc) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 70), 1, true),
            BorderFactory.createEmptyBorder(20, 18, 18, 18)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(TEXT);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel("<html><body style='width: 200px'>" + desc + "</body></html>");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(TEXT_DIM);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        int best = player.getGameHighScore(name);
        JLabel bestLabel = new JLabel(best > 0 ? "Best Score: " + best : "No record yet");
        bestLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        bestLabel.setForeground(best > 0 ? GOLD : TEXT_DIM);
        bestLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(descLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(bestLabel);

        // Hover + click
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                card.setBackground(new Color(45, 45, 70));
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 1, true),
                    BorderFactory.createEmptyBorder(20, 18, 18, 18)
                ));
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                card.setBackground(CARD_BG);
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(50, 50, 70), 1, true),
                    BorderFactory.createEmptyBorder(20, 18, 18, 18)
                ));
            }
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispose();
                if (callback != null) callback.onPlayGame(name);
            }
        });

        return card;
    }

    /* ─────────────────── PROFILE SIDEBAR ─────────────────── */

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_BG);
        panel.setPreferredSize(new Dimension(220, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(50, 50, 70)),
            BorderFactory.createEmptyBorder(20, 15, 20, 15)
        ));

        // Avatar circle placeholder
        JLabel avatar = new JLabel("👤");
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel username = new JLabel(player.getUsername());
        username.setFont(new Font("Segoe UI", Font.BOLD, 14));
        username.setForeground(ACCENT);
        username.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(avatar);
        panel.add(Box.createVerticalStrut(5));
        panel.add(username);
        panel.add(Box.createVerticalStrut(20));

        // Stats
        addStatRow(panel, "Level", String.valueOf(player.getLevel()), ACCENT);
        addStatRow(panel, "XP", String.valueOf(player.getXp()), GOLD);
        addStatRow(panel, "Coins", String.valueOf(player.getCoins()), GREEN);

        panel.add(Box.createVerticalStrut(15));
        addDivider(panel);
        panel.add(Box.createVerticalStrut(15));

        addStatRow(panel, "Games Played", String.valueOf(player.getGamesPlayed()), TEXT);
        addStatRow(panel, "Wins", String.valueOf(player.getWins()), GREEN);
        addStatRow(panel, "Win Rate", String.format("%.0f%%", player.getWinRate()), TEXT);

        // XP to next level
        XPManager xpMgr = XPManager.getInstance();
        int xpToNext = xpMgr.getXpToNextLevel(player);
        panel.add(Box.createVerticalStrut(20));
        addDivider(panel);
        panel.add(Box.createVerticalStrut(12));

        if (xpToNext >= 0) {
            JLabel xpLabel = new JLabel("XP to next level: " + xpToNext);
            xpLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            xpLabel.setForeground(TEXT_DIM);
            xpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(xpLabel);
            panel.add(Box.createVerticalStrut(6));

            // XP progress bar
            int currentLevelXp = player.getLevel() * xpMgr.getXpPerLevel();
            int prevLevelXp = (player.getLevel() - 1) * xpMgr.getXpPerLevel();
            float progress = (float)(player.getXp() - prevLevelXp) / (currentLevelXp - prevLevelXp);
            JProgressBar xpBar = new JProgressBar(0, 100);
            xpBar.setValue((int)(progress * 100));
            xpBar.setStringPainted(true);
            xpBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
            xpBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            xpBar.setBackground(new Color(40, 40, 55));
            xpBar.setForeground(ACCENT);
            panel.add(xpBar);
        } else {
            JLabel maxLbl = new JLabel("🏆 Max Level Reached!");
            maxLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
            maxLbl.setForeground(GOLD);
            maxLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(maxLbl);
        }

        panel.add(Box.createVerticalGlue());

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
        panel.add(Box.createVerticalStrut(4));
    }

    private void addDivider(JPanel panel) {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(50, 50, 70));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(sep);
    }

    /* ─────────────────── ACHIEVEMENTS DIALOG ─────────────────── */

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
        card.setBackground(owned ? new Color(28, 55, 40) : CARD_BG);
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

    /* ─────────────────── LEADERBOARD DIALOG ─────────────────── */

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
                row.setBackground(me ? new Color(45, 60, 105) : CARD_BG);
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
