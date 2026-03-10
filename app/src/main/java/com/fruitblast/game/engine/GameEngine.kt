package com.fruitblast.game.engine

import com.fruitblast.game.data.*
import kotlin.random.Random

/**
 * Core game engine handling all match-3 logic:
 * - Board initialization (no initial matches)
 * - Swap validation and match detection
 * - Score calculation with combo multipliers
 * - Gravity (fruit fall) after matches
 * - Special fruit creation and activation
 * - Shuffle when no moves available
 */
class GameEngine(val difficulty: Difficulty) {

    val rows: Int = difficulty.rows
    val cols: Int = difficulty.cols
    val board: Array<Array<Cell>> = Array(rows) { Array(cols) { Cell.empty() } }
    val gameState: GameState = GameState(difficulty)

    // Listener for game events (animations, sounds, UI updates)
    var listener: GameEventListener? = null

    // ─── Board Initialisation ─────────────────────────────────────────────────

    fun initBoard() {
        // Place obstacles (locked ice cells) for Hard mode
        val obstacles = mutableSetOf<Pair<Int, Int>>()
        if (difficulty.hasObstacles) {
            while (obstacles.size < difficulty.obstacleCount) {
                val r = Random.nextInt(2, rows - 2)
                val c = Random.nextInt(1, cols - 1)
                obstacles.add(r to c)
            }
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (r to c in obstacles) {
                    board[r][c] = Cell.locked()
                    continue
                }
                board[r][c] = Cell.of(randomFruitAvoidingMatch(r, c))
            }
        }

