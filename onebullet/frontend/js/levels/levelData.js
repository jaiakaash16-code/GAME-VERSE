// Level definitions for One Bullet - HARDER VERSION
const LEVEL_DATA = {
    // Level 1: Tutorial - tough moving enemy
    1: {
        name: "The Tutorial",
        description: "The enemy is fast and aggressive. Time your shot!",
        width: 800,
        height: 600,
        playerStart: { x: 100, y: 500 },
        enemies: [
            { x: 600, y: 300, health: 3, speed: 80, patrolDistance: 150, damage: 1 }
        ],
        objects: [],
        ropes: [],
        walls: [
            { x: 0, y: 580, width: 800, height: 20 },
            { x: 0, y: 0, width: 800, height: 20 },
            { x: 0, y: 0, width: 20, height: 600 },
            { x: 780, y: 0, width: 20, height: 600 }
        ],
        winCondition: 'kill_all'
    },

    // Level 2: Rope Drop - enemy is alert
    2: {
        name: "Rope Drop",
        description: "Enemy patrols below. Drop the rock or shoot it!",
        width: 800,
        height: 600,
        playerStart: { x: 100, y: 500 },
        enemies: [
            { x: 600, y: 500, health: 4, speed: 70, patrolDistance: 100, damage: 1 }
        ],
        objects: [
            { type: 'rock', x: 600, y: 150, mass: 3 }
        ],
        ropes: [
            { x1: 600, y1: 50, x2: 600, y2: 150, attachedObjectIndex: 0 }
        ],
        walls: [
            { x: 0, y: 580, width: 800, height: 20 },
            { x: 0, y: 0, width: 800, height: 20 },
            { x: 0, y: 0, width: 20, height: 600 },
            { x: 780, y: 0, width: 20, height: 600 },
            { x: 500, y: 400, width: 200, height: 20 }
        ],
        winCondition: 'kill_all'
    },

    // Level 3: Seesaw - enemy guards the platform
    3: {
        name: "Seesaw",
        description: "Enemy has the high ground and deals heavy damage!",
        width: 800,
        height: 600,
        playerStart: { x: 100, y: 500 },
        enemies: [
            { x: 600, y: 200, health: 3, speed: 40, patrolDistance: 60, damage: 2 }
        ],
        objects: [
            { type: 'ball', x: 200, y: 480, mass: 1.5, bouncy: true },
            { type: 'rock', x: 350, y: 450, mass: 2 }
        ],
        ropes: [],
        walls: [
            { x: 0, y: 580, width: 800, height: 20 },
            { x: 0, y: 0, width: 800, height: 20 },
            { x: 0, y: 0, width: 20, height: 600 },
            { x: 780, y: 0, width: 20, height: 600 },
            { x: 500, y: 300, width: 200, height: 20 },
            { x: 330, y: 500, width: 40, height: 10 }
        ],
        seesaw: { x: 350, y: 490, width: 120 },
        winCondition: 'kill_all'
    },

    // Level 4: Chain Reaction - multiple fast enemies
    4: {
        name: "Chain Reaction",
        description: "Three aggressive enemies. Chain the explosions!",
        width: 800,
        height: 600,
        playerStart: { x: 100, y: 500 },
        enemies: [
            { x: 350, y: 500, health: 3, speed: 90, patrolDistance: 120, damage: 1 },
            { x: 550, y: 500, health: 3, speed: 100, patrolDistance: 120, damage: 1 },
            { x: 700, y: 300, health: 4, speed: 80, patrolDistance: 100, damage: 2 }
        ],
        objects: [
            { type: 'barrel', x: 300, y: 500, explosive: true, explosionRadius: 130 },
            { type: 'barrel', x: 500, y: 500, explosive: true, explosionRadius: 130 },
            { type: 'crate', x: 650, y: 500 }
        ],
        ropes: [],
        walls: [
            { x: 0, y: 580, width: 800, height: 20 },
            { x: 0, y: 0, width: 800, height: 20 },
            { x: 0, y: 0, width: 20, height: 600 },
            { x: 780, y: 0, width: 20, height: 600 },
            { x: 600, y: 380, width: 180, height: 20 }
        ],
        winCondition: 'kill_all'
    },

    // Level 5: The Gauntlet - very fast, very aggressive
    5: {
        name: "The Gauntlet",
        description: "Four deadly enemies. One bullet. Survive!",
        width: 1000,
        height: 700,
        playerStart: { x: 100, y: 600 },
        enemies: [
            { x: 250, y: 600, health: 4, speed: 120, patrolDistance: 150, damage: 2 },
            { x: 450, y: 400, health: 5, speed: 110, patrolDistance: 120, damage: 2 },
            { x: 650, y: 600, health: 4, speed: 130, patrolDistance: 180, damage: 2 },
            { x: 850, y: 300, health: 6, speed: 100, patrolDistance: 100, damage: 3 }
        ],
        objects: [
            { type: 'barrel', x: 200, y: 600, explosive: true, explosionRadius: 140 },
            { type: 'rock', x: 400, y: 200, mass: 3 },
            { type: 'crate', x: 600, y: 600 },
            { type: 'ball', x: 800, y: 600, mass: 1.5, bouncy: true }
        ],
        ropes: [
            { x1: 400, y1: 100, x2: 400, y2: 200, attachedObjectIndex: 1 }
        ],
        walls: [
            { x: 0, y: 680, width: 1000, height: 20 },
            { x: 0, y: 0, width: 1000, height: 20 },
            { x: 0, y: 0, width: 20, height: 700 },
            { x: 980, y: 0, width: 20, height: 700 },
            { x: 150, y: 500, width: 150, height: 20 },
            { x: 350, y: 350, width: 200, height: 20 },
            { x: 550, y: 500, width: 150, height: 20 },
            { x: 750, y: 400, width: 150, height: 20 }
        ],
        seesaw: { x: 650, y: 650, width: 100 },
        winCondition: 'kill_all'
    }
};

// Level manager
class LevelManager {
    constructor() {
        this.currentLevel = 1;
        this.totalLevels = Object.keys(LEVEL_DATA).length;
    }
    
    getLevel(levelNumber) {
        return LEVEL_DATA[levelNumber] || null;
    }
    
    getLevelCount() {
        return this.totalLevels;
    }
    
    nextLevel() {
        if (this.currentLevel < this.totalLevels) {
            this.currentLevel++;
            return true;
        }
        return false;
    }
    
    setLevel(levelNumber) {
        if (LEVEL_DATA[levelNumber]) {
            this.currentLevel = levelNumber;
            return true;
        }
        return false;
    }
}
