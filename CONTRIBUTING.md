# 🤝 Contributing to GameVerse

Thank you for your interest in contributing to GameVerse! This document outlines how to contribute to the project.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** to your local machine
3. **Create a feature branch** for your changes
4. **Make your changes** following our guidelines
5. **Submit a Pull Request** with a clear description

```bash
# Clone and setup
git clone https://github.com/YOUR-USERNAME/GameVerse.git
cd GameVerse
git checkout -b feature/your-feature-name

# Build and test
gradle clean build
gradle test

# Commit and push
git add .
git commit -m "Add your feature description"
git push origin feature/your-feature-name
```

---

## Code of Conduct

- Be respectful to all contributors
- Provide constructive feedback
- Focus on the code, not the person
- No harassment, discrimination, or hate speech
- Help others learn and grow

---

## Areas for Contribution

### 🎮 New Games
Create new games by:
1. Creating a new module in `games/`
2. Extending `BaseGame` class
3. Implementing the `Game` interface
4. Registering in `GameRegistry`

**Game Template:**
```java
package com.gameverse.games.yourname;

import com.gameverse.games.core.BaseGame;

public class YourGameName extends BaseGame {
    
    public YourGameName() {
        super("Your Game Name");
    }
    
    @Override
    public void initialize() {
        // Setup game
    }
    
    @Override
    public void start() {
        super.start();
        // Start game logic
    }
    
    @Override
    public void update(float deltaTime) {
        if (!isRunning()) return;
        // Update game state
    }
}
```

### 🎨 UI/UX Improvements
- Improve login/sign up pages
- Create game selection menu
- Design player profile screen
- Build leaderboard visualization
- Add settings screen

### 🔧 Bug Fixes
- Report issues on GitHub
- Test existing code
- Submit PRs with fixes
- Include test cases

### 📝 Documentation
- Improve README files
- Add code comments
- Create tutorials
- Write API documentation

### 🧪 Testing
- Write unit tests
- Add integration tests
- Test edge cases
- Improve test coverage

### ⚡ Performance
- Optimize game loops
- Reduce memory usage
- Speed up loading
- Improve responsiveness

### 🌐 Multiplayer
- Add WebSocket support
- Implement matchmaking
- Create multiplayer games
- Handle player synchronization

### 💾 Database
- Implement SQLite persistence
- Add PostgreSQL support
- Create migration scripts
- Build data repositories

---

## Code Guidelines

### Java Style Guide

```java
// 1. Class and method names
public class PlayerManager { }
public void managePlayer() { }

// 2. Variable names (camelCase)
private int playerLevel;
private String userName;

// 3. Constants (UPPER_CASE)
private static final int MAX_PLAYERS = 100;

// 4. Always use braces
if (isValid) {
    process();
}

// 5. Indent with 4 spaces
if (condition) {
    doSomething();
}

// 6. Comment your code
// Calculate XP reward based on score
int xpReward = score / 10;

// 7. Use meaningful variable names
int playerLevel;  // Good
int pl;           // Bad
```

### File Structure

- **One class per file** (with rare exceptions for inner classes)
- **Package names lowercase**: `com.gameverse.games.snake`
- **Source files in `src/main/java`**
- **Tests in `src/test/java`**
- **Resources in `src/main/resources`**

### Javadoc Comments

```java
/**
 * Calculate the XP reward for a game result.
 * 
 * @param score the game score
 * @param won whether the player won
 * @return the calculated XP reward
 */
public int calculateXpReward(int score, boolean won) {
    // Implementation
}
```

### Method Organization

```java
public class Player {
    // 1. Constants
    private static final int MAX_LEVEL = 100;
    
    // 2. Fields
    private String username;
    private int level;
    
    // 3. Constructors
    public Player(String username) { }
    
    // 4. Public methods
    public void addXp(int amount) { }
    
    // 5. Private methods
    private void updateLevel() { }
    
    // 6. Getters/Setters
    public String getUsername() { }
    public void setLevel(int level) { }
    
    // 7. Override methods
    @Override
    public String toString() { }
}
```

---

## Commit Guidelines

### Commit Message Format

```
[TYPE] Brief description (50 chars max)

Longer explanation if needed. This is optional but helpful
for complex changes. Explain WHY, not WHAT.

Fixes #123
```

### Commit Types

- `[FEATURE]` - New feature or game
- `[FIX]` - Bug fix
- `[DOCS]` - Documentation update
- `[STYLE]` - Code style (formatting, etc.)
- `[REFACTOR]` - Code refactoring
- `[TEST]` - Adding or updating tests
- `[PERF]` - Performance improvement

### Examples

```
[FEATURE] Add Snake game implementation

[FIX] Fix leaderboard sorting bug

[DOCS] Update authentication documentation

[TEST] Add login validator unit tests

[REFACTOR] Extract common game logic into BaseGame
```

---

## Testing Requirements

### Write Tests For:
- ✅ New features
- ✅ Bug fixes
- ✅ Edge cases
- ✅ Error conditions

### Test Format

```java
@DisplayName("Should validate email correctly")
@Test
public void testEmailValidation() {
    assertTrue(LoginValidator.isValidEmail("user@example.com"));
    assertFalse(LoginValidator.isValidEmail("invalid.email"));
}
```

### Run Tests

```bash
# All tests
gradle test

# Specific module
gradle :ui:test

# With coverage
gradle test --info
```

