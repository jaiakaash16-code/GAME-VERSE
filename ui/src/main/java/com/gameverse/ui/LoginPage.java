package com.gameverse.ui;

import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Login page GUI for GameVerse platform.
 * Cyberpunk restyle ported from the Stitch "GameVerse - Player Login" screen:
 * atmospheric backdrop with neon glows and a dot grid, glowing crest, neon
 * tabs, terminal-style fields, rocket launch CTA and a platform matrix.
 * All original login behavior (validation, PlayerManager, callbacks) preserved.
 */
public class LoginPage extends JFrame {

    /* ═══════════════ Stitch palette ═══════════════ */
    private static final Color SURFACE           = new Color(0x11131B);
    private static final Color CONTAINER         = new Color(0x1D1F28);
    private static final Color CONTAINER_LOW     = new Color(0x191B24);
    private static final Color CONTAINER_LOWEST  = new Color(0x0C0E16);
    private static final Color CONTAINER_HIGH    = new Color(0x282A32);
    private static final Color CONTAINER_HIGHEST = new Color(0x33343E);
    private static final Color ON_SURFACE        = new Color(0xE2E1EE);
    private static final Color ON_SURFACE_VAR    = new Color(0xCBC3D7);
    private static final Color OUTLINE           = new Color(0x958EA0);
    private static final Color OUTLINE_VAR       = new Color(0x494454);
    private static final Color PRIMARY           = new Color(0xD0BCFF);
    private static final Color PRIMARY_CONTAINER = new Color(0xA078FF);
    private static final Color ON_PRIMARY        = new Color(0x3C0091);
    private static final Color PRIMARY_DEEP      = new Color(0x5516BE);
    private static final Color SECONDARY         = new Color(0x4CD7F6);
    private static final Color SECONDARY_CONT    = new Color(0x03B5D3);
    private static final Color TERTIARY          = new Color(0xFF516A);
    private static final Color ERROR             = new Color(0xFFB4AB);
    private static final Color DISCORD           = new Color(0x5865F2);

    /* Fonts matching the Stitch screen (Space Grotesk / Inter / JetBrains Mono) */
    private static final Font F_HEADLINE = new Font("Space Grotesk", Font.BOLD, 24);
    private static final Font F_TAB      = new Font("Space Grotesk", Font.BOLD, 13);
    private static final Font F_LABEL    = new Font("JetBrains Mono", Font.BOLD, 10);
    private static final Font F_SMALL    = new Font("JetBrains Mono", Font.PLAIN, 10);
    private static final Font F_BODY     = new Font("Inter", Font.PLAIN, 13);
    private static final Font F_EMOJI    = new Font("Segoe UI Emoji", Font.PLAIN, 12);

    private static final String EMAIL_PLACEHOLDER = "e.g. ShadowHunter_99";

    /** Fixed width of the centered content column. */
    private static final int CONTENT_WIDTH = 404;

    private JTextField emailField;
    private JButton loginButton;
    private JLabel errorMessageLabel;
    private CyberPasswordFieldRow passwordRow;
    private PlayerManager playerManager;
    private LoginCallback loginCallback;

    public interface LoginCallback {
        void onLoginSuccess(Player player);
        void onLoginFailed(String message);
        void onSignUp();
    }

    public LoginPage(LoginCallback callback) {
        this.loginCallback = callback;
        this.playerManager = PlayerManager.getInstance();

        initializeUI();
        attachListeners();

        setVisible(true);
    }

