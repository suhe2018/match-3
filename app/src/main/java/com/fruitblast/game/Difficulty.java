package com.fruitblast.game;

public enum Difficulty {

    EASY("简单", "🌱", 5, 90, false),
    MEDIUM("普通", "🌿", 6, 60, false),
    HARD("困难", "🔥", 8, 45, true);

    public final String displayName;
    public final String emoji;
    public final int fruitCount;   // number of distinct fruit types
    public final int timeSeconds;
    public final boolean hasObstacles;

    Difficulty(String displayName, String emoji, int fruitCount, int timeSeconds, boolean hasObstacles) {
        this.displayName = displayName;
        this.emoji = emoji;
        this.fruitCount = fruitCount;
        this.timeSeconds = timeSeconds;
        this.hasObstacles = hasObstacles;
    }
}
