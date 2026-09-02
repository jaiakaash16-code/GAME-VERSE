# 🔐 GameVerse Authentication System

## Overview

The GameVerse authentication system provides secure user login and account creation with email validation and strong password requirements.

## Features

### 📧 Email Validation
- Validates email format using regex pattern
- Ensures email is not empty
- Supports standard email formats (user@domain.com)
- Prevents registration with invalid emails

### 🔒 Strong Password Requirements
All passwords must meet the following criteria:

- **Minimum Length:** 6 characters
- **Capital Letter:** At least 1 uppercase letter (A-Z)
- **Special Symbol:** At least 1 symbol (!@#$%^&*...)

**Examples:**
- ✅ Valid: `Password@123`, `MyPass!456`, `Secure#Pass99`
- ❌ Invalid: `password@123` (no capital), `Password123` (no symbol), `Pass@1` (too short)

### 👤 Player Profile Integration
- Each user gets a unique player profile after successful login/signup
- Profile includes level, XP, coins, and game statistics
- Player data persists across sessions (with database implementation)

## Architecture

### Components

#### 1. **LoginValidator** (`LoginValidator.java`)
Utility class for validating credentials:
- `isValidEmail(String email)` - Validates email format
- `isValidPassword(String password)` - Validates password requirements
- `validateLoginCredentials(String email, String password)` - Complete validation
- `getPasswordErrorMessage(String password)` - Provides specific error messages

#### 2. **LoginPage** (`LoginPage.java`)
Modern Swing GUI for user login:
- Professional dark theme interface
- Email and password input fields
- Real-time validation feedback
- "Remember me" option (for future implementation)
- Sign up button for new users
- Password requirements display

#### 3. **SignUpPage** (`SignUpPage.java`)
Account creation interface:
- Email validation
- Password requirement verification
- Confirm password matching
- Duplicate email detection
- Clear error messages for guidance

#### 4. **GameLauncher** (`GameLauncher.java`)
Application entry point:
- Initializes login flow
- Handles navigation between login/signup
- Manages successful authentication
- Integrates with game lobby (future)

## Usage

### Running the Application

```bash
# Build the project
gradle build

# Run the application (starts with login page)
gradle :ui:run

# Or directly run GameLauncher
gradle :ui:run --args="com.gameverse.ui.GameLauncher"
```

### Programmatic Login Validation

```java
// Validate complete credentials
String error = LoginValidator.validateLoginCredentials(
    "user@example.com",
    "Password@123"
);

if (error.isEmpty()) {
    System.out.println("Login successful!");
} else {
    System.out.println("Error: " + error);
}
```

### Email Validation Only

```java
if (LoginValidator.isValidEmail("user@example.com")) {
    System.out.println("Email is valid");
}
```

### Password Validation Only

```java
if (LoginValidator.isValidPassword("Password@123")) {
    System.out.println("Password meets requirements");
} else {
    String error = LoginValidator.getPasswordErrorMessage("invalid");
    System.out.println(error);
}
```

### Using Login Page in Your Code

```java
new LoginPage(new LoginPage.LoginCallback() {
    @Override
    public void onLoginSuccess(Player player) {
        System.out.println("Welcome: " + player.getUsername());
        // Navigate to game lobby
    }
    
    @Override
    public void onLoginFailed(String message) {
        System.out.println("Login error: " + message);
    }
    
    @Override
    public void onSignUp() {
        // Show sign up page
    }
});
```

## Password Requirements Examples

### ✅ Valid Passwords
```
MyPassword@123
Secure#Pass99
Test$1Password
GameVerse!2024
Admin@Login
MyGame!123
```

### ❌ Invalid Passwords
```
password123        // No capital letter
PASSWORD@123       // No capital letter (all capitals don't count as "at least one")
MyPassword123      // No symbol
MyPass@            // Too short
password@          // No capital letter, too short
```

## Validation Error Messages

The system provides specific error messages to guide users:

| Issue | Error Message |
|-------|---------------|
| Empty email | "Email cannot be empty" |
| Invalid email format | "Invalid email format. Please enter a valid email address." |
| Empty password | "Password cannot be empty" |
| Too short | "Password must be at least 6 characters long" |
| Missing capital letter | "Password must contain at least one capital letter" |
| Missing symbol | "Password must contain at least one symbol (!@#$%^&...)" |
| Both missing | "Password must contain at least one capital letter and one symbol" |

## Testing

Run the test suite to verify login validation:

```bash
gradle :ui:test
```

Tests cover:
- Valid and invalid email formats
- Password requirements (length, capitals, symbols)
- Complete credential validation
- Error message accuracy

## Security Considerations

### Current Implementation (MVP)
- Client-side validation for user experience
- Passwords stored in PlayerManager (in-memory)
- Basic email uniqueness check

### Future Enhancements
- Password hashing (bcrypt/Argon2)
- Server-side validation
- Secure token-based authentication
- Rate limiting on login attempts
- Account lockout after failed attempts
- Email verification process
- Forget password functionality
- Two-factor authentication (2FA)
- Session management
- HTTPS for all communications

## UI Features

### Login Page
- Dark professional theme
- Email input with placeholder
- Password input (masked characters)
- Remember me checkbox
- Password requirements display
- Real-time error messages
- Sign up link for new users
- Enter key support for quick login

### Sign Up Page
- Similar professional design
- Email input with validation
- Password input with requirements
- Confirm password field
- Password strength indicator (in future)
- Clear error guidance
- Back to login button

## Database Integration (Future)

When database support is added:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    last_login TIMESTAMP
);