    private void initializeUI() {
        setTitle("GameVerse — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(470, Math.min(920, Toolkit.getDefaultToolkit().getScreenSize().height - 60));
        setLocationRelativeTo(null);
        setResizable(true);
        // Open maximized: the backdrop fills the screen behind the centered column.
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        AtmospherePanel root = new AtmospherePanel();
        root.setLayout(new BorderLayout());
        setContentPane(root);

        // Cap the column width; the wrapper centers it both ways when it fits
        // and falls back to top-anchored scrolling on short screens.
        JPanel content = buildContentColumn();
        Dimension contentPref = content.getPreferredSize();
        content.setPreferredSize(new Dimension(CONTENT_WIDTH, contentPref.height));

        GridBagConstraints centerConstraints = new GridBagConstraints();
        centerConstraints.gridx = 0;
        centerConstraints.gridy = 0;
        centerConstraints.weightx = 1.0;
        centerConstraints.weighty = 1.0;
        centerConstraints.fill = GridBagConstraints.NONE;
        centerConstraints.anchor = GridBagConstraints.CENTER;
        centerConstraints.insets = new Insets(12, 12, 12, 12);

        JPanel centered = new JPanel(new GridBagLayout()) {
            @Override public Dimension getPreferredSize() {
                // Grow to the content size so short screens scroll instead of clip.
                Dimension d = super.getPreferredSize();
                d.height = Math.max(d.height, contentPref.height + 40);
                d.width = Math.max(d.width, CONTENT_WIDTH + 40);
                return d;
            }
        };
        centered.setOpaque(false);
        centered.add(content, centerConstraints);

        JScrollPane scroller = new JScrollPane(centered,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        scroller.getVerticalScrollBar().setUnitIncrement(18);
        scroller.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0)); // hidden
        root.add(scroller, BorderLayout.CENTER);

        // Always open at the very top of the column so the crest header shows
        // and the whole block reads as vertically centered when it fits.
        // (The email caret can otherwise scroll the view mid-way on open.)
        SwingUtilities.invokeLater(() ->
            scroller.getViewport().setViewPosition(new Point(0, 0)));
    }

    /* ═══════════════ Content column ═══════════════ */

    private JPanel buildContentColumn() {
        JPanel col = UiKit.verticalBox();

        col.add(buildTelemetryChip());
        col.add(Box.createVerticalStrut(12));

        col.add(buildBrandHeader());
        col.add(Box.createVerticalStrut(16));

        col.add(buildFormCard());
        col.add(Box.createVerticalStrut(10));

        col.add(buildArenaBadge());
        col.add(Box.createVerticalStrut(16));

        col.add(buildFooter());
        return col;
    }

    /* ═══════════════ Telemetry chip ═══════════════ */

    private JComponent buildTelemetryChip() {
        JPanel chip = new GlassPanel(new FlowLayout(FlowLayout.CENTER, 8, 5), CONTAINER_LOW, 999);
        chip.setAlignmentX(Component.CENTER_ALIGNMENT);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel pulse = new JLabel("●");
        pulse.setFont(F_SMALL);
        pulse.setForeground(SECONDARY);

        chip.add(pulse);
        chip.add(mono("SERVER STATUS: OPTIMAL", SECONDARY));
        chip.add(mono("•", OUTLINE_VAR));
        chip.add(mono("142K PLAYERS LIVE", ON_SURFACE_VAR));
        return chip;
    }

    /* ═══════════════ Brand header ═══════════════ */

    private JComponent buildBrandHeader() {
        JPanel header = UiKit.verticalBox();
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Crest with glow halo
        GlowHalo halo = new GlowHalo(84);
        halo.setLayout(new GridBagLayout());
        halo.add(new CrestIcon(72));
        halo.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(halo);
        header.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("GAMEVERSE");
        title.setFont(F_HEADLINE);
        title.setForeground(PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(5));

        // Tagline: Enter the Arena • Play • Compete • Dominate
        JPanel tagline = UiKit.transparent(new FlowLayout(FlowLayout.CENTER, 6, 0));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        tagline.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        tagline.add(tagPart("Enter the Arena"));
        tagline.add(tagDot(SECONDARY));
        tagline.add(tagPart("Play"));
        tagline.add(tagDot(PRIMARY));
        tagline.add(tagPart("Compete"));
        tagline.add(tagDot(TERTIARY));
        tagline.add(tagPart("Dominate"));
        header.add(tagline);
        return header;
    }

