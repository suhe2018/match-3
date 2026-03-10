package com.fruitblast.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.fruitblast.game.R
import com.fruitblast.game.data.Difficulty
import com.fruitblast.game.data.HighScore
import com.fruitblast.game.databinding.ActivityResultBinding

/**
 * Game over / result screen.
 * Displays final score, rank, combo, and new record badge.
 */
class ResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val score = intent.getIntExtra(GameActivity.EXTRA_SCORE, 0)
        val diffName = intent.getStringExtra(GameActivity.EXTRA_DIFFICULTY_NAME) ?: Difficulty.MEDIUM.name
        val maxCombo = intent.getIntExtra(GameActivity.EXTRA_MAX_COMBO, 0)
        val totalMatches = intent.getIntExtra(GameActivity.EXTRA_TOTAL_MATCHES, 0)
        val isRecord = intent.getBooleanExtra(GameActivity.EXTRA_IS_RECORD, false)
        val difficulty = Difficulty.valueOf(diffName)

        setupUI(score, difficulty, maxCombo, totalMatches, isRecord)
    }

    private fun setupUI(
        score: Int,
        difficulty: Difficulty,
        maxCombo: Int,
        totalMatches: Int,
        isRecord: Boolean
    ) {
        val tempScore = HighScore(score, difficulty, maxCombo, totalMatches)

        binding.tvScore.text = score.toString()
        binding.tvRank.text = tempScore.rank
        binding.tvDifficulty.text = "${difficulty.emoji} ${difficulty.displayName}"
        binding.tvMaxCombo.text = getString(R.string.result_max_combo, maxCombo)
        binding.tvTotalMatches.text = getString(R.string.result_total_matches, totalMatches)

        if (isRecord) {
            binding.tvNewRecord.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            binding.tvNewRecord.startAnimation(anim)
        }

        // Animate score counter
        animateScoreCount(score)

        binding.btnPlayAgain.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        binding.btnMainMenu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        binding.btnHighScores.setOnClickListener {
            startActivity(Intent(this, HighScoreActivity::class.java))
        }
    }

    private fun animateScoreCount(target: Int) {
        val duration = 1200L
        val startTime = System.currentTimeMillis()
        val runnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val fraction = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                val current = (target * fraction).toInt()
                binding.tvScore.text = current.toString()
                if (fraction < 1f) binding.tvScore.postDelayed(this, 16)
                else binding.tvScore.text = target.toString()
            }
        }
        binding.tvScore.post(runnable)
    }
}
