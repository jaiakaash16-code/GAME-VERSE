package com.gameverse.games.memory;

import com.gameverse.core.Difficulty;
import com.gameverse.core.GameResult;
import com.gameverse.games.core.BaseGame;
import java.util.*;

/**
 * Memory game with AI opponent and difficulty levels.
 * Easy: AI remembers few cards, makes many mistakes
 * Medium: AI remembers some cards
 * Hard: AI remembers most cards, plays efficiently
 */
public class MemoryGame extends BaseGame {

    private int gridSize;
    private Card[][] cards;
    private int firstRow = -1, firstCol = -1;
    private int secondRow = -1, secondCol = -1;
    private int matchesFound;
    private int attempts;
    private boolean waitingToUnflip = false;
    private int unflipTimer = 0;
    private boolean isPlayerTurn = true;

    // AI state
    private int aiMatchesFound;
    private Map<Integer, List<int[]>> aiMemory; // value -> list of positions seen
    private int aiTurnTimer = 0;
    private boolean aiThinking = false;
    private int aiFirstRow = -1, aiFirstCol = -1;
    private int aiSecondRow = -1, aiSecondCol = -1;
    private boolean aiWaitingToUnflip = false;
    private int aiUnflipTimer = 0;

    // Emoji symbols for cards (8 pairs for 4x4 grid)
    public static final String[] SYMBOLS = {
        "\uD83C\uDF4E", "\uD83C\uDF4A", "\uD83C\uDF4B", "\uD83C\uDF47",
        "\uD83C\uDF53", "\uD83E\uDED0", "\uD83C\uDF51", "\uD83C\uDF52"
    };

    public MemoryGame() {
        super("Memory Game");
        this.gridSize = 4;
        this.cards = new Card[gridSize][gridSize];
        this.matchesFound = 0;
        this.attempts = 0;
        this.aiMatchesFound = 0;
        this.aiMemory = new HashMap<>();
    }

    @Override
    public void initialize() {
        initializeCards();
    }

    @Override
    public void start() {
        super.start();
        initializeCards();
        matchesFound = 0;
        attempts = 0;
        aiMatchesFound = 0;
        waitingToUnflip = false;
        isPlayerTurn = true;
        aiMemory.clear();
    }

    @Override
    public void restart() {
        super.restart();
        initializeCards();
        matchesFound = 0;
        attempts = 0;
        aiMatchesFound = 0;
        waitingToUnflip = false;
        isPlayerTurn = true;
        aiMemory.clear();
    }

    @Override
    public void update(float deltaTime) {
        if (!isRunning()) return;

        // Handle player's unflip timer
        if (waitingToUnflip) {
            unflipTimer++;
            if (unflipTimer > 30) {
                cards[firstRow][firstCol].flip();
                cards[secondRow][secondCol].flip();
                firstRow = -1;
                firstCol = -1;
                secondRow = -1;
                secondCol = -1;
                waitingToUnflip = false;
                unflipTimer = 0;
                isPlayerTurn = false;
                aiTurnTimer = 0;
                aiThinking = true;
            }
        }
        // Handle AI turn
        else if (aiThinking && !aiWaitingToUnflip) {
            aiTurnTimer++;
            if (aiTurnTimer > 20) { // AI thinks for ~1 second
                executeAITurn();
                aiTurnTimer = 0;
            }
        }
        // Handle AI's unflip timer
        else if (aiWaitingToUnflip) {
            aiUnflipTimer++;
            if (aiUnflipTimer > 30) {
                cards[aiFirstRow][aiFirstCol].flip();
                cards[aiSecondRow][aiSecondCol].flip();
                aiFirstRow = -1;
                aiFirstCol = -1;
                aiSecondRow = -1;
                aiSecondCol = -1;
                aiWaitingToUnflip = false;
                aiUnflipTimer = 0;
                isPlayerTurn = true;
                aiThinking = false;
            }
        }
    }