        // Guarantee at least one valid move exists
        if (!hasAnyValidMove()) shuffleBoard()
    }

    private fun randomFruitAvoidingMatch(row: Int, col: Int): FruitType {
        val pool = difficulty.fruitTypes.toMutableList()
        // Shuffle and pick one that doesn't create an initial match
        pool.shuffle()
        for (fruit in pool) {
            if (!wouldCreateMatch(row, col, fruit)) return fruit
        }
        return pool.first()
    }

    private fun wouldCreateMatch(row: Int, col: Int, fruit: FruitType): Boolean {
        // Check horizontal: need 2 matching to the left
        if (col >= 2 &&
            board[row][col - 1].fruitType == fruit &&
            board[row][col - 2].fruitType == fruit) return true
        // Check vertical: need 2 matching above
        if (row >= 2 &&
            board[row - 1][col].fruitType == fruit &&
            board[row - 2][col].fruitType == fruit) return true
        return false
    }

    // ─── Swap Logic ───────────────────────────────────────────────────────────

    /**
     * Attempt to swap two cells. Returns true if a match was made.
     * Fires events for animation and sound.
     */
    fun trySwap(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        if (!isAdjacent(r1, c1, r2, c2)) return false
        if (board[r1][c1].isEmpty || board[r2][c2].isEmpty) return false
        if (board[r1][c1].isLocked || board[r2][c2].isLocked) return false

        swapCells(r1, c1, r2, c2)

        val matches = findAllMatches()
        if (matches.isEmpty()) {
            swapCells(r1, c1, r2, c2)  // revert
            listener?.onInvalidSwap(r1, c1, r2, c2)
            return false
        }

        listener?.onValidSwap(r1, c1, r2, c2)
        processMatchCascade(matches)
        return true
    }

    private fun isAdjacent(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val dr = Math.abs(r1 - r2)
        val dc = Math.abs(c1 - c2)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    private fun swapCells(r1: Int, c1: Int, r2: Int, c2: Int) {
        val tmp = board[r1][c1].copy()
        board[r1][c1] = board[r2][c2].copy()
        board[r2][c2] = tmp
    }

    // ─── Match Detection ──────────────────────────────────────────────────────

    /**
     * Returns all matched groups on the board as sets of (row, col) pairs.
     * Each element is a connected match group.
     */
    fun findAllMatches(): List<MatchGroup> {
        val allMatched = mutableSetOf<Pair<Int, Int>>()
        val groups = mutableListOf<MatchGroup>()

        // Horizontal matches
        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                val fruit = board[r][c].fruitType ?: run { c++; continue }
                if (board[r][c].isLocked) { c++; continue }
                var len = 1
                while (c + len < cols &&
                    board[r][c + len].fruitType == fruit &&
                    !board[r][c + len].isLocked) len++
                if (len >= 3) {
                    val cells = (c until c + len).map { r to it }.toSet()
                    groups.add(MatchGroup(cells, MatchDirection.HORIZONTAL, len, fruit))
                    allMatched.addAll(cells)
                }
                c += len
            }
        }

        // Vertical matches
        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                val fruit = board[r][c].fruitType ?: run { r++; continue }
                if (board[r][c].isLocked) { r++; continue }
                var len = 1
                while (r + len < rows &&
                    board[r + len][c].fruitType == fruit &&
                    !board[r + len][c].isLocked) len++
                if (len >= 3) {
                    val cells = (r until r + len).map { it to c }.toSet()
                    // Merge overlapping groups
                    val existing = groups.firstOrNull { it.cells.intersect(cells).isNotEmpty() }
                    if (existing != null) {
                        val merged = existing.cells + cells
                        groups.remove(existing)
                        groups.add(MatchGroup(merged, MatchDirection.CROSS, merged.size, fruit))
                    } else {
                        groups.add(MatchGroup(cells, MatchDirection.VERTICAL, len, fruit))
                    }
                    allMatched.addAll(cells)
                }
                r += len
            }
        }

        return groups
    }

    // ─── Match Processing & Cascade ──────────────────────────────────────────

    private fun processMatchCascade(initialMatches: List<MatchGroup>) {
        var matches = initialMatches
        var isFirstMatch = true

        while (matches.isNotEmpty()) {
            if (isFirstMatch) {
                isFirstMatch = false
            } else {
                // Subsequent cascades reset combo counter (chain still builds)
            }

            // Score all matches
            val totalCellsCleared = matches.sumOf { it.cells.size }
            val hasSpecial = matches.any { it.cells.any { (r, c) -> board[r][c].isSpecial } }
            gameState.addScore(totalCellsCleared, hasSpecial)

            // Determine special fruits to create before clearing
            val specialsToCreate = mutableListOf<Triple<Int, Int, SpecialType>>()
            for (group in matches) {
                val special = determineSpecialType(group)
                if (special != SpecialType.NONE) {
                    val pivotCell = group.cells.first()
                    specialsToCreate.add(Triple(pivotCell.first, pivotCell.second, special))
                }
            }

            // Collect all cells to clear (including activated specials)
            val cellsToClear = mutableSetOf<Pair<Int, Int>>()
            for (group in matches) {
                cellsToClear.addAll(group.cells)
                // Activate any special fruits in the group
                for ((r, c) in group.cells) {
                    if (board[r][c].isSpecial) {
                        cellsToClear.addAll(activateSpecial(r, c))
                    }
                }
            }

            // Notify listener for flash animation
            listener?.onCellsMatched(cellsToClear.toList(), gameState.combo, gameState.score)

            // Clear matched cells
            for ((r, c) in cellsToClear) {
                if (!board[r][c].isLocked) {
                    board[r][c] = Cell.empty()
                }
            }

            // Create special fruits at pivot positions
            for ((r, c, specialType) in specialsToCreate) {
                if (board[r][c].isEmpty) {
                    val fruitType = difficulty.fruitTypes.random()
                    board[r][c] = Cell(fruitType, isSpecial = true, specialType = specialType)
                }
            }

            // Apply gravity
            applyGravity()

            // Fill empty cells from top
            fillFromTop()

            // Check for new matches (cascade)
            matches = findAllMatches()

            if (matches.isEmpty()) {
                gameState.resetCombo()
                listener?.onCascadeEnd()
            } else {
                listener?.onCascadeStart()
            }
        }

        if (!hasAnyValidMove()) {
            listener?.onNoMovesAvailable()
            shuffleBoard()
        }
    }

    private fun determineSpecialType(group: MatchGroup): SpecialType {
        return when {
            group.cells.size >= 5 && group.direction == MatchDirection.HORIZONTAL -> SpecialType.RAINBOW_FRUIT
            group.cells.size >= 5 && group.direction == MatchDirection.VERTICAL   -> SpecialType.RAINBOW_FRUIT
            group.direction == MatchDirection.CROSS                               -> SpecialType.CROSS_BOMB
            group.cells.size == 4 && group.direction == MatchDirection.HORIZONTAL -> SpecialType.ROW_BOMB
            group.cells.size == 4 && group.direction == MatchDirection.VERTICAL   -> SpecialType.COL_BOMB
            else -> SpecialType.NONE
        }
    }

    private fun activateSpecial(row: Int, col: Int): Set<Pair<Int, Int>> {
        val cleared = mutableSetOf<Pair<Int, Int>>()
        val fruit = board[row][col]
        when (fruit.specialType) {
            SpecialType.ROW_BOMB -> {
                for (c in 0 until cols) cleared.add(row to c)
            }
            SpecialType.COL_BOMB -> {
                for (r in 0 until rows) cleared.add(r to col)
            }
            SpecialType.CROSS_BOMB -> {
                for (c in 0 until cols) cleared.add(row to c)
                for (r in 0 until rows) cleared.add(r to col)
            }
            SpecialType.RAINBOW_FRUIT -> {
                // Clear all fruits of a random type
                val targetType = difficulty.fruitTypes.random()
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        if (board[r][c].fruitType == targetType) cleared.add(r to c)
                    }
                }
            }
            else -> {}
        }
        return cleared
    }

    // ─── Gravity & Fill ──────────────────────────────────────────────────────

    private fun applyGravity() {
        for (c in 0 until cols) {
            // Compact column downward (ignore locked cells)
            val stack = mutableListOf<Cell>()
            for (r in rows - 1 downTo 0) {
                if (board[r][c].isLocked) continue
                if (!board[r][c].isEmpty) stack.add(board[r][c].copy())
            }
            var stackIdx = 0
            for (r in rows - 1 downTo 0) {
                if (board[r][c].isLocked) continue
                board[r][c] = if (stackIdx < stack.size) stack[stackIdx++] else Cell.empty()
            }
        }
    }

    private fun fillFromTop() {
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                if (board[r][c].isEmpty) {
                    val fruit = difficulty.fruitTypes.random()
                    board[r][c] = Cell.of(fruit)
                }
            }
        }
    }

    // ─── Valid Move Detection ─────────────────────────────────────────────────

    fun hasAnyValidMove(): Boolean {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r][c].isEmpty || board[r][c].isLocked) continue
                // Try swap right
                if (c + 1 < cols && !board[r][c + 1].isLocked) {
                    swapCells(r, c, r, c + 1)
                    val hasMatch = findAllMatches().isNotEmpty()
                    swapCells(r, c, r, c + 1)
                    if (hasMatch) return true
                }
                // Try swap down
                if (r + 1 < rows && !board[r + 1][c].isLocked) {
                    swapCells(r, c, r + 1, c)
                    val hasMatch = findAllMatches().isNotEmpty()
                    swapCells(r, c, r + 1, c)
                    if (hasMatch) return true
                }
            }
        }
        return false
    }

    /**
     * Returns a hint: the first valid swap found as a pair of positions.
     */
    fun getHint(): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r][c].isEmpty || board[r][c].isLocked) continue
                if (c + 1 < cols && !board[r][c + 1].isLocked) {
                    swapCells(r, c, r, c + 1)
                    val hasMatch = findAllMatches().isNotEmpty()
                    swapCells(r, c, r, c + 1)
                    if (hasMatch) return (r to c) to (r to c + 1)
                }
                if (r + 1 < rows && !board[r + 1][c].isLocked) {
                    swapCells(r, c, r + 1, c)
                    val hasMatch = findAllMatches().isNotEmpty()
                    swapCells(r, c, r + 1, c)
                    if (hasMatch) return (r to c) to (r + 1 to c)
                }
            }
        }
        return null
    }

    // ─── Shuffle ─────────────────────────────────────────────────────────────

    fun shuffleBoard() {
        val fruits = mutableListOf<FruitType>()
        val lockedCells = mutableSetOf<Pair<Int, Int>>()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (board[r][c].isLocked) {
                    lockedCells.add(r to c)
                } else {
                    board[r][c].fruitType?.let { fruits.add(it) }
                }
            }
        }

        fruits.shuffle()
        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (r to c !in lockedCells) {
                    board[r][c] = if (idx < fruits.size) Cell.of(fruits[idx++]) else Cell.empty()
                }
            }
        }

        // Ensure valid moves after shuffle
        if (!hasAnyValidMove()) shuffleBoard()

        listener?.onBoardShuffled()
    }

    // ─── Timer & End ─────────────────────────────────────────────────────────

    fun tickTimer(deltaMs: Long) {
        if (gameState.isPaused || gameState.isGameOver) return
        gameState.timeRemainingMs -= deltaMs
        if (gameState.timeRemainingMs <= 0) {
            gameState.timeRemainingMs = 0
            gameState.endGame()
            listener?.onGameOver(gameState)
        }
    }
}

// ─── Support Types ────────────────────────────────────────────────────────────

data class MatchGroup(
    val cells: Set<Pair<Int, Int>>,
    val direction: MatchDirection,
    val length: Int,
    val fruitType: FruitType
)

enum class MatchDirection { HORIZONTAL, VERTICAL, CROSS }

interface GameEventListener {
    fun onValidSwap(r1: Int, c1: Int, r2: Int, c2: Int)
    fun onInvalidSwap(r1: Int, c1: Int, r2: Int, c2: Int)
    fun onCellsMatched(cells: List<Pair<Int, Int>>, combo: Int, score: Int)
    fun onCascadeStart()
    fun onCascadeEnd()
    fun onNoMovesAvailable()
    fun onBoardShuffled()
    fun onGameOver(state: GameState)
}
