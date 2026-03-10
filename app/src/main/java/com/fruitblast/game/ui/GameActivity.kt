package com.fruitblast.game.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fruitblast.game.R
import com.fruitblast.game.data.Difficulty
import com.fruitblast.game.data.GameState
import com.fruitblast.game.data.GameStatus
import com.fruitblast.game.databinding.ActivityGameBinding
import com.fruitblast.game.engine.GameEngine
import com.fruitblast.game.utils.PreferenceHelper
import com.fruitblast.game.utils.SoundManager

/**
 * Main game screen.
 *
 * Manages:
 * - Game initialization
 * - Countdown timer (updates every 100ms for smooth display)
 * - Score/combo HUD updates
 * - Pause/resume logic
 * - Hint system
 * - Navigation to ResultActivity on game over
 */
class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DIFFICULTY = "difficulty"
        const val EXTRA_SCORE = "score"
        const val EXTRA_DIFFICULTY_NAME = "difficulty_name"
        const val EXTRA_MAX_COMBO = "max_combo"
        const val EXTRA_TOTAL_MATCHES = "total_matches"
        const val EXTRA_IS_RECORD = "is_record"
    }

    private lateinit var binding: ActivityGameBinding
    private lateinit var engine: GameEngine
    private lateinit var soundManager: SoundManager
    private lateinit var prefs: PreferenceHelper
    private lateinit var difficulty: Difficulty

    // Timer
    private val handler = Handler(Looper.getMainLooper())
    private val timerInterval = 100L
    private var lastTickMs = 0L

    // Hint auto-show
    private var lastMoveTimeMs = 0L
    private val hintCheckInterval = 1000L

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (::engine.isInitialized && !engine.gameState.isGameOver) {
                val now = System.currentTimeMillis()
                if (lastTickMs > 0) {
                    val delta = now - lastTickMs
                    engine.tickTimer(delta)
                    updateTimerUI()
                }
                lastTickMs = now

                // Auto-hint if player has been idle
                val idleMs = now - lastMoveTimeMs
                if (idleMs > difficulty.hintDelaySeconds * 1000L) {
                    binding.gameView.showHint()
                    lastMoveTimeMs = now
                }
            }
            if (!engine.gameState.isGameOver) {
                handler.postDelayed(this, timerInterval)
            }
        }
    }

    private val hintRunnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, hintCheckInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val diffName = intent.getStringExtra(EXTRA_DIFFICULTY) ?: Difficulty.MEDIUM.name
        difficulty = Difficulty.valueOf(diffName)

        prefs = PreferenceHelper(this)
        soundManager = SoundManager(this)
        soundManager.setEnabled(prefs.soundEnabled)
        soundManager.setVibrationEnabled(prefs.vibrationEnabled)

        setupEngine()
        setupUI()

        lastMoveTimeMs = System.currentTimeMillis()
        lastTickMs = System.currentTimeMillis()
        handler.post(timerRunnable)
    }

    private fun setupEngine() {
        engine = GameEngine(difficulty)
        engine.initBoard()

        binding.gameView.attachEngine(engine)
        binding.gameView.onScoreChanged = { score -> updateScoreUI(score) }
        binding.gameView.onComboChanged = { combo -> updateComboUI(combo) }
        binding.gameView.onGameOver = { state -> handleGameOver(state) }
        binding.gameView.onSwapSound = { soundManager.playSwap(); resetIdleTimer() }
        binding.gameView.onMatchSound = { soundManager.playMatch() }
        binding.gameView.onInvalidSound = { soundManager.playInvalid() }
        binding.gameView.onShuffleSound = {
            soundManager.playShuffle()
            showToast(getString(R.string.shuffle_notice))
        }
    }

    private fun setupUI() {
        // Difficulty label and timer color
        binding.tvDifficulty.text = "${difficulty.emoji} ${difficulty.displayName}"
        updateTimerUI()
        updateScoreUI(0)
        updateComboUI(0)

        // Best score display
        val best = prefs.getBestScore(difficulty)
        if (best > 0) {
            binding.tvBestScore.visibility = View.VISIBLE
            binding.tvBestScore.text = getString(R.string.best_score, best)
        }

        // Pause button
        binding.btnPause.setOnClickListener { showPauseDialog() }

        // Hint button
        binding.btnHint.setOnClickListener {
            binding.gameView.showHint()
            resetIdleTimer()
        }
    }

    private fun updateTimerUI() {
        val totalSeconds = difficulty.timeLimitSeconds
        val remainSec = (engine.gameState.timeRemainingMs / 1000).toInt()
        val minutes = remainSec / 60
        val seconds = remainSec % 60
        binding.tvTimer.text = String.format("%d:%02d", minutes, seconds)

        // Color warning when <15 seconds
        val warningColor = when {
            remainSec <= 10 -> getColor(R.color.timer_danger)
            remainSec <= 20 -> getColor(R.color.timer_warning)
            else -> getColor(R.color.timer_normal)
        }
        binding.tvTimer.setTextColor(warningColor)

        // Progress bar
        binding.progressTimer.progress = ((remainSec.toFloat() / totalSeconds) * 100).toInt()
    }

    private fun updateScoreUI(score: Int) {
        binding.tvScore.text = score.toString()
        binding.tvScore.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100)
            .withEndAction {
                binding.tvScore.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
    }

    private fun updateComboUI(combo: Int) {
        if (combo > 1) {
            binding.tvCombo.visibility = View.VISIBLE
            binding.tvCombo.text = getString(R.string.combo_label, combo)
            binding.tvCombo.animate()
                .scaleX(1.3f).scaleY(1.3f).setDuration(150)
                .withEndAction {
                    binding.tvCombo.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()
        } else {
            binding.tvCombo.visibility = View.INVISIBLE
        }
    }

    private fun handleGameOver(state: GameState) {
        handler.removeCallbacks(timerRunnable)
        soundManager.playGameOver()

        val isRecord = prefs.isNewRecord(state.score, difficulty)
        if (isRecord) {
            val hs = com.fruitblast.game.data.HighScore(
                score = state.score,
                difficulty = difficulty,
                maxCombo = state.maxCombo,
                totalMatches = state.totalMatches
            )
            prefs.saveHighScore(hs)
        }

        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra(EXTRA_SCORE, state.score)
            putExtra(EXTRA_DIFFICULTY_NAME, difficulty.name)
            putExtra(EXTRA_MAX_COMBO, state.maxCombo)
            putExtra(EXTRA_TOTAL_MATCHES, state.totalMatches)
            putExtra(EXTRA_IS_RECORD, isRecord)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showPauseDialog() {
        engine.gameState.status = com.fruitblast.game.data.GameStatus.PAUSED
        handler.removeCallbacks(timerRunnable)
        lastTickMs = 0L

        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle(getString(R.string.pause_title))
            .setMessage(getString(R.string.pause_message))
            .setPositiveButton(getString(R.string.resume)) { _, _ -> resumeGame() }
            .setNegativeButton(getString(R.string.quit)) { _, _ ->
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            .setCancelable(false)
            .show()
    }

    private fun resumeGame() {
        engine.gameState.status = com.fruitblast.game.data.GameStatus.PLAYING
        lastTickMs = System.currentTimeMillis()
        handler.post(timerRunnable)
    }

    private fun resetIdleTimer() {
        lastMoveTimeMs = System.currentTimeMillis()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        if (!engine.gameState.isGameOver) {
            engine.gameState.status = com.fruitblast.game.data.GameStatus.PAUSED
            handler.removeCallbacks(timerRunnable)
            lastTickMs = 0L
        }
    }

    override fun onResume() {
        super.onResume()
        if (!engine.gameState.isGameOver && engine.gameState.isPaused) {
            resumeGame()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        binding.gameView.cleanup()
        soundManager.release()
    }

    override fun onBackPressed() {
        showPauseDialog()
    }
}
