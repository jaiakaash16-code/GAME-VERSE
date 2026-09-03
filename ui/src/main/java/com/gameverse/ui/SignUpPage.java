package com.gameverse.ui;

import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;

import javax.swing.*;
import java.awt.*;

/**
 * Sign Up page GUI for GameVerse platform.
 * Allows new users to create accounts with email and password.
 */
public class SignUpPage extends JFrame {
    
    private JTextField emailField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton signUpButton;
    private JButton backButton;
    private JLabel errorMessageLabel;
    private PlayerManager playerManager;
    private SignUpCallback signUpCallback;
    
    // UI Constants
    private static final Color BACKGROUND_COLOR = new Color(20, 20, 30);
    private static final Color PANEL_COLOR = new Color(30, 30, 45);
    private static final Color BUTTON_COLOR = new Color(100, 200, 100);
    private static final Color BUTTON_HOVER_COLOR = new Color(120, 220, 120);
    private static final Color TEXT_COLOR = new Color(200, 200, 220);
    private static final Color ERROR_COLOR = new Color(255, 100, 100);
    
    public interface SignUpCallback {
        void onSignUpSuccess(Player player);
        void onSignUpFailed(String message);
        void onBackToLogin();
    }
    
    public SignUpPage(SignUpCallback callback) {
        this.signUpCallback = callback;
        this.playerManager = PlayerManager.getInstance();
        
        initializeUI();
        setupLayout();
        attachListeners();
        
        setVisible(true);
    }
    
    private void initializeUI() {
        setTitle("GameVerse - Create Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 650);
        setLocationRelativeTo(null);
        setResizable(false);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(BACKGROUND_COLOR);
        setContentPane(mainPanel);
    }
    
    private void setupLayout() {
        JPanel mainPanel = (JPanel) getContentPane();
        mainPanel.setLayout(new BorderLayout(0, 20));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        
        // Title Panel
        JPanel titlePanel = createTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Content Panel
        JPanel contentPanel = createContentPanel();
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Button Panel
        JPanel buttonPanel = createButtonPanel();
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("GameVerse");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(BUTTON_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel subtitleLabel = new JLabel("Create Your Account");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_COLOR);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(subtitleLabel);
        panel.add(Box.createVerticalStrut(20));
        
        return panel;
    }
    
    private JPanel createContentPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Email Label and Field
        JLabel emailLabel = createLabel("Email Address");
        panel.add(emailLabel);
        
        emailField = createTextField("Enter your email");
        panel.add(emailField);
        panel.add(Box.createVerticalStrut(15));
        
        // Password Label and Field
        JLabel passwordLabel = createLabel("Password");
        panel.add(passwordLabel);
        
        passwordField = createPasswordField("Enter your password");
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(10));
        
        // Password Requirements
        JLabel requirementsLabel = new JLabel(
            "<html>Password requirements:<br>" +
            "• At least 6 characters<br>" +
            "• At least 1 capital letter (A-Z)<br>" +
            "• At least 1 symbol (!@#$%^&*...)" +
            "</html>"
        );
        requirementsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        requirementsLabel.setForeground(new Color(150, 150, 170));
        panel.add(requirementsLabel);
        panel.add(Box.createVerticalStrut(15));
        
        // Confirm Password Label and Field
        JLabel confirmLabel = createLabel("Confirm Password");
        panel.add(confirmLabel);
        
        confirmPasswordField = createPasswordField("Re-enter your password");
        panel.add(confirmPasswordField);
        panel.add(Box.createVerticalStrut(20));
        
        // Error Message Label
        errorMessageLabel = new JLabel("");
        errorMessageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        errorMessageLabel.setForeground(ERROR_COLOR);
        panel.add(errorMessageLabel);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BACKGROUND_COLOR);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        // Sign Up Button
        signUpButton = createButton("Create Account", BUTTON_COLOR);
        panel.add(signUpButton);
        panel.add(Box.createVerticalStrut(10));
        
        // Back Button
        backButton = createButton("Back to Login", new Color(60, 80, 100));
        panel.add(backButton);
        
        return panel;
    }
    
    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 13));
        label.setForeground(TEXT_COLOR);
        return label;
    }
    
    private JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setBackground(PANEL_COLOR);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(BUTTON_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 90), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(300, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        // Placeholder text
        field.setText(placeholder);
        field.setForeground(new Color(120, 120, 140));
        
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_COLOR);
                }
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(120, 120, 140));
                }
            }
        });
        
        return field;
    }
    
    private JPasswordField createPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Arial", Font.PLAIN, 13));
        field.setBackground(PANEL_COLOR);
        field.setForeground(TEXT_COLOR);
        field.setCaretColor(BUTTON_COLOR);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 90), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setPreferredSize(new Dimension(300, 40));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        return field;
    }
    
    private JButton createButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(backgroundColor);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(300, 45));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (button == signUpButton) {
                    button.setBackground(BUTTON_HOVER_COLOR);
                }
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(backgroundColor);
            }
        });
        
        return button;
    }
    
    private void attachListeners() {
        // Sign Up Button
        signUpButton.addActionListener(e -> handleSignUp());
        
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
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Clear previous error
        errorMessageLabel.setText("");
        
        // Validate email
        String validationError = LoginValidator.validateLoginCredentials(email, password);
        
        if (!validationError.isEmpty()) {
            errorMessageLabel.setText(validationError);
            return;
        }
        
        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            errorMessageLabel.setText("Passwords do not match");
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
        emailField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        errorMessageLabel.setText("");
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
