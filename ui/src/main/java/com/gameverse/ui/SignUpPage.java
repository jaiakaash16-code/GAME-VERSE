package com.gameverse.ui;

import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;

import javax.swing.*;
import java.awt.*;

/**
 * Sign Up page GUI for GameVerse platform.
 * Allows new users to create accounts with email and password.
 * Styled with the shared UiKit: dark brand sidebar + rounded form card.
 */
public class SignUpPage extends JFrame {

    private JTextField emailField;
    private JButton signUpButton;
    private JButton backButton;
    private JButton checkPasswordButton;
    private JLabel errorMessageLabel;
    private PasswordFieldRow passwordRow;
    private PasswordFieldRow confirmRow;
    private PlayerManager playerManager;
    private SignUpCallback signUpCallback;

    private static final String EMAIL_PLACEHOLDER = "Choose an email";

    public interface SignUpCallback {
        void onSignUpSuccess(Player player);
        void onSignUpFailed(String message);
        void onBackToLogin();
    }

    public SignUpPage(SignUpCallback callback) {
        this.signUpCallback = callback;
        this.playerManager = PlayerManager.getInstance();

        initializeUI();
        attachListeners();

        setVisible(true);
    }

    private void initializeUI() {
        setTitle("GameVerse — Create Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 640);
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UiKit.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        setContentPane(root);

        // ── Card container: brand sidebar (left) + form (right) ──
        JPanel card = UiKit.card(20);

        card.add(createBrandPanel(), BorderLayout.WEST);
        card.add(createFormPanel(), BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);
    }

    /* ═══════════════ Brand sidebar ═══════════════ */

    private JPanel createBrandPanel() {
        JPanel side = new UiKit.RoundedPanel(new BorderLayout(0, 0),
            new Color(24, 44, 38), new Color(50, 96, 78), 16, true);
        side.setPreferredSize(new Dimension(300, 0));
        side.setBorder(BorderFactory.createEmptyBorder(28, 26, 24, 26));

        JPanel top = UiKit.verticalBox();

        JLabel logo = new JLabel("🎮", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 34));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(logo);
        top.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("GameVerse");
        title.setFont(UiKit.TITLE);
        title.setForeground(new Color(230, 248, 238));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(title);
        top.add(Box.createVerticalStrut(6));

        JLabel tagline = new JLabel("Join the arcade.");
        tagline.setFont(UiKit.BODY);
        tagline.setForeground(new Color(160, 195, 175));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        top.add(tagline);

        side.add(top, BorderLayout.NORTH);

        // Perks list in the middle
        JPanel perks = UiKit.verticalBox();
        perks.add(Box.createVerticalGlue());
        perks.add(perkRow("⚡", "Instant account setup"));
        perks.add(Box.createVerticalStrut(10));
        perks.add(perkRow("🎯", "Daily missions and rewards"));
        perks.add(Box.createVerticalStrut(10));
        perks.add(perkRow("🥇", "High-score history saved"));
        perks.add(Box.createVerticalStrut(10));
        perks.add(perkRow("🤝", "Free forever — no card needed"));
        perks.add(Box.createVerticalGlue());
        side.add(perks, BorderLayout.CENTER);

        JLabel footer = new JLabel("No password ever leaves your PC", SwingConstants.CENTER);
        footer.setFont(UiKit.SMALL);
        footer.setForeground(new Color(115, 155, 135));
        side.add(footer, BorderLayout.SOUTH);

        return side;
    }

    private JPanel perkRow(String icon, String text) {
        JPanel row = UiKit.leftFlow(10, 0);
        JLabel i = new JLabel(icon);
        i.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        JLabel t = new JLabel(text);
        t.setFont(UiKit.BODY);
        t.setForeground(new Color(180, 210, 195));
        row.add(i);
        row.add(t);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        return row;
    }

    /* ═══════════════ Form panel ═══════════════ */

