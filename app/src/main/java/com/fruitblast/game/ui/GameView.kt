package com.fruitblast.game.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import com.fruitblast.game.data.*
import com.fruitblast.game.engine.GameEngine
import com.fruitblast.game.engine.GameEventListener
import kotlin.math.abs
import kotlin.math.min

/**
 * Custom View that renders the match-3 game board using Canvas APIs.
 *
 * Responsibilities:
 * - Drawing fruit cells, grid, highlights, special indicators
 * - Handling touch input (tap-to-select + tap-to-swap)
 * - Running animations: match flash, gravity fall, spawn pop, swap wobble
 * - Showing hint highlights
 */
class GameView(context: Context) : View(context), GameEventListener {

    // ─── Engine ───────────────────────────────────────────────────────────────
    lateinit var engine: GameEngine
        private set

    fun attachEngine(eng: GameEngine) {
        engine = eng
        eng.listener = this
    }

    // ─── Callbacks for Activity ───────────────────────────────────────────────
    var onScoreChanged: ((Int) -> Unit)? = null
    var onComboChanged: ((Int) -> Unit)? = null
    var onGameOver: ((GameState) -> Unit)? = null
    var onSwapSound: (() -> Unit)? = null
    var onMatchSound: (() -> Unit)? = null
    var onInvalidSound: (() -> Unit)? = null
    var onShuffleSound: (() -> Unit)? = null

    // ─── Selection State ──────────────────────────────────────────────────────
    private var selectedRow = -1
    private var selectedCol = -1
    private var isAnimating = false
    private var hintRow1 = -1; private var hintCol1 = -1
    private var hintRow2 = -1; private var hintCol2 = -1

    // ─── Layout ───────────────────────────────────────────────────────────────
    private var cellSize = 0f
    private var boardOffsetX = 0f
    private var boardOffsetY = 0f
    private val cellPadding get() = cellSize * 0.07f

