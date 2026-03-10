package com.fruitblast.game.data

import android.graphics.Color

/**
 * Represents the type of a fruit on the game board.
 * Each fruit has an emoji symbol and a background color for canvas rendering.
 */
enum class FruitType(
    val emoji: String,
    val bgColor: Int,
    val glowColor: Int
) {
    APPLE("🍎", Color.parseColor("#FF3B30"), Color.parseColor("#FF6B6B")),
    BANANA("🍌", Color.parseColor("#FFCC00"), Color.parseColor("#FFE066")),
    CHERRY("🍒", Color.parseColor("#E91E8C"), Color.parseColor("#FF64B8")),
    GRAPE("🍇", Color.parseColor("#7B2D8B"), Color.parseColor("#B05DC8")),
    ORANGE("🍊", Color.parseColor("#FF8C00"), Color.parseColor("#FFB347")),
    WATERMELON("🍉", Color.parseColor("#2ECC71"), Color.parseColor("#5DE89A")),
    STRAWBERRY("🍓", Color.parseColor("#E74C3C"), Color.parseColor("#FF7675")),
    LEMON("🍋", Color.parseColor("#F39C12"), Color.parseColor("#FDCB6E")),
    ;

    companion object {
        val BASIC = values().take(5).toList()
        val STANDARD = values().take(6).toList()
        val FULL = values().toList()
    }
}
