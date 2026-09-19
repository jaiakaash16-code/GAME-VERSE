package com.gameverse.ui;

import com.gameverse.achievements.AchievementManager;
import com.gameverse.leaderboard.LeaderboardManager;
import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;
import com.gameverse.rewards.XPManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Main menu / game hub for GameVerse.
 * Cyber arcade restyle: header command bar with live ticker, tournament
 * spotlight hero, welcome rig card, filterable "Arcade Modules" grid for all
 * 13 games, and the player rig sidebar with telemetry, bounties and squad.
 */
public class MainMenu extends JFrame {

    private Player player;
    private JPanel gamesPanel;
    private Runnable onLogout;
    private MainMenuCallback callback;

    /* ═══════════════ Cyber palette ═══════════════ */
    private static final Color BG            = new Color(0x0C0E17);
    private static final Color BG_MESH       = new Color(0x080A12);
    private static final Color HEADER_BG     = new Color(0x0A0C16);
    private static final Color CARD_BG       = new Color(0x15192C);
    private static final Color CARD_BG_DEEP  = new Color(0x0F1220);
    private static final Color CARD_HOVER    = new Color(0x1C223A);
    private static final Color CYBER_LINE    = new Color(0x212845);
    private static final Color WHITE_LINE    = new Color(255, 255, 255, 22);
    private static final Color CYAN          = new Color(0x06B6D4);
    private static final Color VIOLET        = new Color(0x8B5CF6);
    private static final Color PINK          = new Color(0xEC4899);
    private static final Color AMBER         = new Color(0xF59E0B);
    private static final Color EMERALD       = new Color(0x10B981);
    private static final Color ROSE          = new Color(0xF43F5E);
    private static final Color INDIGO        = new Color(0x818CF8);
    private static final Color RED           = new Color(0xEF4444);
    private static final Color TEXT          = new Color(0xE2E8F0);
    private static final Color TEXT_DIM      = new Color(0x94A3B8);
    private static final Color TEXT_FAINT    = new Color(0x64748B);

    /* Font families: Stitch fonts with Windows-safe fallbacks */
    private static final String F_DISPLAY = pick("Space Grotesk", "Segoe UI");
    private static final String F_SANS    = pick("Inter", "Segoe UI");
    private static final String F_MONO    = pick("JetBrains Mono", "Consolas");

    private String activeCategory = "All Games";
    private String searchText = "";
    private JPanel gridContainer;
    /** Minimum comfortable width of one game card before we add a column. */
    private static final int MIN_CARD_W = 330;
    /** Upper bound on grid columns so cards never get squashed on huge screens. */
    private static final int MAX_COLUMNS = 6;
    /** Gap between cards in the dashboard grid. */
    private static final int CARD_GAP = 16;
    /** Columns currently rendered in the grid (recomputed on resize/filter). */
    private int builtColumns = 0;
    /** Last known width available to the main column (for filter rebuilds). */
    private int lastMainW = 0;
    /** Width of one card in the currently built grid (drives text reflow). */
    private int builtCardW = 366;

    /* ═══════════════ Game catalogue ═══════════════ */

    private static class GameInfo {
        final String name, icon, desc, tag, mode, meta, xpLabel;
        final Color accent;
        final boolean featured;
        final Set<String> categories;
        final int xp;

        GameInfo(String name, String icon, String desc, String tag, String mode,
                 String meta, int xp, Color accent, boolean featured, String... categories) {
            this.name = name; this.icon = icon; this.desc = desc; this.tag = tag;
            this.mode = mode; this.meta = meta; this.xp = xp; this.accent = accent;
            this.featured = featured;
            this.xpLabel = featured ? "+%d XP (2X BONUS)".formatted(xp) : "+%d XP".formatted(xp);
            this.categories = Set.of(categories);
        }
    }

    private static final GameInfo[] GAMES = {
        new GameInfo("Chess", "♟️", "Cyber Tactical Master • Holographic King & Deep AI",
            "TACTICAL", "1v1 PvP / AI", "Best: 1,420 ELO", 150, CYAN, false, "Strategy & Logic"),
        new GameInfo("Snake", "🐍", "Neon Synthwave • Glow Ribbon & Hyper Speed Orbs",
            "RETRO", "HOT • 60 FPS", "Record: 2,840 pts", 120, EMERALD, false, "Retro Classics"),
        new GameInfo("Pong", "🏓", "Reflex Arena • Hyper Dynamic Laser Velocity",
            "REFLEX", "1.5x XP EVENT", "Difficulty: Med", 180, VIOLET, false, "Retro Classics", "Action & Shooter"),
        new GameInfo("Tic-Tac-Toe", "❌", "Quantum Matrix • Neon Grid 3x3 Showdown",
            "MATRIX", "CASUAL • MULTI", "Win Streak: 4", 80, AMBER, false, "Retro Classics", "Strategy & Logic"),
        new GameInfo("Memory Game", "🧠", "Neural Sync • Cybernetic Brain Recall & Synthesis",
            "NEURAL", "BRAIN TRAIN", "16 Cards • 4x4", 100, PINK, false, "Strategy & Logic"),
        new GameInfo("Mini Racing", "🏎️", "Nitro Velocity • Neon Supercars on Cyber Circuits",
            "NITRO", "NEW TRACK", "Best Lap: 00:42.1", 140, RED, false, "Action & Shooter"),
        new GameInfo("Connect Four", "🔴", "Gravity Grid • Classic Four-in-a-Row Showdown",
            "GRID", "CASUAL • AI", "Streak: 6 wins", 90, CYAN, false, "Strategy & Logic", "Retro Classics"),
        new GameInfo("Blackjack", "🂡", "Neon Casino • Beat the Dealer Without Busting",
            "CARDS", "1v1 DEALER", "Best Hand: 21", 120, EMERALD, false, "Strategy & Logic"),
        new GameInfo("2D Soccer", "⚽", "Cyber Striker Arena • Precision Kick & Deflection Physics",
            "STRIKER", "CO-OP / 1V1", "Goals: 18", 110, EMERALD, false, "Action & Shooter"),
        new GameInfo("Zombie Survival", "🧟", "Bio-Hazard Protocol • Endless Wave Defense & Extract",
            "HORDE", "FEATURED • TOURNAMENT", "Wave Record: 26", 250, PINK, true, "Horror & Survival", "Action & Shooter"),
        new GameInfo("Dungeon Escape", "🏰", "Rogue Descent • Dark Obsidian Vault & Procedural Traps",
            "RPG", "ROGUELIKE", "Floor: B14 Reached", 160, AMBER, false, "Horror & Survival", "Strategy & Logic"),
        new GameInfo("DON'T LOOK", "👁️", "Paranoid Horror • Night Vision Sensor in the Dark Void",
            "HORROR", "HARDCORE • 18+", "Sanity: 82%", 190, ROSE, false, "Horror & Survival"),
        new GameInfo("UNKNOWN SIGNAL", "📡", "Deep Void Telemetry • Radio Waves & Cipher Audio",
            "SCI-FI", "PUZZLE • LORE", "Decrypted: 7/12", 130, CYAN, false, "Strategy & Logic"),
        new GameInfo("Mirror World", "🪞", "Dimensional Fracture • Inverted Spatial Gravity Runner",
            "PHYSICS", "GRAVITY RUN", "Dimension: #07", 150, INDIGO, false, "Strategy & Logic", "Action & Shooter"),
        new GameInfo("One Bullet", "🔫", "Ricochet Assassin • Precision Bullet Bouncing Mechanics",
            "SNIPER", "1-SHOT KILL", "Accuracy: 88.4%", 175, AMBER, false, "Action & Shooter"),
    };

