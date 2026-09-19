package com.gameverse.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * A self-contained password input paired with an inline "Show/Hide" toggle.
 *
 * The field is dot-masked by default. Clicking the toggle reveals the typed
 * characters (label switches to "Hide"); clicking again re-masks them. Focus
 * and caret stay inside the field across toggles so typing is never
 * interrupted. Also hosts the shared pop-up password check used by the login
 * and sign-up pages.
 */
public class PasswordFieldRow extends JPanel {

    private static final Color TOGGLE_HOVER_COLOR = new Color(40, 42, 62);
    private static final Color VISIBLE_TEXT_COLOR = new Color(255, 190, 90);

    private final Color accentColor;
    private final JPasswordField field = new JPasswordField();
    private final JButton toggle = new JButton("Show");
    private boolean passwordVisible;

    /** Creates the row with the shared UiKit accent color. */
    public PasswordFieldRow() {
        this(UiKit.ACCENT);
    }

    public PasswordFieldRow(Color accentColor) {
        this(accentColor, null);
    }

    /** @deprecated use {@link #PasswordFieldRow()} — the hover color is derived now. */
    @Deprecated
    public PasswordFieldRow(Color accentColor, Color accentHoverColor) {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        this.accentColor = accentColor;

        field.setFont(UiKit.BODY);
        field.setBackground(UiKit.FIELD_BG);
        field.setForeground(UiKit.TEXT);
        field.setCaretColor(UiKit.ACCENT);
        field.setSelectionColor(new Color(100, 150, 255, 90));
        field.setEchoChar('\u2022');
        field.setBorder(BorderFactory.createCompoundBorder(
            new FieldBorder(UiKit.FIELD_LINE),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        field.setPreferredSize(new Dimension(300, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                field.repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                field.repaint();
            }
        });

        toggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        toggle.setBackground(UiKit.FIELD_BG);
        toggle.setForeground(accentColor);
        toggle.setFocusPainted(false);
        toggle.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setToolTipText("Show password");
        toggle.setPreferredSize(new Dimension(64, 40));
        toggle.setMaximumSize(new Dimension(64, 40));
        toggle.setContentAreaFilled(false);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> toggleVisibility());
        toggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                toggle.setBackground(TOGGLE_HOVER_COLOR);
                toggle.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                toggle.setBackground(UiKit.FIELD_BG);
            }
        });

        add(field, BorderLayout.CENTER);
        add(toggle, BorderLayout.EAST);
    }

    /** Rounded border for the password field; glows blue while focused. */
    private static class FieldBorder implements javax.swing.border.Border {
        private final Color base;

        FieldBorder(Color base) {
            this.base = base;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean focused = c instanceof JComponent && ((JComponent) c).hasFocus();
            g2.setColor(focused ? UiKit.FIELD_FOCUS : base);
            g2.drawRoundRect(x, y, width - 1, height - 1, 12, 12);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(10, 12, 10, 12);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UiKit.FIELD_BG);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Swap the echo character between masked dots and plain text without
     * moving focus out of the field.
     */
    private void toggleVisibility() {
        passwordVisible = !passwordVisible;
        field.setEchoChar(passwordVisible ? (char) 0 : '\u2022');
        toggle.setText(passwordVisible ? "Hide" : "Show");
        toggle.setForeground(passwordVisible ? VISIBLE_TEXT_COLOR : accentColor);
        toggle.setToolTipText(passwordVisible ? "Hide password" : "Show password");
        toggle.repaint();
        field.requestFocusInWindow();
    }

    public JPasswordField getField() {
        return field;
    }

    public String getPassword() {
        return new String(field.getPassword());
    }

    public void clear() {
        field.setText("");
    }

    /**
     * Pop up a tailored result for the given password: a success message when
     * every requirement passes, otherwise a warning dialog listing only the
     * rules that are still missing, each phrased for that specific rule.
     *
     * @param parent   the owner window for the dialog
     * @param password the password value to check
     */
    public static void showPasswordCheck(Component parent, String password) {
        UIManager.put("OptionPane.background", UiKit.PANEL_BG);
        UIManager.put("Panel.background", UiKit.PANEL_BG);
        UIManager.put("OptionPane.messageForeground", UiKit.TEXT);
        if (password == null || password.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                "Type a password first, then run the check.",
                "Password Check", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<String> missing = LoginValidator.getMissingPasswordRequirements(password);
        if (missing.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                "Excellent \u2014 your password meets every requirement.",
                "Password Check", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder message = new StringBuilder("Your password still needs:\n\n");
        for (String requirement : missing) {
            message.append("\u2022  ").append(requirement).append('\n');
        }
        message.append("\nFix the items above, then check again.");

        JOptionPane.showMessageDialog(parent, message.toString(),
            "Password Check", JOptionPane.WARNING_MESSAGE);
    }
}
