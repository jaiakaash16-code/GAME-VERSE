"""DON'T LOOK room generator service.

Procedurally generates room layouts (JSON) for Endless mode.
Called by the Java server via POST /generate-room.
"""
import json
import random

from flask import Flask, jsonify, request

app = Flask(__name__)


def flood_reachable(solid, w, h, sx, sy, tx, ty):
    if solid[sy][sx] or solid[ty][tx]:
        return False
    seen = {(sx, sy)}
    stack = [(sx, sy)]
    while stack:
        x, y = stack.pop()
        if (x, y) == (tx, ty):
            return True
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and not solid[ny][nx] and (nx, ny) not in seen:
                seen.add((nx, ny))
                stack.append((nx, ny))
    return False


def try_room(rng, w, h, difficulty):
    solid = [[False] * w for _ in range(h)]
    walls = []
    for x in range(w):
        solid[0][x] = solid[h - 1][x] = True
        walls.append([x, 0])
        walls.append([x, h - 1])
    for y in range(h):
        solid[y][0] = solid[y][w - 1] = True
        walls.append([0, y])
        walls.append([w - 1, y])

    furniture = []
    for _ in range(4 + difficulty * 2):
        fx = rng.randint(2, w - 3)
        fy = rng.randint(2, h - 3)
        solid[fy][fx] = True
        furniture.append([fx, fy])
        if rng.random() < 0.35 and fx + 1 < w - 1 and not solid[fy][fx + 1]:
            solid[fy][fx + 1] = True
            furniture.append([fx + 1, fy])

    ptx, pty = 1, h - 2
    etx, ety = w - 2, 1
    solid[pty][ptx] = False
    solid[ety][etx] = False

    candidates = []
    for y in range(2, h - 2):
        for x in range(2, w - 2):
            if solid[y][x] or (x, y) in ((ptx, pty), (etx, ety)):
                continue
            d = (x - ptx) ** 2 + (y - pty) ** 2
            max_d = (w - 3) ** 2 + (h - 3) ** 2
            if d > max_d * 0.45:
                candidates.append((x, y))
    if not candidates:
        return None
    mtx, mty = rng.choice(candidates)
    solid[mty][mtx] = False

    if not flood_reachable(solid, w, h, ptx, pty, etx, ety):
        return None

    return {
        "name": f"Generator Room #{rng.randint(1000, 9999)}",
        "width": w,
        "height": h,
        "tile": 40,
        "walls": walls,
        "furniture": furniture,
        "playerSpawn": [ptx, pty],
        "monsterSpawn": [mtx, mty],
        "exit": [etx, ety],
        "generated": True,
    }


def empty_room(w, h, label):
    walls = []
    solid = [[False] * w for _ in range(h)]
    for x in range(w):
        solid[0][x] = solid[h - 1][x] = True
        walls.append([x, 0])
        walls.append([x, h - 1])
    for y in range(h):
        solid[y][0] = solid[y][w - 1] = True
        walls.append([0, y])
        walls.append([w - 1, y])
    return {
        "name": label,
        "width": w,
        "height": h,
        "tile": 40,
        "walls": walls,
        "furniture": [],
        "playerSpawn": [1, h - 2],
        "monsterSpawn": [w - 3, 2],
        "exit": [w - 2, 1],
        "generated": True,
    }


def generate_room(seed, difficulty):
    rng = random.Random(seed)
    w = 16 + difficulty * 2
    h = 11 + difficulty
    for _ in range(80):
        room = try_room(rng, w, h, difficulty)
        if room:
            return room
    # last resort: empty room with borders
    return empty_room(w, h, "Generator Room #0")


@app.route("/generate-room", methods=["POST"])
def generate():
    data = request.get_json(silent=True) or {}
    try:
        seed = int(data.get("seed", 0))
        difficulty = int(data.get("difficulty", 1))
    except (TypeError, ValueError):
        seed, difficulty = 0, 1
    difficulty = max(1, min(difficulty, 12))
    return jsonify(generate_room(seed, difficulty))


@app.route("/health")
def health():
    return jsonify({"ok": True, "service": "dontlook-room-generator"})


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=8001, debug=False)