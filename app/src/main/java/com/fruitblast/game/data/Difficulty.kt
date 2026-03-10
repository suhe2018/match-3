package com.fruitblast.game.data

/**
 * Difficulty levels for the game.
 * Each difficulty configures the board size, fruit pool, time limit and scoring.
 */
enum class Difficulty(
    val displayName: String,
    val emoji: String,
    val description: String,
    val rows: Int,
    val cols: Int,
    val timeLimitSeconds: Int,
    val fruitTypes: List<FruitType>,
    val baseMatchScore: Int,
    val hasObstacles: Boolean,
    val obstacleCount: Int,
    val scoreMultiplier: Float,
    val hintDelaySeconds: Int
) {
    EASY(
        displayName = "简单",
        emoji = "🌱",
        description = "5种水果 · 90秒 · 慢速下落",
        rows = 8,
        cols = 8,
        timeLimitSeconds = 90,
        fruitTypes = FruitType.BASIC,
        baseMatchScore = 30,
        hasObstacles = false,
        obstacleCount = 0,
        scoreMultiplier = 1.0f,
        hintDelaySeconds = 5
    ),
    MEDIUM(
        displayName = "普通",
        emoji = "🌿",
        description = "6种水果 · 60秒 · 中速",
        rows = 8,
        cols = 8,
        timeLimitSeconds = 60,
        fruitTypes = FruitType.STANDARD,
        baseMatchScore = 50,
        hasObstacles = false,
        obstacleCount = 0,
        scoreMultiplier = 1.5f,
        hintDelaySeconds = 8
    ),
    HARD(
        displayName = "困难",
        emoji = "🔥",
        description = "8种水果 · 45秒 · 冰块障碍",
        rows = 8,
        cols = 8,
        timeLimitSeconds = 45,
        fruitTypes = FruitType.FULL,
        baseMatchScore = 80,
        hasObstacles = true,
        obstacleCount = 6,
        scoreMultiplier = 2.5f,
        hintDelaySeconds = 12
    );
}