    private void initializeCards() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < gridSize * gridSize / 2; i++) {
            values.add(i);
            values.add(i);
        }
        Collections.shuffle(values);

        int index = 0;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                cards[i][j] = new Card(values.get(index++));
            }
        }
        firstRow = -1;
        secondRow = -1;
        waitingToUnflip = false;
        unflipTimer = 0;
        aiFirstRow = -1;
        aiSecondRow = -1;
        aiWaitingToUnflip = false;
        aiUnflipTimer = 0;
        aiMemory.clear();
    }

    /**
     * Flip a card at the given position.
     * @return true if the flip was accepted
     */
    public boolean flipCard(int row, int col) {
        if (!isPlayerTurn) return false; // Not player's turn
        if (row < 0 || row >= gridSize || col < 0 || col >= gridSize) return false;
        if (waitingToUnflip) return false;

        Card card = cards[row][col];
        if (card.isFlipped() || card.isMatched()) return false;

        card.flip();

        // Add to AI memory when player flips
        aiMemory.computeIfAbsent(card.getValue(), k -> new ArrayList<>())
                .add(new int[]{row, col});

        if (firstRow == -1) {
            firstRow = row;
            firstCol = col;
            return true;
        } else if (secondRow == -1) {
            secondRow = row;
            secondCol = col;
            attempts++;

            if (cards[firstRow][firstCol].getValue() == cards[secondRow][secondCol].getValue()) {
                cards[firstRow][firstCol].setMatched(true);
                cards[secondRow][secondCol].setMatched(true);
                matchesFound++;
                score += 50;
                firstRow = -1;
                firstCol = -1;
                secondRow = -1;
                secondCol = -1;
                // Player gets another turn on match
                if (matchesFound + aiMatchesFound == (gridSize * gridSize) / 2) {
                    endGame();
                }
            } else {
                waitingToUnflip = true;
                unflipTimer = 0;
            }
            return true;
        }
        return false;
    }

    /** AI turn logic based on difficulty */
    private void executeAITurn() {
        int[] firstPick = null;
        int[] secondPick = null;

        switch (difficulty) {
            case EASY -> {
                // Easy AI: remembers very little, picks mostly random
                // 20% chance to use memory, 80% random
                if (Math.random() < 0.2 && !aiMemory.isEmpty()) {
                    int[][] result = findFromMemory();
                    if (result != null) {
                        firstPick = result[0];
                        secondPick = result[1];
                    }
                }
                if (firstPick == null) {
                    firstPick = findRandomUnmatched();
                }
                if (firstPick != null) {
                    // Add to AI memory
                    int val = cards[firstPick[0]][firstPick[1]].getValue();
                    aiMemory.computeIfAbsent(val, k -> new ArrayList<>())
                            .add(new int[]{firstPick[0], firstPick[1]});
                }
            }
            case MEDIUM -> {
                // Medium AI: uses memory sometimes, 50% chance
                if (Math.random() < 0.5) {
                    int[][] result = findFromMemory();
                    if (result != null) {
                        firstPick = result[0];
                        secondPick = result[1];
                    }
                }
                if (firstPick == null) {
                    firstPick = findRandomUnmatched();
                }
                if (firstPick != null) {
                    int val = cards[firstPick[0]][firstPick[1]].getValue();
                    aiMemory.computeIfAbsent(val, k -> new ArrayList<>())
                            .add(new int[]{firstPick[0], firstPick[1]});
                }
            }
            case HARD -> {
                // Hard AI: always uses memory, picks optimally
                int[][] result = findFromMemory();
                if (result != null) {
                    firstPick = result[0];
                    secondPick = result[1];
                }
                if (firstPick == null) {
                    firstPick = findRandomUnmatched();
                }
                if (firstPick != null) {
                    int val = cards[firstPick[0]][firstPick[1]].getValue();
                    aiMemory.computeIfAbsent(val, k -> new ArrayList<>())
                            .add(new int[]{firstPick[0], firstPick[1]});
                }
            }
        }

        if (firstPick == null) {
            // No valid moves, end AI turn
            aiThinking = false;
            isPlayerTurn = true;
            return;
        }

        // Flip first card
        cards[firstPick[0]][firstPick[1]].flip();
        aiFirstRow = firstPick[0];
        aiFirstCol = firstPick[1];

        // If we already know a matching pair, flip the second one immediately
        if (secondPick != null) {
            cards[secondPick[0]][secondPick[1]].flip();
            aiSecondRow = secondPick[0];
            aiSecondCol = secondPick[1];

            // It's a match!
            cards[aiFirstRow][aiFirstCol].setMatched(true);
            cards[aiSecondRow][aiSecondCol].setMatched(true);
            aiMatchesFound++;
            score += 25; // AI matching costs player points

            // Remove from AI memory
            int val = cards[aiFirstRow][aiFirstCol].getValue();
            aiMemory.remove(val);

            aiFirstRow = -1;
            aiFirstCol = -1;
            aiSecondRow = -1;
            aiSecondCol = -1;

            if (matchesFound + aiMatchesFound == (gridSize * gridSize) / 2) {
                endGame();
            } else {
                // AI gets another turn on match
                aiTurnTimer = 0;
            }
        } else {
            // Flip a random second card
            int[] secondRandom = findRandomUnmatchedExcluding(firstPick[0], firstPick[1]);
            if (secondRandom != null) {
                cards[secondRandom[0]][secondRandom[1]].flip();
                aiSecondRow = secondRandom[0];
                aiSecondCol = secondRandom[1];

                int val1 = cards[aiFirstRow][aiFirstCol].getValue();
                int val2 = cards[aiSecondRow][aiSecondCol].getValue();

                if (val1 == val2) {
                    // Match!
                    cards[aiFirstRow][aiFirstCol].setMatched(true);
                    cards[aiSecondRow][aiSecondCol].setMatched(true);
                    aiMatchesFound++;
                    score += 25;
                    aiMemory.remove(val1);

                    aiFirstRow = -1;
                    aiFirstCol = -1;
                    aiSecondRow = -1;
                    aiSecondCol = -1;

                    if (matchesFound + aiMatchesFound == (gridSize * gridSize) / 2) {
                        endGame();
                    } else {
                        aiTurnTimer = 0;
                    }
                } else {
                    // No match — schedule unflip
                    aiWaitingToUnflip = true;
                    aiUnflipTimer = 0;
                    // Add second card to memory
                    aiMemory.computeIfAbsent(val2, k -> new ArrayList<>())
                            .add(new int[]{aiSecondRow, aiSecondCol});
                }
            } else {
                // No second card available
                cards[aiFirstRow][aiFirstCol].flip();
                aiFirstRow = -1;
                aiFirstCol = -1;
                aiThinking = false;
                isPlayerTurn = true;
            }
        }
    }

    /** Try to find a matching pair from AI memory. Returns [first, second] or null if none found. */
    private int[][] findFromMemory() {
        for (var entry : aiMemory.entrySet()) {
            List<int[]> positions = entry.getValue();
            // Remove matched positions
            positions.removeIf(p -> cards[p[0]][p[1]].isMatched());
            if (positions.size() >= 2) {
                return new int[][]{positions.get(0), positions.get(1)};
            }
        }
        return null;
    }

    /** Find a random unmatched card */
    private int[] findRandomUnmatched() {
        List<int[]> available = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (!cards[i][j].isMatched() && !cards[i][j].isFlipped()) {
                    available.add(new int[]{i, j});
                }
            }
        }
        if (available.isEmpty()) return null;
        return available.get((int)(Math.random() * available.size()));
    }

    /** Find random unmatched card excluding a position */
    private int[] findRandomUnmatchedExcluding(int skipRow, int skipCol) {
        List<int[]> available = new ArrayList<>();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (!cards[i][j].isMatched() && !cards[i][j].isFlipped()
                    && !(i == skipRow && j == skipCol)) {
                    available.add(new int[]{i, j});
                }
            }
        }
        if (available.isEmpty()) return null;
        return available.get((int)(Math.random() * available.size()));
    }

    private void endGame() {
        score += 100;
        isRunning = false;
        // Player wins if they found more matches
        GameResult.Status status = matchesFound > aiMatchesFound
            ? GameResult.Status.WON
            : (matchesFound < aiMatchesFound ? GameResult.Status.LOST : GameResult.Status.COMPLETED);
        result = new GameResult(name, status, score, getElapsedTime());
    }

    /* ──── Public getters for rendering ──── */

    public int getGridSize() { return gridSize; }

    public boolean isCardFlipped(int row, int col) {
        return cards[row][col].isFlipped();
    }

    public boolean isCardMatched(int row, int col) {
        return cards[row][col].isMatched();
    }

    public int getCardValue(int row, int col) {
        return cards[row][col].getValue();
    }

    public boolean isWaitingToUnflip() { return waitingToUnflip; }
    public int getAttempts() { return attempts; }
    public int getMatchesFound() { return matchesFound; }
    public int getAiMatchesFound() { return aiMatchesFound; }
    public boolean isPlayerTurn() { return isPlayerTurn; }
    public boolean isAiThinking() { return aiThinking; }
    public int getTotalPairs() { return (gridSize * gridSize) / 2; }

    public static String getSymbolForValue(int value) {
        return SYMBOLS[value % SYMBOLS.length];
    }

    /* ──── Inner Card class ──── */

    private static class Card {
        private int value;
        private boolean flipped;
        private boolean matched;

        Card(int value) {
            this.value = value;
            this.flipped = false;
            this.matched = false;
        }

        void flip() { flipped = !flipped; }
        boolean isFlipped() { return flipped; }
        void setMatched(boolean m) { matched = m; }
        boolean isMatched() { return matched; }
        int getValue() { return value; }
    }
}