    // ─── Paints ───────────────────────────────────────────────────────────────
    private val cellBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.WHITE
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.YELLOW
    }
    private val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val lockedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#A0B8D8E0")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000")
    }
    private val specialBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.SOLID)
    }

    // Per-cell animation state (alpha for flash, scaleY for fall)
    private val cellAnimAlpha = Array(9) { FloatArray(9) { 1f } }
    private val cellAnimScale = Array(9) { FloatArray(9) { 1f } }
    private val cellAnimOffsetY = Array(9) { FloatArray(9) { 0f } }

    // Hint pulse animation
    private var hintPulseAlpha = 1f
    private val hintAnimator = ValueAnimator.ofFloat(0.3f, 1f).apply {
        duration = 600
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { hintPulseAlpha = it.animatedValue as Float; invalidate() }
    }

    private val handler = Handler(Looper.getMainLooper())

    // ─── Layout ───────────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!::engine.isInitialized) return
        computeLayout(w, h)
    }

    private fun computeLayout(w: Int, h: Int) {
        val rows = engine.rows
        val cols = engine.cols
        val maxCellW = w.toFloat() / cols
        val maxCellH = h.toFloat() / rows
        cellSize = min(maxCellW, maxCellH) * 0.97f
        boardOffsetX = (w - cellSize * cols) / 2f
        boardOffsetY = (h - cellSize * rows) / 2f
        emojiPaint.textSize = cellSize * 0.58f
        specialBadgePaint.textSize = cellSize * 0.25f
    }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!::engine.isInitialized) return

        drawBackground(canvas)
        drawCells(canvas)
        drawSelectionHighlight(canvas)
        drawHintHighlight(canvas)
    }

    private fun drawBackground(canvas: Canvas) {
        val bgPaint = Paint().apply { color = Color.parseColor("#1A1A2E") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        // Subtle grid lines
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22FFFFFF")
            strokeWidth = 1f
        }
        val rows = engine.rows; val cols = engine.cols
        for (r in 0..rows) {
            val y = boardOffsetY + r * cellSize
            canvas.drawLine(boardOffsetX, y, boardOffsetX + cols * cellSize, y, gridPaint)
        }
        for (c in 0..cols) {
            val x = boardOffsetX + c * cellSize
            canvas.drawLine(x, boardOffsetY, x, boardOffsetY + rows * cellSize, gridPaint)
        }
    }

    private fun drawCells(canvas: Canvas) {
        val rows = engine.rows; val cols = engine.cols
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                drawCell(canvas, r, c)
            }
        }
    }

    private fun drawCell(canvas: Canvas, row: Int, col: Int) {
        val cell = engine.board[row][col]
        val left = boardOffsetX + col * cellSize + cellPadding
        val top = boardOffsetY + row * cellSize + cellPadding + cellAnimOffsetY[row][col]
        val right = left + cellSize - cellPadding * 2
        val bottom = top + cellSize - cellPadding * 2

        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        val radius = (cellSize / 2f) - cellPadding

        val alpha = (cellAnimAlpha[row][col] * 255).toInt().coerceIn(0, 255)
        val scale = cellAnimScale[row][col]

        canvas.save()
        canvas.scale(scale, scale, cx, cy)

        // Locked ice cell
        if (cell.isLocked) {
            lockedPaint.alpha = alpha
            canvas.drawRoundRect(left, top, right, bottom, radius * 0.3f, radius * 0.3f, lockedPaint)
            emojiPaint.alpha = alpha
            canvas.drawText("🧊", cx, cy + emojiPaint.textSize * 0.35f, emojiPaint)
            canvas.restore()
            return
        }

        val fruitType = cell.fruitType ?: run { canvas.restore(); return }

        // Drop shadow
        shadowPaint.alpha = (alpha * 0.4f).toInt()
        canvas.drawCircle(cx + 3f, cy + 5f, radius, shadowPaint)

        // Glow for special fruits
        if (cell.isSpecial) {
            glowPaint.color = Color.argb((alpha * 0.6f).toInt(), 255, 255, 100)
            canvas.drawCircle(cx, cy, radius + 8f, glowPaint)
        }

        // Background circle
        cellBgPaint.color = fruitType.bgColor
        cellBgPaint.alpha = alpha
        canvas.drawCircle(cx, cy, radius, cellBgPaint)

        // Highlight shine (top-left)
        val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.alpha = (alpha * 0.3f).toInt()
        }
        canvas.drawCircle(cx - radius * 0.25f, cy - radius * 0.25f, radius * 0.35f, shinePaint)

        // Emoji fruit
        emojiPaint.alpha = alpha
        val textY = cy + emojiPaint.textSize * 0.35f
        canvas.drawText(fruitType.emoji, cx, textY, emojiPaint)

        // Special badge overlay
        if (cell.isSpecial) {
            val badge = when (cell.specialType) {
                SpecialType.ROW_BOMB      -> "↔"
                SpecialType.COL_BOMB      -> "↕"
                SpecialType.CROSS_BOMB    -> "✚"
                SpecialType.RAINBOW_FRUIT -> "★"
                else -> ""
            }
            if (badge.isNotEmpty()) {
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = cellSize * 0.28f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    this.alpha = alpha
                }
                canvas.drawText(badge, cx, bottom - cellPadding * 1.5f, badgePaint)
            }
        }

        canvas.restore()
    }

    private fun drawSelectionHighlight(canvas: Canvas) {
        if (selectedRow < 0 || selectedCol < 0) return
        val cx = boardOffsetX + selectedCol * cellSize + cellSize / 2f
        val cy = boardOffsetY + selectedRow * cellSize + cellSize / 2f
        val radius = cellSize / 2f - cellPadding

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = Color.WHITE
            alpha = 220
        }
        canvas.drawCircle(cx, cy, radius + 4f, ringPaint)

        val glowRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.parseColor("#80FFFFFF")
        }
        canvas.drawCircle(cx, cy, radius + 8f, glowRingPaint)
    }

    private fun drawHintHighlight(canvas: Canvas) {
        if (hintRow1 < 0) return
        drawHintRing(canvas, hintRow1, hintCol1)
        drawHintRing(canvas, hintRow2, hintCol2)
    }

    private fun drawHintRing(canvas: Canvas, row: Int, col: Int) {
        val cx = boardOffsetX + col * cellSize + cellSize / 2f
        val cy = boardOffsetY + row * cellSize + cellSize / 2f
        val radius = cellSize / 2f - cellPadding
        hintPaint.alpha = (hintPulseAlpha * 255).toInt()
        hintPaint.color = Color.YELLOW
        canvas.drawCircle(cx, cy, radius + 4f, hintPaint)
    }

    // ─── Touch Handling ───────────────────────────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        if (isAnimating) return true
        if (!::engine.isInitialized) return true
        if (engine.gameState.isGameOver || engine.gameState.isPaused) return true

        val touchX = event.x
        val touchY = event.y
        val col = ((touchX - boardOffsetX) / cellSize).toInt()
        val row = ((touchY - boardOffsetY) / cellSize).toInt()

        if (row < 0 || row >= engine.rows || col < 0 || col >= engine.cols) {
            clearSelection()
            return true
        }

        clearHint()

        if (selectedRow < 0) {
            // First tap: select
            if (!engine.board[row][col].isEmpty && !engine.board[row][col].isLocked) {
                selectedRow = row; selectedCol = col
                animateCellPop(row, col)
                invalidate()
            }
        } else {
            // Second tap: attempt swap or change selection
            if (row == selectedRow && col == selectedCol) {
                clearSelection()
            } else if (isAdjacent(selectedRow, selectedCol, row, col)) {
                val r1 = selectedRow; val c1 = selectedCol
                clearSelection()
                animateSwapAttempt(r1, c1, row, col)
            } else {
                // Re-select
                if (!engine.board[row][col].isEmpty && !engine.board[row][col].isLocked) {
                    selectedRow = row; selectedCol = col
                    animateCellPop(row, col)
                    invalidate()
                } else {
                    clearSelection()
                }
            }
        }
        return true
    }

    private fun isAdjacent(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val dr = abs(r1 - r2); val dc = abs(c1 - c2)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    private fun clearSelection() {
        selectedRow = -1; selectedCol = -1
        invalidate()
    }

    // ─── Animations ───────────────────────────────────────────────────────────

    private fun animateCellPop(row: Int, col: Int) {
        ValueAnimator.ofFloat(1f, 1.2f, 1f).apply {
            duration = 200
            addUpdateListener {
                cellAnimScale[row][col] = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateSwapAttempt(r1: Int, c1: Int, r2: Int, c2: Int) {
        isAnimating = true
        onSwapSound?.invoke()

        // Visual swap tween
        val dx = (c2 - c1) * cellSize
        val dy = (r2 - r1) * cellSize

        val swapAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val frac = anim.animatedValue as Float
                cellAnimOffsetY[r1][c1] = dy * frac
                cellAnimOffsetY[r2][c2] = -dy * frac
                invalidate()
            }
        }
        swapAnim.start()
        handler.postDelayed({
            cellAnimOffsetY[r1][c1] = 0f
            cellAnimOffsetY[r2][c2] = 0f

            val success = engine.trySwap(r1, c1, r2, c2)
            if (!success) {
                // Snap back (engine already reverted swap)
                animateSnapBack(r1, c1, r2, c2)
            } else {
                isAnimating = false
                invalidate()
                notifyUIUpdate()
            }
        }, 200)
    }

    private fun animateSnapBack(r1: Int, c1: Int, r2: Int, c2: Int) {
        onInvalidSound?.invoke()
        ValueAnimator.ofFloat(0f, 0.15f, -0.1f, 0f).apply {
            duration = 280
            addUpdateListener { anim ->
                val frac = anim.animatedValue as Float
                cellAnimScale[r1][c1] = 1f + frac
                cellAnimScale[r2][c2] = 1f + frac
                invalidate()
            }
            start()
        }
        handler.postDelayed({
            cellAnimScale[r1][c1] = 1f
            cellAnimScale[r2][c2] = 1f
            isAnimating = false
            invalidate()
        }, 300)
    }

    private fun animateMatchFlash(cells: List<Pair<Int, Int>>) {
        onMatchSound?.invoke()
        ValueAnimator.ofFloat(1f, 0.2f, 1f, 0f).apply {
            duration = 350
            addUpdateListener { anim ->
                val alpha = anim.animatedValue as Float
                for ((r, c) in cells) cellAnimAlpha[r][c] = alpha
                invalidate()
            }
            start()
        }
    }

    private fun animateGravityFall() {
        ValueAnimator.ofFloat(-cellSize * 0.5f, 0f).apply {
            duration = 280
            interpolator = BounceInterpolator()
            addUpdateListener { anim ->
                val offset = anim.animatedValue as Float
                for (r in 0 until engine.rows) {
                    for (c in 0 until engine.cols) {
                        if (!engine.board[r][c].isEmpty) {
                            cellAnimOffsetY[r][c] = offset
                        }
                    }
                }
                invalidate()
            }
            start()
        }
    }

    // ─── Hint System ──────────────────────────────────────────────────────────

    fun showHint() {
        val hint = engine.getHint() ?: return
        hintRow1 = hint.first.first;  hintCol1 = hint.first.second
        hintRow2 = hint.second.first; hintCol2 = hint.second.second
        if (!hintAnimator.isRunning) hintAnimator.start()
        invalidate()

        // Auto-clear hint after 3 seconds
        handler.postDelayed({ clearHint() }, 3000)
    }

    private fun clearHint() {
        hintAnimator.cancel()
        hintRow1 = -1; hintCol1 = -1; hintRow2 = -1; hintCol2 = -1
        invalidate()
    }

    // ─── GameEventListener ────────────────────────────────────────────────────

    override fun onValidSwap(r1: Int, c1: Int, r2: Int, c2: Int) { /* handled in touch */ }
    override fun onInvalidSwap(r1: Int, c1: Int, r2: Int, c2: Int) { /* handled in touch */ }

    override fun onCellsMatched(cells: List<Pair<Int, Int>>, combo: Int, score: Int) {
        animateMatchFlash(cells)
        // Reset matched cells alpha after animation
        handler.postDelayed({
            for ((r, c) in cells) {
                cellAnimAlpha[r][c] = 1f
                cellAnimScale[r][c] = 1f
                cellAnimOffsetY[r][c] = 0f
            }
        }, 360)
        notifyUIUpdate()
    }

    override fun onCascadeStart() {
        handler.postDelayed({ animateGravityFall() }, 100)
    }

    override fun onCascadeEnd() {
        resetAllAnimStates()
        isAnimating = false
        invalidate()
        notifyUIUpdate()
    }

    override fun onNoMovesAvailable() {
        onShuffleSound?.invoke()
    }

    override fun onBoardShuffled() {
        resetAllAnimStates()
        invalidate()
    }

    override fun onGameOver(state: GameState) {
        handler.postDelayed({
            onGameOver?.invoke(state)
        }, 500)
    }

    private fun resetAllAnimStates() {
        for (r in 0 until engine.rows) {
            for (c in 0 until engine.cols) {
                cellAnimAlpha[r][c] = 1f
                cellAnimScale[r][c] = 1f
                cellAnimOffsetY[r][c] = 0f
            }
        }
    }

    private fun notifyUIUpdate() {
        if (::engine.isInitialized) {
            onScoreChanged?.invoke(engine.gameState.score)
            onComboChanged?.invoke(engine.gameState.combo)
        }
    }

    fun cleanup() {
        hintAnimator.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
