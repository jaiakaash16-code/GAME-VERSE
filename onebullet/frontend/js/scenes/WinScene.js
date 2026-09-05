class WinScene extends Phaser.Scene {
    constructor() {
        super({ key: 'WinScene' });
    }
    
    init(data) {
        this.levelNumber = data.level || 1;
        this.score = data.score || 0;
        this.timeMs = data.time || 0;
        this.bullets = data.bullets || 1;
    }
    
    create() {
        const width = this.cameras.main.width;
        const height = this.cameras.main.height;
        
        // Background
        this.cameras.main.setBackgroundColor(0x0a0a0f);
        
        // Title
        const title = this.add.text(width / 2, 100, 'LEVEL COMPLETE!', {
            font: 'bold 48px monospace',
            fill: '#00ff00',
            stroke: '#004400',
            strokeThickness: 4
        }).setOrigin(0.5);
        
        // Animate title
        this.tweens.add({
            targets: title,
            y: 105,
            duration: 1500,
            yoyo: true,
            repeat: -1,
            ease: 'Sine.easeInOut'
        });
        
        // Level info
        this.add.text(width / 2, 170, `Level ${this.levelNumber} Complete`, {
            font: '24px monospace',
            fill: '#ffffff'
        }).setOrigin(0.5);
        
        // Score breakdown
        const timeSeconds = (this.timeMs / 1000).toFixed(1);
        const timeBonus = Math.max(0, 10000 - Math.floor(this.timeMs / 100));
        const enemyScore = this.levelNumber * 100; // Approximate
        
        this.add.text(width / 2, 230, [
            '━━━━━━━━━━━━━━━━━━━━',
            '',
            `⏱  Time: ${timeSeconds}s`,
            `🎯  Shots Fired: ${this.bullets}`,
            `💀  Enemies Defeated: ${enemyScore / 100}`,
            '',
            `📊  Score Breakdown:`,
            `    Enemy Bonus: ${enemyScore}`,
            `    Time Bonus: ${timeBonus}`,
            '',
            `🏆  Total Score: ${this.score}`,
            '',
            '━━━━━━━━━━━━━━━━━━━━'
        ].join('\n'), {
            font: '16px monospace',
            fill: '#888888',
            align: 'center',
            lineSpacing: 4
        }).setOrigin(0.5);
        
        // Total score display
        this.add.text(width / 2, 450, `Total Score: ${this.game.state.totalScore}`, {
            font: 'bold 24px monospace',
            fill: '#ffff00'
        }).setOrigin(0.5);
        
        // Buttons
        const buttonY = 520;
        
        // Next Level button (if available)
        const nextLevel = this.levelNumber + 1;
        if (nextLevel <= 5 && this.game.state.unlockedLevels.includes(nextLevel)) {
            const nextButton = this.add.text(width / 2 - 120, buttonY, '▶ Next Level', {
                font: 'bold 20px monospace',
                fill: '#00ffff',
                backgroundColor: '#003333',
                padding: { x: 20, y: 10 }
            }).setOrigin(0.5).setInteractive({ useHandCursor: true });
            
            nextButton.on('pointerover', () => nextButton.setStyle({ fill: '#ffffff' }));
            nextButton.on('pointerout', () => nextButton.setStyle({ fill: '#00ffff' }));
            nextButton.on('pointerdown', () => {
                this.scene.start('GameScene', { level: nextLevel });
            });
        }
        
        // Retry button
        const retryButton = this.add.text(width / 2 + 120, buttonY, '↺ Retry', {
            font: 'bold 20px monospace',
            fill: '#ffff00',
            backgroundColor: '#333300',
            padding: { x: 20, y: 10 }
        }).setOrigin(0.5).setInteractive({ useHandCursor: true });
        
        retryButton.on('pointerover', () => retryButton.setStyle({ fill: '#ffffff' }));
        retryButton.on('pointerout', () => retryButton.setStyle({ fill: '#ffff00' }));
        retryButton.on('pointerdown', () => {
            this.scene.start('GameScene', { level: this.levelNumber });
        });
        
        // Menu button
        const menuButton = this.add.text(width / 2, buttonY + 70, '← Menu', {
            font: '18px monospace',
            fill: '#888888',
            padding: { x: 20, y: 10 }
        }).setOrigin(0.5).setInteractive({ useHandCursor: true });
        
        menuButton.on('pointerover', () => menuButton.setStyle({ fill: '#ffffff' }));
        menuButton.on('pointerout', () => menuButton.setStyle({ fill: '#888888' }));
        menuButton.on('pointerdown', () => {
            this.scene.start('MenuScene');
        });
        
        // Leaderboard preview
        this.loadLeaderboard();
        
        // Fade in
        this.cameras.main.fadeIn(500);
    }
    
    async loadLeaderboard() {
        const width = this.cameras.main.width;
        
        try {
            const leaderboard = await GameAPI.getLeaderboard();
            if (leaderboard && leaderboard.length > 0) {
                this.add.text(width / 2, 600, '━━━ LEADERBOARD ━━━', {
                    font: '14px monospace',
                    fill: '#666666'
                }).setOrigin(0.5);
                
                leaderboard.slice(0, 5).forEach((entry, index) => {
                    this.add.text(width / 2, 625 + index * 20, 
                        `${index + 1}. ${entry.username || 'Player'} - ${entry.score}`, {
                        font: '12px monospace',
                        fill: '#555555'
                    }).setOrigin(0.5);
                });
            }
        } catch (error) {
            console.warn('Could not load leaderboard');
        }
    }
}
