package com.fruitblast.game.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.fruitblast.game.R
import com.fruitblast.game.data.Difficulty
import com.fruitblast.game.databinding.ActivityMainBinding
import com.fruitblast.game.utils.PreferenceHelper

/**
 * Main menu screen.
 * Shows app title, difficulty selection buttons, and high scores entry.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferenceHelper(this)

        setupUI()
        animateEntrance()
    }

    private fun setupUI() {
        binding.btnEasy.setOnClickListener { launchGame(Difficulty.EASY) }
        binding.btnMedium.setOnClickListener { launchGame(Difficulty.MEDIUM) }
        binding.btnHard.setOnClickListener { launchGame(Difficulty.HARD) }
        binding.btnHighScores.setOnClickListener {
            startActivity(Intent(this, HighScoreActivity::class.java))
        }

        // Show best scores for each difficulty as subtitles
        updateBestScores()

        // Sound toggle
        updateSoundButton()
        binding.btnSound.setOnClickListener {
            prefs.soundEnabled = !prefs.soundEnabled
            updateSoundButton()
        }
    }

    private fun updateBestScores() {
        val easyBest = prefs.getBestScore(Difficulty.EASY)
        val medBest = prefs.getBestScore(Difficulty.MEDIUM)
        val hardBest = prefs.getBestScore(Difficulty.HARD)

        if (easyBest > 0) binding.tvEasyBest.text = getString(R.string.best_score, easyBest)
        if (medBest > 0)  binding.tvMediumBest.text = getString(R.string.best_score, medBest)
        if (hardBest > 0) binding.tvHardBest.text = getString(R.string.best_score, hardBest)
    }

    private fun updateSoundButton() {
        binding.btnSound.text = if (prefs.soundEnabled) "🔊" else "🔇"
    }

    private fun launchGame(difficulty: Difficulty) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun animateEntrance() {
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)
        binding.titleLayout.startAnimation(slideUp)
    }

    override fun onResume() {
        super.onResume()
        updateBestScores()
    }
}
