package com.gameverse.games.pong;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;

/**
 * Pong game implementation with difficulty-based AI.
 * Easy: slow paddle, slow reactions, misses sometimes
 * Medium: moderate speed, tracks ball well
 * Hard: fast paddle, predictive tracking, rarely misses
 */
public class PongGame extends BaseGame {
    
    private int screenWidth;
    private int screenHeight;
    private Ball ball;
    private Paddle playerPaddle;
    private Paddle aiPaddle;
    private int playerScore;
    private int aiScore;
    
    // AI tracking
    private double aiPredictionY;
    private int aiReactionDelay;
    private int aiReactionCounter;
    
    public PongGame() {
        super("Pong");
        this.screenWidth = 800;
        this.screenHeight = 600;
        this.playerScore = 0;
        this.aiScore = 0;
    }
    
    @Override
    public void initialize() {
        ball = new Ball(screenWidth / 2, screenHeight / 2, 5, 5);
        playerPaddle = new Paddle(10, screenHeight / 2 - 50, 10, 100);
        aiPaddle = new Paddle(screenWidth - 20, screenHeight / 2 - 50, 10, 100);
        aiPredictionY = screenHeight / 2.0;
        aiReactionCounter = 0;
        setupDifficulty();
    }
    
    @Override
    public void start() {
        super.start();
        initialize();
    }
    
    @Override
    public void restart() {
        super.restart();
        playerScore = 0;
        aiScore = 0;
        initialize();
    }
    
    @Override
    public void update(float deltaTime) {
        if (!isRunning()) {
            return;
        }
        
        // Update ball position
        ball.update(deltaTime);
        
        // Update AI paddle with difficulty-based logic
        updateAIPaddle(deltaTime);
        
        // Check collisions
        checkCollisions();
        
        // Check scoring
        checkScoring();
    }
    
    private void setupDifficulty() {
        switch (difficulty) {
            case EASY -> {
                aiPaddle.speed = 3;
                aiReactionDelay = 15; // reacts slowly
            }
            case MEDIUM -> {
                aiPaddle.speed = 5;
                aiReactionDelay = 5; // reacts moderately
            }
            case HARD -> {
                aiPaddle.speed = 7;
                aiReactionDelay = 0; // instant reaction
            }
        }
    }
    
    private void updateAIPaddle(float deltaTime) {
        aiReactionCounter++;
        
        // Only update prediction after reaction delay
        if (aiReactionCounter >= aiReactionDelay) {
            aiReactionCounter = 0;
            
            switch (difficulty) {
                case EASY -> updateAIEasy(deltaTime);
                case MEDIUM -> updateAIMedium(deltaTime);
                case HARD -> updateAIHard(deltaTime);
            }
        }
        
        // Move paddle towards prediction
        double paddleCenter = aiPaddle.y + aiPaddle.height / 2.0;
        if (paddleCenter < aiPredictionY - 10) {
            aiPaddle.moveDown((int) aiPaddle.speed);
        } else if (paddleCenter > aiPredictionY + 10) {
            aiPaddle.moveUp((int) aiPaddle.speed);
        }
    }
    
    /** Easy AI: slow, imprecise tracking, sometimes loses track */
    private void updateAIEasy(float deltaTime) {
        if (ball.velocityX < 0) {
            // Ball moving away — slowly return to center
            aiPredictionY = screenHeight / 2.0;
        } else {
            // Ball coming towards AI — track with some error
            double error = (Math.random() - 0.5) * 100; // random offset
            aiPredictionY = ball.y + error;
        }
        // 30% chance to completely lose track
        if (Math.random() < 0.3) {
            aiPredictionY = screenHeight / 2.0;
        }
    }
    
    /** Medium AI: moderate tracking, slight prediction */
    private void updateAIMedium(float deltaTime) {
        if (ball.velocityX > 0) {
            // Ball moving towards AI — predict where it will be
            double timeToReach = (aiPaddle.x - ball.x) / (ball.velocityX * 100);
            double predictedY = ball.y + ball.velocityY * 100 * timeToReach * deltaTime;
            
            // Add some error
            double error = (Math.random() - 0.5) * 40;
            aiPredictionY = predictedY + error;
        } else {
            // Ball moving away — track loosely
            aiPredictionY = ball.y + (Math.random() - 0.5) * 60;
        }
    }
    
