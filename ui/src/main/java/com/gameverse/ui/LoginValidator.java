package com.gameverse.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for login credentials.
 * Validates email format and password requirements.
 */
public class LoginValidator {
    
    private static final String EMAIL_REGEX = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    
    /**
     * Validate email format
     * @param email the email address to validate
     * @return true if email is valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Validate password requirements
     * Password must have at least one capital letter and one symbol
     * @param password the password to validate
     * @return true if password meets requirements, false otherwise
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        
        boolean hasCapitalLetter = false;
        boolean hasSymbol = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasCapitalLetter = true;
            }
            
            // Check for symbol (special character)
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                hasSymbol = true;
            }
        }
        
        return hasCapitalLetter && hasSymbol;
    }
    
    /**
     * Get password validation error message
     * @param password the password to check
     * @return error message describing what's wrong with the password
     */
    public static String getPasswordErrorMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        
        if (password.length() < 6) {
            return "Password must be at least 6 characters long";
        }
        
        boolean hasCapitalLetter = password.matches(".*[A-Z].*");
        boolean hasSymbol = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
        
        if (!hasCapitalLetter && !hasSymbol) {
            return "Password must contain at least one capital letter and one symbol";
        } else if (!hasCapitalLetter) {
            return "Password must contain at least one capital letter";
        } else if (!hasSymbol) {
            return "Password must contain at least one symbol (!@#$%^&*...)";
        }
        
        return "";
    }
    
    /**
     * Validate complete login credentials
     * @param email the email address
     * @param password the password
     * @return empty string if valid, or error message if invalid
     */
    public static String validateLoginCredentials(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return "Email cannot be empty";
        }
        
        if (!isValidEmail(email)) {
            return "Invalid email format. Please enter a valid email address.";
        }
        
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }
        
        if (!isValidPassword(password)) {
            return getPasswordErrorMessage(password);
        }
        
        return ""; // Valid
    }
    
    /**
     * Return the list of password requirements this password still fails, with
     * one tailored entry per missing rule. An empty list means every rule
     * passes.
     * @param password the password to check
     * @return missing-requirement messages, never null
     */
    public static List<String> getMissingPasswordRequirements(String password) {
        List<String> missing = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            return missing;
        }

        if (password.length() < 6) {
            missing.add("At least 6 characters (currently " + password.length() + ")");
        }

        if (!password.matches(".*[A-Z].*")) {
            missing.add("At least 1 capital letter (A-Z)");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            missing.add("At least 1 symbol (!@#$%^&*...)");
        }

        return missing;
    }
}
