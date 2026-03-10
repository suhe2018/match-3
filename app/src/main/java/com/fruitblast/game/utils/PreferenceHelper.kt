package com.fruitblast.game.utils

import android.content.Context
import com.fruitblast.game.data.Difficulty
import com.fruitblast.game.data.HighScore

/**
 * Manages persistence of high scores and user settings via SharedPreferences.
 * Stores top-5 scores per difficulty level.
 */
class PreferenceHelper(context: Context) {

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "fruit_blast_prefs"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val MAX_SCORES = 5
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, true)
        set(value) = prefs.edit().putBoolean(KEY_SOUND, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    // ─── High Scores ──────────────────────────────────────────────────────────

    fun saveHighScore(score: HighScore) {
        val key = scoreKey(score.difficulty)
        val existing = getHighScores(score.difficulty).toMutableList()
        existing.add(score)
        existing.sortByDescending { it.score }
        val top5 = existing.take(MAX_SCORES)

        val editor = prefs.edit()
        editor.putInt("${key}_count", top5.size)
        top5.forEachIndexed { i, hs ->
            editor.putInt("${key}_${i}_score", hs.score)
            editor.putString("${key}_${i}_diff", hs.difficulty.name)
            editor.putInt("${key}_${i}_combo", hs.maxCombo)
            editor.putInt("${key}_${i}_matches", hs.totalMatches)
            editor.putLong("${key}_${i}_time", hs.timestamp)
        }
        editor.apply()
    }

    fun getHighScores(difficulty: Difficulty): List<HighScore> {
        val key = scoreKey(difficulty)
        val count = prefs.getInt("${key}_count", 0)
        return (0 until count).mapNotNull { i ->
            val score = prefs.getInt("${key}_${i}_score", 0)
            val diffName = prefs.getString("${key}_${i}_diff", null) ?: return@mapNotNull null
            val combo = prefs.getInt("${key}_${i}_combo", 0)
            val matches = prefs.getInt("${key}_${i}_matches", 0)
            val time = prefs.getLong("${key}_${i}_time", 0)
            HighScore(score, Difficulty.valueOf(diffName), combo, matches, time)
        }
    }

    fun getBestScore(difficulty: Difficulty): Int {
        return getHighScores(difficulty).firstOrNull()?.score ?: 0
    }

    fun isNewRecord(score: Int, difficulty: Difficulty): Boolean {
        return score > getBestScore(difficulty)
    }

    fun clearAllScores() {
        prefs.edit().clear().apply()
    }

    private fun scoreKey(difficulty: Difficulty) = "scores_${difficulty.name.lowercase()}"
}
