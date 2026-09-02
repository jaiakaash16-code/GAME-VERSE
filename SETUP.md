# 🚀 GameVerse Setup Guide for Windows

This guide will help you get GameVerse up and running on your Windows system, including the login/authentication system.

## Prerequisites

### 1. Install Java JDK 21+

**Option A: Using Official Oracle JDK**
1. Download from: https://www.oracle.com/java/technologies/downloads/
2. Select "Windows x64 Installer" for Java 21+
3. Run the installer and follow the installation wizard
4. Accept the default installation path (usually `C:\Program Files\Java\`)

**Option B: Using Chocolatey (Recommended)**
```powershell
# Open PowerShell as Administrator
choco install openjdk21
```

**Option C: Using Scoop**
```powershell
# Open PowerShell as Administrator
scoop install openjdk21
```

**Verify Installation:**
```powershell
java -version
```

You should see output like:
```
openjdk version "21.x.x" ...
```

### 2. Install Gradle

**Option A: Using Official Gradle Installer**
1. Download from: https://gradle.org/releases/
2. Download Gradle 8.5+ (binary-only)
3. Extract to a folder (e.g., `C:\gradle`)
4. Add to PATH environment variable

**Option B: Using Chocolatey (Recommended)**
```powershell
# Open PowerShell as Administrator
choco install gradle
```

**Option C: Using Scoop**
```powershell
# Open PowerShell as Administrator
scoop install gradle
```

**Verify Installation:**
```powershell
gradle -version
```

You should see:
```
Gradle 8.x.x
```

### 3. Install Git (Optional)
```powershell
choco install git
```

---

## Building GameVerse

### Step 1: Navigate to Project Directory
```powershell
cd "C:\Users\HP\OneDrive\Desktop\GAME VERSE"
```

### Step 2: Clean Build
```powershell
gradle clean build
```

This will:
- Download all dependencies
- Compile all modules
- Run tests
- Create build outputs

**Expected Output:**
```
> Task :core:compileJava
> Task :player:compileJava
> Task :achievements:compileJava
> Task :rewards:compileJava
> Task :leaderboard:compileJava
> Task :games:core:compileJava
> Task :games:snake:compileJava
> Task :games:pong:compileJava
> Task :games:tictactoe:compileJava
> Task :games:memory:compileJava
> Task :games:chess:compileJava
> Task :games:racing:compileJava
> Task :ui:compileJava
> Task :core:test
> Task :player:test
...
BUILD SUCCESSFUL
```

---

## Running the Application

### Method 1: Run with Gradle (Recommended for First Time)

```powershell
gradle :ui:run
```

This will:
1. Start the GameLauncher
2. Display the Login Page
3. Wait for user input

### Method 2: Create Gradle Wrapper (For CI/CD)

```powershell
gradle wrapper --gradle-version 8.5
```

Then use:
```powershell
.\gradlew.bat :ui:run
```

---

## Login Page Features

### Email Validation
- Must be a valid email format: `user@domain.com`
- Cannot contain spaces or special characters (except @ and .)

**Valid Examples:**
- user@example.com
- john.doe@company.co.uk
- test+tag@domain.com

### Password Requirements
Your password must:
1. **Be at least 6 characters long**
2. **Contain at least 1 CAPITAL letter** (A-Z)
3. **Contain at least 1 symbol** (!@#$%^&*...)

**Valid Passwords:**
- `MyPassword@123`
- `Secure#Pass99`
- `Test$1Password`
- `GameVerse!2024`

**Invalid Passwords:**
- `password123` ❌ (no capital letter)
- `PASSWORD@123` ❌ (no symbol)
- `MyPass@` ❌ (too short)
- `MyPassword123` ❌ (no symbol)

### Sign Up
1. Click "Create New Account"
2. Enter a valid email
3. Enter a password meeting requirements
4. Confirm your password
5. Click "Create Account"

---

## Troubleshooting

### Issue: "gradle command not found"

**Solution:**
1. Add Gradle to PATH:
   - Right-click "This PC" → Properties
   - Click "Advanced system settings"
   - Click "Environment Variables"
   - Add Gradle bin folder to PATH
   - Restart PowerShell

2. Or use full path:
   ```powershell
   C:\gradle\bin\gradle build
   ```

### Issue: "java command not found"

**Solution:**
1. Add Java to PATH:
   - Same steps as Gradle above
   - Add Java bin folder to PATH (e.g., `C:\Program Files\Java\jdk-21\bin`)
   - Restart PowerShell

2. Verify:
   ```powershell
   java -version
   ```

### Issue: Build fails with "Module not found"

**Solution:**
```powershell
# Clean cache and rebuild
gradle clean --refresh-dependencies build
```

### Issue: "Email already registered" on login

**Solution:**
- Each email can only have one account
- Use a different email for sign up
- Or click back to login with existing email

### Issue: "Passwords do not match" on sign up

**Solution:**
- Ensure both password fields are identical
- Check for accidental spaces
- Verify caps lock is off

---

## Project Structure

```
GameVerse/
├── core/                    # Core platform
├── player/                  # Player management
├── games/                   # Game modules
├── rewards/                 # XP & coins
├── achievements/            # Achievement system
├── leaderboard/            # Leaderboards
├── database/               # Database layer
├── ui/                     # UI with Login/SignUp
├── build.gradle            # Build configuration
├── settings.gradle         # Multi-module settings
├── README.md              # Project overview
├── AUTHENTICATION.md      # Login system docs
└── SETUP.md              # This file
```

---

## Common Gradle Commands

```powershell
# Build project
gradle build

# Run application
gradle :ui:run

# Run tests
gradle test

# Run specific module tests
gradle :player:test

# Build specific module
gradle :games:snake:build

# Clean build directory
gradle clean

# View all tasks
gradle tasks

# Build with detailed output
gradle build --info

# Refresh dependencies
gradle build --refresh-dependencies
```

---

## Running Tests

### All Tests
```powershell
gradle test
```

### Login Validation Tests
```powershell
gradle :ui:test
```

### Player Tests
```powershell
gradle :player:test
```

---

## Development Workflow

### Making Changes
1. Edit Java files in `src/main/java`
2. Edit tests in `src/test/java`
3. Run: `gradle clean build`
4. Test: `gradle test`

### Adding New Game
1. Create new folder in `games/`
2. Extend `BaseGame` class
3. Add module to `settings.gradle`
4. Register game in `GameRegistry`

### Testing Changes
```powershell
# Run full test suite
gradle test

# Run and watch for changes
gradle build --continuous
```

---

## Performance Tips

### Faster Builds
```powershell
# Use daemon (keeps Gradle running between builds)
gradle build --daemon

# Parallel builds
gradle build --parallel
```

### Incremental Compilation
Gradle automatically recompiles only changed files. Avoid `clean` unless necessary:

```powershell
# Fast incremental build
gradle build

# Full clean rebuild (slower)
gradle clean build
```

---

## IDE Integration

### VS Code Setup
1. Install "Extension Pack for Java"
2. Install "Gradle for Java"
3. Open project folder
4. VS Code will auto-detect Gradle setup

### IntelliJ IDEA Setup
1. Open project
2. File → Project Structure → Project
3. Set JDK to Java 21
4. Run → Edit Configurations
5. Add Gradle task: `:ui:run`

---

## Next Steps

1. ✅ Install Java & Gradle
2. ✅ Run `gradle build`
3. ✅ Run `gradle :ui:run`
4. ✅ Create account with valid credentials
5. 📝 Explore the code
6. 🎮 Add your own game

---

## Getting Help

### Resources
- [Java 21 Documentation](https://docs.oracle.com/en/java/javase/21/)
- [Gradle Documentation](https://docs.gradle.org/)
- [GameVerse GitHub](https://github.com/YOUR-USERNAME/GameVerse)

### Common Issues
See **AUTHENTICATION.md** for login-related issues

---

## Quick Reference

```powershell
# Setup (one-time)
cd "C:\Users\HP\OneDrive\Desktop\GAME VERSE"
gradle clean build

# Running the app
gradle :ui:run

# Running tests
gradle test

# Clean rebuild
gradle clean build
```

---

**Happy Gaming! 🎮**
