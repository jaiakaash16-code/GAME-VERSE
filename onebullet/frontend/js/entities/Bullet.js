class Bullet {
    constructor(scene) {
        this.scene = scene;
        this.sprite = null;
        this.isFlying = false;
        this.isReturning = false;
        this.speed = 500;
        this.returnSpeed = 300;
        this.owner = null; // reference to player
        this.damage = 1;
        
        // Create bullet group for pooling
        this.group = scene.physics.add.group({
            defaultKey: 'bullet',
            maxSize: 1
        });
    }
    
    fire(x, y, angle) {
        if (this.isFlying || this.isReturning) return false;
        
        // Get bullet from pool or create new
        if (!this.sprite || !this.sprite.active) {
            this.sprite = this.group.get(x, y);
            if (!this.sprite) return false;
        }
        
        this.sprite.setPosition(x, y);
        this.sprite.setActive(true);
        this.sprite.setVisible(true);
        this.sprite.enableBody(true, x, y, true, true);
        
        // Set velocity based on angle
        this.sprite.setVelocity(
            Math.cos(angle) * this.speed,
            Math.sin(angle) * this.speed
        );
        
        // Rotate bullet to face direction
        this.sprite.setRotation(angle);
        
        this.isFlying = true;
        this.isReturning = false;
        this.flyTimer = 0;
        this.maxFlyTime = 2000; // 2 seconds max flight
        
        // Add trail effect
        this.scene.tweens.add({
            targets: this.sprite,
            alpha: 0.6,
            duration: 50,
            yoyo: true
        });
        
        return true;
    }
    
    update(time, delta) {
        if (!this.sprite || !this.sprite.active) return;
        
        if (this.isFlying) {
            this.flyTimer += delta;
            
            // Create trail particles
            if (this.flyTimer % 3 < delta) {
                this.createTrail();
            }
            
            // Return if max flight time reached
            if (this.flyTimer >= this.maxFlyTime) {
                this.startReturn();
            }
        }
        
        if (this.isReturning) {
            const player = this.scene.player;
            if (!player) return;
            
            const dist = Phaser.Math.Distance.Between(
                this.sprite.x, this.sprite.y,
                player.sprite.x, player.sprite.y
            );
            
            // Move toward player
            const angle = Phaser.Math.Angle.Between(
                this.sprite.x, this.sprite.y,
                player.sprite.x, player.sprite.y
            );
            
            this.sprite.setVelocity(
                Math.cos(angle) * this.returnSpeed,
                Math.sin(angle) * this.returnSpeed
            );
            
            // Rotate to face player
            this.sprite.setRotation(angle);
            
            // Return when close enough
            if (dist < 20) {
                this.retrieve();
            }
        }
    }
    
    createTrail() {
        const particles = this.scene.add.circle(
            this.sprite.x,
            this.sprite.y,
            3,
            0x00ffff,
            0.8
        );
        
        this.scene.tweens.add({
            targets: particles,
            alpha: 0,
            scaleX: 0.1,
            scaleY: 0.1,
            duration: 300,
            onComplete: () => particles.destroy()
        });
    }
    
    hitWall() {
        // Bounce off wall, then return
        this.startReturn();
        
        // Impact effect
        this.scene.tweens.add({
            targets: this.sprite,
            alpha: 0.3,
            duration: 100,
            yoyo: true
        });
    }
    
    hitEnemy(enemy) {
        // Damage enemy
        const killed = enemy.takeDamage(this.damage);
        
        // Visual feedback
        this.scene.cameras.main.shake(100, 0.01);
        
        // Start return
        this.startReturn();
        
        return killed;
    }
    
    hitObject(object) {
        // Apply force to physics object
        if (object.sprite && object.sprite.body) {
            const angle = this.sprite.rotation;
            const force = 200;
            object.sprite.body.velocity.x += Math.cos(angle) * force;
            object.sprite.body.velocity.y += Math.sin(angle) * force;
        }
        
        // Start return
        this.startReturn();
    }
    
    startReturn() {
        this.isFlying = false;
        this.isReturning = true;
        
        // Slow down first
        if (this.sprite && this.sprite.active) {
            this.sprite.setVelocity(0, 0);
        }
    }
    
    retrieve() {
        this.isFlying = false;
        this.isReturning = false;
        
        if (this.sprite) {
            this.sprite.setActive(false);
            this.sprite.setVisible(false);
            this.sprite.disableBody(true, true);
        }
        
        // Notify scene that bullet is available
        if (this.scene.onBulletReturned) {
            this.scene.onBulletReturned();
        }
    }
    
    isActive() {
        return this.isFlying || this.isReturning;
    }
    
    getPosition() {
        if (this.sprite && this.sprite.active) {
            return { x: this.sprite.x, y: this.sprite.y };
        }
        return null;
    }
    
    destroy() {
        if (this.sprite) {
            this.sprite.destroy();
        }
        this.group.clear(true, true);
    }
}