    private static final String[] CATEGORIES = {
        "All Games", "Popular", "Action & Shooter", "Strategy & Logic", "Retro Classics", "Horror & Survival"
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
        setTitle("GameVerse — Ultimate Cyber Arcade Hub");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_MESH);
        setContentPane(root);

        root.add(createHeader(), BorderLayout.NORTH);

        // Scrolling two-column body: main column + rig sidebar.
        // BodyPanel tracks the scroll-pane viewport width, so the dashboard
        // always fills the window with no dead space and no horizontal scroll.
        JPanel body = new BodyPanel();
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        body.add(createMainColumn(), BorderLayout.CENTER);
        body.add(createSidebar(), BorderLayout.EAST);

        JScrollPane scroller = new JScrollPane(body,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.getVerticalScrollBar().setUnitIncrement(20);
        scroller.getViewport().setOpaque(false);
        scroller.setOpaque(false);
        root.add(scroller, BorderLayout.CENTER);

        root.add(createFooter(), BorderLayout.SOUTH);
    }

    /** First installed font family among the candidates. */
    private static String pick(String... candidates) {
        Set<String> available = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames());
        for (String c : candidates) {
            if (available.contains(c)) return c;
        }
        return "Dialog";
    }

    private static Font font(String family, int style, int size) {
        return new Font(family, style, size);
    }

    /* ═══════════════════ HEADER ═══════════════════ */

    private JPanel createHeader() {
        JPanel strip = new JPanel(new BorderLayout(16, 0));
        strip.setBackground(HEADER_BG);
        strip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, WHITE_LINE),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        // ── Brand: gradient logo tile + live dot + name + version badge ──
        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brand.setOpaque(false);

        JPanel logoWrap = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, CYAN, w, h, PINK));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.setColor(BG);
                g2.fillRoundRect(2, 2, w - 5, h - 5, 12, 12);
                g2.dispose();
            }
        };
        logoWrap.setPreferredSize(new Dimension(34, 34));
        logoWrap.setLayout(new BorderLayout());
        JLabel logo = new JLabel("🎮", SwingConstants.CENTER);
        logo.setFont(font("Segoe UI Emoji", Font.PLAIN, 17));
        logoWrap.add(logo);
        brand.add(logoWrap);

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nameRow.setOpaque(false);
        JLabel title = new JLabel("GameVerse");
        title.setFont(font(F_DISPLAY, Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        JLabel version = new JLabel("v2.4 NEXUS");
        version.setFont(font(F_MONO, Font.BOLD, 9));
        version.setForeground(new Color(0x67E8F9));
        version.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(6, 182, 212, 110)),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        nameRow.add(title);
        nameRow.add(version);
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Cyber Arcade & Competitive Launcher");
        sub.setFont(font(F_MONO, Font.PLAIN, 10));
        sub.setForeground(TEXT_DIM);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandText.add(nameRow);
        brandText.add(sub);
        brand.add(brandText);

        // ── Center: search + server ticker ──
        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        center.setOpaque(false);

        JTextField search = new JTextField("Search games, lobbies, netrunners...", 24);
        styleSearchField(search);
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void changed() {
                String q = search.getText().trim();
                searchText = q.startsWith("Search games") ? "" : q.toLowerCase();
                refreshGamesGrid();
            }
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { changed(); }
        });
        center.add(search);

        JPanel ticker = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 999, 999);
                g2.setColor(WHITE_LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 999, 999);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        ticker.setOpaque(false);
        ticker.add(pulseDot());
        ticker.add(pill("US-East 14ms", EMERALD, Font.BOLD));
        ticker.add(new JLabel(" "));
        ticker.add(pill("👥 142.8k Netrunners", TEXT_DIM, Font.PLAIN));
        center.add(ticker);

        // ── Right actions ──
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        AchievementManager mgr = AchievementManager.getInstance();
        int owned = mgr.getPlayerAchievements(player.getUsername()).size();
        int total = mgr.getAllAchievements().size();

        JButton achievementsBtn = navButton("🎖 Achievements", new Color(139, 92, 246, 60));
        achievementsBtn.setText("🎖 Achievements  " + owned + "/" + total);
        achievementsBtn.addActionListener(e -> showAchievements());

        JButton leaderboardBtn = navButton("🏆 Leaderboards", new Color(6, 182, 212, 60));
        leaderboardBtn.addActionListener(e -> showLeaderboard());

        JPanel wallet = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(245, 158, 11, 26), getWidth(), getHeight(), new Color(217, 119, 6, 52)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(245, 158, 11, 110));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        wallet.setOpaque(false);
        JLabel coins = new JLabel("🪙 " + player.getCoins() + " GVR");
        coins.setFont(font(F_MONO, Font.BOLD, 11));
        coins.setForeground(new Color(0xFCD34D));
        wallet.add(coins);

        JButton logoutBtn = navButton("⏻ Exit", new Color(244, 63, 94, 50));
        logoutBtn.addActionListener(e -> {
            PlayerManager.getInstance().setCurrentPlayer(null);
            dispose();
            if (callback != null) callback.onLogout();
        });

        right.add(achievementsBtn);
        right.add(leaderboardBtn);
        right.add(wallet);
        right.add(logoutBtn);

        strip.add(brand, BorderLayout.WEST);
        strip.add(center, BorderLayout.CENTER);
        strip.add(right, BorderLayout.EAST);
        return strip;
    }

    private void styleSearchField(JTextField field) {
        field.setFont(font(F_MONO, Font.PLAIN, 11));
        field.setForeground(TEXT);
        field.setBackground(new Color(255, 255, 255, 10));
        field.setCaretColor(CYAN);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WHITE_LINE),
            BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().startsWith("Search games")) {
                    field.setText("");
                    field.setForeground(TEXT);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText("Search games, lobbies, netrunners...");
                    field.setForeground(TEXT_FAINT);
                }
            }
        });
        field.setForeground(TEXT_FAINT);
    }

    private JButton navButton(String text, Color hoverBg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(WHITE_LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font(F_SANS, Font.PLAIN, 11));
        b.setForeground(TEXT);
        b.setBackground(new Color(255, 255, 255, 10));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 11, 5, 11));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(hoverBg); b.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(new Color(255, 255, 255, 10)); b.repaint(); }
        });
        return b;
    }

    /** Small pulsing-look status dot. */
    private JComponent pulseDot() {
        return new JComponent() {
            { setPreferredSize(new Dimension(10, 10)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(16, 185, 129, 70));
                g2.fillOval(0, 0, w - 1, h - 1);
                g2.setColor(EMERALD);
                g2.fillOval(2, 2, w - 5, h - 5);
                g2.dispose();
            }
        };
    }

    private JLabel pill(String text, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(font(F_MONO, style, 11));
        l.setForeground(color);
        return l;
    }

    /* ═══════════════════ MAIN COLUMN (hero + arcade) ═══════════════════ */

    private JPanel createMainColumn() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JPanel heroRow = new JPanel(new GridLayout(1, 2, 16, 0)) {
            @Override public Dimension getMinimumSize() {
                // Allow the row to shrink so BoxLayout never splits leftover
                // width between it and the arcade section below (which would
                // push the game cards off the left edge).
                return new Dimension(0, super.getMinimumSize().height);
            }
        };
        heroRow.setOpaque(false);
        heroRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        heroRow.add(createTournamentHero());
        heroRow.add(createWelcomeCard());
        col.add(heroRow);
        col.add(Box.createVerticalStrut(20));

        col.add(createArcadeSection());
        col.add(Box.createVerticalGlue());
        return col;
    }

    /* ── Tournament spotlight ── */

    private JPanel createTournamentHero() {
        RoundedPanel hero = new RoundedPanel(new BorderLayout(0, 0), CARD_BG, WHITE_LINE, 18, true);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 16, 22));

        // Badge row
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.add(tagPill("● SEASON 04 PROTOCOL", new Color(236, 72, 153, 46), new Color(0xF9A8D4)));
        badges.add(tagPill("🏆 Tournament Grand Prix", new Color(139, 92, 246, 46), new Color(0xC4B5FD)));
        badges.add(tagPill("⏱ Ends in 03h : 24m : 18s", new Color(0, 0, 0, 90), new Color(0x67E8F9)));
        content.add(badges);
        content.add(Box.createVerticalStrut(14));

        JLabel headline = new JLabel("<html><body style='width: 520px'>Zombie Survival: "
            + "<font color='#67E8F9'>Nightfall</font> <font color='#C4B5FD'>Outbreak</font></body></html>");
        headline.setFont(font(F_DISPLAY, Font.BOLD, 27));
        headline.setForeground(Color.WHITE);
        headline.putClientProperty("gvHeroText", Boolean.TRUE);
        headline.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(headline);
        content.add(Box.createVerticalStrut(8));

        JLabel blurb = new JLabel("<html><body style='width: 520px'>Descend into sector 07, repel "
            + "bio-mechanical swarms, and extract the encrypted mainframe data. Compete against "
            + "2,400+ Netrunners for the weekly championship vault.</body></html>");
        blurb.setFont(font(F_SANS, Font.PLAIN, 12));
        blurb.setForeground(TEXT_DIM);
        blurb.putClientProperty("gvHeroText", true);
        blurb.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(blurb);
        content.add(Box.createVerticalStrut(12));

        JPanel rewards = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        rewards.setOpaque(false);
        rewards.setAlignmentX(Component.LEFT_ALIGNMENT);
        rewards.add(tagPill("🏆 Prize Pool: $10,000 GVR + 5,000 XP", new Color(245, 158, 11, 30), new Color(0xFCD34D)));
        rewards.add(pill("● Free Entry • Modifiers: Instagib & Hyper-Speed", EMERALD, Font.PLAIN));
        content.add(rewards);

        content.add(Box.createVerticalGlue());

        // Action bar
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton drop = neonButton("▶  DROP INTO ARENA", CYAN, VIOLET);
        drop.addActionListener(e -> launchGame("Zombie Survival"));
        actions.add(drop);

        JButton rules = ghostButton("Rules & Tiers");
        actions.add(rules);
        content.add(actions);

        hero.add(content, BorderLayout.CENTER);
        return hero;
    }

    /* ── Welcome / quick stats card ── */

    private JPanel createWelcomeCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 0), CARD_BG, WHITE_LINE, 18, true);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        // Identity row
        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        identity.setOpaque(false);
        identity.setAlignmentX(Component.LEFT_ALIGNMENT);
        identity.add(new Avatar(player.getUsername(), 54));
        identity.add(new JLabel(" "));

        JPanel idText = new JPanel();
        idText.setOpaque(false);
        idText.setLayout(new BoxLayout(idText, BoxLayout.Y_AXIS));

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        nameRow.setOpaque(false);
        JLabel name = new JLabel(displayName(player.getUsername()));
        name.setFont(font(F_DISPLAY, Font.BOLD, 16));
        name.setForeground(Color.WHITE);
        nameRow.add(name);
        nameRow.add(tagPill("LV. " + player.getLevel(), new Color(6, 182, 212, 40), new Color(0x67E8F9)));
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel email = new JLabel(player.getUsername());
        email.setFont(font(F_MONO, Font.PLAIN, 10));
        email.setForeground(TEXT_DIM);
        email.setAlignmentX(Component.LEFT_ALIGNMENT);

        idText.add(nameRow);
        idText.add(email);
        identity.add(idText);
        content.add(identity);
        content.add(Box.createVerticalStrut(14));

        // 3-stat grid
        XPManager xpMgr = XPManager.getInstance();
        int xpPerLevel = xpMgr.getXpPerLevel();
        JPanel stats = new JPanel(new GridLayout(1, 3, 8, 0));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        stats.add(statTile("NETRUNNER", "Tier " + player.getLevel(), "Novice", CYAN));
        stats.add(statTile("TOTAL XP", String.valueOf(player.getXp()), "/ " + xpPerLevel + " XP", VIOLET));
        stats.add(statTile("TOKENS", player.getCoins() + " 🪙", "Arcade Vault", AMBER));
        content.add(stats);
        content.add(Box.createVerticalStrut(14));

        // XP progress
        JPanel xpRow = new JPanel(new BorderLayout(0, 0));
        xpRow.setOpaque(false);
        xpRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        xpRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        xpRow.add(pill("⚡ XP to Level " + (player.getLevel() + 1), TEXT_DIM, Font.PLAIN), BorderLayout.WEST);
        int xpToNext = xpMgr.getXpToNextLevel(player);
        xpRow.add(pill(xpToNext >= 0 ? xpToNext + " to go" : "MAX", new Color(0xC4B5FD), Font.BOLD), BorderLayout.EAST);
        content.add(xpRow);
        content.add(Box.createVerticalStrut(6));

        int currentLevelXp = player.getLevel() * xpPerLevel;
        int prevLevelXp = (player.getLevel() - 1) * xpPerLevel;
        float progress = (float) (player.getXp() - prevLevelXp) / Math.max(1, currentLevelXp - prevLevelXp);
        ProgressBar xpBar = new ProgressBar(Math.round(progress * 100), CYAN, VIOLET, PINK);
        xpBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        xpBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(xpBar);

        content.add(Box.createVerticalGlue());
        content.add(Box.createVerticalStrut(10));

        // Next unlock teaser
        JPanel teaser = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(139, 92, 246, 34), getWidth(), getHeight(), new Color(6, 182, 212, 22)));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(new Color(139, 92, 246, 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        teaser.setOpaque(false);
        teaser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        teaser.setAlignmentX(Component.LEFT_ALIGNMENT);
        teaser.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        teaser.add(pill("🔓 Level " + (player.getLevel() + 1) + " Perk: Custom Crosshairs", TEXT, Font.PLAIN), BorderLayout.WEST);
        teaser.add(pill("+" + Math.max(100, xpPerLevel - player.getXp()) + " XP", new Color(0x67E8F9), Font.BOLD), BorderLayout.EAST);
        content.add(teaser);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel statTile(String caption, String value, String sub, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WHITE_LINE),
            BorderFactory.createEmptyBorder(8, 4, 8, 4)));
        tile.setBackground(new Color(255, 255, 255, 8));

        JLabel cap = new JLabel(caption, SwingConstants.CENTER);
        cap.setFont(font(F_MONO, Font.PLAIN, 9));
        cap.setForeground(TEXT_FAINT);
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setFont(font(F_DISPLAY, Font.BOLD, 15));
        val.setForeground(valueColor);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel s = new JLabel(sub, SwingConstants.CENTER);
        s.setFont(font(F_MONO, Font.PLAIN, 9));
        s.setForeground(TEXT_FAINT);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);

        tile.add(cap);
        tile.add(val);
        tile.add(s);
        return tile;
    }

    /* ── Arcade section: filter bar + grid ── */

    private JPanel createArcadeSection() {
        JPanel section = new JPanel(new BorderLayout(0, 14)) {
            @Override public Dimension getMinimumSize() {
                // Shrinkable: keeps BoxLayout from reserving width for the
                // filter row, so the section always gets the full column
                // width and the game grid stays flush with the left edge.
                return new Dimension(0, super.getMinimumSize().height);
            }
        };
        section.setOpaque(false);
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Let BoxLayout stretch the section to the full main-column width so
        // the grid always reaches the right edge with no dead space.
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4000));

        // Header: title row stacked above the filter tabs, so nothing clips
        // on narrower windows and the section stays flush with the left edge.
        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        JPanel marker = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, CYAN, 0, getHeight(), VIOLET));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                g2.dispose();
            }
        };
        marker.setPreferredSize(new Dimension(10, 24));
        titleRow.add(marker);
        JLabel title = new JLabel("Arcade Modules");
        title.setFont(font(F_DISPLAY, Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        titleRow.add(title);
        titleRow.add(tagPill(GAMES.length + " Modules Online", new Color(6, 182, 212, 44), new Color(0x67E8F9)));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String cat : CATEGORIES) {
            JButton tab = categoryButton(cat, cat.equals(activeCategory));
            tab.addActionListener(e -> {
                activeCategory = cat;
                refreshGamesGrid();
            });
            filters.add(tab);
        }
        head.add(titleRow);
        head.add(Box.createVerticalStrut(10));
        head.add(filters);
        section.add(head, BorderLayout.NORTH);

        // Grid container (rebuilt on filter change or column-count change;
        // BodyPanel.doLayout triggers the rebuild when the window resizes).
        gridContainer = new JPanel(new BorderLayout());
        gridContainer.setOpaque(false);
        refreshGamesGrid();
        section.add(gridContainer, BorderLayout.CENTER);
        return section;
    }

    private JButton categoryButton(String label, boolean active) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setPaint(new GradientPaint(0, 0, CYAN, getWidth(), getHeight(), new Color(0x2563EB)));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    setForeground(new Color(0x08101A));
                } else {
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    g2.setColor(WHITE_LINE);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                    setForeground(TEXT_DIM);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font(F_MONO, Font.PLAIN, 11));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (!active) b.setForeground(TEXT); b.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { if (!active) b.setForeground(TEXT_DIM); b.repaint(); }
        });
        return b;
    }

    /** Rebuild the game grid for the active category + search text. */
    private void refreshGamesGrid() {
        if (gridContainer == null) return;

        List<GameInfo> visible = new ArrayList<>();
        for (GameInfo g : GAMES) {
            boolean catOk = activeCategory.equals("All Games")
                || (activeCategory.equals("Popular")
                    ? (g.name.equals("Chess") || g.name.equals("Snake") || g.featured
                       || g.name.equals("Mini Racing") || g.name.equals("One Bullet")
                       || g.name.equals("DON'T LOOK"))
                    : g.categories.contains(activeCategory));
            boolean searchOk = searchText.isEmpty()
                || g.name.toLowerCase().contains(searchText)
                || g.desc.toLowerCase().contains(searchText);
            if (catOk && searchOk) visible.add(g);
        }

        // Responsive grid: the column count adapts to the available width so
        // cards always fill the row from the left edge with no dead space.
        int cols = gridColumns();
        builtColumns = cols;
        int gridW = lastMainW > 0 ? lastMainW : 900;
        builtCardW = Math.max(MIN_CARD_W, (gridW - (cols - 1) * CARD_GAP) / cols);
        JPanel grid = new GameGrid(new GridBagLayout());
        grid.setOpaque(false);

        if (visible.isEmpty()) {
            JLabel empty = new JLabel("No modules match your filter — try another search.",
                SwingConstants.CENTER);
            empty.setFont(font(F_MONO, Font.PLAIN, 12));
            empty.setForeground(TEXT_FAINT);
            empty.setBorder(BorderFactory.createEmptyBorder(40, 0, 40, 0));
            GridBagConstraints ec = new GridBagConstraints();
            ec.gridx = 0;
            ec.gridy = 0;
            ec.gridwidth = GridBagConstraints.REMAINDER;
            ec.fill = GridBagConstraints.HORIZONTAL;
            ec.weightx = 1.0;
            ec.insets = new Insets(8, 0, 8, 0);
            grid.add(empty, ec);
        } else {
            for (int i = 0; i < visible.size(); i++) {
                GameInfo g = visible.get(i);
                GameCard card = new GameCard(g);
                boolean lastCol = (i % cols) == cols - 1;
                boolean lastRow = (i / cols) == (visible.size() - 1) / cols;

                GridBagConstraints c = new GridBagConstraints();
                c.gridx = i % cols;
                c.gridy = i / cols;
                c.weightx = 1.0;
                c.fill = GridBagConstraints.BOTH;
                c.insets = new Insets(
                    c.gridy == 0 ? 0 : CARD_GAP / 2,
                    c.gridx == 0 ? 0 : CARD_GAP / 2,
                    lastRow ? 0 : CARD_GAP / 2,
                    lastCol ? 0 : CARD_GAP / 2);
                grid.add(card, c);
            }
            // Pad the final partial row with invisible fillers so the real
            // cards keep equal widths instead of stretching over empty slots.
            int fullRows = (visible.size() + cols - 1) / cols;
            for (int i = visible.size(); i < fullRows * cols; i++) {
                GridBagConstraints c = new GridBagConstraints();
                c.gridx = i % cols;
                c.gridy = i / cols;
                c.weightx = 1.0;
                c.fill = GridBagConstraints.BOTH;
                c.insets = new Insets(
                    c.gridy == 0 ? 0 : CARD_GAP / 2,
                    c.gridx == 0 ? 0 : CARD_GAP / 2,
                    c.gridy == fullRows - 1 ? 0 : CARD_GAP / 2,
                    c.gridx == cols - 1 ? 0 : CARD_GAP / 2);
                JPanel filler = new JPanel();
                filler.setOpaque(false);
                grid.add(filler, c);
            }
        }

        JScrollPane gridScroll = new JScrollPane(grid,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        gridScroll.setBorder(null);
        gridScroll.getVerticalScrollBar().setUnitIncrement(18);
        gridScroll.getViewport().setOpaque(false);
        gridScroll.setOpaque(false);

        gridContainer.removeAll();
        gridContainer.add(gridScroll, BorderLayout.CENTER);
        gridContainer.revalidate();
        gridContainer.repaint();
    }

    /**
     * Columns for the game grid, derived from the width available to the
     * arcade section (window − sidebar − margins). Cards start from the left
     * edge and stretch equally, so there is no dead space on either side.
     */
    private int gridColumns() {
        return columnsFor(lastMainW > 0 ? lastMainW : 900); // sane default before first layout
    }

    private static int columnsFor(int w) {
        // Reserve room for the section's side insets/padding.
        int usable = Math.max(MIN_CARD_W, w - 4);
        return Math.max(1, Math.min(MAX_COLUMNS, usable / MIN_CARD_W));
    }

    /** Grid canvas that always matches the scroll-pane viewport width so
     *  cards stretch to fill the row with no dead space. */
    private static class GameGrid extends JPanel implements Scrollable {
        GameGrid(LayoutManager lm) { super(lm); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /**
     * Two-column body that tracks the scroll-pane viewport width (no horizontal
     * scrolling, no dead space). The fixed-width sidebar hugs the right edge;
     * the main column takes the remainder, so Arcade Modules start at the left
     * edge of the content area directly below the hero. While laying out, the
     * panel clamps the main column to the space actually available and
     * reflows the hero cards' HTML text to the new width.
     */
    private static class BodyPanel extends JPanel implements Scrollable {
        BodyPanel() { super(new BorderLayout(0, 0)); }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }        @Override
        public void doLayout() {
            Component main = getComponent(0);
            Component side = getComponentCount() > 1 ? getComponent(1) : null;
            int sideW = (side != null && side.isVisible()) ? side.getPreferredSize().width : 0;
            int mainW = Math.max(320, getWidth() - sideW);

            // Keep the arcade section's cached width in sync and rebuild the
            // grid when the responsive column count changes.
            if (main instanceof MainMenu m) {
                int cols = MainMenu.columnsFor(mainW);
                if (m.lastMainW == 0 || cols != m.builtColumns) {
                    m.lastMainW = mainW;
                    m.refreshGamesGrid();
                } else {
                    m.lastMainW = mainW;
                }
            }

            // Reflow hero HTML copy to the width the hero cards actually get.
            int heroW = Math.max(240, (mainW - 16) / 2 - 44);
            if (heroW != lastHeroW) {
                lastHeroW = heroW;
                reflowHero(main, heroW);
            }

            main.setMaximumSize(new Dimension(mainW, Integer.MAX_VALUE));
            super.doLayout();
        }

        private int lastHeroW = -1;

        /** Rewrite the width:...px in hero headline/blurb HTML to fit. Only
         *  labels tagged gvHeroText are touched — game-card copy is left alone. */
        private static void reflowHero(Component root, int width) {
            if (root instanceof JLabel l) {
                String t = l.getText();
                if (t != null && t.contains("body style='width:")
                    && Boolean.TRUE.equals(l.getClientProperty("gvHeroText"))) {
                    l.setText(t.replaceFirst("width: \\d+px", "width: " + width + "px"));
                }
            }
            if (root instanceof Container cont) {
                for (Component c : cont.getComponents()) reflowHero(c, width);
            }
        }
    }

    /* ═══════════════════ GAME CARD ═══════════════════ */

    private class GameCard extends RoundedPanel {
        final GameInfo info;
        final JLabel playLabel;
        final String basePlayText;

        GameCard(GameInfo info) {
            super(new BorderLayout(0, 0), CARD_BG,
                info.featured ? new Color(236, 72, 153, 110) : WHITE_LINE, 18, true);
            this.info = info;
            this.basePlayText = info.featured ? "ENTER SWARM  ⚔" : "Play Now  →";
            setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

            JPanel inner = new JPanel();
            inner.setOpaque(false);
            inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

            // Top row: icon tile + right pills
            JPanel topRow = new JPanel(new BorderLayout(8, 0));
            topRow.setOpaque(false);
            topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            RoundedPanel tile = new RoundedPanel(new BorderLayout(),
                new Color(info.accent.getRed(), info.accent.getGreen(), info.accent.getBlue(), 42),
                new Color(info.accent.getRed(), info.accent.getGreen(), info.accent.getBlue(), 120), 14, false);
            tile.setPreferredSize(new Dimension(50, 50));
            JLabel icon = new JLabel(info.icon, SwingConstants.CENTER);
            icon.setFont(font("Segoe UI Emoji", Font.PLAIN, 24));
            tile.add(icon, BorderLayout.CENTER);
            topRow.add(tile, BorderLayout.WEST);

            JPanel pills = new JPanel();
            pills.setOpaque(false);
            pills.setLayout(new BoxLayout(pills, BoxLayout.Y_AXIS));
            JPanel modePill = tagPill(info.mode,
                new Color(info.accent.getRed(), info.accent.getGreen(), info.accent.getBlue(), 46), info.accent);
            modePill.setAlignmentX(Component.RIGHT_ALIGNMENT);
            int best = player.getGameHighScore(info.name);
            String metaText = best > 0 ? "Best: " + best + " pts" : info.meta;
            JLabel meta = new JLabel(metaText);
            meta.setFont(font(F_MONO, Font.PLAIN, 9));
            meta.setForeground(TEXT_FAINT);
            meta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WHITE_LINE),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            meta.setAlignmentX(Component.RIGHT_ALIGNMENT);
            pills.add(modePill);
            pills.add(Box.createVerticalStrut(4));
            pills.add(meta);
            JPanel pillWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            pillWrap.setOpaque(false);
            pillWrap.add(pills);
            topRow.add(pillWrap, BorderLayout.EAST);
            inner.add(topRow);
            inner.add(Box.createVerticalStrut(10));

            // Name + genre tag
            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            nameRow.setOpaque(false);
            nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel name = new JLabel(info.name);
            name.setFont(font(F_DISPLAY, Font.BOLD, 17));
            name.setForeground(TEXT);
            nameRow.add(name);
            nameRow.add(new JLabel(info.tag) {
                {
                    setFont(font(F_MONO, Font.BOLD, 9));
                    setForeground(info.accent);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(info.accent.getRed(), info.accent.getGreen(), info.accent.getBlue(), 70)),
                        BorderFactory.createEmptyBorder(1, 5, 1, 5)));
                }
            });
            inner.add(nameRow);
            inner.add(Box.createVerticalStrut(3));

            int descW = Math.max(200, Math.min(560, builtCardW - 46));
            JLabel desc = new JLabel("<html><body style='width: " + descW + "px'>" + info.desc + "</body></html>");
            desc.setFont(font(F_SANS, Font.PLAIN, 11));
            desc.setForeground(TEXT_DIM);
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            inner.add(desc);
            inner.add(Box.createVerticalStrut(12));

            // Footer: XP + play
            JPanel footer = new JPanel(new BorderLayout(0, 0));
            footer.setOpaque(false);
            footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            footer.setAlignmentX(Component.LEFT_ALIGNMENT);

            playLabel = new JLabel(basePlayText) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (info.featured) {
                        g2.setPaint(new GradientPaint(0, 0, PINK, getWidth(), getHeight(), VIOLET));
                        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                        setForeground(Color.WHITE);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            playLabel.setFont(font(F_DISPLAY, Font.BOLD, 11));
            if (!info.featured) {
                playLabel.setForeground(info.accent);
                playLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(info.accent.getRed(), info.accent.getGreen(), info.accent.getBlue(), 90)),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            } else {
                playLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            }

            JPanel xpWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            xpWrap.setOpaque(false);
            xpWrap.add(new JLabel("●") {
                { setForeground(info.accent); setFont(font(F_SANS, Font.PLAIN, 8)); }
            });
            xpWrap.add(new JLabel(info.xpLabel) {
                { setFont(font(F_MONO, Font.BOLD, 10)); setForeground(info.accent); }
            });
            footer.add(xpWrap, BorderLayout.WEST);
            footer.add(playLabel, BorderLayout.EAST);
            inner.add(footer);
            inner.add(Box.createVerticalGlue());

            add(inner, BorderLayout.CENTER);

            // Hover + click on the card and all children
            MouseAdapter adapter = new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    setFill(CARD_HOVER);
                    setLine(new Color(info.accent.getRed(), info.accent.getGreen(), info.accent.getBlue(), 200));
                    repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    setFill(CARD_BG);
                    setLine(info.featured ? new Color(236, 72, 153, 110) : WHITE_LINE);
                    repaint();
                }
                @Override public void mouseClicked(MouseEvent e) {
                    launchGame(info.name);
                }
            };
            wireMouse(this, adapter);
        }
    }

    private void wireMouse(JComponent root, MouseAdapter adapter) {
        root.addMouseListener(adapter);
        if (root instanceof Container) {
            for (Component c : ((Container) root).getComponents()) {
                if (c instanceof JComponent) wireMouse((JComponent) c, adapter);
            }
        }
    }

    /** Route a game launch: external web games keep the hub open. */
    private void launchGame(String name) {
        switch (name) {
            case "DON'T LOOK" -> DontLookLauncher.launch();
            case "UNKNOWN SIGNAL" -> UnknownSignalLauncher.launch();
            case "Mirror World" -> MirrorWorldLauncher.launch();
            case "One Bullet" -> OneBulletLauncher.launch();
            default -> {
                dispose();
                if (callback != null) callback.onPlayGame(name);
            }
        }
    }

    /* ═══════════════════ SIDEBAR (player rig) ═══════════════════ */

    private JPanel createSidebar() {
        RoundedPanel panel = new RoundedPanel(new BorderLayout(0, 0), CARD_BG, WHITE_LINE, 18, true);
        panel.setPreferredSize(new Dimension(370, 0));

        // Top-anchor the body: when the content is shorter than the viewport,
        // JViewport centers it by default (alignment 0.5), which made the
        // LEVEL/XP/COINS stats and telemetry float mid-panel with dead space.
        JPanel body = sidebarBody();
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setAlignmentY(Component.TOP_ALIGNMENT);

        JScrollPane sp = new JScrollPane(body);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getViewport().setOpaque(false);
        sp.setOpaque(false);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private JPanel sidebarBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(18, 18, 16, 18));

        // Terminal header strip
        JPanel strip = new JPanel(new BorderLayout(0, 0));
        strip.setOpaque(false);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        strip.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel rigId = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        rigId.setOpaque(false);
        rigId.add(pulseDot());
        rigId.add(pill("RIG: TERMINAL #09-A", new Color(0x67E8F9), Font.BOLD));
        strip.add(rigId, BorderLayout.WEST);
        strip.add(new JLabel("⚙") {
            { setForeground(TEXT_FAINT); setFont(font(F_SANS, Font.PLAIN, 13)); }
        }, BorderLayout.EAST);
        body.add(strip);
        body.add(Box.createVerticalStrut(14));

        // Avatar + identity
        Avatar avatar = new Avatar(player.getUsername(), 88);
        avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(avatar);
        body.add(Box.createVerticalStrut(8));

        JLabel name = new JLabel(player.getUsername(), SwingConstants.CENTER);
        name.setFont(font(F_DISPLAY, Font.BOLD, 14));
        name.setForeground(TEXT);
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(name);
        body.add(Box.createVerticalStrut(4));

        LeaderboardManager lbMgr = LeaderboardManager.getInstance();
        int rank = lbMgr.getPlayerGlobalRank(player.getUsername());
        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        badgeRow.setOpaque(false);
        badgeRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        badgeRow.add(tagPill("NETRUNNER CADET", new Color(6, 182, 212, 40), new Color(0x67E8F9)));
        badgeRow.add(pill(rank > 0 ? "Rank #" + rank : "Unranked", VIOLET, Font.BOLD));
        body.add(badgeRow);
        body.add(Box.createVerticalStrut(14));

        // Stat box
        XPManager xpMgr = XPManager.getInstance();
        int xpPerLevel = xpMgr.getXpPerLevel();
        JPanel statBox = new JPanel(new GridLayout(1, 3, 0, 0)) {
            @Override protected void paintBorder(Graphics g) { /* custom inset below */ }
        };
        statBox.setOpaque(false);
        statBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        statBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        statBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(WHITE_LINE),
            BorderFactory.createEmptyBorder(8, 6, 8, 6)));
        statBox.add(sidebarStat("LEVEL", String.valueOf(player.getLevel()), "Tier: Novice", CYAN));
        statBox.add(sidebarStat("XP", String.valueOf(player.getXp()), "/ " + xpPerLevel + " XP", VIOLET));
        statBox.add(sidebarStat("COINS", player.getCoins() + " 🪙", "Vault GVR", AMBER));
        body.add(statBox);
        body.add(Box.createVerticalStrut(12));

        // XP bar
        JPanel xpHead = new JPanel(new BorderLayout(0, 0));
        xpHead.setOpaque(false);
        xpHead.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        xpHead.setAlignmentX(Component.LEFT_ALIGNMENT);
        int xpToNext = xpMgr.getXpToNextLevel(player);
        xpHead.add(pill("XP to Level " + (player.getLevel() + 1) + ": " + (xpToNext >= 0 ? xpToNext : "MAX"),
            TEXT_DIM, Font.PLAIN), BorderLayout.WEST);
        int currentLevelXp = player.getLevel() * xpPerLevel;
        int prevLevelXp = (player.getLevel() - 1) * xpPerLevel;
        int percent = Math.round((float) (player.getXp() - prevLevelXp) / Math.max(1, currentLevelXp - prevLevelXp) * 100);
        xpHead.add(pill(percent + "%", new Color(0x67E8F9), Font.BOLD), BorderLayout.EAST);
        body.add(xpHead);
        body.add(Box.createVerticalStrut(5));

        ProgressBar xpBar = new ProgressBar(percent, CYAN, VIOLET, null);
        xpBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        xpBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(xpBar);
        body.add(Box.createVerticalStrut(14));

        // Performance telemetry
        body.add(sectionHead("PERFORMANCE TELEMETRY", "All Modules"));
        body.add(Box.createVerticalStrut(8));
        body.add(telemetryRow("🎮", "Games Played", String.valueOf(player.getGamesPlayed()), TEXT, CYAN));
        body.add(telemetryRow("🏅", "Total Wins", String.valueOf(player.getWins()), EMERALD, EMERALD));
        body.add(telemetryRow("📈", "Win Rate", String.format("%.1f%%", player.getWinRate()), new Color(0xC4B5FD), VIOLET));
        body.add(telemetryRow("📊", "Tournament Rank", rank > 0 ? "#" + rank : "Unranked", new Color(0xFCD34D), AMBER));
        body.add(Box.createVerticalStrut(14));

        // Daily bounties
        body.add(sectionHead("⚡ DAILY BOUNTIES", "Resets in 08:35", AMBER));
        body.add(Box.createVerticalStrut(8));
        body.add(bountyCard("✔ First Blood", "Win 1 match in any arcade module.",
            "+100 XP (Claimed)", 100, EMERALD, true));
        body.add(bountyCard("◐ Tactical Mind", "Play 2 rounds of Cyber Chess.",
            "+80 XP • 40 🪙", player.getGamesPlayed() >= 2 ? 100 : 50, VIOLET, false));
        body.add(bountyCard("○ Speed Demon", "Reach 500m in Mini Racing without crash.",
            "+120 XP • 60 🪙", 0, CYAN, false));
        body.add(Box.createVerticalStrut(14));

        // Squad
        body.add(sectionHead("👥 NETRUNNER SQUAD (3)", "+ Invite", CYAN));
        body.add(Box.createVerticalStrut(8));
        body.add(squadRow(EMERALD, "CyberGhost_99", "Playing Zombie Survival", "Join", true));
        body.add(squadRow(AMBER, "NeonValkyrie", "In Arcade Lobby", "Invite", true));
        body.add(squadRow(new Color(0x475569), "GlitchMaster", "Offline • 2h ago", "Zzz", false));

        body.add(Box.createVerticalGlue());
        return body;
    }

    private JPanel sidebarStat(String caption, String value, String sub, Color valueColor) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel cap = new JLabel(caption, SwingConstants.CENTER);
        cap.setFont(font(F_MONO, Font.PLAIN, 9));
        cap.setForeground(TEXT_FAINT);
        cap.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setFont(font(F_DISPLAY, Font.BOLD, 18));
        val.setForeground(valueColor);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel s = new JLabel(sub, SwingConstants.CENTER);
        s.setFont(font(F_MONO, Font.PLAIN, 9));
        s.setForeground(TEXT_FAINT);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(cap);
        p.add(val);
        p.add(s);
        return p;
    }

    private JPanel sectionHead(String left, String right) {
        return sectionHead(left, right, TEXT);
    }

    private JPanel sectionHead(String left, String right, Color leftColor) {
        JPanel head = new JPanel(new BorderLayout(0, 0));
        head.setOpaque(false);
        head.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        head.add(pill(left, leftColor, Font.BOLD), BorderLayout.WEST);
        head.add(pill(right, TEXT_FAINT, Font.PLAIN), BorderLayout.EAST);
        return head;
    }

    private JPanel telemetryRow(String icon, String label, String value, Color valueColor, Color iconColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel i = new JLabel(icon);
        i.setFont(font("Segoe UI Emoji", Font.PLAIN, 12));
        i.setForeground(iconColor);
        left.add(i);
        left.add(pill(label, new Color(0xCBD5E1), Font.PLAIN));
        row.add(left, BorderLayout.WEST);
        row.add(pill(value, valueColor, Font.BOLD), BorderLayout.EAST);
        return row;
    }

    private JPanel bountyCard(String title, String desc, String reward, int progress, Color color, boolean done) {
        JPanel card = new JPanel();
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(color.getRed(), color.getGreen(), color.getBlue(), done ? 80 : 40)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JPanel top = new JPanel(new BorderLayout(0, 0));
        top.setOpaque(false);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        top.add(pill(title, done ? color : TEXT, Font.BOLD), BorderLayout.WEST);
        top.add(pill(reward, color, Font.BOLD), BorderLayout.EAST);

        JLabel d = new JLabel(desc);
        d.setFont(font(F_SANS, Font.PLAIN, 10));
        d.setForeground(TEXT_FAINT);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);

        ProgressBar bar = new ProgressBar(progress, color, null, null);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(top);
        card.add(Box.createVerticalStrut(3));
        card.add(d);
        card.add(Box.createVerticalStrut(5));
        card.add(bar);
        return card;
    }

    private JPanel squadRow(Color dotColor, String name, String status, String action, boolean online) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 6));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        left.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setFont(font(F_SANS, Font.PLAIN, 10));
        dot.setForeground(dotColor);
        left.add(dot);
        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel n = new JLabel(name);
        n.setFont(font(F_DISPLAY, Font.BOLD, 11));
        n.setForeground(online ? TEXT : TEXT_DIM);
        JLabel s = new JLabel(status);
        s.setFont(font(F_MONO, Font.PLAIN, 9));
        s.setForeground(TEXT_FAINT);
        texts.add(n);
        texts.add(s);
        left.add(texts);
        row.add(left, BorderLayout.WEST);

        if (online && !"Zzz".equals(action)) {
            JButton b = new JButton(action) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            b.setFont(font(F_MONO, Font.BOLD, 10));
            b.setForeground(new Color(0xA7F3D0));
            b.setBackground(new Color(16, 185, 129, 46));
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.add(b, BorderLayout.EAST);
        } else {
            row.add(pill(action, TEXT_FAINT, Font.PLAIN), BorderLayout.EAST);
        }
        return row;
    }

    /* ═══════════════════ FOOTER ═══════════════════ */

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(new Color(0x080A13));
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, WHITE_LINE),
            BorderFactory.createEmptyBorder(10, 22, 10, 22)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(pill("GameVerse Platform", TEXT_DIM, Font.BOLD));
        left.add(pill("•", TEXT_FAINT, Font.PLAIN));
        left.add(pulseDot());
        left.add(pill("Connected to Cluster #09-CyberNet", TEXT_FAINT, Font.PLAIN));
        left.add(pill("•", TEXT_FAINT, Font.PLAIN));
        left.add(pill("Latency: 14ms", TEXT_FAINT, Font.PLAIN));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        right.setOpaque(false);
        for (String link : new String[]{"System Status", "Privacy Protocol", "Terms of Service", "Arcade Neural Support"}) {
            right.add(footerLink(link));
        }

        footer.add(left, BorderLayout.WEST);
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    private JLabel footerLink(String text) {
        JLabel l = new JLabel(text);
        l.setFont(font(F_MONO, Font.PLAIN, 11));
        l.setForeground(TEXT_FAINT);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { l.setForeground(CYAN); }
            @Override public void mouseExited(MouseEvent e)  { l.setForeground(TEXT_FAINT); }
        });
        return l;
    }

    /* ═══════════════════ SHARED WIDGETS ═══════════════════ */

    /** Small bordered tag pill. */
    private JPanel tagPill(String text, Color bg, Color fg) {
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pill.setOpaque(false);
        JLabel l = new JLabel(text);
        l.setFont(font(F_MONO, Font.BOLD, 10));
        l.setForeground(fg);
        pill.add(l);
        return pill;
    }

    /** Gradient CTA button. */
    private JButton neonButton(String text, Color from, Color to) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, from, getWidth(), getHeight(), to));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font(F_DISPLAY, Font.BOLD, 13));
        b.setForeground(new Color(0x06131B));
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Subtle bordered ghost button. */
    private JButton ghostButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(WHITE_LINE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(font(F_DISPLAY, Font.PLAIN, 12));
        b.setForeground(TEXT);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(11, 18, 11, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(CYAN); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(TEXT); }
        });
        return b;
    }

    /** Rounded progress bar (optional 3-stop gradient). */
    private static class ProgressBar extends JComponent {
        private final int percent;
        private final Color from, mid, to;

        ProgressBar(int percent, Color from, Color mid, Color to) {
            this.percent = Math.max(0, Math.min(100, percent));
            this.from = from;
            this.mid = mid;
            this.to = to;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0x0F172A));
            g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
            int fillW = (int) ((w - 2) * percent / 100f);
            if (fillW > 0) {
                if (mid != null && to != null) {
                    g2.setPaint(new GradientPaint(0, 0, from, w / 2f, 0, mid));
                    g2.fillRoundRect(1, 1, fillW, h - 2, h, h);
                    g2.setPaint(new GradientPaint(w / 2f, 0, mid, w, 0, to));
                    g2.fillRoundRect(fillW / 2, 1, fillW - fillW / 2, h - 2, h, h);
                } else if (mid != null) {
                    g2.setPaint(new GradientPaint(0, 0, from, w, 0, mid));
                    g2.fillRoundRect(1, 1, fillW, h - 2, h, h);
                } else {
                    g2.setColor(from);
                    g2.fillRoundRect(1, 1, fillW, h - 2, h, h);
                }
            }
            g2.dispose();
        }
    }

    /** Rounded glass panel with hover-able fill/line, hand-painted. */
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

        void setFill(Color fill) { this.fill = fill; repaint(); }

        void setLine(Color line) { this.line = line; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (fill != null) {
                if (gradient && h > 0) {
                    g2.setPaint(new GradientPaint(0, 0,
                        new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 216),
                        0, h, CARD_BG_DEEP));
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

    /** Square avatar tile with gradient ring, initial and online dot. */
    private static class Avatar extends JComponent {
        private final String initial;
        private final int size;

        Avatar(String username, int size) {
            String display = displayName(username);
            this.initial = display.substring(0, 1).toUpperCase();
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int pad = 3;
            int s = Math.min(w, h) - pad * 2;
            int x = (w - s) / 2, y = (h - s) / 2;

            // Gradient ring
            g2.setPaint(new GradientPaint(x, y, CYAN, x + s, y + s, PINK));
            g2.fillRoundRect(x, y, s, s, 20, 20);
            g2.setColor(new Color(0x0C0E18));
            g2.fillRoundRect(x + pad, y + pad, s - pad * 2, s - pad * 2, 16, 16);

            // Initial
            g2.setColor(Color.WHITE);
            g2.setFont(font(F_DISPLAY, Font.BOLD, s / 2));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(initial,
                x + (s - fm.stringWidth(initial)) / 2,
                y + (s + fm.getAscent() - fm.getDescent()) / 2 - 1);

            // Online dot
            int dot = Math.max(10, s / 7);
            g2.setColor(EMERALD);
            g2.fillOval(x + s - dot, y + s - dot, dot, dot);
            g2.setColor(new Color(0x0C0E18));
            g2.drawOval(x + s - dot, y + s - dot, dot - 1, dot - 1);
            g2.dispose();
        }
    }

    /* ═══════════════════ ACHIEVEMENTS DIALOG ═══════════════════ */

    private void showAchievements() {
        AchievementManager mgr = AchievementManager.getInstance();
        var allAchievements = mgr.getAllAchievements();
        Set<String> owned = mgr.getPlayerAchievements(player.getUsername());

        JDialog dlg = new JDialog(this, "🎖 Achievements", true);
        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dlg.setSize(640, 500);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JLabel head = new JLabel("Achievements  —  " + owned.size() + " / " + allAchievements.size() + " unlocked",
            SwingConstants.CENTER);
        head.setFont(font(F_DISPLAY, Font.BOLD, 17));
        head.setForeground(VIOLET);
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
        card.setBackground(owned ? new Color(6, 60, 45) : CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(owned ? EMERALD : CYBER_LINE, 1, true),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JLabel title = new JLabel((owned ? "✅ " : "🔒 ") + name);
        title.setFont(font(F_SANS, Font.BOLD, 13));
        title.setForeground(owned ? EMERALD : TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(5));
        JLabel descLbl = new JLabel("<html><body style='width: 220px'>" + desc + "</body></html>");
        descLbl.setFont(font(F_SANS, Font.PLAIN, 11));
        descLbl.setForeground(TEXT_DIM);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(descLbl);
        if (!owned) {
            card.add(Box.createVerticalStrut(6));
            JLabel reward = new JLabel("Reward: +" + xp + " XP  +" + coins + " 🪙");
            reward.setFont(font(F_MONO, Font.PLAIN, 11));
            reward.setForeground(AMBER);
            reward.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(reward);
        }
        return card;
    }

    /* ═══════════════════ LEADERBOARD DIALOG ═══════════════════ */

    private void showLeaderboard() {
        LeaderboardManager mgr = LeaderboardManager.getInstance();
        int myRank = mgr.getPlayerGlobalRank(player.getUsername());

        JDialog dlg = new JDialog(this, "🏆 Leaderboards", true);
        dlg.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dlg.setSize(540, 480);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        JLabel head = new JLabel("GLOBAL LEADERBOARD", SwingConstants.CENTER);
        head.setFont(font(F_DISPLAY, Font.BOLD, 17));
        head.setForeground(CYAN);
        head.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));
        root.add(head, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BG);
        list.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        var global = mgr.getGlobalLeaderboard(10);
        if (global.isEmpty()) {
            JLabel empty = new JLabel("No scores yet — be the first!", SwingConstants.CENTER);
            empty.setFont(font(F_SANS, Font.PLAIN, 13));
            empty.setForeground(TEXT_DIM);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            list.add(empty);
        } else {
            for (var entry : global) {
                boolean me = entry.getUsername().equals(player.getUsername());
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(me);
                row.setBackground(me ? new Color(8, 62, 76) : CARD_BG);
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
                r.setFont(font(F_SANS, Font.BOLD, 13));
                r.setForeground(me ? AMBER : TEXT);
                JLabel s = new JLabel(entry.getScore() + " pts");
                s.setFont(font(F_MONO, Font.BOLD, 13));
                s.setForeground(me ? AMBER : CYAN);
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
        footer.setFont(font(F_MONO, Font.BOLD, 13));
        footer.setForeground(AMBER);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 0, 14, 0));
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    /** Turn "ashaz@gmail.com" into "Ashaz" for the greeting. */
    private static String displayName(String username) {
        if (username == null) return "player";
        int at = username.indexOf('@');
        String base = at > 0 ? username.substring(0, at) : username;
        if (base.isEmpty()) return "player";
        return base.substring(0, 1).toUpperCase() + base.substring(1);
    }
}
