package com.gameverse.games.snake;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

/**
 * Snake game with difficulty-based speed and AI snake opponent.
 * Easy: slow snake, no AI opponent
 * Medium: moderate speed, simple AI snake
 * Hard: fast snake, aggressive AI snake that chases food
 */
public class SnakeGame extends BaseGame {
    
    private int boardWidth;
    private int boardHeight;
    private Snake snake;
    private Food food;
    private GameResult.Status status;
    
    // AI Snake
    private Snake aiSnake;
    private Food aiFood;
    private boolean hasAiSnake;
    private int aiScore;
    
    // Difficulty-based speed
    private int updateInterval;
    private int updateCounter;
    
    // The game stays frozen until the player presses a direction, so it can't
    // run into a wall before the window has keyboard focus.
    private boolean awaitingFirstMove;
    
    private final java.util.Random random = new java.util.Random();
    
    public SnakeGame() {
        super("Snake");
        this.boardWidth = 20;
        this.boardHeight = 20;
        this.status = GameResult.Status.LOST;
        this.hasAiSnake = false;
        this.aiScore = 0;
    }
    
    @Override
    public void initialize() {
        snake = new Snake(boardWidth / 2, boardHeight / 2);
        spawnFood();
        setupDifficulty();
        awaitingFirstMove = true;
    }
    
    @Override
    public void start() {
        super.start();
        reset();
    }
    
    @Override
    public void restart() {
        super.restart();
        initialize();
    }
    
    @Override
    public void update(float deltaTime) {
        if (!isRunning()) {
            return;
        }
        
        // Nothing moves (player or AI) until the player makes their first move,
        // otherwise the snake can hit a wall before the window has focus.
        if (awaitingFirstMove) {
            return;
        }
        
        // Speed control based on difficulty
        updateCounter++;
        if (updateCounter < updateInterval) return;
        updateCounter = 0;
        
        snake.update();
        
        // AI snake update (Medium and Hard only)
        if (hasAiSnake && aiSnake != null) {
            updateAISnake();
            aiSnake.update();
            
            // AI snake eats food
            if (aiSnake.getHead().equals(aiFood.getPosition())) {
                aiScore += 100;
                aiSnake.grow();
                spawnAIFood();
            }
            
            // AI snake collision with itself or walls — respawn it somewhere far
            // from the player instead of resetting it onto the player's starting
            // square (which used to kill the player instantly head-on).
            if (aiSnake.checkSelfCollision() || !isWithinBounds(aiSnake.getHead())) {
                aiSnake = spawnAISnake();
                aiScore = Math.max(0, aiScore - 50);
            }
            
            // AI snake collides with player snake
            if (aiSnake.getHead().equals(snake.getHead())) {
                endGame(GameResult.Status.LOST);
                return;
            }
        }
        
        // Check collision with food
        if (snake.getHead().equals(food.getPosition())) {
            score += 100;
            snake.grow();
            spawnFood();
        }
        
        // Check collision with self
        if (snake.checkSelfCollision()) {
            endGame(GameResult.Status.LOST);
        }
        
        // Check collision with walls
        if (!isWithinBounds(snake.getHead())) {
            endGame(GameResult.Status.LOST);
        }
        
        // Check win condition (reached max length or AI snake score limit)
        if (hasAiSnake && aiScore >= 500) {
            endGame(GameResult.Status.LOST);
        }
    }
    
    private void setupDifficulty() {
        switch (difficulty) {
            case EASY -> {
                updateInterval = 3; // Slow updates
                hasAiSnake = false;
                aiSnake = null;
                aiFood = null;
            }
            case MEDIUM -> {
                updateInterval = 2; // Moderate speed
                hasAiSnake = true;
                aiSnake = spawnAISnake();
                spawnAIFood();
            }
            case HARD -> {
                updateInterval = 1; // Fast updates
                hasAiSnake = true;
                aiSnake = spawnAISnake();
                spawnAIFood();
            }
        }
        updateCounter = 0;
    }
    
    /**
     * Difficulty changes must take effect immediately. The UI applies the
     * chosen difficulty after the game object is created (which happens after
     * initialize()), so re-apply the speed/AI setup here instead of leaving
     * the default difficulty's setup active.
     */
    @Override
    public void setDifficulty(Difficulty difficulty) {
        super.setDifficulty(difficulty);
        if (snake != null) {
            setupDifficulty();
        }
    }
    
