# 🎮 GameVerse

### A Modular Multi-Game Platform Built with Java

> **One platform. Multiple games. One player ecosystem.**

GameVerse is a Java-based modular gaming platform that brings multiple mini-games together inside a single application. Unlike traditional projects where every game is developed as a completely separate application, GameVerse provides a **common platform architecture** where different games can share player profiles, XP, achievements, statistics and leaderboards.

The long-term goal is to create an extensible platform where developers can build and add new games using a common Game API/SDK.

---

## ✨ Features

### 🎮 Multiple Games
GameVerse currently hosts the following games:

- ♟️ **Chess** - Classic chess game with AI opponent
- 🐍 **Snake** - Classic snake game
- 🏓 **Pong** - Classic Pong game with AI
- ❌ **Tic-Tac-Toe** - Simple turn-based game with AI
- 🧠 **Memory Game** - Classic memory/matching game
- 🏎️ **Mini Racing** - Simple racing game
- 👁️ **DON'T LOOK** - Stealth horror: it only moves when you're not looking (launches in your browser from the hub)
- 📡 **UNKNOWN SIGNAL** - Radio-mystery: tune, decode Morse, and discover the signal is aware of you (launches in your browser from the hub)

More games can be added in the future using the modular game API.

### � Secure Authentication
Users must login with a valid email and strong password:

- **Email Validation** - Valid email format required (user@domain.com)
- **Strong Passwords** - Must contain:
  - At least 6 characters
  - 1 capital letter (A-Z)
  - 1 special symbol (!@#$%^&*)
- **Sign Up Support** - Create new accounts with validated credentials
- **Modern GUI** - Professional dark-themed login interface

**Example Login:**
```
Email: player@gameverse.com
Password: GameVerse@2024 ✓
```

### 👤 Unified Player Profile
Players have a single profile across all games:

```
Username: player@gameverse.com
Level: 12
XP: 2450
Coins: 750

Games Played: 86
Wins: 52
Achievements: 14
Global Rank: #27
```

### ⭐ XP & Level System
Players earn XP through gameplay and level up to unlock achievements and cosmetics.

- Base XP calculated from game score
- Bonus multipliers for wins and streaks
- Level progression with configurable requirements

### 🪙 Virtual Coin System
Players can earn virtual coins through various activities:

- Game wins: +50 coins
- Achievements: +100 coins
- High scores: +75 coins
- Daily missions: +25 coins

### 🏆 Achievement System
Unlock achievements based on gameplay:

- 🏅 First Victory
- 🎮 Game Explorer
- 🔥 Winning Streak
- 🐍 Snake Master
- ♟️ Chess Beginner
- 🏆 Multi-Game Champion

### 📊 Leaderboards
- Global leaderboard
- Game-specific leaderboards
- Player rankings based on score
- Historical score tracking

---

## 🧩 Modular Game Architecture

Every game implements the **Game interface**, allowing the platform to interact with different games uniformly:

```java
public interface Game {
    String getName();
    void initialize();
    void start();
    void pause();
    void resume();
    void restart();
    GameResult getResult();
    void update(float deltaTime);
    boolean isRunning();
}
```

Individual games extend `BaseGame` for common functionality:

```
BaseGame (Abstract)
  ├── SnakeGame
  ├── PongGame
  ├── TicTacToeGame
  ├── MemoryGame
  ├── ChessGame
  └── RacingGame
```

---

## 📁 Project Structure

```
GameVerse/
├── core/                          # Core platform interfaces and managers
│   ├── Game.java                 # Base game interface
│   ├── GameManager.java          # Manages game lifecycle
│   ├── GameRegistry.java         # Registers available games
│   └── GameResult.java           # Game result data structure
│
├── player/                        # Player system
│   ├── Player.java               # Player profile and stats
│   └── PlayerManager.java        # Player account management
│
├── games/                         # Game implementations
│   ├── core/
│   │   └── BaseGame.java         # Abstract base for all games
│   ├── chess/
│   │   └── ChessGame.java
│   ├── snake/
│   │   └── SnakeGame.java
│   ├── pong/
│   │   └── PongGame.java
│   ├── tictactoe/
│   │   └── TicTacToeGame.java
│   ├── memory/
│   │   └── MemoryGame.java
│   └── racing/
│       └── RacingGame.java
│
├── achievements/                  # Achievement system
│   ├── Achievement.java          # Achievement definition
│   └── AchievementManager.java   # Achievement tracking
│
├── rewards/                       # Reward systems
│   ├── XPManager.java            # XP and level progression
│   └── CoinManager.java          # Virtual currency
│
├── leaderboard/                   # Leaderboard system
│   ├── LeaderboardEntry.java    # Leaderboard entry
│   └── LeaderboardManager.java  # Leaderboard management
│
├── database/                      # Data persistence (future)
│   ├── DatabaseManager.java
│   └── repositories/
│
├── ui/                           # User interface
│   ├── LoginValidator.java       # Email & password validation
│   ├── LoginPage.java            # Login GUI (Swing)
│   ├── SignUpPage.java           # Account creation GUI
│   ├── GameLauncher.java         # Application entry point
│   ├── MainMenu.java             # Main menu (future)
│   ├── GameLibrary.java          # Game selection (future)
│   ├── ProfileScreen.java        # Player profile (future)
│   └── LeaderboardScreen.java    # Leaderboard view (future)
│
├── build.gradle                  # Gradle configuration
├── settings.gradle               # Multi-module settings
└── README.md                     # This file
```

---

## 🛠️ Technology Stack

| Technology | Purpose |
|------------|---------|
| **Java 21+** | Core programming language |
| **Gradle** | Build and dependency management |
| **SQLite** | Local database for MVP |
| **PostgreSQL** | Future online database |
| **Spring Boot** | Future backend |
| **JUnit 5** | Testing framework |
| **Git** | Version control |

---

## 🚀 Getting Started

### Prerequisites

- Java JDK 21 or higher
- Gradle 8.0 or higher
- Git
- VS Code (recommended)

For detailed Windows setup instructions, see **[SETUP.md](SETUP.md)**

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone https://github.com/YOUR-USERNAME/GameVerse.git
   cd GameVerse
   ```

2. **Build the project:**
   ```bash
   gradle build
   ```

3. **Run the application:**
   ```bash
   gradle :ui:run
   ```

   This will launch the **Login Page**. Create an account or login with:
   - **Valid Email:** user@example.com
   - **Valid Password:** MyPassword@123 (must have capital letter + symbol)

### Building Individual Modules

```bash
# Build specific module
gradle :games:snake:build

# Run tests
gradle test

# Run specific test suite (e.g., Login validation)
gradle :ui:test

# Clean build
gradle clean build
```

### Documentation

- **[README.md](README.md)** - Project overview
- **[AUTHENTICATION.md](AUTHENTICATION.md)** - Login system details
- **[SETUP.md](SETUP.md)** - Windows setup guide
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute

---

## 🏗️ Architecture Overview

```
                      GAMEVERSE PLATFORM
                             │
              ┌───────────────┼───────────────┐
              │               │               │
          Game Manager    Player System   Reward Engine
              │               │               │
        ┌─────┴─────┐    ┌────┴────┐    ┌────┴────┐
        │           │    │         │    │         │
     Games       Registry Profile  Stats  XP    Coins
        │           │    │         │    │         │
        └─────┬─────┘    └────┬────┘    └────┬────┘
              │               │              │
              └───────────────┼──────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
              Achievements        Leaderboards
```

---

## 🎯 MVP Features

The first version focuses on:

- ✅ Game Hub/Launcher
- ✅ 6 Initial Games
- ✅ Player Profile System
- ✅ XP & Level System
- ✅ Coin System
- ✅ Achievement System
- ✅ Leaderboard System
- ✅ Game Result System
- ✅ Modular Game Interface

---

## 🗺️ Development Roadmap

### Phase 1 — Core ✅
- [x] Create Java/Gradle project
- [x] Create Game interface
- [x] Create GameManager
- [x] Create GameRegistry
- [x] Create core module structure

### Phase 2 — Games (In Progress)
- [x] Snake
- [x] Tic-Tac-Toe
- [x] Pong
- [ ] Chess (full implementation)
- [ ] Memory Game (full implementation)
- [ ] Racing Game (full implementation)

### Phase 3 — Player System ✅
- [x] Player profile
- [x] XP system
- [x] Levels
- [x] Coins
- [x] Statistics

### Phase 4 — Competition ✅
- [x] Achievements
- [x] Global leaderboard
- [x] Game-specific leaderboards

### Phase 5 — Persistence
- [ ] SQLite integration
- [ ] Player data persistence
- [ ] Game results database
- [ ] Achievement tracking

### Phase 6 — Advanced Architecture
- [ ] Plugin system
- [ ] Game SDK
- [ ] Game version management
- [ ] Plugin validation

### Phase 7 — Online Version
- [ ] Spring Boot backend
- [ ] PostgreSQL database
- [ ] Authentication system
- [ ] REST API
- [ ] Online profiles

### Phase 8 — Multiplayer
- [ ] WebSocket server
- [ ] Matchmaking system
- [ ] Multiplayer Chess
- [ ] Multiplayer Pong
- [ ] Multiplayer Tic-Tac-Toe

---

## 💡 How to Add a New Game

1. Create a new module in `games/`
2. Implement a class extending `BaseGame`
3. Register the game in `GameRegistry`
4. Add module to `settings.gradle`

**Example:**

```java
package com.gameverse.games.yourname;

import com.gameverse.games.core.BaseGame;

public class YourGameName extends BaseGame {
    
    public YourGameName() {
        super("Your Game");
    }
    
    // Implement required methods
}
```

Then register it:

```java
GameRegistry registry = GameRegistry.getInstance();
registry.registerGame("yourname", YourGameName.class);
```

---

## 🧪 Testing

The project uses JUnit 5 for testing:

```bash
# Run all tests
gradle test

# Run tests for specific module
gradle :core:test
gradle :player:test
```

---

## 🔐 Security Considerations

For future online version:

- Password hashing and validation
- Authentication and authorization
- Input validation
- Rate limiting
- Server-side score validation
- Secure API communication
- Plugin validation
- Encryption for sensitive data

---

## 🌐 Future Vision

Transform GameVerse from simple games into a complete **game platform ecosystem**:

```
Individual Games
        ↓
   GameVerse
   Platform
        ↓
  Plugin System
        ↓
    Game SDK
        ↓
Open-Source Game Ecosystem
```

---

## 🤝 Contributing

Contributions are welcome! Possible areas:

- New games
- UI/UX improvements
- Game optimization
- Bug fixes
- Achievement additions
- Leaderboard improvements
- Documentation
- Testing
- Performance tuning

**Basic Workflow:**

```bash
git clone <repository>
git checkout -b feature/your-feature
# Make changes
git commit -m "Add your feature"
git push origin feature/your-feature
# Create Pull Request
```

---

## 📜 License

This project is released under the **MIT License**.

---

## 👨‍💻 Project Philosophy

GameVerse demonstrates how to transform **multiple independent mini-games** into a **single extensible software platform** using:

- Modular architecture
- Common interfaces
- Reusable services
- Clean separation of concerns

> **Build the platform, not just the games.**

---

## 📊 Project Status

- **Current Version:** 1.0.0 (MVP)
- **Status:** Active Development (Phase 2)
- **Java Version:** 21+
- **Last Updated:** 2024

---

**GameVerse — One Platform. Multiple Games. Infinite Possibilities.** 🚀
