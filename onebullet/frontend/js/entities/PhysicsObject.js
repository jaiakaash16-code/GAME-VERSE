class PhysicsObject {
    constructor(scene, x, y, type = 'crate', config = {}) {
        this.scene = scene;
        this.type = type;
        this.sprite = null;
        
        // Object properties based on type
        this.config = {
            crate: {
                color: 0x8B4513,
                width: 32,
                height: 32,
                mass: 1,
                breakable: false
            },
            barrel: {
                color: 0x228B22,
                width: 28,
                height: 28,
                mass: 0.8,
                breakable: true,
                explosive: true,
                explosionRadius: 80,
                explosionDamage: 2
            },
            ball: {
                color: 0x4169E1,
                width: 24,
                height: 24,
                mass: 1.5,
                breakable: false,
                bouncy: true
            },
            rock: {
                color: 0x808080,
                width: 40,
                height: 40,
                mass: 2,
                breakable: false
            },
            box: {
                color: 0xD2691E,
                width: 28,
                height: 28,
                mass: 0.5,
                breakable: true
            }
        };
        
        const objConfig = this.config[type] || this.config.crate;
        Object.assign(this.config, objConfig, config);
        
        this.createSprite(x, y);
    }
    
    createSprite(x, y) {
        // Create colored rectangle based on type
        const graphics = this.scene.add.graphics();
        graphics.fillStyle(this.config.color, 1);
        
        if (this.type === 'barrel') {
            // Draw circle for barrel
            graphics.fillCircle(
                this.config.width / 2,
                this.config.height / 2,
                this.config.width / 2
            );
        } else {
            // Draw rectangle for other objects
            graphics.fillRect(0, 0, this.config.width, this.config.height);
        }
        
        // Generate texture
        const textureKey = 'obj_' + this.type + '_' + Date.now();
        graphics.generateTexture(textureKey, this.config.width, this.config.height);
        graphics.destroy();
        
        // Create physics sprite
        this.sprite = this.scene.physics.add.sprite(x, y, textureKey);
        this.sprite.setSize(this.config.width, this.config.height);
        this.sprite.setCollideWorldBounds(true);
        this.sprite.setBounce(this.config.bouncy ? 0.8 : 0.2);
        this.sprite.setDrag(50);
        this.sprite.setDepth(1);
        
        // Set mass
        this.sprite.body.mass = this.config.mass;
    }
    
    update(time, delta) {
        // Physics objects don't need much update logic
        // They're handled by Phaser's physics engine
    }
    
    onHit() {
        if (this.config.explosive) {
            this.explode();
            return true;
        }
        
        if (this.config.breakable) {
            this.break();
            return true;
        }
        
        return false;
    }
    
    explode() {
        const x = this.sprite.x;
        const y = this.sprite.y;
        const radius = this.config.explosionRadius;
        
        // Create explosion effect
        this.createExplosionEffect(x, y, radius);
        
        // Damage nearby enemies
        if (this.scene.enemies) {
            this.scene.enemies.forEach(enemy => {
                if (enemy.isActive()) {
                    const dist = Phaser.Math.Distance.Between(x, y, enemy.sprite.x, enemy.sprite.y);
                    if (dist < radius) {
                        const damage = Math.ceil(this.config.explosionDamage * (1 - dist / radius));
                        enemy.takeDamage(damage);
                    }
                }
            });
        }
        
        // Apply force to nearby physics objects
        if (this.scene.physicsObjects) {
            this.scene.physicsObjects.forEach(obj => {
                if (obj !== this && obj.sprite && obj.sprite.active) {
                    const dist = Phaser.Math.Distance.Between(x, y, obj.sprite.x, obj.sprite.y);
                    if (dist < radius) {
                        const angle = Phaser.Math.Angle.Between(x, y, obj.sprite.x, obj.sprite.y);
                        const force = 300 * (1 - dist / radius);
                        obj.sprite.body.velocity.x += Math.cos(angle) * force;
                        obj.sprite.body.velocity.y += Math.sin(angle) * force;
                    }
                }
            });
        }
        
        // Camera shake
        this.scene.cameras.main.shake(200, 0.02);
        
        // Destroy barrel
        this.sprite.destroy();
    }
    
    createExplosionEffect(x, y, radius) {
        // Flash
        const flash = this.scene.add.circle(x, y, radius, 0xffff00, 0.8);
        this.scene.tweens.add({
            targets: flash,
            alpha: 0,
            scaleX: 1.5,
            scaleY: 1.5,
            duration: 200,
            onComplete: () => flash.destroy()
        });
        
        // Particles
        const colors = [0xff4400, 0xff8800, 0xffcc00, 0xffffff];
        for (let i = 0; i < 20; i++) {
            const particle = this.scene.add.circle(
                x, y,
                3 + Math.random() * 5,
                colors[i % colors.length],
                1
            );
            
            const angle = Math.random() * Math.PI * 2;
            const speed = 50 + Math.random() * 150;
            
            this.scene.tweens.add({
                targets: particle,
                x: particle.x + Math.cos(angle) * speed,
                y: particle.y + Math.sin(angle) * speed,
                alpha: 0,
                scaleX: 0.1,
                scaleY: 0.1,
                duration: 300 + Math.random() * 200,
                onComplete: () => particle.destroy()
            });
        }
        
        // Smoke rings
        for (let i = 0; i < 3; i++) {
            this.scene.time.delayedCall(i * 100, () => {
                const ring = this.scene.add.circle(x, y, 20, 0x444444, 0.6);
                this.scene.tweens.add({
                    targets: ring,
                    scaleX: 3,
                    scaleY: 3,
                    alpha: 0,
                    duration: 400,
                    onComplete: () => ring.destroy()
                });
            });
        }
    }
    
    break() {
        const x = this.sprite.x;
        const y = this.sprite.y;
        
        // Create break effect
        const colors = [this.config.color, 0xffffff, 0x888888];
        for (let i = 0; i < 6; i++) {
            const fragment = this.scene.add.rectangle(
                x, y,
                8, 8,
                colors[i % colors.length],
                1
            );
            
            const angle = (Math.PI * 2 / 6) * i;
            const speed = 50 + Math.random() * 50;
            
            this.scene.tweens.add({
                targets: fragment,
                x: fragment.x + Math.cos(angle) * speed,
                y: fragment.y + Math.sin(angle) * speed,
                rotation: Math.random() * Math.PI,
                alpha: 0,
                duration: 400,
                onComplete: () => fragment.destroy()
            });
        }
        
        // Destroy object
        this.sprite.destroy();
    }
    
    isActive() {
        return this.sprite && this.sprite.active;
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
    }
}