CREATE TABLE player_profiles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT UNIQUE,
    username VARCHAR(50),
    level INT DEFAULT 1,
    xp INT DEFAULT 0,
    coins INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## Flow Diagram

```
┌─────────────┐
│   START     │
└──────┬──────┘
       │
       ▼
┌────────────────────┐
│   Login Page       │
│ (Email + Password) │
└──────┬─────────────┘
       │
       ├─► Validate Credentials
       │
       ├─► Valid?
       │   ├─ Yes → Check in Database
       │   │        └─► Found? → Login Success
       │   │        └─► Not Found? → Create New Account
       │   │
       │   └─ No → Show Error
       │
       ├─► Sign Up?
       │   └─► Show Sign Up Page
       │       ├─► Validate Email & Password
       │       ├─► Check Email Exists?
       │       ├─► Create Account
       │       └─► Login Success
       │
       ▼
┌────────────────────┐
│   Game Lobby       │
│   (Main Menu)      │
└────────────────────┘
```

## File Structure

```
ui/
├── src/main/java/com/gameverse/ui/
│   ├── LoginValidator.java      # Validation logic
│   ├── LoginPage.java           # Login GUI
│   ├── SignUpPage.java          # Sign up GUI
│   └── GameLauncher.java        # Application entry point
│
└── src/test/java/com/gameverse/ui/
    └── LoginValidatorTest.java  # Unit tests
```

## Troubleshooting

### "Email already registered"
- This email is already in use
- Use a different email or click "Back to Login" to sign in

### "Passwords do not match"
- Password fields don't match
- Re-enter passwords and ensure they are identical

### "Invalid email format"
- Email doesn't follow standard format (user@domain.com)
- Common mistakes:
  - Missing @ symbol
  - No domain name
  - Spaces in email

### "Password must contain..."
- Review password requirements
- At least 6 characters
- Include 1 capital letter (A-Z)
- Include 1 symbol (!@#$%^&*...)

## Contributing

To enhance the authentication system:

1. Add database integration
2. Implement password hashing
3. Add 2FA support
4. Implement social login (Google, Discord)
5. Add email verification
6. Implement password recovery
7. Add user profile customization

---

**GameVerse Authentication — Secure. Simple. Scalable.** 🔐
