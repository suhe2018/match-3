package com.fruitblast.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View {

    // ── Constructors (all 3 required for XML inflation) ───────────────────────

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // ── Callback interfaces ────────────────────────────────────────────────────

    public interface SwapListener {
        void onSwapAttempted(boolean valid);
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private GameEngine engine;
    private SwapListener swapListener;

    // Selection
    private int selRow = -1;
    private int selCol = -1;

    // Hint
    private int[] hintCells = null;       // {r1,c1,r2,c2}
    private boolean hintVisible = false;
    private final Handler hintHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideHint = () -> {
        hintCells = null;
        hintVisible = false;
        invalidate();
    };

    // Layout
    private float cellSize;
    private float boardLeft;
    private float boardTop;

    // Paints
    private Paint cellPaint;
    private Paint selectedPaint;
    private Paint hintPaint;
    private Paint lockedPaint;
    private Paint textPaint;
    private Paint overlayPaint;

    // ── Init ───────────────────────────────────────────────────────────────────

    private void init() {
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setStyle(Paint.Style.FILL);

        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setColor(Color.WHITE);
        selectedPaint.setStrokeWidth(5f);

        hintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hintPaint.setStyle(Paint.Style.STROKE);
        hintPaint.setColor(0xFFFFD700);
        hintPaint.setStrokeWidth(5f);

        lockedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lockedPaint.setStyle(Paint.Style.FILL);
        lockedPaint.setColor(0xFF546E7A);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        overlayPaint.setStyle(Paint.Style.FILL);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public void setEngine(GameEngine engine) {
        this.engine = engine;
        invalidate();
    }

    public void setSwapListener(SwapListener listener) {
        this.swapListener = listener;
    }

    public void showHint() {
        if (engine == null) return;
        hintCells = engine.findHint();
        hintVisible = hintCells != null;
        hintHandler.removeCallbacks(hideHint);
        if (hintVisible) hintHandler.postDelayed(hideHint, 2000);
        invalidate();
    }

    public void cancelHint() {
        hintHandler.removeCallbacks(hideHint);
        hintCells = null;
        hintVisible = false;
    }

    // ── Layout ─────────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (engine == null) return;
        recalcLayout(w, h);
    }

    private void recalcLayout(int w, int h) {
        int size = engine.getBoardSize();
        float available = Math.min(w, h);
        cellSize = available / size;
        boardLeft = (w - cellSize * size) / 2f;
        boardTop = (h - cellSize * size) / 2f;
    }

    // ── Draw ───────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (engine == null) return;

        int size = engine.getBoardSize();
        float pad = cellSize * 0.06f;
        float radius = cellSize * 0.18f;
        float emojiSize = cellSize * 0.56f;
        textPaint.setTextSize(emojiSize);

        RectF rect = new RectF();

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                Cell cell = engine.getCell(r, c);
                float left = boardLeft + c * cellSize + pad;
                float top = boardTop + r * cellSize + pad;
                float right = left + cellSize - 2 * pad;
                float bottom = top + cellSize - 2 * pad;
                rect.set(left, top, right, bottom);

                if (cell.isLocked) {
                    drawLockedCell(canvas, rect, radius, emojiSize);
                } else if (cell.isEmpty()) {
                    overlayPaint.setColor(0x22FFFFFF);
                    canvas.drawRoundRect(rect, radius, radius, overlayPaint);
                } else {
                    drawFruitCell(canvas, rect, radius, emojiSize, cell.fruitIndex);
                }

                // Selected highlight
                if (r == selRow && c == selCol) {
                    canvas.drawRoundRect(rect, radius, radius, selectedPaint);
                }

                // Hint highlight
                if (hintVisible && hintCells != null) {
                    if ((r == hintCells[0] && c == hintCells[1])
                            || (r == hintCells[2] && c == hintCells[3])) {
                        canvas.drawRoundRect(rect, radius, radius, hintPaint);
                    }
                }
            }
        }
    }

    private void drawFruitCell(Canvas canvas, RectF rect, float radius, float emojiSize, int fruitIndex) {
        int baseColor = GameEngine.FRUIT_COLORS[Math.min(fruitIndex, GameEngine.FRUIT_COLORS.length - 1)];
        // Semi-transparent background tinted to fruit colour
        cellPaint.setColor(Color.argb(70,
                Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)));
        canvas.drawRoundRect(rect, radius, radius, cellPaint);

        // Emoji
        String emoji = GameEngine.FRUIT_EMOJIS[Math.min(fruitIndex, GameEngine.FRUIT_EMOJIS.length - 1)];
        float cx = rect.centerX();
        float cy = rect.centerY() + emojiSize * 0.35f;
        canvas.drawText(emoji, cx, cy, textPaint);
    }

    private void drawLockedCell(Canvas canvas, RectF rect, float radius, float emojiSize) {
        canvas.drawRoundRect(rect, radius, radius, lockedPaint);
        textPaint.setTextSize(emojiSize * 0.75f);
        canvas.drawText("🧊", rect.centerX(), rect.centerY() + emojiSize * 0.3f, textPaint);
        textPaint.setTextSize(emojiSize); // restore
    }

    // ── Touch ──────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (engine == null) return true;
        if (engine.getState().isGameOver || engine.getState().isPaused) return true;
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        int col = (int) ((event.getX() - boardLeft) / cellSize);
        int row = (int) ((event.getY() - boardTop) / cellSize);
        int sz = engine.getBoardSize();

        if (row < 0 || row >= sz || col < 0 || col >= sz) {
            clearSelection();
            return true;
        }

        Cell cell = engine.getCell(row, col);
        if (cell.isLocked || cell.isEmpty()) {
            clearSelection();
            return true;
        }

        if (selRow == -1) {
            // First tap: select
            selRow = row;
            selCol = col;
            cancelHint();
            invalidate();
        } else if (selRow == row && selCol == col) {
            // Same cell: deselect
            clearSelection();
        } else {
            int dr = Math.abs(row - selRow);
            int dc = Math.abs(col - selCol);
            if ((dr == 1 && dc == 0) || (dr == 0 && dc == 1)) {
                // Adjacent: attempt swap
                boolean valid = engine.trySwap(selRow, selCol, row, col);
                if (swapListener != null) swapListener.onSwapAttempted(valid);
                clearSelection();
                invalidate();
            } else {
                // Non-adjacent: re-select new cell
                selRow = row;
                selCol = col;
                invalidate();
            }
        }
        return true;
    }

    private void clearSelection() {
        selRow = -1;
        selCol = -1;
        invalidate();
    }
}
