class Player {
    constructor(scene, x, y) {
        this.scene = scene;
        this.sprite = scene.physics.add.sprite(x, y, 'player');
        this.sprite.setSize(24, 24);
        this.sprite.setCollideWorldBounds(true);
        this.sprite.setDrag(500);
        this.sprite.setMaxVelocity(200, 200);
        
        // Player stats
        this.health = 3;
        this.speed = 160;
        this.invulnerable = false;
        this.invulnerableTimer = 0;
        
        // Movement keys
        this.cursors = scene.input.keyboard.createCursorKeys();
        this.wasd = {
            up: scene.input.keyboard.addKey('W'),
            down: scene.input.keyboard.addKey('S'),
            left: scene.input.keyboard.addKey('A'),
            right: scene.input.keyboard.addKey('D')
        };
        
        // Aim direction
        this.aimAngle = 0;
    }
    
    update(time, delta) {
        // Handle invulnerability
        if (this.invulnerable) {
            this.invulnerableTimer -= delta;
            this.sprite.setAlpha(Math.sin(time * 0.01) * 0.5 + 0.5);
            if (this.invulnerableTimer <= 0) {
                this.invulnerable = false;
                this.sprite.setAlpha(1);
            }
        }
        
        // Movement
        const speed = this.speed;
        let vx = 0;
        let vy = 0;
        
        if (this.cursors.left.isDown || this.wasd.left.isDown) {
            vx = -speed;
        } else if (this.cursors.right.isDown || this.wasd.right.isDown) {
            vx = speed;
        }
        
        if (this.cursors.up.isDown || this.wasd.up.isDown) {
            vy = -speed;
        } else if (this.cursors.down.isDown || this.wasd.down.isDown) {
            vy = speed;
        }
        
        // Normalize diagonal movement
        if (vx !== 0 && vy !== 0) {
            vx *= 0.707;
            vy *= 0.707;
        }
        
        this.sprite.setVelocity(vx, vy);
        
        // Aim toward mouse
        const pointer = this.scene.input.activePointer;
        const worldPoint = this.scene.cameras.main.getWorldPoint(pointer.x, pointer.y);
        this.aimAngle = Phaser.Math.Angle.Between(
            this.sprite.x, this.sprite.y,
            worldPoint.x, worldPoint.y
        );
    }
    
    takeDamage(amount = 1) {
        if (this.invulnerable) return false;
        
        this.health -= amount;
        this.invulnerable = true;
        this.invulnerableTimer = 1000; // 1 second invulnerability
        
        // Flash red
        this.sprite.setTint(0xff0000);
        this.scene.time.delayedCall(100, () => {
            this.sprite.clearTint();
        });
        
        return this.health <= 0;
    }
    
    getPosition() {
        return { x: this.sprite.x, y: this.sprite.y };
    }
    
    getAimAngle() {
        return this.aimAngle;
    }
    
    destroy() {
        this.sprite.destroy();
    }
}
