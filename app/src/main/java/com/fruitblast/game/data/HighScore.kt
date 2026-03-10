package com.fruitblast.game.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HighScore(
    val score: Int,
    val difficulty: Difficulty,
    val maxCombo: Int,
    val totalMatches: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedDate: String
        get() = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            .format(Date(timestamp))

    val rank: String
        get() = when {
            score >= 50000 -> "🏆 传奇"
            score >= 30000 -> "💎 钻石"
            score >= 15000 -> "🥇 黄金"
            score >= 8000  -> "🥈 白银"
            score >= 3000  -> "🥉 青铜"
            else           -> "🌱 新手"
        }
}