    /** Hard AI: fast, accurate prediction, rarely misses */
    private void updateAIHard(float deltaTime) {
        if (ball.velocityX > 0) {
            // Ball coming — predict precisely
            double timeToReach = (aiPaddle.x - ball.x) / (ball.velocityX * 100);
            double predictedY = ball.y + ball.velocityY * 100 * timeToReach * deltaTime;
            
            // Minimal error
            double error = (Math.random() - 0.5) * 10;
            aiPredictionY = predictedY + error;
        } else {
            // Ball moving away — return to center
            aiPredictionY = screenHeight / 2.0;
        }
        
        // Hard AI can also speed up when ball is close
        if (ball.x > screenWidth * 0.7 && ball.velocityX > 0) {
            aiPaddle.speed = 8;
        } else {
            aiPaddle.speed = 7;
        }
    }
    
    private void checkCollisions() {
        // Check paddle collisions
        if (ball.collidesWith(playerPaddle)) {
            ball.reverseXVelocity();
            // Add a bit of angle based on where it hit the paddle
            double hitPos = (ball.y - playerPaddle.y) / playerPaddle.height;
            ball.velocityY = (hitPos - 0.5) * 8;
        }
        if (ball.collidesWith(aiPaddle)) {
            ball.reverseXVelocity();
            double hitPos = (ball.y - aiPaddle.y) / aiPaddle.height;
            ball.velocityY = (hitPos - 0.5) * 8;
        }
        
        // Check wall collisions
        if (ball.y <= 0 || ball.y >= screenHeight) {
            ball.reverseYVelocity();
        }
    }
    
    private void checkScoring() {
        if (ball.x <= 0) {
            aiScore++;
            resetBall();
            score = playerScore;
        } else if (ball.x >= screenWidth) {
            playerScore++;
            resetBall();
            score = playerScore;
        }
        
        // Check win condition
        if (playerScore >= 5) {
            result = new GameResult(name, GameResult.Status.WON, score, getElapsedTime());
            isRunning = false;
        } else if (aiScore >= 5) {
            result = new GameResult(name, GameResult.Status.LOST, score, getElapsedTime());
            isRunning = false;
        }
    }
    
    private void resetBall() {
        ball.x = screenWidth / 2;
        ball.y = screenHeight / 2;
        ball.velocityX = 5 * (Math.random() > 0.5 ? 1 : -1);
        ball.velocityY = 3 * (Math.random() > 0.5 ? 1 : -1);
    }
    
    public void movePaddleUp() {
        playerPaddle.moveUp(10);
    }
    
    public void movePaddleDown() {
        playerPaddle.moveDown(10);
    }
    
    // Inner classes
    
    private static class Ball {
        double x, y;
        double velocityX, velocityY;
        int radius;
        
        Ball(double x, double y, double velX, double velY) {
            this.x = x;
            this.y = y;
            this.velocityX = velX;
            this.velocityY = velY;
            this.radius = 5;
        }
        
        void update(float deltaTime) {
            x += velocityX * deltaTime * 100;
            y += velocityY * deltaTime * 100;
        }
        
        void reverseXVelocity() {
            velocityX = -velocityX;
        }
        
        void reverseYVelocity() {
            velocityY = -velocityY;
        }
        
        boolean collidesWith(Paddle paddle) {
            return x >= paddle.x && x <= paddle.x + paddle.width &&
                   y >= paddle.y && y <= paddle.y + paddle.height;
        }
    }
    
    private static class Paddle {
        int x, y, width, height;
        double speed;
        
        Paddle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.speed = 5;
        }
        
        void moveUp(int amount) {
            y = Math.max(0, y - amount);
        }
        
        void moveDown(int amount) {
            y = Math.min(600 - height, y + amount);
        }
    }
}
