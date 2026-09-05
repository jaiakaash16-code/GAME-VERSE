class GameScene extends Phaser.Scene {
    constructor() {
        super({ key: 'GameScene' });
    }
    
    init(data) {
        this.levelNumber = data.level || 1;
        this.levelData = LEVEL_DATA[this.levelNumber];
    }
    
    create() {
        const level = this.levelData;
        
        // Set world bounds
        this.physics.world.setBounds(0, 0, level.width, level.height);
        this.cameras.main.setBounds(0, 0, level.width, level.height);
        
        // Create walls
        this.walls = this.physics.add.staticGroup();
        level.walls.forEach(wall => {
            const w = this.add.rectangle(
                wall.x + wall.width / 2,
                wall.y + wall.height / 2,
                wall.width,
                wall.height,
                0x333344
            );
            this.physics.add.existing(w, true);
            this.walls.add(w);
            
            // Add border effect
            const border = this.add.rectangle(
                wall.x + wall.width / 2,
                wall.y + wall.height / 2,
                wall.width,
                wall.height
            );
            border.setStrokeStyle(1, 0x444455);
            border.setFillStyle(0x000000, 0);
        });
        
        // Create player
        this.player = new Player(this, level.playerStart.x, level.playerStart.y);
        this.physics.add.collider(this.player.sprite, this.walls);
        
        // Create bullet
        this.bullet = new Bullet(this);
        this.bulletAvailable = true;
        
        // Create enemies
        this.enemies = [];
        level.enemies.forEach(enemyData => {
            const enemy = new Enemy(this, enemyData.x, enemyData.y, enemyData);
            this.physics.add.collider(enemy.sprite, this.walls);
            this.enemies.push(enemy);
        });
        
        // Create physics objects
        this.physicsObjects = [];
        level.objects.forEach(objData => {
            const obj = new PhysicsObject(this, objData.x, objData.y, objData.type, objData);
            this.physics.add.collider(obj.sprite, this.walls);
            this.physicsObjects.push(obj);
        });
        
        // Create ropes
        this.ropes = [];
        level.ropes.forEach(ropeData => {
            const attachedObj = ropeData.attachedObjectIndex !== undefined 
                ? this.physicsObjects[ropeData.attachedObjectIndex] 
                : null;
            const rope = new Rope(this, ropeData.x1, ropeData.y1, ropeData.x2, ropeData.y2, attachedObj);
            this.ropes.push(rope);
        });
        
        // Create seesaw if present
        if (level.seesaw) {
            this.createSeesaw(level.seesaw);
        }
        
        // Setup collisions
        this.setupCollisions();
        
        // Input
        this.input.on('pointerdown', this.shoot, this);
        this.input.keyboard.on('keydown-R', this.restartLevel, this);
        
        // UI
        this.createUI();
        
        // Game state
        this.levelComplete = false;
        this.levelFailed = false;
        this.startTime = this.time.now;
        this.shotsFired = 0;
        
        // Fade in
        this.cameras.main.fadeIn(500);
        
        // Show level name
        this.showLevelName();
    }
    
    createSeesaw(seesawData) {
        // Create seesaw as a physics object
        const seesaw = this.add.rectangle(
            seesawData.x,
            seesawData.y,
            seesawData.width,
            10,
            0x8B4513
        );
        this.physics.add.existing(seesaw);
        seesaw.body.setImmovable(false);
        seesaw.body.setAllowGravity(true);
        
        // Add pivot point visual
        const pivot = this.add.triangle(
            seesawData.x,
            seesawData.y + 15,
            0, 20,
            10, 0,
            20, 20,
            0x666666
        );
        
        this.seesaw = seesaw;
        this.seesawPivot = pivot;
        
        // Add collision with walls
        this.physics.add.collider(seesaw, this.walls);
    }
    
    setupCollisions() {
        // We'll add bullet collisions dynamically when bullet is fired
        // This prevents errors when bullet sprite doesn't exist yet
        
        // Player vs enemies (damage)
        this.enemies.forEach(enemy => {
            this.physics.add.overlap(this.player.sprite, enemy.sprite, () => {
                if (enemy.isActive()) {
                    const dead = this.player.takeDamage(enemy.damage);
                    if (dead) {
                        this.onPlayerDeath();
                    }
                }
            });
        });
        
        // Player vs physics objects
        this.physicsObjects.forEach(obj => {
            if (obj.sprite && obj.sprite.active) {
                this.physics.add.collider(this.player.sprite, obj.sprite);
            }
        });
        
        // Physics objects vs walls
        this.physicsObjects.forEach(obj => {
            if (obj.sprite && obj.sprite.active) {
                this.physics.add.collider(obj.sprite, this.walls);
            }
        });
        
        // Physics objects vs each other
        for (let i = 0; i < this.physicsObjects.length; i++) {
            for (let j = i + 1; j < this.physicsObjects.length; j++) {
                if (this.physicsObjects[i].sprite && this.physicsObjects[i].sprite.active &&
                    this.physicsObjects[j].sprite && this.physicsObjects[j].sprite.active) {
                    this.physics.add.collider(
                        this.physicsObjects[i].sprite,
                        this.physicsObjects[j].sprite
                    );
                }
            }
        }
    }
    
    createUI() {
        // Level info
        this.add.text(10, 10, `Level ${this.levelNumber}`, {
            font: 'bold 20px monospace',
            fill: '#00ffff'
        });
        
        // Bullet indicator
        this.bulletIndicator = this.add.text(10, 40, '🔫 ●', {
            font: '18px monospace',
            fill: '#ffff00'
        });
        
        // Health display
        this.healthText = this.add.text(10, 70, '♥♥♥', {
            font: '18px monospace',
            fill: '#ff4444'
        });
        
        // Enemies remaining
        this.enemyCount = this.enemies.length;
        this.enemyText = this.add.text(this.cameras.main.width - 10, 10, `Enemies: ${this.enemyCount}`, {
            font: '16px monospace',
            fill: '#ff4444'
        }).setOrigin(1, 0);
        
        // Timer
        this.timerText = this.add.text(this.cameras.main.width - 10, 35, 'Time: 0.0s', {
            font: '14px monospace',
            fill: '#888888'
        }).setOrigin(1, 0);
        
        // Level description
        this.add.text(this.cameras.main.width / 2, this.cameras.main.height - 20, 
            this.levelData.description, {
            font: '14px monospace',
            fill: '#666666'
        }).setOrigin(0.5, 1);
    }
    
    update(time, delta) {
        if (this.levelComplete || this.levelFailed) return;
        
        // Update player
        this.player.update(time, delta);
        
        // Update bullet
        this.bullet.update(time, delta);
        
        // Update enemies
        this.enemies.forEach(enemy => enemy.update(time, delta));
        
        // Update physics objects
        this.physicsObjects.forEach(obj => {
            if (obj.isActive()) obj.update(time, delta);
        });
        
        // Update ropes
        this.ropes.forEach(rope => {
            if (rope.isActive()) rope.update();
        });
        
        // Check rope collisions with bullet
        this.checkRopeCollisions();
        
        // Update UI
        this.updateUI(time);
        
        // Check win condition
        this.checkWinCondition();
    }
    
    shoot(pointer) {
        if (!this.bulletAvailable || this.bullet.isActive()) return;
        
        const playerPos = this.player.getPosition();
        const angle = this.player.getAimAngle();
        
        // Fire bullet
        if (this.bullet.fire(playerPos.x, playerPos.y, angle)) {
            this.bulletAvailable = false;
            this.shotsFired++;
            
            // Add bullet collisions dynamically
            this.setupBulletCollisions();
            
            // Update bullet indicator
            this.bulletIndicator.setText('🔫 ○');
            this.bulletIndicator.setColor('#666666');
            
            // Muzzle flash effect
            this.createMuzzleFlash(playerPos.x, playerPos.y, angle);
        }
    }
    
    setupBulletCollisions() {
        if (!this.bullet.sprite || !this.bullet.sprite.active) return;
        
        // Bullet vs walls
        this.physics.add.collider(this.bullet.sprite, this.walls, () => {
            this.bullet.hitWall();
        });
        
        // Bullet vs enemies
        this.enemies.forEach(enemy => {
            this.physics.add.overlap(this.bullet.sprite, enemy.sprite, () => {
                if (this.bullet.isFlying && enemy.isActive()) {
                    const killed = this.bullet.hitEnemy(enemy);
                    if (killed) {
                        this.onEnemyKilled(enemy);
                    }
                }
            });
        });
        
        // Bullet vs physics objects
        this.physicsObjects.forEach(obj => {
            this.physics.add.overlap(this.bullet.sprite, obj.sprite, () => {
                if (this.bullet.isFlying && obj.isActive()) {
                    this.bullet.hitObject(obj);
                    const destroyed = obj.onHit();
                    if (destroyed) {
                        this.checkRopesForObject(obj);
                    }
                }
            });
        });
    }
    
    createMuzzleFlash(x, y, angle) {
        const flash = this.add.circle(
            x + Math.cos(angle) * 15,
            y + Math.sin(angle) * 15,
            8,
            0xffff00,
            1
        );
        
        this.tweens.add({
            targets: flash,
            alpha: 0,
            scaleX: 2,
            scaleY: 2,
            duration: 100,
            onComplete: () => flash.destroy()
        });
    }
    
    checkRopeCollisions() {
        if (!this.bullet.isFlying) return;
        
        const bulletPos = this.bullet.getPosition();
        if (!bulletPos) return;
        
        this.ropes.forEach(rope => {
            if (!rope.isActive()) return;
            
            // Simple distance check to rope line
            const dist = this.distanceToLine(
                bulletPos.x, bulletPos.y,
                rope.x1, rope.y1,
                rope.x2, rope.y2
            );
            
            if (dist < 10) {
                rope.break();
                this.bullet.hitWall();
            }
        });
    }
    
    distanceToLine(px, py, x1, y1, x2, y2) {
        const A = px - x1;
        const B = py - y1;
        const C = x2 - x1;
        const D = y2 - y1;
        
        const dot = A * C + B * D;
        const lenSq = C * C + D * D;
        let param = -1;
        
        if (lenSq !== 0) param = dot / lenSq;
        
        let xx, yy;
        
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        const dx = px - xx;
        const dy = py - yy;
        
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    checkRopesForObject(destroyedObj) {
        this.ropes.forEach(rope => {
            if (rope.attachedObject === destroyedObj) {
                rope.break();
            }
        });
    }
    
    onBulletReturned() {
        this.bulletAvailable = true;
        this.bulletIndicator.setText('🔫 ●');
        this.bulletIndicator.setColor('#ffff00');
    }
    
    onEnemyKilled(enemy) {
        this.enemyCount--;
        this.enemyText.setText(`Enemies: ${this.enemyCount}`);
        
        // Score popup
        const pos = enemy.getPosition();
        const scoreText = this.add.text(pos.x, pos.y - 20, '+100', {
            font: 'bold 16px monospace',
            fill: '#00ff00'
        }).setOrigin(0.5);
        
        this.tweens.add({
            targets: scoreText,
            y: pos.y - 50,
            alpha: 0,
            duration: 800,
            onComplete: () => scoreText.destroy()
        });
    }
    
    checkWinCondition() {
        if (this.levelData.winCondition === 'kill_all') {
            const allDead = this.enemies.every(e => !e.isActive());
            if (allDead && !this.levelComplete) {
                this.onLevelComplete();
            }
        }
    }
    
    onLevelComplete() {
        this.levelComplete = true;
        
        // Calculate score
        const timeElapsed = this.time.now - this.startTime;
        const timeBonus = Math.max(0, 10000 - Math.floor(timeElapsed / 100));
        const enemyScore = this.enemies.length * 100;
        const totalScore = enemyScore + timeBonus;
        
        // Unlock next level
        const nextLevel = this.levelNumber + 1;
        if (!this.game.state.unlockedLevels.includes(nextLevel) && nextLevel <= 5) {
            this.game.state.unlockedLevels.push(nextLevel);
        }
        
        // Update total score
        this.game.state.totalScore += totalScore;
        
        // Save state
        localStorage.setItem('onebullet_state', JSON.stringify(this.game.state));
        
        // Save to backend
        GameAPI.saveProgress(this.game.state.playerId, this.levelNumber, true, totalScore);
        GameAPI.submitScore(this.game.state.playerId, this.levelNumber, totalScore, timeElapsed, this.shotsFired);
        
        // Victory effect
        this.cameras.main.flash(500, 0, 255, 255);
        
        // Show victory text
        const victoryText = this.add.text(
            this.cameras.main.width / 2,
            this.cameras.main.height / 2,
            'LEVEL COMPLETE!',
            {
                font: 'bold 48px monospace',
                fill: '#00ff00',
                stroke: '#004400',
                strokeThickness: 4
            }
        ).setOrigin(0.5).setScrollFactor(0);
        
        // Score breakdown
        const scoreBreakdown = this.add.text(
            this.cameras.main.width / 2,
            this.cameras.main.height / 2 + 60,
            [
                `Enemies: ${enemyScore}`,
                `Time Bonus: ${timeBonus}`,
                `Total: ${totalScore}`
            ].join('\n'),
            {
                font: '18px monospace',
                fill: '#ffffff',
                align: 'center'
            }
        ).setOrigin(0.5).setScrollFactor(0);
        
        // Continue button
        this.time.delayedCall(1500, () => {
            const continueText = this.add.text(
                this.cameras.main.width / 2,
                this.cameras.main.height / 2 + 140,
                'Click to continue',
                {
                    font: '16px monospace',
                    fill: '#00ffff'
                }
            ).setOrigin(0.5).setScrollFactor(0);
            
            this.tweens.add({
                targets: continueText,
                alpha: 0.5,
                duration: 500,
                yoyo: true,
                repeat: -1
            });
            
            this.input.once('pointerdown', () => {
                this.scene.start('WinScene', {
                    level: this.levelNumber,
                    score: totalScore,
                    time: timeElapsed,
                    bullets: this.shotsFired
                });
            });
        });
    }
    
    onPlayerDeath() {
        if (this.levelFailed) return;
        
        this.levelFailed = true;
        
        // Death effect
        this.cameras.main.shake(300, 0.05);
        this.cameras.main.flash(500, 255, 0, 0);
        
        // Death text
        const deathText = this.add.text(
            this.cameras.main.width / 2,
            this.cameras.main.height / 2,
            'YOU DIED',
            {
                font: 'bold 48px monospace',
                fill: '#ff0000',
                stroke: '#440000',
                strokeThickness: 4
            }
        ).setOrigin(0.5).setScrollFactor(0);
        
        // Retry button
        this.time.delayedCall(1000, () => {
            const retryText = this.add.text(
                this.cameras.main.width / 2,
                this.cameras.main.height / 2 + 60,
                'Click to retry (or press R)',
                {
                    font: '16px monospace',
                    fill: '#ffff00'
                }
            ).setOrigin(0.5).setScrollFactor(0);
            
            this.tweens.add({
                targets: retryText,
                alpha: 0.5,
                duration: 500,
                yoyo: true,
                repeat: -1
            });
            
            this.input.once('pointerdown', () => {
                this.restartLevel();
            });
        });
    }
    
    restartLevel() {
        this.scene.restart({ level: this.levelNumber });
    }
    
    updateUI(time) {
        // Update timer
        const elapsed = ((time - this.startTime) / 1000).toFixed(1);
        this.timerText.setText(`Time: ${elapsed}s`);
        
        // Update health
        const health = this.player.health;
        this.healthText.setText('♥'.repeat(Math.max(0, health)));
        
        // Update bullet indicator color based on availability
        if (!this.bulletAvailable && !this.bullet.isActive()) {
            // Bullet was returned
            this.onBulletReturned();
        }
    }
    
    showLevelName() {
        const nameText = this.add.text(
            this.cameras.main.width / 2,
            this.cameras.main.height / 2 - 50,
            this.levelData.name,
            {
                font: 'bold 36px monospace',
                fill: '#00ffff',
                stroke: '#004444',
                strokeThickness: 3
            }
        ).setOrigin(0.5).setScrollFactor(0).setAlpha(0);
        
        this.tweens.add({
            targets: nameText,
            alpha: 1,
            duration: 500,
            yoyo: true,
            hold: 1000,
            onComplete: () => nameText.destroy()
        });
    }
}