    private JPanel createFormPanel() {
        JPanel form = UiKit.verticalBox();
        form.setBorder(BorderFactory.createEmptyBorder(32, 40, 26, 40));

        JLabel heading = new JLabel("Create your account");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 22));
        heading.setForeground(UiKit.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Free, instant, and saved locally on your machine.");
        sub.setFont(UiKit.BODY);
        sub.setForeground(UiKit.TEXT_DIM);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(heading);
        form.add(Box.createVerticalStrut(3));
        form.add(sub);
        form.add(Box.createVerticalStrut(22));

        // Email
        form.add(UiKit.fieldLabel("Email address"));
        form.add(Box.createVerticalStrut(6));
        emailField = UiKit.placeholderField(EMAIL_PLACEHOLDER);
        form.add(emailField);
        form.add(Box.createVerticalStrut(14));

        // Password
        form.add(UiKit.fieldLabel("Password"));
        form.add(Box.createVerticalStrut(6));
        passwordRow = new PasswordFieldRow();
        form.add(passwordRow);
        form.add(Box.createVerticalStrut(4));

        // The requirements list is intentionally not shown here; the Check
        // Password link pops up only the rules the typed password is missing.
        checkPasswordButton = createCheckPasswordButton();
        checkPasswordButton.addActionListener(e ->
            PasswordFieldRow.showPasswordCheck(this, passwordRow.getPassword()));
        form.add(createRightRow(checkPasswordButton));
        form.add(Box.createVerticalStrut(10));

        // Confirm Password
        form.add(UiKit.fieldLabel("Confirm password"));
        form.add(Box.createVerticalStrut(6));
        confirmRow = new PasswordFieldRow();
        form.add(confirmRow);
        form.add(Box.createVerticalStrut(16));

        // Error message
        errorMessageLabel = new JLabel(" ");
        errorMessageLabel.setFont(UiKit.SMALL);
        errorMessageLabel.setForeground(UiKit.ERROR);
        errorMessageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(errorMessageLabel);
        form.add(Box.createVerticalStrut(6));

        // Sign Up button
        signUpButton = greenButton("Create Account  ✓");
        signUpButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(signUpButton);
        form.add(Box.createVerticalStrut(12));

        // Back to login switch
        backButton = UiKit.ghostButton("Already have an account? Log in");
        backButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(backButton);

        form.add(Box.createVerticalGlue());
        return form;
    }

    /** Filled green CTA — same shape as the primary button, green gradient. */
    private JButton greenButton(String text) {
        UiKit.FilledButton b = new UiKit.FilledButton(text,
            new Color(60, 175, 110), new Color(40, 140, 90), new Color(90, 200, 140), true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton createCheckPasswordButton() {
        JButton check = new JButton("Password rules?");
        check.setFont(UiKit.SMALL);
        check.setForeground(UiKit.ACCENT);
        check.setContentAreaFilled(false);
        check.setBorderPainted(false);
        check.setFocusPainted(false);
        check.setCursor(new Cursor(Cursor.HAND_CURSOR));
        check.setToolTipText("Show missing password requirements");
        check.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { check.setForeground(UiKit.ACCENT_HOVER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { check.setForeground(UiKit.ACCENT); }
        });
        return check;
    }

    private JPanel createRightRow(JComponent control) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(control);
        return row;
    }

    private void attachListeners() {
        // Sign Up Button
        signUpButton.addActionListener(e -> handleSignUp());

        // Pressing Enter walks through the fields, then submits
        emailField.addActionListener(e -> passwordRow.getField().requestFocusInWindow());
        passwordRow.getField().addActionListener(e -> confirmRow.getField().requestFocusInWindow());
        confirmRow.getField().addActionListener(e -> handleSignUp());

        // Back Button
        backButton.addActionListener(e -> {
            if (signUpCallback != null) {
                signUpCallback.onBackToLogin();
            }
            dispose();
        });
    }

    private void handleSignUp() {
        String email = emailField.getText().trim();
        if (email.isEmpty() || email.equalsIgnoreCase(EMAIL_PLACEHOLDER)) {
            email = "";
        }
        String password = passwordRow.getPassword();
        String confirmPassword = confirmRow.getPassword();

        // Clear previous error
        errorMessageLabel.setText(" ");

        // Validate email
        String validationError = LoginValidator.validateLoginCredentials(email, password);

        if (!validationError.isEmpty()) {
            errorMessageLabel.setText(validationError);
            if (LoginValidator.isValidEmail(email)) {
                passwordRow.getField().requestFocusInWindow();
            } else {
                emailField.requestFocusInWindow();
            }
            return;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            errorMessageLabel.setText("Passwords do not match");
            confirmRow.getField().requestFocusInWindow();
            return;
        }

        // Check if email already exists
        if (playerManager.playerExists(email)) {
            errorMessageLabel.setText("Email already registered. Please login instead.");
            return;
        }

        // Create new player
        Player player = playerManager.createPlayer(email);

        if (player == null) {
            errorMessageLabel.setText("Failed to create account. Please try again.");
            return;
        }

        // Set as current player
        playerManager.setCurrentPlayer(email);

        // Notify callback
        if (signUpCallback != null) {
            signUpCallback.onSignUpSuccess(player);
        }

        dispose();
    }

    /**
     * Show error message on sign up page
     * @param message the error message to display
     */
    public void showError(String message) {
        errorMessageLabel.setText(message);
    }

    /**
     * Clear all fields
     */
    public void clearFields() {
        emailField.setText(EMAIL_PLACEHOLDER);
        emailField.setForeground(UiKit.TEXT_DIM);
        passwordRow.clear();
        confirmRow.clear();
        errorMessageLabel.setText(" ");
    }

    /**
     * Running this class directly from an IDE launches the full GameVerse flow:
     * the sign-up page is shown by GameLauncher, and a successful sign-up
     * continues into the main menu / game hub instead of stopping at a
     * confirmation dialog.
     */
    public static void main(String[] args) {
        GameLauncher.main(args);
    }
}
