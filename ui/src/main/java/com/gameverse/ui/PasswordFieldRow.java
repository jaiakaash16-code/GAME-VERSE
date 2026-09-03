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

    private static final Color PANEL_COLOR = new Color(30, 30, 45);
    private static final Color BACKGROUND_COLOR = new Color(20, 20, 30);
    private static final Color FIELD_BORDER_COLOR = new Color(70, 70, 90);
    private static final Color TEXT_COLOR = new Color(200, 200, 220);
    private static final Color TOGGLE_HOVER_COLOR = new Color(40, 40, 60);
    private static final Color VISIBLE_TEXT_COLOR = new Color(255, 190, 90);

    private final Color accentColor;
    private final JPasswordField field = new JPasswordField();
    private final JButton toggle = new JButton("Show");
    private boolean passwordVisible;

    public PasswordFieldRow(Color accentColor, Color accentHoverColor) {
        super(new BorderLayout(6, 0));
        setBackground(BACKGROUND_COLOR);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        this.accentColor = accentColor;

        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setBackground(PANEL_COLOR);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(accentColor);
        field.setEchoChar('\u2022');
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(300, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        toggle.setFont(new Font("Arial", Font.BOLD, 11));
        toggle.setBackground(PANEL_COLOR);
        toggle.setForeground(accentColor);
        toggle.setFocusPainted(false);
        toggle.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.setToolTipText("Show password");
        toggle.setPreferredSize(new Dimension(64, 40));
        toggle.setMaximumSize(new Dimension(64, 40));
        toggle.addActionListener(e -> toggleVisibility());
        toggle.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                toggle.setBackground(TOGGLE_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                toggle.setBackground(PANEL_COLOR);
            }
        });

        add(field, BorderLayout.CENTER);
        add(toggle, BorderLayout.EAST);
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
