package com.fruitblast.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameEngine {

    public static final int BOARD_SIZE = 8;

    // Fruit emojis and background tint colors (index 0 unused, 1–8 are fruits)
    public static final String[] FRUIT_EMOJIS = {
            "", "🍎", "🍊", "🍋", "🍇", "🍒", "🫐", "🍓", "🍑"
    };
    public static final int[] FRUIT_COLORS = {
            0x00000000,
            0xFFFF3B30, // apple
            0xFFFF8C00, // orange
            0xFFFFCC00, // lemon
            0xFF7B2D8B, // grape
            0xFFE91E8C, // cherry
            0xFF2E4A9E, // blueberry
            0xFFE74C3C, // strawberry
            0xFFFF6B35  // peach
    };

    public interface GameCallback {
        void onScoreChanged(int score);
        void onComboChanged(int combo);
        void onGameOver(GameState state);
        void onBoardChanged();
        void onShuffled();
    }

    private final Cell[][] board;
    private final Difficulty difficulty;
    private final GameState state;
    private final Random random = new Random();
    private GameCallback callback;

    public GameEngine(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.board = new Cell[BOARD_SIZE][BOARD_SIZE];
        this.state = new GameState(difficulty.timeSeconds * 1000L);
        initBoard();
    }

    public void setCallback(GameCallback callback) {
        this.callback = callback;
    }

    // ── Board initialisation ──────────────────────────────────────────────────

    private void initBoard() {
        boolean[][] obstacles = new boolean[BOARD_SIZE][BOARD_SIZE];
        if (difficulty.hasObstacles) {
            int placed = 0;
            while (placed < 6) {
                int r = random.nextInt(BOARD_SIZE);
                int c = random.nextInt(BOARD_SIZE);
                if (!obstacles[r][c]) {
                    obstacles[r][c] = true;
                    placed++;
                }
            }
        }
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (obstacles[r][c]) {
                    board[r][c] = new Cell(0, true);
                } else {
                    board[r][c] = new Cell(randomFruitAvoidingMatch(r, c), false);
                }
            }
        }
        ensureValidMoves();
    }

    private int randomFruitAvoidingMatch(int r, int c) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int f = random.nextInt(difficulty.fruitCount) + 1;
            if (!wouldCreateMatch(r, c, f)) return f;
        }
        return random.nextInt(difficulty.fruitCount) + 1;
    }

    private boolean wouldCreateMatch(int r, int c, int fruit) {
        if (c >= 2 && board[r][c - 1] != null && board[r][c - 2] != null
                && !board[r][c - 1].isLocked && !board[r][c - 2].isLocked
                && board[r][c - 1].fruitIndex == fruit && board[r][c - 2].fruitIndex == fruit)
            return true;
        if (r >= 2 && board[r - 1][c] != null && board[r - 2][c] != null
                && !board[r - 1][c].isLocked && !board[r - 2][c].isLocked
                && board[r - 1][c].fruitIndex == fruit && board[r - 2][c].fruitIndex == fruit)
            return true;
        return false;
    }

    private void ensureValidMoves() {
        for (int i = 0; i < 10; i++) {
            if (hasValidMove()) return;
            shuffleFruits();
        }
    }

    private void shuffleFruits() {
        List<Integer> fruits = new ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                if (!board[r][c].isLocked && !board[r][c].isEmpty())
                    fruits.add(board[r][c].fruitIndex);

        for (int i = fruits.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = fruits.get(i);
            fruits.set(i, fruits.get(j));
            fruits.set(j, tmp);
        }
        int idx = 0;
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                if (!board[r][c].isLocked && !board[r][c].isEmpty())
                    board[r][c].fruitIndex = fruits.get(idx++);
    }

    // ── Swap & match logic ────────────────────────────────────────────────────

    /** Returns true if the swap is valid (produces a match) and processes it. */
    public boolean trySwap(int r1, int c1, int r2, int c2) {
        if (!inBounds(r1, c1) || !inBounds(r2, c2)) return false;
        if (board[r1][c1].isLocked || board[r2][c2].isLocked) return false;
        if (board[r1][c1].isEmpty() || board[r2][c2].isEmpty()) return false;

        // Perform swap
        int tmp = board[r1][c1].fruitIndex;
        board[r1][c1].fruitIndex = board[r2][c2].fruitIndex;
        board[r2][c2].fruitIndex = tmp;

        List<int[]> matches = findMatches();
        if (matches.isEmpty()) {
            // Revert
            board[r2][c2].fruitIndex = board[r1][c1].fruitIndex;
            board[r1][c1].fruitIndex = tmp;
            state.combo = 0;
            if (callback != null) callback.onComboChanged(0);
            return false;
        }
        processMatches(matches);
        return true;
    }

    private void processMatches(List<int[]> matches) {
        state.combo++;
        if (state.combo > state.maxCombo) state.maxCombo = state.combo;
        state.totalMatches += matches.size();
        state.score += matches.size() * 10 * Math.max(1, state.combo);

        for (int[] cell : matches) board[cell[0]][cell[1]].fruitIndex = 0;

        if (callback != null) {
            callback.onScoreChanged(state.score);
            callback.onComboChanged(state.combo);
        }

        applyGravity();
        refillBoard();

        List<int[]> chain = findMatches();
        if (!chain.isEmpty()) {
            processMatches(chain);
        } else {
            state.combo = 0;
            if (callback != null) callback.onComboChanged(0);
            if (!hasValidMove()) {
                shuffleFruits();
                if (callback != null) callback.onShuffled();
            }
            if (callback != null) callback.onBoardChanged();
        }
    }

    private void applyGravity() {
        for (int c = 0; c < BOARD_SIZE; c++) {
            // Collect non-locked fruits from bottom to top
            List<Integer> fruits = new ArrayList<>();
            for (int r = BOARD_SIZE - 1; r >= 0; r--)
                if (!board[r][c].isLocked && !board[r][c].isEmpty())
                    fruits.add(board[r][c].fruitIndex);
            // Clear non-locked cells
            for (int r = 0; r < BOARD_SIZE; r++)
                if (!board[r][c].isLocked) board[r][c].fruitIndex = 0;
            // Re-fill from bottom, skipping locked cells
            int fi = 0;
            for (int r = BOARD_SIZE - 1; r >= 0 && fi < fruits.size(); r--)
                if (!board[r][c].isLocked) board[r][c].fruitIndex = fruits.get(fi++);
        }
    }

    private void refillBoard() {
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                if (!board[r][c].isLocked && board[r][c].isEmpty())
                    board[r][c].fruitIndex = random.nextInt(difficulty.fruitCount) + 1;
    }

    private List<int[]> findMatches() {
        boolean[][] matched = new boolean[BOARD_SIZE][BOARD_SIZE];
        // Horizontal
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE - 2; c++) {
                int f = board[r][c].fruitIndex;
                if (f == 0 || board[r][c].isLocked) continue;
                if (board[r][c + 1].fruitIndex == f && !board[r][c + 1].isLocked
                        && board[r][c + 2].fruitIndex == f && !board[r][c + 2].isLocked) {
                    matched[r][c] = matched[r][c + 1] = matched[r][c + 2] = true;
                    for (int k = c + 3; k < BOARD_SIZE && board[r][k].fruitIndex == f
                            && !board[r][k].isLocked; k++) matched[r][k] = true;
                }
            }
        }
        // Vertical
        for (int r = 0; r < BOARD_SIZE - 2; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                int f = board[r][c].fruitIndex;
                if (f == 0 || board[r][c].isLocked) continue;
                if (board[r + 1][c].fruitIndex == f && !board[r + 1][c].isLocked
                        && board[r + 2][c].fruitIndex == f && !board[r + 2][c].isLocked) {
                    matched[r][c] = matched[r + 1][c] = matched[r + 2][c] = true;
                    for (int k = r + 3; k < BOARD_SIZE && board[k][c].fruitIndex == f
                            && !board[k][c].isLocked; k++) matched[k][c] = true;
                }
            }
        }
        List<int[]> result = new ArrayList<>();
        for (int r = 0; r < BOARD_SIZE; r++)
            for (int c = 0; c < BOARD_SIZE; c++)
                if (matched[r][c]) result.add(new int[]{r, c});
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public boolean hasValidMove() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c].isEmpty() || board[r][c].isLocked) continue;
                if (c + 1 < BOARD_SIZE && !board[r][c + 1].isLocked && !board[r][c + 1].isEmpty()
                        && swapProducesMatch(r, c, r, c + 1)) return true;
                if (r + 1 < BOARD_SIZE && !board[r + 1][c].isLocked && !board[r + 1][c].isEmpty()
                        && swapProducesMatch(r, c, r + 1, c)) return true;
            }
        }
        return false;
    }

    private boolean swapProducesMatch(int r1, int c1, int r2, int c2) {
        int tmp = board[r1][c1].fruitIndex;
        board[r1][c1].fruitIndex = board[r2][c2].fruitIndex;
        board[r2][c2].fruitIndex = tmp;
        boolean has = !findMatches().isEmpty();
        board[r2][c2].fruitIndex = board[r1][c1].fruitIndex;
        board[r1][c1].fruitIndex = tmp;
        return has;
    }

    /** Returns {r1, c1, r2, c2} of a valid swap, or null if none. */
    public int[] findHint() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (board[r][c].isEmpty() || board[r][c].isLocked) continue;
                if (c + 1 < BOARD_SIZE && !board[r][c + 1].isLocked && !board[r][c + 1].isEmpty()
                        && swapProducesMatch(r, c, r, c + 1))
                    return new int[]{r, c, r, c + 1};
                if (r + 1 < BOARD_SIZE && !board[r + 1][c].isLocked && !board[r + 1][c].isEmpty()
                        && swapProducesMatch(r, c, r + 1, c))
                    return new int[]{r, c, r + 1, c};
            }
        }
        return null;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    public void tick(long deltaMs) {
        if (state.isGameOver || state.isPaused) return;
        state.timeRemainingMs -= deltaMs;
        if (state.timeRemainingMs <= 0) {
            state.timeRemainingMs = 0;
            state.isGameOver = true;
            if (callback != null) callback.onGameOver(state);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Cell getCell(int r, int c) { return board[r][c]; }
    public int getBoardSize() { return BOARD_SIZE; }
    public GameState getState() { return state; }
    public Difficulty getDifficulty() { return difficulty; }
}
