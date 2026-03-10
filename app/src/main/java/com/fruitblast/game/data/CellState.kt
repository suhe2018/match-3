package com.fruitblast.game.data

/**
 * Represents the visual/animation state of a board cell.
 */
enum class CellState {
    IDLE,        // Normal display
    SELECTED,    // Player has selected this cell
    HINTED,      // Shown as a hint for a valid move
    MATCHING,    // Part of a match (flash animation)
    FALLING,     // Falling after a match above was cleared
    SWAPPING,    // Being swapped with another cell
    EXPLODING,   // Special bomb/power-up explosion
    LOCKED       // Locked by ice (Hard mode obstacle)
}
