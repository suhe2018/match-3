package com.fruitblast.game;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fruitblast.game.databinding.ActivityGameBinding;

public class GameActivity extends AppCompatActivity {

    public static final String EXTRA_DIFFICULTY    = "difficulty";
    public static final String EXTRA_SCORE         = "score";
    public static final String EXTRA_DIFFICULTY_NAME = "difficulty_name";
    public static final String EXTRA_MAX_COMBO     = "max_combo";
    public static final String EXTRA_TOTAL_MATCHES = "total_matches";
    public static final String EXTRA_IS_RECORD     = "is_record";

    private ActivityGameBinding binding;
    private GameEngine engine;
    private PreferenceHelper prefs;
    private Difficulty difficulty;

    // Timer
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastTickMs = 0;
    private boolean running = false;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = System.currentTimeMillis();
            if (lastTickMs > 0) {
                engine.tick(now - lastTickMs);
                updateTimerUI();
            }
            lastTickMs = now;
            if (!engine.getState().isGameOver) {
                handler.postDelayed(this, 100);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String diffName = getIntent().getStringExtra(EXTRA_DIFFICULTY);
        if (diffName == null) diffName = Difficulty.MEDIUM.name();
        difficulty = Difficulty.valueOf(diffName);

        prefs = new PreferenceHelper(this);

        setupEngine();
        setupUI();

        // Back press → pause dialog (replaces deprecated onBackPressed)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showPauseDialog();
            }
        });
    }

    // ── Engine setup ──────────────────────────────────────────────────────────

    private void setupEngine() {
        engine = new GameEngine(difficulty);
        engine.setCallback(new GameEngine.GameCallback() {
            @Override public void onScoreChanged(int score) { updateScoreUI(score); }
            @Override public void onComboChanged(int combo) { updateComboUI(combo); }
            @Override public void onGameOver(GameState state) { handleGameOver(state); }
            @Override public void onBoardChanged() { binding.gameView.invalidate(); }
            @Override public void onShuffled() {
                runOnUiThread(() ->
                    android.widget.Toast.makeText(GameActivity.this,
                        R.string.shuffle_notice, android.widget.Toast.LENGTH_SHORT).show());
            }
        });

        binding.gameView.setEngine(engine);
        binding.gameView.setSwapListener(valid -> {
            // Could add sound/vibration here
        });
    }

    // ── UI setup ──────────────────────────────────────────────────────────────

    private void setupUI() {
        binding.tvDifficulty.setText(difficulty.emoji + " " + difficulty.displayName);
        updateTimerUI();
        updateScoreUI(0);
        updateComboUI(0);

        int best = prefs.getBestScore(difficulty);
        if (best > 0) {
            binding.tvBestScore.setVisibility(View.VISIBLE);
            binding.tvBestScore.setText(getString(R.string.best_score, best));
        }

        binding.btnPause.setOnClickListener(v -> showPauseDialog());
        binding.btnHint.setOnClickListener(v -> binding.gameView.showHint());
    }

    // ── Timer UI ──────────────────────────────────────────────────────────────

    private void updateTimerUI() {
        long remaining = engine.getState().timeRemainingMs;
        int totalSec = difficulty.timeSeconds;
        int remSec = (int) (remaining / 1000);
        int min = remSec / 60;
        int sec = remSec % 60;
        binding.tvTimer.setText(String.format(java.util.Locale.getDefault(), "%d:%02d", min, sec));

        // Colour warning
        int color;
        if (remSec <= 10) color = getColor(R.color.timer_danger);
        else if (remSec <= 20) color = getColor(R.color.timer_warning);
        else color = getColor(R.color.timer_normal);
        binding.tvTimer.setTextColor(color);

        // Progress bar (LinearProgressIndicator uses 0-100)
        int progress = totalSec > 0 ? (int) (remaining * 100 / (difficulty.timeSeconds * 1000L)) : 0;
        binding.progressTimer.setProgress(Math.max(0, Math.min(100, progress)));
    }

    // ── Score / Combo UI ──────────────────────────────────────────────────────

    private void updateScoreUI(final int score) {
        runOnUiThread(() -> {
            binding.tvScore.setText(String.valueOf(score));
            binding.tvScore.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
                    .withEndAction(() ->
                            binding.tvScore.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
        });
    }

    private void updateComboUI(final int combo) {
        runOnUiThread(() -> {
            if (combo > 1) {
                binding.tvCombo.setVisibility(View.VISIBLE);
                binding.tvCombo.setText(getString(R.string.combo_label, combo));
                binding.tvCombo.animate().scaleX(1.3f).scaleY(1.3f).setDuration(150)
                        .withEndAction(() ->
                                binding.tvCombo.animate().scaleX(1f).scaleY(1f).setDuration(150).start())
                        .start();
            } else {
                binding.tvCombo.setVisibility(View.INVISIBLE);
            }
        });
    }

    // ── Game over ─────────────────────────────────────────────────────────────

    private void handleGameOver(GameState state) {
        stopTimer();
        boolean isRecord = prefs.saveScore(difficulty, state.score, state.maxCombo, state.totalMatches);

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(EXTRA_SCORE, state.score);
        intent.putExtra(EXTRA_DIFFICULTY_NAME, difficulty.name());
        intent.putExtra(EXTRA_MAX_COMBO, state.maxCombo);
        intent.putExtra(EXTRA_TOTAL_MATCHES, state.totalMatches);
        intent.putExtra(EXTRA_IS_RECORD, isRecord);
        startActivity(intent);
        finish();
    }

    // ── Pause dialog ──────────────────────────────────────────────────────────

    private void showPauseDialog() {
        if (engine.getState().isGameOver) return;
        engine.getState().isPaused = true;
        stopTimer();

        new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(R.string.pause_title)
                .setMessage(R.string.pause_message)
                .setPositiveButton(R.string.resume, (d, w) -> resumeGame())
                .setNegativeButton(R.string.quit, (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    private void resumeGame() {
        engine.getState().isPaused = false;
        startTimer();
    }

    // ── Timer control ─────────────────────────────────────────────────────────

    private void startTimer() {
        running = true;
        lastTickMs = System.currentTimeMillis();
        handler.post(timerRunnable);
    }

    private void stopTimer() {
        running = false;
        handler.removeCallbacks(timerRunnable);
        lastTickMs = 0;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        if (!engine.getState().isGameOver && !engine.getState().isPaused) {
            startTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
        if (!engine.getState().isGameOver) {
            engine.getState().isPaused = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
