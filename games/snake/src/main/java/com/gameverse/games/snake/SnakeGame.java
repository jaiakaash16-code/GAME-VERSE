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
            
            // AI snake collision with itself or walls
            if (aiSnake.checkSelfCollision() || !isWithinBounds(aiSnake.getHead())) {
                aiSnake.reset();
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
            }
            case MEDIUM -> {
                updateInterval = 2; // Moderate speed
                hasAiSnake = true;
                aiSnake = new Snake(5, 5);
                spawnAIFood();
            }
            case HARD -> {
                updateInterval = 1; // Fast updates
                hasAiSnake = true;
                aiSnake = new Snake(5, 5);
                spawnAIFood();
            }
        }
        updateCounter = 0;
    }
    
    /** AI snake moves towards food or away from player */
    private void updateAISnake() {
        if (aiSnake == null || aiFood == null) return;
        
        Position head = aiSnake.getHead();
        Position target = aiFood.getPosition();
        
        int dx = target.x - head.x;
        int dy = target.y - head.y;
        
        // Hard AI: also considers avoiding player snake
        if (difficulty == Difficulty.HARD) {
            Position playerHead = snake.getHead();
            int distToPlayer = Math.abs(head.x - playerHead.x) + Math.abs(head.y - playerHead.y);
            if (distToPlayer < 5) {
                // Too close to player — move away
                dx = head.x - playerHead.x;
                dy = head.y - playerHead.y;
            }
        }
        
        // Choose direction with preference towards target
        if (Math.abs(dx) > Math.abs(dy)) {
            aiSnake.setDirection(dx > 0 ? 1 : -1, 0);
        } else if (Math.abs(dy) > 0) {
            aiSnake.setDirection(0, dy > 0 ? 1 : -1);
        } else {
            // Random direction if at same position
            int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
            int[] d = dirs[(int)(Math.random() * 4)];
            aiSnake.setDirection(d[0], d[1]);
        }
    }
    
    private void spawnFood() {
        food = new Food((int) (Math.random() * boardWidth), (int) (Math.random() * boardHeight));
    }
    
    private void spawnAIFood() {
        aiFood = new Food((int) (Math.random() * boardWidth), (int) (Math.random() * boardHeight));
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
        snake.reset();
        spawnFood();
        aiScore = 0;
        if (hasAiSnake) {
            aiSnake = new Snake(5, 5);
            spawnAIFood();
        }
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
        
        void reset() {
            body.clear();
            body.add(new Position(10, 10));
        }
        
        void setDirection(int dirX, int dirY) {
            // Prevent 180-degree turns
            if (nextDirX != -dirX || nextDirY != -dirY) {
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
}