    /** AI snake moves towards food or away from player */
    private void updateAISnake() {
        if (aiSnake == null || aiFood == null) return;
        
        Position head = aiSnake.getHead();
        Position target = aiFood.getPosition();
        
        int chaseX = target.x;
        int chaseY = target.y;
        
        // Hard AI: when the player is close, prefer moving away from the player
        // instead of chasing the food.
        if (difficulty == Difficulty.HARD) {
            Position playerHead = snake.getHead();
            int distToPlayer = Math.abs(head.x - playerHead.x) + Math.abs(head.y - playerHead.y);
            if (distToPlayer < 5) {
                chaseX = head.x + (head.x - playerHead.x);
                chaseY = head.y + (head.y - playerHead.y);
            }
        }
        
        // Pick the safe neighbour that gets closest to the chase point. Walls,
        // the AI's own body and the player's snake are all avoided, so the AI
        // rarely suicides and respawns onto the player.
        int bestScore = Integer.MAX_VALUE;
        int bestDirX = 0, bestDirY = 0;
        boolean found = false;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int nx = head.x + d[0];
            int ny = head.y + d[1];
            if (!isWithinBounds(new Position(nx, ny))) continue;
            if (aiSnake.contains(nx, ny)) continue;
            if (snake != null && snake.contains(nx, ny)) continue;
            int score = Math.abs(nx - chaseX) + Math.abs(ny - chaseY);
            if (score < bestScore) {
                bestScore = score;
                bestDirX = d[0];
                bestDirY = d[1];
                found = true;
            }
        }
        if (found) {
            aiSnake.setDirection(bestDirX, bestDirY);
        }
        // No safe neighbour: keep the current heading; the wall / self-collision
        // check in update() respawns the AI somewhere safe next tick.
    }
    
    private void spawnFood() {
        food = new Food(randomFreeCell(snake));
    }
    
    private void spawnAIFood() {
        aiFood = new Food(randomFreeCell(snake, aiSnake));
    }
    
    /** Find a random board cell that none of the given snakes occupies. */
    private Position randomFreeCell(Snake... snakes) {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = random.nextInt(boardWidth);
            int y = random.nextInt(boardHeight);
            boolean occupied = false;
            for (Snake s : snakes) {
                if (s != null && s.contains(x, y)) {
                    occupied = true;
                    break;
                }
            }
            if (!occupied) return new Position(x, y);
        }
        return new Position(random.nextInt(boardWidth), random.nextInt(boardHeight));
    }
    
    /**
     * Create a fresh AI snake on a free cell as far as possible from the player,
     * so a respawned AI never appears on top of the player's snake.
     */
    private Snake spawnAISnake() {
        Position playerHead = snake != null ? snake.getHead() : null;
        Position best = null;
        int bestDist = -1;
        for (int x = 1; x < boardWidth - 1; x++) {
            for (int y = 1; y < boardHeight - 1; y++) {
                if (snake != null && snake.contains(x, y)) continue;
                int dist = playerHead == null ? Integer.MAX_VALUE
                    : Math.abs(x - playerHead.x) + Math.abs(y - playerHead.y);
                if (dist > bestDist) {
                    bestDist = dist;
                    best = new Position(x, y);
                }
            }
        }
        if (best == null) {
            best = randomFreeCell(snake);
        }
        return new Snake(best.x, best.y);
    }
    
    private boolean isWithinBounds(Position position) {
        return position.x >= 0 && position.x < boardWidth && position.y >= 0 && position.y < boardHeight;
    }
    
    private void endGame(GameResult.Status status) {
        this.status = status;
        isRunning = false;
        // Add AI score penalty to player score
        if (hasAiSnake) {
            score = Math.max(0, score - aiScore / 2);
        }
        result = new GameResult(name, status, score, getElapsedTime());
    }
    
    private void reset() {
        snake.reset(boardWidth / 2, boardHeight / 2);
        spawnFood();
        aiScore = 0;
        if (hasAiSnake) {
            aiSnake = spawnAISnake();
            spawnAIFood();
        }
        awaitingFirstMove = true;
    }
    
    // Inner classes
    
    private static class Position {
        int x, y;
        
        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Position)) return false;
            Position p = (Position) o;
            return x == p.x && y == p.y;
        }
        
        @Override
        public int hashCode() {
            return java.util.Objects.hash(x, y);
        }
    }
    
    private static class Snake {
        private java.util.LinkedList<Position> body;
        private int nextDirX, nextDirY;
        
        Snake(int x, int y) {
            body = new java.util.LinkedList<>();
            body.add(new Position(x, y));
            nextDirX = 1;
            nextDirY = 0;
        }
        
        void update() {
            Position head = body.getFirst();
            Position newHead = new Position(head.x + nextDirX, head.y + nextDirY);
            body.addFirst(newHead);
            if (body.size() > 1) {
                body.removeLast();
            }
        }
        
        void grow() {
            Position tail = body.getLast();
            body.add(new Position(tail.x, tail.y));
        }
        
        boolean checkSelfCollision() {
            Position head = body.getFirst();
            for (int i = 1; i < body.size(); i++) {
                if (head.equals(body.get(i))) {
                    return true;
                }
            }
            return false;
        }
        
        Position getHead() {
            return body.getFirst();
        }
        
        void reset(int x, int y) {
            body.clear();
            body.add(new Position(x, y));
            nextDirX = 1;
            nextDirY = 0;
        }
        
        boolean contains(int x, int y) {
            for (Position p : body) {
                if (p.x == x && p.y == y) return true;
            }
            return false;
        }
        
        void setDirection(int dirX, int dirY) {
            // Prevent 180-degree turns… but only once there is a body to run
            // into. A single-segment snake may reverse freely, which matters for
            // the very first move (it has not moved yet, so "reversing" is safe).
            boolean reversing = nextDirX == -dirX && nextDirY == -dirY;
            if (body.size() == 1 || !reversing) {
                nextDirX = dirX;
                nextDirY = dirY;
            }
        }
    }
    
    private static class Food {
        Position position;
        
        Food(int x, int y) {
            position = new Position(x, y);
        }
        
        Food(Position p) {
            position = p;
        }
        
        Position getPosition() {
            return position;
        }
    }
    
    // Public methods
    
    public void moveSnake(String direction) {
        if (snake == null) return;
        
        switch (direction.toLowerCase()) {
            case "up" -> snake.setDirection(0, -1);
            case "down" -> snake.setDirection(0, 1);
            case "left" -> snake.setDirection(-1, 0);
            case "right" -> snake.setDirection(1, 0);
        }
        // The first directional input releases the frozen start state, so the
        // game never ends before the player has a chance to react.
        awaitingFirstMove = false;
    }
    
    /** Get the update interval for the game loop (ticks between moves) */
    public int getUpdateInterval() {
        return updateInterval;
    }
    
    /** Check if AI snake is active */
    public boolean hasAiSnake() {
        return hasAiSnake;
    }
    
    /** Get AI snake head position (for rendering) */
    public Position getAiSnakeHead() {
        return aiSnake != null ? aiSnake.getHead() : null;
    }
    
    /** Get AI food position (for rendering) */
    public Position getAiFoodPosition() {
        return aiFood != null ? aiFood.getPosition() : null;
    }
    
    /** Get AI score */
    public int getAiScore() {
        return aiScore;
    }
    
    // Rendering API (plain getters, so the UI never needs reflection into
    // private fields / inner classes)
    
    /** Board width in cells */
    public int getBoardWidth() {
        return boardWidth;
    }
    
    /** Board height in cells */
    public int getBoardHeight() {
        return boardHeight;
    }
    
    /** Player head column, or -1 if the snake is not initialized */
    public int getPlayerHeadX() {
        return snake != null ? snake.getHead().x : -1;
    }
    
    /** Player head row, or -1 if the snake is not initialized */
    public int getPlayerHeadY() {
        return snake != null ? snake.getHead().y : -1;
    }
    
    /** Player food column, or -1 if not spawned */
    public int getFoodX() {
        return food != null ? food.getPosition().x : -1;
    }
    
    /** Player food row, or -1 if not spawned */
    public int getFoodY() {
        return food != null ? food.getPosition().y : -1;
    }
    
    /** AI head column, or -1 when there is no AI snake */
    public int getAiHeadX() {
        return hasAiSnake && aiSnake != null ? aiSnake.getHead().x : -1;
    }
    
    /** AI head row, or -1 when there is no AI snake */
    public int getAiHeadY() {
        return hasAiSnake && aiSnake != null ? aiSnake.getHead().y : -1;
    }
    
    /** AI food column, or -1 when there is no AI food */
    public int getAiFoodX() {
        return hasAiSnake && aiFood != null ? aiFood.getPosition().x : -1;
    }
    
    /** AI food row, or -1 when there is no AI food */
    public int getAiFoodY() {
        return hasAiSnake && aiFood != null ? aiFood.getPosition().y : -1;
    }
    
    /** True while the game is frozen, waiting for the player's first move */
    public boolean isWaitingForFirstMove() {
        return awaitingFirstMove;
    }
}