// Rope class for breakable connections
class Rope {
    constructor(scene, x1, y1, x2, y2, attachedObject = null) {
        this.scene = scene;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.attachedObject = attachedObject;
        this.health = 1;
        this.broken = false;
        
        // Draw rope
        this.graphics = scene.add.graphics();
        this.draw();
    }
    
    draw() {
        this.graphics.clear();
        this.graphics.lineStyle(3, 0x8B4513, 1);
        this.graphics.beginPath();
        this.graphics.moveTo(this.x1, this.y1);
        this.graphics.lineTo(this.x2, this.y2);
        this.graphics.strokePath();
    }
    
    update() {
        // Update rope position if attached object moves
        if (this.attachedObject && this.attachedObject.sprite && this.attachedObject.sprite.active) {
            this.x2 = this.attachedObject.sprite.x;
            this.y2 = this.attachedObject.sprite.y;
            this.draw();
        }
    }
    
    hit() {
        this.health--;
        if (this.health <= 0) {
            this.break();
            return true;
        }
        return false;
    }
    
    break() {
        this.broken = true;
        
        // Break effect
        const midX = (this.x1 + this.x2) / 2;
        const midY = (this.y1 + this.y2) / 2;
        
        // Snap particles
        for (let i = 0; i < 5; i++) {
            const particle = this.scene.add.circle(
                midX, midY,
                2,
                0x8B4513,
                1
            );
            
            this.scene.tweens.add({
                targets: particle,
                y: particle.y + 20 + Math.random() * 20,
                alpha: 0,
                duration: 300,
                onComplete: () => particle.destroy()
            });
        }
        
        // Release attached object
        if (this.attachedObject && this.attachedObject.sprite) {
            // Object falls due to gravity
            this.attachedObject.sprite.body.enable = true;
        }
        
        // Fade out rope
        this.scene.tweens.add({
            targets: this.graphics,
            alpha: 0,
            duration: 200,
            onComplete: () => this.graphics.destroy()
        });
    }
    
    isActive() {
        return !this.broken;
    }
    
    destroy() {
        if (this.graphics) {
            this.graphics.destroy();
        }
    }
}
