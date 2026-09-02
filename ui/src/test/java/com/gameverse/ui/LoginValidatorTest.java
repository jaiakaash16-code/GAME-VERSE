package com.gameverse.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LoginValidator
 */
@DisplayName("Login Validator Tests")
public class LoginValidatorTest {
    
    // Email Validation Tests
    
    @Test
    @DisplayName("Valid email addresses should pass validation")
    public void testValidEmails() {
        assertTrue(LoginValidator.isValidEmail("user@example.com"));
        assertTrue(LoginValidator.isValidEmail("john.doe@company.co.uk"));
        assertTrue(LoginValidator.isValidEmail("test+tag@domain.com"));
        assertTrue(LoginValidator.isValidEmail("user123@test.org"));
    }
    
    @Test
    @DisplayName("Invalid email addresses should fail validation")
    public void testInvalidEmails() {
        assertFalse(LoginValidator.isValidEmail(""));
        assertFalse(LoginValidator.isValidEmail("   "));
        assertFalse(LoginValidator.isValidEmail(null));
        assertFalse(LoginValidator.isValidEmail("invalid.email"));
        assertFalse(LoginValidator.isValidEmail("@nodomain.com"));
        assertFalse(LoginValidator.isValidEmail("user@"));
        assertFalse(LoginValidator.isValidEmail("user name@example.com"));
    }
    
    // Password Validation Tests
    
    @Test
    @DisplayName("Valid passwords with capital letter and symbol should pass")
    public void testValidPasswords() {
        assertTrue(LoginValidator.isValidPassword("Password@123"));
        assertTrue(LoginValidator.isValidPassword("MyPass!456"));
        assertTrue(LoginValidator.isValidPassword("Secure#Pass99"));
        assertTrue(LoginValidator.isValidPassword("Test$123Pwd"));
    }
    
    @Test
    @DisplayName("Password without capital letter should fail")
    public void testPasswordWithoutCapitalLetter() {
        assertFalse(LoginValidator.isValidPassword("password@123"));
        assertFalse(LoginValidator.isValidPassword("mypass!456"));
        assertFalse(LoginValidator.isValidPassword("test#123pwd"));
    }
    
    @Test
    @DisplayName("Password without symbol should fail")
    public void testPasswordWithoutSymbol() {
        assertFalse(LoginValidator.isValidPassword("Password123"));
        assertFalse(LoginValidator.isValidPassword("MyPassword456"));
        assertFalse(LoginValidator.isValidPassword("Test123"));
    }
    
    @Test
    @DisplayName("Password shorter than 6 characters should fail")
    public void testShortPassword() {
        assertFalse(LoginValidator.isValidPassword("Pass@"));
        assertFalse(LoginValidator.isValidPassword("P@123"));
        assertFalse(LoginValidator.isValidPassword(""));
    }
    
    @Test
    @DisplayName("Null or empty password should fail")
    public void testNullOrEmptyPassword() {
        assertFalse(LoginValidator.isValidPassword(null));
        assertFalse(LoginValidator.isValidPassword(""));
        assertFalse(LoginValidator.isValidPassword("   "));
    }
    
    // Complete Credential Validation Tests
    
    @Test
    @DisplayName("Valid credentials should pass complete validation")
    public void testValidCredentials() {
        String result = LoginValidator.validateLoginCredentials(
            "user@example.com", 
            "Password@123"
        );
        assertTrue(result.isEmpty(), "Valid credentials should return empty error message");
    }
    
    @Test
    @DisplayName("Empty email should return error")
    public void testEmptyEmail() {
        String result = LoginValidator.validateLoginCredentials("", "Password@123");
        assertFalse(result.isEmpty());
        assertTrue(result.contains("Email"));
    }
    
    @Test
    @DisplayName("Invalid email format should return error")
    public void testInvalidEmailFormat() {
        String result = LoginValidator.validateLoginCredentials(
            "invalid.email", 
            "Password@123"
        );
        assertFalse(result.isEmpty());
        assertTrue(result.contains("email format"));
    }
    
    @Test
    @DisplayName("Invalid password should return descriptive error")
    public void testInvalidPasswordError() {
        String result = LoginValidator.validateLoginCredentials(
            "user@example.com", 
            "password123"
        );
        assertFalse(result.isEmpty());
        assertTrue(result.contains("capital letter"));
    }
    
    @Test
    @DisplayName("Password error message should indicate missing symbol")
    public void testPasswordMissingSymbol() {
        String error = LoginValidator.getPasswordErrorMessage("Password123");
        assertTrue(error.contains("symbol"));
    }
    
    @Test
    @DisplayName("Password error message should indicate missing capital letter")
    public void testPasswordMissingCapitalLetter() {
        String error = LoginValidator.getPasswordErrorMessage("password@123");
        assertTrue(error.contains("capital letter"));
    }
}
