# Mirror World — 2D Indie Game

A complete 2D puzzle-platformer where every action in Normal World affects Mirror World and vice versa.

## Stack

- **Frontend:** HTML + CSS + JavaScript (vanilla, Canvas)
- **Backend:** Java 21 (built-in `com.sun.net.httpserver`)
- **Storage:** in-memory save/leaderboard via REST API

> Python was originally mentioned for the frontend, but a pure HTML+CSS+JS frontend needs no Python runtime. The Java backend handles all dynamic data via a JSON API.

## Architecture

```
mirrorworld/
├── backend/
│   ├── MirrorWorldServer.java   # single-file HTTP server
│   └── MirrorWorldServer.class  # compiled
├── frontend/
│   ├── index.html               # game shell + HUD
│   ├── style.css                # neon/cyber theme
│   └── game.js                  # engine, render, input, physics
└── run.bat                      # build + launch
```

## Run

```cmd
cd mirrorworld
run.bat
```

Then open: **http://localhost:8080/**

## REST API

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/levels` | List all levels |
| GET | `/api/level/{id}` | Level data (tilemap + hint) |
| POST | `/api/save` | Save player progress |
| GET | `/api/load/{playerId}` | Load saves for player |
| POST | `/api/score` | Submit run score |
| GET | `/api/leaderboard` | Top scores |

## Controls

- **A / D** or **← / →** — move
- **W / S** or **↑ / ↓** or **Space** — flip world
- **R** — restart level

## Levels

1. **Tutorial** — Learn the flip mechanic
2. **Two Buttons** — Two doors, two buttons, one path
3. **Chain Reaction** — Sequence of switches
4. **Split Path** — Hazards exist in one world only
5. **The Mirror Heart** — Final challenge

## Core Mechanic

There is **one shared map**. The player simply exists in one of two *interpretations* of it:

- **Normal world (blue):** some doors/hazards are active
- **Mirror world (pink):** different doors/hazards are active
- **Flip with W/S** — everything is the same map, just your interpretation toggles
- **Buttons persist** regardless of which world you're in (both worlds observe them)

This gives a small content footprint but huge puzzle depth — each level is a few tiles and one elegant idea.