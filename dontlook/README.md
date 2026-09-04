# DON'T LOOK 👁️

Top-down 2D stealth horror. You hold a flashlight with a **vision cone**. The monster
**only moves while you're not looking at it**. Walls block your light — so corners are
where it gets you. Reach the glowing door of each room. Escape the Kitchen, the Hallway
and the Basement, then survive Endless mode with procedurally generated rooms.

## Architecture

```
Browser (HTML/CSS/JS Canvas)  ──input POST──►  Java 21 (authoritative sim, port 8080)  ──REST──►  Python (Flask generator, port 8001)
       ▲                                         │                                                      │
       └───────────── state over SSE ◄───────────┘                                                      │
                                                                                                        ▼
                              Java falls back to its own generator if Python is down      random room JSON
```

- **Browser** — renders the scene and flashlight darkness; sends input packets (move + aim) ~30×/s. Never decides game state.
- **Java 21 (JDK only, zero dependencies)** — serves the frontend, runs the simulation at 30 Hz (player physics, line-of-sight raycast, monster AI), streams state over SSE, records per-room best times to `best_time.json`.
- **Python (Flask)** — optional companion service that procedurally generates rooms for Endless mode. If it's down, Java uses its built-in fallback generator, so the game always works.

## Home page & levels

The game opens on a home menu:
- **▶ PLAY** — starts (or resumes) the current run.
- **LEVELS** — lists Kitchen / Hallway / Basement with a green **✓** and best time for completed
  levels, plus **∞ Endless Mode**. Click any row to jump straight into that level.
- **Esc** during a run opens the menu and pauses the hunt (time freezes). Esc again resumes.

## Run it

### 1. Java server (required)

```bash
cd java-server
./run.sh        # bash / Git Bash on Windows
# or: run.cmd   # cmd/PowerShell
```

Then open **http://localhost:8080**.

### 2. Python generator (optional)

```bash
cd python-service
./run.sh        # creates venv, installs flask, serves on http://127.0.0.1:8001
# or: run.cmd
```

The Java server picks the Python service up automatically; restart Java if you start Python afterwards.

## Controls

| Key | Action |
| --- | --- |
| `WASD` / arrows | Move |
| Mouse | Aim the flashlight (monster freezes only inside the cone with clear line of sight) |
| `Esc` | Menu / pause (resume with Esc or PLAY) |
| `R` | Retry after death |
| `N` | Next room after escaping |
| `Space` / `Enter` | Start from the menu |

## Files

```
frontend/          index.html, style.css, game.js (renderer + SSE client + menu/levels + audio)
java-server/src/   GameServer, Simulation, Vision, Room, Json, FallbackGenerator, PythonClient, BestTimes
python-service/    app.py (Flask room generator)
rooms/             hand-authored story rooms: 01-kitchen.json, 02-hallway.json, 03-basement.json
```
