# 🔫 One Bullet

A 2D puzzle-action game where you have **one bullet** per level. Use the environment to eliminate enemies and solve physics-based challenges.

## 🎮 Gameplay

- **WASD / Arrow Keys** — Move
- **Mouse** — Aim
- **Left Click** — Shoot
- **R** — Restart Level
- **Escape** — Return to Menu

### Core Mechanic
You have ONE bullet. It fires toward your mouse, and when it hits something (wall, enemy, or object), it returns to you. Use the environment creatively:
- Shoot ropes to drop heavy objects on enemies
- Trigger explosive barrels for chain reactions
- Use physics to launch objects at targets

## 🏗️ Architecture

```
┌─────────────────────────┐         ┌──────────────────────────┐
│      FRONTEND           │         │       JAVA BACKEND        │
│                         │  REST   │                          │
│  HTML + CSS + JS        │◄──────►│  Spring Boot              │
│  Phaser 3 (game engine) │  API   │  H2 Database (in-memory)  │
│  Canvas rendering       │         │  JPA / Hibernate          │
└─────────────────────────┘         └──────────────────────────┘
```

- **Frontend**: Pure HTML/CSS/JS with Phaser 3 for the game
- **Backend**: Java Spring Boot REST API + H2 embedded database

## 🚀 Setup & Run

### Prerequisites
- Java 17+ (tested with Java 21)
- Maven (will be downloaded automatically)
- Python 3 (for frontend server)

### Step 1: Start the Backend

```bash
cd backend

# Maven will download automatically on first run
export PATH="$(pwd)/apache-maven-3.9.6/bin:$PATH"
mvn spring-boot:run
```

Backend runs at: **http://localhost:8080**

### Step 2: Start the Frontend

```bash
cd frontend

# Using Python's built-in HTTP server
python -m http.server 3000
```

Frontend runs at: **http://localhost:3000**

### Step 3: Play!

Open **http://localhost:3000** in your browser.

## 📁 Project Structure

```
one-bullet/
├── backend/                    # Java Spring Boot backend
│   ├── pom.xml                 # Maven configuration
│   └── src/main/java/com/onebullet/
│       ├── OneBulletApplication.java
│       ├── WebConfig.java      # CORS configuration
│       ├── controller/         # REST controllers
│       ├── model/              # JPA entities
│       ├── repository/         # Spring Data repositories
│       └── service/            # Business logic
├── frontend/                   # HTML/CSS/JS game
│   ├── index.html
│   ├── css/style.css
│   └── js/
│       ├── main.js             # Phaser config
│       ├── api.js              # Backend API client
│       ├── entities/           # Player, Bullet, Enemy, PhysicsObject
│       ├── levels/             # Level definitions
│       └── scenes/             # Boot, Menu, Game, Win scenes
└── README.md
```

## 🎯 Levels

1. **The Tutorial** — Learn to shoot and retrieve your bullet
2. **Rope Drop** — Shoot ropes to drop objects on enemies
3. **Seesaw** — Use physics to launch objects at elevated targets
4. **Chain Reaction** — Trigger explosive barrel chains
5. **The Gauntlet** — All mechanics combined

## 🔧 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/players` | Create/get player |
| GET | `/api/players/{id}` | Get player data |
| GET | `/api/players/{id}/progress` | Get level progress |
| POST | `/api/players/{id}/progress` | Save progress |
| POST | `/api/scores` | Submit score |
| GET | `/api/scores/leaderboard` | Get top scores |

## 🎨 Graphics

All graphics are code-drawn using Phaser's graphics API — no external image assets required. The game uses a minimal geometric aesthetic with:
- Cyan player character
- Red enemies with patrol AI
- Yellow bullet with trail effects
- Color-coded physics objects (brown crates, green barrels, blue balls, gray rocks)

## 🔊 Features

- **Physics-based gameplay** — Arcade physics for realistic object interactions
- **Particle effects** — Explosions, bullet trails, death effects
- **Screen shake** — Impact feedback on hits and explosions
- **Progressive difficulty** — 5 levels with increasing complexity
- **Score system** — Time bonus + enemy score
- **Backend persistence** — Save progress and leaderboard via Java API
- **Local storage fallback** — Works offline without backend

## 📝 License

Built with ❤️ using Phaser 3 and Spring Boot
