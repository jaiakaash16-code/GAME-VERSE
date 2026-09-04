# UNKNOWN SIGNAL 📡

A radio-mystery game. You are the only operator at Listening Station 7.
Tune the receiver, separate signals from static, decode Morse, triangulate
sources — and slowly discover the signal is aware of you. It starts
answering back. Your choices decide how it ends.

**6 levels · 3 endings · zero external assets.** All visuals are CSS/canvas,
all audio is synthesized with WebAudio, all logic lives in a pure-JDK Java
backend.

## Requirements

- Java 21+ (JDK with `javac`). Nothing else — no Maven, no npm.

## Run

```bash
./run.sh          # compile + launch on http://127.0.0.1:8090
```

Windows: `run.bat` (or `./run.sh` inside Git Bash).

Change the port:

```bash
SIGNAL_PORT=8091 ./run.sh
```

Progress auto-saves to `save.json` — delete it (or click RESET in-game) to
start over.

## How to play

1. **POWER ON** the station.
2. Drag the dial, use ← → keys, or type an exact frequency to tune.
3. When the signal locks, read its pattern in the DECODE panel or press
   **PLAY AUDIO** to hear real Morse.
4. Transcribe what you hear into the input (letters), press **DECODE**.
   The Morse chart button in the header is your cheat sheet.
5. Progress through the six levels:

| # | Level | What you do |
|---|-------|-------------|
| 1 | FIRST CONTACT | Tune in and decode the first message |
| 2 | DISTRESS | Narrow the filter, decode both overlapping signals, answer one |
| 3 | TRIANGULATION | Acquire the source, take 3 bearings, mark it on the map |
| 4 | THE REPLY | It echoes your words back corrupted — correct each echo |
| 5 | ECHOES | Four mirrors, one real source — find the stable one, answer it |
| 6 | FINALE | Decode the last transmission, then choose your ending |

## Architecture

```
server/src/com/freebuff/signal/   Java backend (pure JDK 21, com.sun.net.httpserver)
  Main            entry point; SIGNAL_PORT env, default 8090
  HttpServer      REST API routes + static serving of web/
  GameState       session state machine: levels, decodes, bearings, endings
  LevelData       the six levels: frequencies, messages, narrative
  Morse           text <-> morse codec, symbol corruption
  SignalGenerator seeded procedural bearings/source positions
  SaveStore       save.json persistence
  Json            tiny dependency-free JSON writer/parser

web/                              frontend (served by the Java server)
  index.html      CRT console layout
  style.css       phosphor theme: scanlines, glow, flicker
  js/main.js      game controller, level flows
  js/radio.js     dial / filter / antenna
  js/spectrum.js  waterfall + oscilloscope renderers
  js/morse.js     WebAudio Morse synthesis + chart
  js/map.js       triangulation map
  js/fx.js        static, screen shake
```

The client renders; the server judges. Decode answers and narrative truth
live server-side.

## API

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/session` | GET | current session (level, archive, ending) |
| `/api/level` | GET | current level briefing + state |
| `/api/tune` | POST `{frequency, bandwidth}` | what you hear at a frequency |
| `/api/decode` | POST `{text}` | submit a transcription |
| `/api/transmit` | POST `{message}` | key the transmitter / make choices |
| `/api/bearing` | POST `{angle}` | take an antenna bearing |
| `/api/mark` | POST `{x, y}` | mark the source on the map |
| `/api/advance` | POST | move to the next level |
| `/api/reset` | POST | fresh session |