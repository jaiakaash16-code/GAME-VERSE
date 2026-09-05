// Phaser Game Configuration
const config = {
    type: Phaser.AUTO,
    width: 800,
    height: 600,
    parent: 'game-container',
    backgroundColor: '#0a0a0f',
    physics: {
        default: 'arcade',
        arcade: {
            gravity: { y: 300 },
            debug: false
        }
    },
    scene: [BootScene, MenuScene, GameScene, WinScene],
    render: {
        pixelArt: false,
        antialias: true
    },
    scale: {
        mode: Phaser.Scale.FIT,
        autoCenter: Phaser.Scale.CENTER_BOTH
    }
};

// Initialize game
const game = new Phaser.Game(config);

// Initialize player with backend
async function initPlayer() {
    const username = localStorage.getItem('onebullet_username') || 'Player' + Math.floor(Math.random() * 1000);
    localStorage.setItem('onebullet_username', username);
    
    try {
        const player = await GameAPI.getOrCreatePlayer(username);
        game.state = game.state || {};
        game.state.playerId = player.id;
        game.state.playerUsername = player.username;
        console.log('Player initialized:', player);
    } catch (error) {
        console.warn('Backend not available, using local mode');
        game.state = game.state || {};
        game.state.playerId = Date.now();
        game.state.playerUsername = username;
    }
}

// Start player initialization
initPlayer();

// Handle window focus
window.addEventListener('blur', () => {
    // Pause game when window loses focus
    if (game.scene.isActive('GameScene')) {
        game.scene.getScene('GameScene').scene.pause();
    }
});

window.addEventListener('focus', () => {
    // Resume game when window gains focus
    if (game.scene.isPaused('GameScene')) {
        game.scene.getScene('GameScene').scene.resume();
    }
});

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    // Escape to menu
    if (e.key === 'Escape') {
        const activeScene = game.scene.getScenes(true)[0];
        if (activeScene && activeScene.scene.key !== 'MenuScene' && activeScene.scene.key !== 'BootScene') {
            activeScene.scene.start('MenuScene');
        }
    }
});

console.log('One Bullet initialized! 🔫');
