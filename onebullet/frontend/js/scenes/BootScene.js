class BootScene extends Phaser.Scene {
    constructor() {
        super({ key: 'BootScene' });
    }
    
    preload() {
        // Create loading bar
        const width = this.cameras.main.width;
        const height = this.cameras.main.height;
        
        const progressBar = this.add.graphics();
        const progressBox = this.add.graphics();
        progressBox.fillStyle(0x222222, 0.8);
        progressBox.fillRect(width / 2 - 160, height / 2 - 25, 320, 50);
        
        const loadingText = this.add.text(width / 2, height / 2 - 50, 'Loading...', {
            font: '20px monospace',
            fill: '#ffffff'
        }).setOrigin(0.5);
        
        const percentText = this.add.text(width / 2, height / 2, '0%', {
            font: '18px monospace',
            fill: '#ffffff'
        }).setOrigin(0.5);
        
        // Progress events
        this.load.on('progress', (value) => {
            percentText.setText(Math.round(value * 100) + '%');
            progressBar.clear();
            progressBar.fillStyle(0x00ffff, 1);
            progressBar.fillRect(width / 2 - 150, height / 2 - 15, 300 * value, 30);
        });
        
        this.load.on('complete', () => {
            progressBar.destroy();
            progressBox.destroy();
            loadingText.destroy();
            percentText.destroy();
        });
        
        // Generate textures programmatically
        this.generateTextures();
    }
    
    generateTextures() {
        // Player texture
        const playerGraphics = this.add.graphics();
        playerGraphics.fillStyle(0x00ffff, 1);
        playerGraphics.fillRect(0, 0, 24, 24);
        playerGraphics.fillStyle(0x0088aa, 1);
        playerGraphics.fillRect(4, 4, 16, 16);
        playerGraphics.generateTexture('player', 24, 24);
        playerGraphics.destroy();
        
        // Enemy texture
        const enemyGraphics = this.add.graphics();
        enemyGraphics.fillStyle(0xff4444, 1);
        enemyGraphics.fillRect(0, 0, 24, 24);
        enemyGraphics.fillStyle(0xaa0000, 1);
        enemyGraphics.fillRect(4, 4, 16, 16);
        enemyGraphics.generateTexture('enemy', 24, 24);
        enemyGraphics.destroy();
        
        // Bullet texture
        const bulletGraphics = this.add.graphics();
        bulletGraphics.fillStyle(0xffff00, 1);
        bulletGraphics.fillCircle(6, 6, 6);
        bulletGraphics.fillStyle(0xffffff, 1);
        bulletGraphics.fillCircle(6, 6, 3);
        bulletGraphics.generateTexture('bullet', 12, 12);
        bulletGraphics.destroy();
        
        // Wall texture
        const wallGraphics = this.add.graphics();
        wallGraphics.fillStyle(0x333344, 1);
        wallGraphics.fillRect(0, 0, 32, 32);
        wallGraphics.lineStyle(1, 0x444455, 1);
        wallGraphics.strokeRect(0, 0, 32, 32);
        wallGraphics.generateTexture('wall', 32, 32);
        wallGraphics.destroy();
    }
    
    create() {
        // Initialize game state
        this.game.state = {
            currentLevel: 1,
            playerUsername: 'Player',
            playerId: null,
            totalScore: 0,
            unlockedLevels: [1]
        };
        
        // Try to load saved state
        const savedState = localStorage.getItem('onebullet_state');
        if (savedState) {
            try {
                Object.assign(this.game.state, JSON.parse(savedState));
            } catch (e) {
                console.warn('Failed to load saved state');
            }
        }
        
        // Transition to menu
        this.scene.start('MenuScene');
    }
}
