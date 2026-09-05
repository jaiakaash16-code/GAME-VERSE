// API Client for One Bullet backend
const API_BASE = 'http://localhost:8080/api';

const GameAPI = {
    // Player endpoints
    async getOrCreatePlayer(username) {
        try {
            const response = await fetch(`${API_BASE}/players`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username })
            });
            if (!response.ok) throw new Error('Failed to create player');
            return await response.json();
        } catch (error) {
            console.warn('Backend not available, using local storage');
            return this.getLocalPlayer(username);
        }
    },

    async getPlayer(playerId) {
        try {
            const response = await fetch(`${API_BASE}/players/${playerId}`);
            if (!response.ok) throw new Error('Player not found');
            return await response.json();
        } catch (error) {
            console.warn('Backend not available');
            return null;
        }
    },

    // Progress endpoints
    async getProgress(playerId) {
        try {
            const response = await fetch(`${API_BASE}/players/${playerId}/progress`);
            if (!response.ok) throw new Error('Failed to get progress');
            return await response.json();
        } catch (error) {
            console.warn('Backend not available, using local storage');
            return this.getLocalProgress(playerId);
        }
    },

    async saveProgress(playerId, levelNumber, completed, score) {
        try {
            const response = await fetch(`${API_BASE}/players/${playerId}/progress`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ levelNumber, completed, score })
            });
            if (!response.ok) throw new Error('Failed to save progress');
            return await response.json();
        } catch (error) {
            console.warn('Backend not available, using local storage');
            return this.saveLocalProgress(playerId, levelNumber, completed, score);
        }
    },

    // Score endpoints
    async submitScore(playerId, levelNumber, score, timeMs, bulletsUsed) {
        try {
            const response = await fetch(`${API_BASE}/scores`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ playerId, levelNumber, score, timeMs, bulletsUsed })
            });
            if (!response.ok) throw new Error('Failed to submit score');
            return await response.json();
        } catch (error) {
            console.warn('Backend not available, using local storage');
            return this.saveLocalScore(playerId, levelNumber, score, timeMs, bulletsUsed);
        }
    },

    async getLeaderboard() {
        try {
            const response = await fetch(`${API_BASE}/scores/leaderboard`);
            if (!response.ok) throw new Error('Failed to get leaderboard');
            return await response.json();
        } catch (error) {
            console.warn('Backend not available, using local storage');
            return this.getLocalLeaderboard();
        }
    },

    // Local storage fallbacks
    getLocalPlayer(username) {
        const players = JSON.parse(localStorage.getItem('onebullet_players') || '{}');
        if (!players[username]) {
            players[username] = {
                id: Date.now(),
                username,
                totalScore: 0,
                createdAt: new Date().toISOString()
            };
        }
        localStorage.setItem('onebullet_players', JSON.stringify(players));
        return players[username];
    },

    getLocalProgress(playerId) {
        const progress = JSON.parse(localStorage.getItem('onebullet_progress') || '{}');
        return progress[playerId] || [];
    },

    saveLocalProgress(playerId, levelNumber, completed, score) {
        const allProgress = JSON.parse(localStorage.getItem('onebullet_progress') || '{}');
        if (!allProgress[playerId]) allProgress[playerId] = [];
        
        const existing = allProgress[playerId].find(p => p.levelNumber === levelNumber);
        if (existing) {
            existing.completed = completed || existing.completed;
            if (score > (existing.bestScore || 0)) existing.bestScore = score;
            existing.attempts = (existing.attempts || 0) + 1;
        } else {
            allProgress[playerId].push({
                levelNumber,
                completed,
                bestScore: score,
                attempts: 1
            });
        }
        
        localStorage.setItem('onebullet_progress', JSON.stringify(allProgress));
        return allProgress[playerId].find(p => p.levelNumber === levelNumber);
    },

    saveLocalScore(playerId, levelNumber, score, timeMs, bulletsUsed) {
        const scores = JSON.parse(localStorage.getItem('onebullet_scores') || '[]');
        scores.push({
            id: Date.now(),
            playerId,
            levelNumber,
            score,
            timeMs,
            bulletsUsed,
            submittedAt: new Date().toISOString()
        });
        localStorage.setItem('onebullet_scores', JSON.stringify(scores));
        return scores[scores.length - 1];
    },

    getLocalLeaderboard() {
        const scores = JSON.parse(localStorage.getItem('onebullet_scores') || '[]');
        return scores.sort((a, b) => b.score - a.score).slice(0, 10);
    }
};
