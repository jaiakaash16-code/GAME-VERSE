package com.gameverse.ui;

import com.gameverse.player.Player;
import com.gameverse.player.PlayerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Login page GUI for GameVerse platform.
 * Handles user authentication with email and password validation.
 */
public class LoginPage extends JFrame {
    
    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signUpButton;
    private JLabel errorMessageLabel;
    private JCheckBox rememberMeCheckBox;
    private PlayerManager playerManager;
    private LoginCallback loginCallback;
    
    // UI Constants
    private static final Color BACKGROUND_COLOR = new Color(20, 20, 30);
    private static final Color PANEL_COLOR = new Color(30, 30, 45);
    private static final Color BUTTON_COLOR = new Color(100, 150, 255);
    private static final Color BUTTON_HOVER_COLOR = new Color(120, 170, 255);
    private static final Color TEXT_COLOR = new Color(200, 200, 220);
    private static final Color ERROR_COLOR = new Color(255, 100, 100);
    
    public interface LoginCallback {
        void onLoginSuccess(Player player);
        void onLoginFailed(String message);
        void onSignUp();
    }
    
    public LoginPage(LoginCallback callback) {
        this.loginCallback = callback;
        this.playerManager = PlayerManager.getInstance();
        
        initializeUI();
        setupLayout();
        attachListeners();
        
        setVisible(true);
    }
    
    private void initializeUI() {
        setTitle("GameVerse - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
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
        
        JLabel subtitleLabel = new JLabel("Login to Your Account");
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
            "<html>Password must contain:<br>" +
            "• At least 6 characters<br>" +
            "• At least 1 capital letter (A-Z)<br>" +
            "• At least 1 symbol (!@#$%^&*...)" +
            "</html>"
        );
        requirementsLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        requirementsLabel.setForeground(new Color(150, 150, 170));
        panel.add(requirementsLabel);
        panel.add(Box.createVerticalStrut(15));
        
        // Remember Me Checkbox
        rememberMeCheckBox = new JCheckBox("Remember me");
        rememberMeCheckBox.setBackground(BACKGROUND_COLOR);
        rememberMeCheckBox.setForeground(TEXT_COLOR);
        rememberMeCheckBox.setFont(new Font("Arial", Font.PLAIN, 12));
        rememberMeCheckBox.setFocusPainted(false);
        panel.add(rememberMeCheckBox);
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
        
        // Login Button
        loginButton = createButton("Login");
        loginButton.setBackground(BUTTON_COLOR);
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(10));
        
        // Sign Up Button
        signUpButton = createButton("Create New Account");
        signUpButton.setBackground(new Color(60, 80, 100));
        panel.add(signUpButton);
        
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
    
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
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
                button.setBackground(BUTTON_HOVER_COLOR);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(button == loginButton ? BUTTON_COLOR : new Color(60, 80, 100));
            }
        });
        
        return button;
    }
    
    private void attachListeners() {
        // Login Button
        loginButton.addActionListener(e -> handleLogin());
        
        // Sign Up Button
        signUpButton.addActionListener(e -> {
            if (loginCallback != null) {
                loginCallback.onSignUp();
            }
            dispose();
        });
        
        // Enter key in password field
        passwordField.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
            
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }
    
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        // Clear previous error
        errorMessageLabel.setText("");
        
        // Validate credentials
        String validationError = LoginValidator.validateLoginCredentials(email, password);
        
        if (!validationError.isEmpty()) {
            errorMessageLabel.setText(validationError);
            return;
        }
        
        // Check if player exists (for MVP, we'll accept any valid credentials)
        Player player = playerManager.getPlayer(email);
        
        if (player == null) {
            // Create new player with valid credentials
            player = playerManager.createPlayer(email);
            if (player == null) {
                errorMessageLabel.setText("Email already registered");
                return;
            }
        }
        
        // Set as current player
        playerManager.setCurrentPlayer(email);
        
        // Notify callback
        if (loginCallback != null) {
            loginCallback.onLoginSuccess(player);
        }
        
        dispose();
    }
    
    /**
     * Show error message on login page
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
        errorMessageLabel.setText("");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginPage(new LoginCallback() {
                @Override
                public void onLoginSuccess(Player player) {
                    JOptionPane.showMessageDialog(null, 
                        "Login successful!\nWelcome, " + player.getUsername(),
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
                
                @Override
                public void onLoginFailed(String message) {
                    JOptionPane.showMessageDialog(null, 
                        "Login failed: " + message,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
                
                @Override
                public void onSignUp() {
                    JOptionPane.showMessageDialog(null, 
                        "Sign up page would open here",
                        "Sign Up",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });
        });
    }
}