---

## Pull Request Guidelines

### Before Submitting

- [ ] Fork and create your branch
- [ ] Follow code guidelines
- [ ] Write tests for new code
- [ ] Update documentation
- [ ] Test your changes: `gradle clean build`
- [ ] Commit with clear messages
- [ ] Push to your fork

### PR Description

```markdown
## Description
Brief description of changes

## Type
- [ ] Bug fix
- [ ] New feature
- [ ] Documentation update
- [ ] Other (specify)

## Related Issues
Fixes #123

## Changes
- Change 1
- Change 2
- Change 3

## Testing
How was this tested?

## Screenshots (if applicable)
Add screenshots for UI changes
```

### Review Process

1. Submit PR with clear description
2. Wait for code review
3. Address feedback/suggestions
4. Push updates to same branch
5. PR merges after approval
6. Celebrate! 🎉

---

## Project Architecture

### Core Modules

```
core/          - Game interface and core logic
player/        - Player management system
games/         - Game implementations
rewards/       - XP and coin systems
achievements/  - Achievement tracking
leaderboard/   - Leaderboard management
ui/            - User interface (login, menus)
database/      - Data persistence layer
```

### Adding a New Module

1. Create folder: `newmodule/`
2. Create structure:
   ```
   newmodule/
   ├── build.gradle
   └── src/
       ├── main/java/com/gameverse/newmodule/
       └── test/java/com/gameverse/newmodule/
   ```
3. Update `settings.gradle`:
   ```gradle
   include ':newmodule'
   ```
4. Update main `build.gradle`:
   ```gradle
   project(':newmodule') {
       dependencies {
           implementation project(':core')
       }
   }
   ```

---

## Running the Project

### Development Workflow

```bash
# Initial setup
gradle clean build

# Development
gradle build         # Incremental build
gradle test         # Run tests
gradle :ui:run      # Run application

# Before committing
gradle clean build test   # Full test
```

### Build Variants

```bash
# Fast build (incremental)
gradle build

# Clean rebuild
gradle clean build

# With more detail
gradle build --info

# Parallel build (faster)
gradle build --parallel
```

---

## Reporting Issues

### Bug Report Template

```markdown
**Describe the bug**
Brief description

**To reproduce**
Steps to reproduce:
1. 
2. 
3. 

**Expected behavior**
What should happen

**Actual behavior**
What actually happens

**Environment**
- OS: Windows 11
- Java: 21.0.1
- Gradle: 8.5

**Logs/Screenshots**
Attach relevant information
```

### Feature Request Template

```markdown
**Is your feature related to a problem?**
Describe the problem

**Describe the solution**
What should be added

**Describe alternatives**
Other approaches

**Additional context**
Any other info
```

---

## Documentation Style

### README Sections

1. **Overview** - What is it?
2. **Features** - What can it do?
3. **Installation** - How to set up?
4. **Usage** - How to use it?
5. **Architecture** - How is it structured?
6. **Contributing** - How to help?
7. **License** - Legal info

### Code Comments

```java
// GOOD: Explains why
// Check if player has unlocked this achievement
if (!player.hasAchievement(achievementId)) {
    unlockAchievement(player, achievementId);
}

// BAD: States the obvious
// Set level to 5
player.setLevel(5);

// GOOD: Complex logic needs explanation
// Exponential XP scaling: each level requires 20% more XP
// than the previous level (base 1000 at level 1)
int requiredXp = (int)(1000 * Math.pow(1.2, level - 1));
```

---

## Development Tools

### Recommended IDE
- **VS Code** with Extension Pack for Java
- **IntelliJ IDEA** Community Edition
- **Eclipse** IDE

### Gradle Tasks

```bash
gradle tasks                    # List all tasks
gradle clean                    # Remove build files
gradle build                    # Compile project
gradle test                     # Run tests
gradle run                      # Run application
gradle --daemon                 # Run with daemon
gradle --parallel              # Parallel build
gradle build --refresh-dependencies  # Clear cache
```

---

## Community

- **GitHub Issues** - Report bugs and request features
- **Discussions** - Ask questions and discuss ideas
- **Pull Requests** - Submit code contributions
- **Email** - contact@gameverse.dev (future)

---

## Recognition

Contributors will be:
- Listed in README.md
- Credited in release notes
- Invited to development discussions
- Recognized for major contributions

---

## FAQ

**Q: Do I need permission to start working?**
A: No! Just fork, branch, and start coding.

**Q: How long does review take?**
A: Usually 1-3 days depending on PR complexity.

**Q: Can I work on existing issues?**
A: Yes! Comment on the issue to let others know.

**Q: What if my PR is rejected?**
A: Feedback will be provided. You can iterate and resubmit.

**Q: Can I contribute without coding?**
A: Absolutely! Documentation, testing, and bug reports are valuable.

---

## License

By contributing to GameVerse, you agree that your contributions will be licensed under the MIT License.

---

## Support

- **Documentation** - See README.md and AUTHENTICATION.md
- **Issues** - GitHub Issues
- **Questions** - GitHub Discussions
- **Examples** - Check existing code in modules

---

**Thank you for contributing to GameVerse! Together we're building something amazing.** 🚀

For more information, visit the [GameVerse GitHub](https://github.com/YOUR-USERNAME/GameVerse)