    private JLabel tagPart(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_BODY);
        l.setForeground(ON_SURFACE_VAR);
        return l;
    }

    private JLabel tagDot(Color color) {
        JLabel l = new JLabel("•");
        l.setFont(F_BODY);
        l.setForeground(color);
        return l;
    }

    /* ═══════════════ Form card ═══════════════ */

    private JComponent buildFormCard() {
        JPanel card = new GlassPanel(new BorderLayout(0, 0), CONTAINER, 18);
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1100));

        // GridBag rows with weightx=1 + HORIZONTAL fill force every row to
        // span the full card width, so labels, fields, the remember row and
        // the tab switcher all share the same left/right edges (BoxLayout was
        // packing rows of different widths raggedly inside the card).
        JPanel inner = new JPanel(new GridBagLayout());
        inner.setOpaque(false);
        inner.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.weightx = 1.0;
        row.fill = GridBagConstraints.HORIZONTAL;
        int y = 0;

        // Tab switcher
        row.gridy = y++;
        inner.add(buildTabSwitcher(), row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(14), row);

        // Player ID / Email
        row.gridy = y++;
        inner.add(fieldLabelRow("🎮", "PLAYER ID / EMAIL", SECONDARY, "SECURE ID", SECONDARY_CONT), row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(5), row);

        emailField = new JTextField();
        styleInput(emailField, EMAIL_PLACEHOLDER);
        row.gridy = y++;
        inner.add(emailField, row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(12), row);

        // Password
        JPanel pwdLabelRow = fieldLabelRow("🔒", "ACCESS CODE / PASSWORD", PRIMARY, null, null);
        pwdLabelRow.add(linkButton("Forgot Code?", PRIMARY), BorderLayout.EAST);
        row.gridy = y++;
        inner.add(pwdLabelRow, row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(5), row);

        passwordRow = new CyberPasswordFieldRow();
        row.gridy = y++;
        inner.add(passwordRow, row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(10), row);

        // Remember + 2FA row
        JCheckBox remember = new JCheckBox("REMEMBER THIS RIG");
        remember.setFont(F_SMALL);
        remember.setForeground(ON_SURFACE_VAR);
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setIcon(toggleIcon(false));
        remember.setSelectedIcon(toggleIcon(true));

        JPanel midRow = UiKit.transparent(new BorderLayout(0, 0));
        midRow.add(remember, BorderLayout.WEST);
        midRow.add(mono("🛡 2FA READY", PRIMARY), BorderLayout.EAST);
        row.gridy = y++;
        inner.add(midRow, row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(12), row);

        // Error message
        errorMessageLabel = new JLabel(" ");
        errorMessageLabel.setFont(F_SMALL);
        errorMessageLabel.setForeground(ERROR);
        row.gridy = y++;
        inner.add(errorMessageLabel, row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(6), row);

        // Launch CTA
        loginButton = new LaunchButton("🚀  LAUNCH GAMEVERSE");
        row.gridy = y++;
        inner.add(loginButton, row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(14), row);

        // Divider
        row.gridy = y++;
        inner.add(buildDivider("OR CONNECT VIA GAMING RIG"), row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(10), row);

        // Platform grid (Steam, Discord, PSN, Xbox)
        JPanel grid = new JPanel(new GridLayout(1, 4, 10, 0));
        grid.setOpaque(false);
        grid.add(platformButton("STEAM", SECONDARY));
        grid.add(platformButton("DISCORD", PRIMARY));
        grid.add(platformButton("PSN", SECONDARY));
        grid.add(platformButton("XBOX", TERTIARY));
        row.gridy = y++;
        inner.add(grid, row);
        row.gridy = y++;
        inner.add(Box.createVerticalStrut(8), row);

        // Secondary pills
        JPanel pills = new JPanel(new GridLayout(1, 2, 10, 0));
        pills.setOpaque(false);
        pills.add(pillButton("▶  Google Play"));
        pills.add(pillButton("⬇  Apple Arcade"));
        row.gridy = y++;
        inner.add(pills, row);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildTabSwitcher() {
        JPanel switcher = new JPanel(new GridLayout(1, 2, 4, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(alpha(CONTAINER_LOWEST, 220));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        switcher.setOpaque(false);
        switcher.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        switcher.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        switcher.setAlignmentX(Component.LEFT_ALIGNMENT);

        // "Sign In" is the active tab on this page; register routes to sign-up.
        switcher.add(new TabButton("▶ SIGN IN", true));
        JButton registerTab = new TabButton("＋ REGISTER", false);
        registerTab.addActionListener(e -> {
            if (loginCallback != null) loginCallback.onSignUp();
            dispose();
        });
        switcher.add(registerTab);
        return switcher;
    }

    private JButton platformButton(String label, Color hoverColor) {
        JButton b = new PlatformButton(label, hoverColor);
        b.addActionListener(e ->
            JOptionPane.showMessageDialog(this,
                label + " sign-in is coming soon in GameVerse v1.0.",
                "Connect a Gaming Rig", JOptionPane.INFORMATION_MESSAGE));
        return b;
    }

    private JButton pillButton(String label) {
        JButton b = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(alpha(CONTAINER_LOWEST, 150));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(F_LABEL);
        b.setForeground(ON_SURFACE_VAR);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /* ═══════════════ Divider ═══════════════ */

    private JComponent buildDivider(String text) {
        JPanel wrap = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(OUTLINE_VAR);
                int y = getHeight() / 2;
                int labelSpace = 10 + g2.getFontMetrics(F_LABEL).stringWidth(text) + 10;
                int mid = getWidth() / 2;
                g2.drawLine(0, y, mid - labelSpace / 2, y);
                g2.drawLine(mid + labelSpace / 2, y, getWidth(), y);
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = mono(text, ON_SURFACE_VAR);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        wrap.add(label, BorderLayout.CENTER);
        return wrap;
    }

    /* ═══════════════ Arena badge ═══════════════ */

    private JComponent buildArenaBadge() {
        JPanel badge = new GlassPanel(new BorderLayout(12, 0), CONTAINER_LOW, 14);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        badge.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));

        RoundedBox iconBox = new RoundedBox(CONTAINER_HIGH, 10);
        iconBox.setPreferredSize(new Dimension(34, 34));
        JLabel medal = new JLabel("🎖", SwingConstants.CENTER);
        medal.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        iconBox.add(medal);

        JPanel texts = UiKit.verticalBox();
        JLabel season = new JLabel("Neo-Tokyo Season 8");
        season.setFont(F_LABEL);
        season.setForeground(ON_SURFACE);
        JLabel prize = new JLabel("Daily Prize Pool: 50,000 $GVR");
        prize.setFont(F_SMALL);
        prize.setForeground(ON_SURFACE_VAR);
        texts.add(season);
        texts.add(prize);

        badge.add(iconBox, BorderLayout.WEST);
        badge.add(texts, BorderLayout.CENTER);
        badge.add(mono("ACTIVE", SECONDARY), BorderLayout.EAST);
        return badge;
    }

    /* ═══════════════ Footer ═══════════════ */

    private JComponent buildFooter() {
        JPanel footer = UiKit.verticalBox();
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Community header
        JPanel commHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        commHeader.setOpaque(false);
        commHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        commHeader.add(dotLabel(SECONDARY));
        commHeader.add(mono("COMMUNITY & SUPPORT", OUTLINE));
        commHeader.add(dotLabel(PRIMARY));
        footer.add(commHeader);
        footer.add(Box.createVerticalStrut(8));

        // Discord + X cards
        JPanel cards = new JPanel(new GridLayout(1, 2, 10, 0));
        cards.setOpaque(false);
        cards.setAlignmentX(Component.CENTER_ALIGNMENT);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        cards.add(communityCard("GameVerse Discord", "Join 24/7 Support", DISCORD));
        cards.add(communityCard("@GameVerseHQ", "Follow Live Updates", ON_SURFACE));
        footer.add(cards);
        footer.add(Box.createVerticalStrut(10));

        // Anti-cheat line
        JPanel antiCheat = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        antiCheat.setOpaque(false);
        antiCheat.setAlignmentX(Component.CENTER_ALIGNMENT);
        antiCheat.add(mono("🛡", SECONDARY_CONT));
        antiCheat.add(mono("Protected by Sentinel Anti-Cheat • v3.4.2", OUTLINE));
        footer.add(antiCheat);
        footer.add(Box.createVerticalStrut(8));

        // Sign-up prompt (drives the real sign-up flow)
        JPanel joinRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        joinRow.setOpaque(false);
        joinRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel intro = new JLabel("New to the Frontier? ");
        intro.setFont(F_BODY);
        intro.setForeground(ON_SURFACE_VAR);
        JButton join = linkButton("Join the Guild", PRIMARY);
        join.addActionListener(e -> {
            if (loginCallback != null) loginCallback.onSignUp();
            dispose();
        });
        joinRow.add(intro);
        joinRow.add(join);
        footer.add(joinRow);
        footer.add(Box.createVerticalStrut(10));

        // Legal row
        JPanel legal = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        legal.setOpaque(false);
        legal.setAlignmentX(Component.CENTER_ALIGNMENT);
        legal.add(linkButton("Server Rules", OUTLINE_VAR));
        legal.add(mono("•", OUTLINE_VAR));
        legal.add(linkButton("Neural Privacy", OUTLINE_VAR));
        legal.add(mono("•", OUTLINE_VAR));
        legal.add(linkButton("System Diagnostics", OUTLINE_VAR));
        footer.add(legal);
        return footer;
    }

    private JLabel dotLabel(Color color) {
        JLabel l = new JLabel("●");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 7));
        l.setForeground(color);
        return l;
    }

    private JComponent communityCard(String title, String sub, Color iconColor) {
        JPanel card = new GlassPanel(new BorderLayout(10, 0), CONTAINER_LOW, 12);
        card.setPreferredSize(new Dimension(190, 58));

        RoundedBox iconBox = new RoundedBox(alpha(iconColor, 40), 10);
        iconBox.setPreferredSize(new Dimension(32, 32));
        JLabel icon = new JLabel("◆", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        icon.setForeground(iconColor);
        iconBox.add(icon);

        JPanel texts = UiKit.verticalBox();
        JLabel t = new JLabel(title);
        t.setFont(F_LABEL);
        t.setForeground(ON_SURFACE);
        JLabel s = new JLabel(sub);
        s.setFont(new Font("Inter", Font.PLAIN, 10));
        s.setForeground(ON_SURFACE_VAR);
        texts.add(t);
        texts.add(s);

        card.add(iconBox, BorderLayout.WEST);
        card.add(texts, BorderLayout.CENTER);
        return card;
    }

    /* ═══════════════ Shared helpers ═══════════════ */

    private JLabel mono(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(color);
        return l;
    }

    /** Uppercase field label row: emoji + label on the left, optional tag right. */
    private JPanel fieldLabelRow(String emoji, String text, Color iconColor,
                                 String rightText, Color rightColor) {
        JPanel row = UiKit.transparent(new BorderLayout(0, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = UiKit.transparent(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel icon = new JLabel(emoji);
        icon.setFont(F_EMOJI);
        icon.setForeground(iconColor);
        left.add(icon);
        left.add(mono(text, ON_SURFACE_VAR));
        row.add(left, BorderLayout.WEST);
        if (rightText != null) {
            row.add(mono(rightText, rightColor), BorderLayout.EAST);
        }
        return row;
    }

    private JButton linkButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(F_LABEL);
        b.setForeground(color);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(ON_SURFACE); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(color); }
        });
        return b;
    }

    private Icon toggleIcon(boolean selected) {
        int s = 15;
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selected ? SECONDARY : CONTAINER_HIGHEST);
                g2.fillOval(x, y, s, s);
                if (selected) {
                    g2.setColor(SURFACE);
                    g2.fillOval(x + 5, y + 5, 5, 5);
                }
                g2.dispose();
            }
            @Override public int getIconWidth() { return s; }
            @Override public int getIconHeight() { return s; }
        };
    }

    private static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    /** Terminal-style input shared by the email field. */
    private void styleInput(JTextField field, String placeholder) {
        field.setFont(F_BODY);
        field.setForeground(OUTLINE);
        field.setOpaque(true);
        field.setBackground(CONTAINER_LOWEST);
        field.setCaretColor(SECONDARY);
        field.setSelectionColor(alpha(SECONDARY, 90));
        // Single padding inside the line border — doubled insets clip the text.
        field.setBorder(BorderFactory.createCompoundBorder(
            new InputBorder(OUTLINE_VAR, SECONDARY),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        field.setPreferredSize(new Dimension(360, 46));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(ON_SURFACE);
                } else {
                    field.selectAll();
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(OUTLINE);
                }
            }
        });
        field.setText(placeholder);
    }

    /** Rounded 1px border that switches to the focus color while focused. */
    private static class InputBorder implements javax.swing.border.Border {
        private final Color base;
        private final Color focus;

        InputBorder(Color base, Color focus) {
            this.base = base;
            this.focus = focus;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean focused = c instanceof JComponent && ((JComponent) c).hasFocus();
            g2.setColor(focused ? focus : base);
            g2.drawRoundRect(x, y, w - 1, h - 1, 12, 12);
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    /* ═══════════════ Custom widgets ═══════════════ */

    /** Backdrop: blurred remote art (graceful fallback), neon glows, dot grid. */
    private static class AtmospherePanel extends JPanel {
        private Image backdrop;
        private boolean loadFailed;

        AtmospherePanel() {
            setOpaque(true);
            setBackground(SURFACE);
            loadImage();
        }

        private void loadImage() {
            try {
                java.net.URI uri = java.net.URI.create(
                    "https://lh3.googleusercontent.com/aida-public/AB6AXuBXhzmu3VziaPTsOIXybxBp9HkTmHgTIlWwR0Ser4KOQ9BgwUGbvxkG3wp1_TYGant07dmZJJF1ylEZWJKle1C0509sEkuShSqbDDoLCh6julC8oQ6JsQclBfo46bXO4I8QbLLmjlE57SlEKK7aDarpEtHLDotN3aE3TJhX0udzJb-GllIqktrFv3j91A_o_6OFyts8laywCEct-VYToRjQ79ntER1KYuzpkavDe8NkAD9hjQ-hSyJs");
                backdrop = Toolkit.getDefaultToolkit().getImage(uri.toURL());
                MediaTracker tracker = new MediaTracker(this);
                tracker.addImage(backdrop, 0);
                tracker.waitForAll();
                if (tracker.isErrorAny()) {
                    backdrop = null;
                    loadFailed = true;
                }
            } catch (Exception e) {
                backdrop = null;
                loadFailed = true;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // 1. Backdrop image covers the full window (aspect-preserving "cover"),
            //    dimmed by a surface scrim (offline-safe gradient fallback).
            if (backdrop != null) {
                int iw = backdrop.getWidth(this), ih = backdrop.getHeight(this);
                if (iw > 0 && ih > 0) {
                    double scale = Math.max(w / (double) iw, h / (double) ih);
                    int dw = (int) Math.ceil(iw * scale), dh = (int) Math.ceil(ih * scale);
                    g2.drawImage(backdrop, (w - dw) / 2, (h - dh) / 2, dw, dh, this);
                }
                g2.setComposite(AlphaComposite.SrcOver.derive(0.66f));
                g2.setColor(SURFACE);
                g2.fillRect(0, 0, w, h);
                g2.setComposite(AlphaComposite.SrcOver);
            } else if (loadFailed) {
                g2.setPaint(new GradientPaint(0, 0, CONTAINER_LOWEST, 0, h, new Color(0x1A1D2E)));
                g2.fillRect(0, 0, w, h);
            }

            // 2. Neon glows anchored to the window edges (violet left, cyan right)
            int glowRadius = Math.max(340, w / 4);
            drawGlow(g2, -80, (int) (h * 0.12), glowRadius, PRIMARY);
            drawGlow(g2, w + 40, (int) (h * 0.30), glowRadius, SECONDARY);

            // 3. Dot grid across the whole window
            g2.setComposite(AlphaComposite.SrcOver.derive(0.10f));
            g2.setColor(PRIMARY);
            for (int x = 12; x < w; x += 24) {
                for (int y = 12; y < h; y += 24) {
                    g2.fillOval(x, y, 2, 2);
                }
            }
            g2.dispose();
        }

        private void drawGlow(Graphics2D g2, int cx, int cy, int radius, Color color) {
            g2.setPaint(new RadialGradientPaint(new Point(cx, cy), radius,
                new float[]{0f, 1f}, new Color[]{alpha(color, 52), new Color(0, 0, 0, 0)}));
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
    }

    /** Translucent rounded card with a top highlight line. */
    private static class GlassPanel extends JPanel {
        private final Color fill;
        private final int arc;

        GlassPanel(LayoutManager layout, Color fill, int arc) {
            super(layout);
            setOpaque(false);
            this.fill = fill;
            this.arc = arc;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(alpha(fill, 205));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(new Color(255, 255, 255, 14));
            g2.drawLine(10, 1, getWidth() - 10, 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small rounded square used for badge icons. */
    private static class RoundedBox extends JPanel {
        private final Color fill;
        private final int arc;

        RoundedBox(Color fill, int arc) {
            super(new GridBagLayout());
            setOpaque(false);
            this.fill = fill;
            this.arc = arc;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Soft radial glow disc behind the crest. */
    private static class GlowHalo extends JPanel {
        GlowHalo(int size) {
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setPaint(new RadialGradientPaint(new Point(w / 2, h / 2), w / 2f,
                new float[]{0f, 1f},
                new Color[]{alpha(PRIMARY_CONTAINER, 90), new Color(0, 0, 0, 0)}));
            g2.fillOval(0, 0, w, h);
            g2.dispose();
        }
    }

    /** Tab in the sign-in / register switcher. */
    private static class TabButton extends JButton {
        private final boolean active;
        private boolean hovered;

        TabButton(String text, boolean active) {
            super(text);
            this.active = active;
            setFont(F_TAB);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active) {
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_CONTAINER, getWidth(), getHeight(), PRIMARY_DEEP));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            } else if (hovered) {
                g2.setColor(new Color(255, 255, 255, 16));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            setForeground(active ? ON_PRIMARY : (hovered ? ON_SURFACE : ON_SURFACE_VAR));
            super.paintComponent(g);
        }
    }

    /** Neon gradient launch CTA with hover sheen. */
    private static class LaunchButton extends JButton {
        private boolean hovered;

        LaunchButton(String text) {
            super(text);
            setFont(F_TAB);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setPaint(hovered
                ? new GradientPaint(0, 0, SECONDARY, w, h, SECONDARY_CONT)
                : new GradientPaint(0, 0, SECONDARY_CONT, w, h, PRIMARY));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            if (hovered) {
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            }
            g2.dispose();
            setForeground(ON_PRIMARY);
            super.paintComponent(g);
        }
    }

    /** Square platform tile with hover glow color. */
    private static class PlatformButton extends JButton {
        private final Color hoverColor;
        private boolean hovered;

        PlatformButton(String text, Color hoverColor) {
            super(text);
            this.hoverColor = hoverColor;
            setFont(F_SMALL);
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? alpha(CONTAINER_HIGH, 235) : alpha(CONTAINER_LOWEST, 200));
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            setForeground(hovered ? hoverColor : ON_SURFACE_VAR);
            super.paintComponent(g);
        }
    }

    /** Terminal-style password row with neon focus ring and SHOW/HIDE toggle. */
    private static class CyberPasswordFieldRow extends JPanel {
        private final JPasswordField field = new JPasswordField();
        private final JButton toggle = new JButton("SHOW");
        private boolean visible;

        CyberPasswordFieldRow() {
            super(new BorderLayout(6, 0));
            setOpaque(false);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            field.setFont(F_BODY);
            field.setForeground(ON_SURFACE);
            field.setOpaque(true);
            field.setBackground(CONTAINER_LOWEST);
            field.setCaretColor(SECONDARY);
            field.setSelectionColor(alpha(SECONDARY, 90));
            field.setEchoChar('•');
            field.setBorder(BorderFactory.createCompoundBorder(
                new InputBorder(OUTLINE_VAR, PRIMARY),
                BorderFactory.createEmptyBorder(10, 14, 10, 10)));
            field.setPreferredSize(new Dimension(300, 46));
            field.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { field.repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e)  { field.repaint(); }
            });

            toggle.setFont(F_SMALL);
            toggle.setForeground(OUTLINE);
            toggle.setContentAreaFilled(false);
            toggle.setBorderPainted(false);
            toggle.setFocusPainted(false);
            toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            toggle.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 4));
            toggle.addActionListener(e -> {
                visible = !visible;
                field.setEchoChar(visible ? (char) 0 : '•');
                toggle.setText(visible ? "HIDE" : "SHOW");
                toggle.setForeground(visible ? PRIMARY : OUTLINE);
                field.requestFocusInWindow();
            });

            add(field, BorderLayout.CENTER);
            add(toggle, BorderLayout.EAST);
        }

        JPasswordField getField() { return field; }

        String getPassword() { return new String(field.getPassword()); }

        void clear() { field.setText(""); }
    }

    /** Glowing SVG crest rendered from the Stitch artwork geometry. */
    private static class CrestIcon extends JComponent {
        private final int size;

        CrestIcon(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int s = Math.min(getWidth(), getHeight());
            double k = s / 200.0; // original viewBox is 200x200
            double ox = (getWidth() - s) / 2.0, oy = (getHeight() - s) / 2.0;

            // Shield crest
            Path2D crest = new Path2D.Double();
            crest.moveTo(ox + 100 * k, oy + 15 * k);
            crest.lineTo(ox + 175 * k, oy + 48 * k);
            crest.lineTo(ox + 175 * k, oy + 108 * k);
            crest.curveTo(ox + 175 * k, oy + 148 * k, ox + 100 * k, oy + 188 * k, ox + 100 * k, oy + 188 * k);
            crest.curveTo(ox + 100 * k, oy + 188 * k, ox + 25 * k, oy + 148 * k, ox + 25 * k, oy + 108 * k);
            crest.lineTo(ox + 25 * k, oy + 48 * k);
            crest.closePath();
            g2.setColor(SURFACE);
            g2.fill(crest);
            g2.setPaint(new GradientPaint((int) (ox + 25 * k), (int) (oy + 15 * k), SECONDARY,
                (int) (ox + 100 * k), (int) (oy + 188 * k), TERTIARY));
            g2.setStroke(new BasicStroke((float) (6 * k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(crest);

            // V chevron
            Path2D v = new Path2D.Double();
            v.moveTo(ox + 68 * k, oy + 68 * k);
            v.lineTo(ox + 100 * k, oy + 134 * k);
            v.lineTo(ox + 132 * k, oy + 68 * k);
            g2.setPaint(new GradientPaint((int) (ox + 68 * k), (int) (oy + 68 * k), SECONDARY,
                (int) (ox + 132 * k), (int) (oy + 134 * k), PRIMARY));
            g2.setStroke(new BasicStroke((float) (12 * k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(v);

            // Crossbar
            g2.setColor(ON_SURFACE);
            g2.setStroke(new BasicStroke((float) (6 * k), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(ox + 80 * k, oy + 92 * k, ox + 120 * k, oy + 92 * k));

            // Circuit nodes
            g2.setColor(SECONDARY);
            g2.fillOval((int) (ox + 54 * k), (int) (oy + 60 * k), (int) (16 * k), (int) (16 * k));
            g2.setColor(TERTIARY);
            g2.fillOval((int) (ox + 130 * k), (int) (oy + 60 * k), (int) (16 * k), (int) (16 * k));
            g2.setColor(PRIMARY_CONTAINER);
            g2.fillOval((int) (ox + 94 * k), (int) (oy + 152 * k), (int) (12 * k), (int) (12 * k));
            g2.dispose();
        }
    }

    /* ═══════════════ Behavior (unchanged) ═══════════════ */

    private void attachListeners() {
        loginButton.addActionListener(e -> handleLogin());
        emailField.addActionListener(e -> handleLogin());
        passwordRow.getField().addActionListener(e -> handleLogin());
    }

    private void handleLogin() {
        String email = emailField.getText().trim();
        if (email.isEmpty() || email.equalsIgnoreCase(EMAIL_PLACEHOLDER)) {
            email = "";
        }
        String password = passwordRow.getPassword();

        errorMessageLabel.setText(" ");

        String validationError = LoginValidator.validateLoginCredentials(email, password);
        if (!validationError.isEmpty()) {
            errorMessageLabel.setText(validationError);
            return;
        }

        Player player = playerManager.getPlayer(email);
        if (player == null) {
            player = playerManager.createPlayer(email);
            if (player == null) {
                errorMessageLabel.setText("EMAIL ALREADY REGISTERED");
                return;
            }
        }

        playerManager.setCurrentPlayer(email);

        if (loginCallback != null) {
            loginCallback.onLoginSuccess(player);
        }
        dispose();
    }

    public void showError(String message) {
        errorMessageLabel.setText(message);
    }

    public void clearFields() {
        emailField.setText(EMAIL_PLACEHOLDER);
        passwordRow.clear();
        errorMessageLabel.setText(" ");
    }

    public static void main(String[] args) {
        GameLauncher.main(args);
    }
}
