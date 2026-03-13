package com.fruitblast.game;

public class GameState {
    public int score;
    public int combo;
    public int maxCombo;
    public int totalMatches;
    public long timeRemainingMs;
    public boolean isGameOver;
    public boolean isPaused;

    public GameState(long timeLimitMs) {
        this.timeRemainingMs = timeLimitMs;
    }
}
