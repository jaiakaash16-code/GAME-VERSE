class MenuScene extends Phaser.Scene {
    constructor() {
        super({ key: 'MenuScene' });
    }
    
    create() {
        const width = this.cameras.main.width;
        const height = this.cameras.main.height;
        
        // Background
        this.cameras.main.setBackgroundColor(0x0a0a0f);
        
        // Title
        const title = this.add.text(width / 2, 100, 'ONE BULLET', {
            font: 'bold 64px monospace',
            fill: '#00ffff',
            stroke: '#004444',
            strokeThickness: 4
        }).setOrigin(0.5);
        
        // Subtitle
        this.add.text(width / 2, 160, '🔫', {
            font: '48px serif'
        }).setOrigin(0.5);
        
        // Animate title
        this.tweens.add({
            targets: title,
            y: 105,
            duration: 2000,
            yoyo: true,
            repeat: -1,
            ease: 'Sine.easeInOut'
        });
        
        // Instructions
        this.add.text(width / 2, 220, 'You have one bullet.\nUse the environment to survive.', {
            font: '18px monospace',
            fill: '#888888',
            align: 'center'
        }).setOrigin(0.5);
        
        // Level buttons
        const levelCount = 5;
        const buttonWidth = 120;
        const buttonHeight = 50;
        const startX = width / 2 - (levelCount * (buttonWidth + 20) - 20) / 2;
        
        for (let i = 1; i <= levelCount; i++) {
            const x = startX + (i - 1) * (buttonWidth + 20) + buttonWidth / 2;
            const y = 350;
            
            const isUnlocked = this.game.state.unlockedLevels.includes(i);
            
            // Button background
            const button = this.add.rectangle(x, y, buttonWidth, buttonHeight, 
                isUnlocked ? 0x004444 : 0x222222);
            button.setStrokeStyle(2, isUnlocked ? 0x00ffff : 0x444444);
            
            // Button text
            const text = this.add.text(x, y, `Level ${i}`, {
                font: '16px monospace',
                fill: isUnlocked ? '#00ffff' : '#666666'
            }).setOrigin(0.5);
            
            // Make interactive if unlocked
            if (isUnlocked) {
                button.setInteractive({ useHandCursor: true });
                
                button.on('pointerover', () => {
                    button.setFillStyle(0x006666);
                    this.tweens.add({
                        targets: button,
                        scaleX: 1.1,
                        scaleY: 1.1,
                        duration: 100
                    });
                });
                
                button.on('pointerout', () => {
                    button.setFillStyle(0x004444);
                    this.tweens.add({
                        targets: button,
                        scaleX: 1,
                        scaleY: 1,
                        duration: 100
                    });
                });
                
                button.on('pointerdown', () => {
                    this.startLevel(i);
                });
            }
        }
        
        // Controls info
        this.add.text(width / 2, 450, 'CONTROLS', {
            font: 'bold 20px monospace',
            fill: '#00ffff'
        }).setOrigin(0.5);
        
        this.add.text(width / 2, 490, [
            'WASD / Arrow Keys - Move',
            'Mouse - Aim',
            'Left Click - Shoot',
            'R - Restart Level'
        ].join('\n'), {
            font: '14px monospace',
            fill: '#666666',
            align: 'center',
            lineSpacing: 8
        }).setOrigin(0.5);
        
        // High score display
        if (this.game.state.totalScore > 0) {
            this.add.text(width / 2, 580, `Total Score: ${this.game.state.totalScore}`, {
                font: '16px monospace',
                fill: '#ffff00'
            }).setOrigin(0.5);
        }
        
        // Bottom text
        this.add.text(width / 2, height - 30, 'A puzzle-action game by One Bullet Studios', {
            font: '12px monospace',
            fill: '#444444'
        }).setOrigin(0.5);
        
        // Fade in
        this.cameras.main.fadeIn(500);
    }
    
    startLevel(levelNumber) {
        this.game.state.currentLevel = levelNumber;
        this.cameras.main.fadeOut(300, 0, 0, 0, (camera, progress) => {
            if (progress === 1) {
                this.scene.start('GameScene', { level: levelNumber });
            }
        });
    }
}
