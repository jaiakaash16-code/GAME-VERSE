class Enemy {
    constructor(scene, x, y, config = {}) {
        this.scene = scene;
        this.sprite = scene.physics.add.sprite(x, y, 'enemy');
        this.sprite.setSize(24, 24);
        this.sprite.setCollideWorldBounds(true);
        
        // Enemy stats - TOUGH
        this.health = config.health || 3;
        this.maxHealth = this.health;
        this.speed = config.speed || 80;
        this.damage = config.damage || 1;
        this.patrolDistance = config.patrolDistance || 120;
        
        // Patrol state
        this.startX = x;
        this.startY = y;
        this.patrolDirection = 1;
        this.state = 'patrol';
        
        // Movement variation - UNPREDICTABLE
        this.speedMultiplier = 1;
        this.directionChangeTimer = 0;
        this.directionChangeInterval = 1500 + Math.random() * 1500;
        this.erraticFactor = 0.4;
        
        // Alert state
        this.alertTimer = 0;
        this.alertDuration = 2000;
        
        // Chase state - AGGRESSIVE
        this.chaseSpeed = this.speed * 2; // Very fast when chasing
        this.chaseRange = 300; // Large detection range
        this.loseRange = 400; // Hard to shake off
        
        // Strafe behavior when close
        this.strafeDirection = 1;
        this.strafeTimer = 0;
        
        // Set color
        this.updateColor();
        
        // Health bar
        this.createHealthBar();
    }
    
    createHealthBar() {
        this.healthBarBg = this.scene.add.rectangle(
            this.sprite.x, this.sprite.y - 22,
            32, 5,
            0x333333
        ).setOrigin(0.5);
        
        this.healthBar = this.scene.add.rectangle(
            this.sprite.x - 16, this.sprite.y - 22,
            32, 5,
            0x00ff00
        ).setOrigin(0, 0.5);
    }
    
    updateHealthBar() {
        if (!this.healthBar || !this.healthBarBg || this.state === 'dead') return;
        
        this.healthBarBg.setPosition(this.sprite.x, this.sprite.y - 22);
        this.healthBar.setPosition(this.sprite.x - 16, this.sprite.y - 22);
        
        const healthPercent = this.health / this.maxHealth;
        this.healthBar.width = 32 * healthPercent;
        
        if (healthPercent > 0.6) {
            this.healthBar.fillColor = 0x00ff00;
        } else if (healthPercent > 0.3) {
            this.healthBar.fillColor = 0xffff00;
        } else {
            this.healthBar.fillColor = 0xff0000;
        }
    }
    
    update(time, delta) {
        if (this.state === 'dead') return;
        
        this.updateHealthBar();
        
        // Random direction changes - ERRATIC
        this.directionChangeTimer += delta;
        if (this.directionChangeTimer >= this.directionChangeInterval) {
            this.directionChangeTimer = 0;
            this.directionChangeInterval = 1000 + Math.random() * 2000;
            this.speedMultiplier = 0.7 + Math.random() * 0.6; // 0.7 to 1.3
            this.strafeDirection *= -1; // Change strafe direction
        }
        
        if (this.state === 'patrol') {
            this.updatePatrol(time, delta);
        } else if (this.state === 'alert') {
            this.updateAlert(time, delta);
        } else if (this.state === 'chasing') {
            this.updateChasing(time, delta);
        }
        
        this.checkForPlayer();
    }
    
    updatePatrol(time, delta) {
        const distFromStart = this.sprite.x - this.startX;
        
        if (distFromStart > this.patrolDistance) {
            this.patrolDirection = -1;
        } else if (distFromStart < -this.patrolDistance) {
            this.patrolDirection = 1;
        }
        
        // Erratic movement
        const currentSpeed = this.speed * this.speedMultiplier;
        this.sprite.setVelocityX(this.speed * this.patrolDirection * this.speedMultiplier);
        
        // Random vertical movement
        const verticalWobble = Math.sin(time * 0.003) * 20 * this.speedMultiplier;
        this.sprite.setVelocityY(verticalWobble);
        
        this.sprite.setFlipX(this.patrolDirection < 0);
    }
    
    updateAlert(time, delta) {
        this.alertTimer += delta;
        
        if (this.alertTimer >= this.alertDuration) {
            this.state = 'chasing';
            this.sprite.setTint(0xff6600);
            return;
        }
        
        // Quick approach
        this.moveTowardPlayer(this.speed * 1.5);
    }
    
    updateChasing(time, delta) {
        if (!this.scene.player) return;
        
        const player = this.scene.player;
        const dist = Phaser.Math.Distance.Between(
            this.sprite.x, this.sprite.y,
            player.sprite.x, player.sprite.y
        );
        
        // Lose interest only if very far
        if (dist > this.loseRange) {
            this.state = 'patrol';
            this.sprite.setTint(0xff4444);
            return;
        }
        
        // Strafe when close - harder to hit
        if (dist < 100) {
            this.strafe(delta);
        } else {
            // Chase directly
            this.moveTowardPlayer(this.chaseSpeed);
        }
    }
    
    strafe(delta) {
        if (!this.scene.player) return;
        
        const player = this.scene.player;
        const angle = Phaser.Math.Angle.Between(
            this.sprite.x, this.sprite.y,
            player.sprite.x, player.sprite.y
        );
        
        // Move perpendicular to player - strafing
        const strafeAngle = angle + (Math.PI / 2) * this.strafeDirection;
        const strafeSpeed = this.chaseSpeed * 0.8;
        
        this.sprite.setVelocity(
            Math.cos(strafeAngle) * strafeSpeed,
            Math.sin(strafeAngle) * strafeSpeed
        );
        
        this.sprite.setFlipX(Math.cos(strafeAngle) < 0);
    }
    
    moveTowardPlayer(speed) {
        if (!this.scene.player) return;
        
        const player = this.scene.player;
        const dist = Phaser.Math.Distance.Between(
            this.sprite.x, this.sprite.y,
            player.sprite.x, player.sprite.y
        );
        
        if (dist > 5) {
            const angle = Phaser.Math.Angle.Between(
                this.sprite.x, this.sprite.y,
                player.sprite.x,
                player.sprite.y
            );
            
            this.sprite.setVelocity(
                Math.cos(angle) * speed,
                Math.sin(angle) * speed
            );
            
            this.sprite.setFlipX(Math.cos(angle) < 0);
        }
    }
    
    checkForPlayer() {
        if (!this.scene.player || this.state === 'dead') return;
        
        const player = this.scene.player;
        const dist = Phaser.Math.Distance.Between(
            this.sprite.x, this.sprite.y,
            player.sprite.x, player.sprite.y
        );
        
        if (dist < this.chaseRange) {
            if (this.state === 'patrol') {
                this.goAlert();
            } else if (this.state === 'alert' && dist < 200) {
                this.state = 'chasing';
                this.sprite.setTint(0xff6600);
            }
        }
    }
    
    goAlert() {
        this.state = 'alert';
        this.alertTimer = 0;
        this.sprite.setTint(0xffff00);
        
        // Alert indicator
        const alertText = this.scene.add.text(
            this.sprite.x, this.sprite.y - 35,
            '!!',
            {
                font: 'bold 28px monospace',
                fill: '#ff0000',
                stroke: '#000000',
                strokeThickness: 3
            }
        ).setOrigin(0.5);
        
        this.scene.tweens.add({
            targets: alertText,
            y: alertText.y - 25,
            alpha: 0,
            scaleX: 1.5,
            scaleY: 1.5,
            duration: 600,
            onComplete: () => alertText.destroy()
        });
    }
    
    takeDamage(amount) {
        if (this.state === 'dead') return false;
        
        this.health -= amount;
        
        // Flash
        this.sprite.setTint(0xffffff);
        this.scene.time.delayedCall(80, () => {
            if (this.state !== 'dead') {
                this.updateColor();
            }
        });
        
        // Strong knockback
        if (this.scene.bullet && this.scene.bullet.sprite) {
            const angle = Phaser.Math.Angle.Between(
                this.scene.bullet.sprite.x, this.scene.bullet.sprite.y,
                this.sprite.x, this.sprite.y
            );
            this.sprite.setVelocity(
                Math.cos(angle) * 300,
                Math.sin(angle) * 300
            );
        }
        
        // Become more aggressive when hit
        if (this.state === 'patrol') {
            this.goAlert();
        } else if (this.state === 'alert') {
            this.state = 'chasing';
            this.sprite.setTint(0xff6600);
        }
        
        if (this.health <= 0) {
            this.die();
            return true;
        }
        
        return false;
    }
    
    die() {
        this.state = 'dead';
        this.sprite.setVelocity(0, 0);
        
        // Hide health bar
        if (this.healthBar) this.healthBar.setVisible(false);
        if (this.healthBarBg) this.healthBarBg.setVisible(false);
        
        // Dramatic death
        this.scene.tweens.add({
            targets: this.sprite,
            alpha: 0,
            scaleX: 2,
            scaleY: 2,
            angle: 360,
            duration: 400,
            onComplete: () => {
                this.spawnDeathParticles();
                this.sprite.destroy();
            }
        });
    }
    
    spawnDeathParticles() {
        const colors = [0xff0000, 0xff4444, 0xff8888, 0xffaaaa, 0xffffff];
        for (let i = 0; i < 15; i++) {
            const particle = this.scene.add.circle(
                this.sprite.x,
                this.sprite.y,
                3 + Math.random() * 4,
                colors[i % colors.length],
                1
            );
            
            const angle = (Math.PI * 2 / 15) * i + Math.random() * 0.5;
            const speed = 60 + Math.random() * 100;
            
            this.scene.tweens.add({
                targets: particle,
                x: particle.x + Math.cos(angle) * speed,
                y: particle.y + Math.sin(angle) * speed,
                alpha: 0,
                scaleX: 0.1,
                scaleY: 0.1,
                duration: 300 + Math.random() * 300,
                onComplete: () => particle.destroy()
            });
        }
    }
    
    updateColor() {
        const healthPercent = this.health / this.maxHealth;
        if (healthPercent > 0.6) {
            this.sprite.setTint(0xff4444);
        } else if (healthPercent > 0.3) {
            this.sprite.setTint(0xff2222);
        } else {
            this.sprite.setTint(0xcc0000);
        }
    }
    
    isActive() {
        return this.state !== 'dead';
    }
    
    getPosition() {
        return { x: this.sprite.x, y: this.sprite.y };
    }
    
    destroy() {
        if (this.sprite) this.sprite.destroy();
        if (this.healthBar) this.healthBar.destroy();
        if (this.healthBarBg) this.healthBarBg.destroy();
    }
}
