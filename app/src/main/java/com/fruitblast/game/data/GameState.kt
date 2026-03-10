package com.fruitblast.game.data

/**
 * Represents the overall state of the game session.
 */
data class GameState(
    val difficulty: Difficulty,
    var score: Int = 0,
    var combo: Int = 0,
    var maxCombo: Int = 0,
    var totalMatches: Int = 0,
    var timeRemainingMs: Long = difficulty.timeLimitSeconds * 1000L,
    var status: GameStatus = GameStatus.PLAYING,
    var lastMatchedFruits: Int = 0
) {
    val isGameOver: Boolean get() = status == GameStatus.GAME_OVER
    val isPaused: Boolean get() = status == GameStatus.PAUSED

    fun addScore(matchedCount: Int, isSpecialActivated: Boolean = false) {
        val baseScore = when {
            matchedCount >= 7 -> difficulty.baseMatchScore * 6
            matchedCount == 6 -> difficulty.baseMatchScore * 5
            matchedCount == 5 -> difficulty.baseMatchScore * 4
            matchedCount == 4 -> difficulty.baseMatchScore * 2
            else              -> difficulty.baseMatchScore
        }
        val comboBonus = 1f + (combo * 0.2f)
        val specialBonus = if (isSpecialActivated) 2.0f else 1.0f
        val points = (baseScore * comboBonus * specialBonus * difficulty.scoreMultiplier).toInt()
        score += points
        combo++
        if (combo > maxCombo) maxCombo = combo
        totalMatches++
        lastMatchedFruits = matchedCount
    }

    fun resetCombo() {
        combo = 0
    }

    fun endGame() {
        // Add time bonus: each remaining second is worth points
        val timeBonusSeconds = (timeRemainingMs / 1000).toInt()
        score += timeBonusSeconds * 5
        status = GameStatus.GAME_OVER
    }
}

enum class GameStatus {
    PLAYING,
    PAUSED,
    ANIMATING,  // Board is mid-animation, no input accepted
    GAME_OVER
}
