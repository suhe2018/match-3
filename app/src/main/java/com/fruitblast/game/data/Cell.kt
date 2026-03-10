package com.fruitblast.game.data

/**
 * Represents a single cell on the game board.
 *
 * @param fruitType The type of fruit in this cell, or null if empty.
 * @param state The current visual/animation state.
 * @param isSpecial Whether this cell holds a special power-up fruit.
 * @param specialType The type of special power-up if isSpecial is true.
 */
data class Cell(
    var fruitType: FruitType? = null,
    var state: CellState = CellState.IDLE,
    var isSpecial: Boolean = false,
    var specialType: SpecialType = SpecialType.NONE,
    var animOffset: Float = 0f,   // Y offset for fall animation
    var animAlpha: Float = 1f,    // Alpha for flash/fade animation
    var animScale: Float = 1f     // Scale for pop/spawn animation
) {
    val isEmpty: Boolean get() = fruitType == null
    val isLocked: Boolean get() = state == CellState.LOCKED

    companion object {
        fun empty() = Cell(fruitType = null)
        fun of(type: FruitType) = Cell(fruitType = type)
        fun locked() = Cell(fruitType = null, state = CellState.LOCKED)
    }
}

/**
 * Power-up types created by special matches.
 */
enum class SpecialType {
    NONE,
    ROW_BOMB,       // Created by matching 4 in a row → clears entire row
    COL_BOMB,       // Created by matching 4 in a column → clears entire column
    CROSS_BOMB,     // Created by L/T shape match → clears row + column
    RAINBOW_FRUIT   // Created by matching 5 in a line → clears all of one type
}
