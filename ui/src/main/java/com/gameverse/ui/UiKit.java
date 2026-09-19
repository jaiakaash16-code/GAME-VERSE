package com.gameverse.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared design system for the GameVerse auth screens.
 * One palette + small factory helpers so LoginPage, SignUpPage and
 * PasswordFieldRow all render with the same dark, rounded look used
 * by the game hub.
 */
public final class UiKit {

    // ── Palette ──
    public static final Color BG          = new Color(15, 16, 26);   // app background
    public static final Color PANEL_BG    = new Color(23, 24, 38);   // card background
    public static final Color CARD_LINE   = new Color(52, 55, 80);   // subtle border
    public static final Color FIELD_BG    = new Color(28, 30, 48);   // input background
    public static final Color FIELD_LINE  = new Color(70, 74, 104);  // input border
    public static final Color FIELD_FOCUS = new Color(100, 150, 255);// focused border
    public static final Color ACCENT      = new Color(100, 150, 255);// primary blue
    public static final Color ACCENT_DEEP = new Color(70, 115, 235); // gradient partner
    public static final Color ACCENT_HOVER= new Color(130, 175, 255);
    public static final Color GREEN       = new Color(80, 200, 120);
    public static final Color TEXT        = new Color(225, 226, 240);
    public static final Color TEXT_DIM    = new Color(148, 150, 170);
    public static final Color ERROR       = new Color(255, 110, 110);

    public static final Font TITLE   = new Font("Segoe UI", Font.BOLD, 30);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font MONO    = new Font("Consolas", Font.BOLD, 12);

    private UiKit() {}

    /** Rounded panel with a subtle gradient fill and thin border. */
    public static JPanel card(int arc) {
        return new RoundedPanel(new BorderLayout(0, 0), PANEL_BG, CARD_LINE, arc, true);
    }

    /** Filled rounded primary button with hand-drawn hover/press states. */
    public static JButton primaryButton(String text) {
        return filledButton(text, ACCENT, ACCENT_DEEP, ACCENT_HOVER, true);
    }

    /** Filled rounded secondary button (ghost style). */
    public static JButton ghostButton(String text) {
        return new GhostButton(text);
    }

    private static JButton filledButton(String text, Color top, Color bottom, Color hover, boolean gradient) {
        FilledButton b = new FilledButton(text, top, bottom, hover, gradient);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Text field with rounded border and focus highlight. */
    public static JTextField placeholderField(String placeholder) {
        JTextField field = new JTextField(placeholder);
        field.setFont(BODY);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setSelectionColor(new Color(100, 150, 255, 90));
        field.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        field.setPreferredSize(new Dimension(300, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT);
                } else {
                    field.selectAll();
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(TEXT_DIM);
                }
            }
        });
        return field;
    }

    /** Small uppercase section label. */
    public static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(SMALL);
        label.setForeground(TEXT_DIM);
        return label;
    }

    /** Transparent panel so custom-painted parents show through. */
    public static JPanel transparent(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    /** Transparent panel with a vertical BoxLayout. */
    public static JPanel verticalBox() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    /** Transparent panel with a left-aligned FlowLayout. */
    public static JPanel leftFlow(int hgap, int vgap) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, hgap, vgap));
        p.setOpaque(false);
        return p;
    }

    /* ═══════════════ Custom components ═══════════════ */

    /** Rounded, gradient-filled button that paints its own label and hover ring. */
    static class FilledButton extends JButton {
        private final Color top;
        private final Color bottom;
        private final Color hoverTop;
        private boolean hovered;

        FilledButton(String text, Color top, Color bottom, Color hoverTop, boolean gradient) {
            super(text);
            this.top = top;
            this.bottom = bottom;
            this.hoverTop = hoverTop;
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(11, 22, 11, 22));
            setPreferredSize(new Dimension(300, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setContentAreaFilled(false);
            setFocusPainted(false);
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
            Color t = hovered ? hoverTop : top;
            if (t.equals(bottom)) {
                g2.setPaint(t);
            } else {
                g2.setPaint(new GradientPaint(0, 0, t, 0, h, bottom));
            }
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            if (hovered) {
                g2.setColor(new Color(255, 255, 255, 60));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }
            g2.dispose();
            super.paintComponent(g); // draws the label
        }
    }

    /** Borderless ghost button: text-only with a hover underline feel. */
    static class GhostButton extends JButton {
        private boolean hovered;

        GhostButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(TEXT_DIM);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; setForeground(TEXT); repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; setForeground(TEXT_DIM); repaint(); }
            });
        }
    }

    /** Same rounded panel used by the hub's MainMenu. */
    static class RoundedPanel extends JPanel {
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

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (fill != null) {
                g2.setPaint(gradient && h > 0
                    ? new GradientPaint(0, 0, fill, 0, h, new Color(19, 20, 36))
                    : fill);
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
}
