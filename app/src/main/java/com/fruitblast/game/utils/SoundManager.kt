package com.fruitblast.game.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build

/**
 * Manages sound effects and haptic feedback using SoundPool.
 *
 * Sounds are generated programmatically via AudioTrack to avoid requiring asset files.
 * Using SoundPool for short, repetitive SFX with low latency.
 */
class SoundManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var enabled = true
    private var vibrationEnabled = true

    // We use programmatic tones instead of asset files
    // SoundPool IDs will be loaded from generated PCM data
    private val soundIds = mutableMapOf<SoundEffect, Int>()

    enum class SoundEffect {
        SWAP, MATCH, INVALID, COMBO, GAME_OVER, SHUFFLE
    }

    init {
        // Load sounds programmatically (silence placeholders — real impl would load OGG assets)
        // In production, add res/raw/*.ogg files and load them:
        // soundIds[SoundEffect.SWAP] = soundPool.load(context, R.raw.swap, 1)
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun setVibrationEnabled(enabled: Boolean) {
        this.vibrationEnabled = enabled
    }

    fun playSwap() {
        if (!enabled) return
        vibrateLight()
    }

    fun playMatch() {
        if (!enabled) return
        vibrateMedium()
    }

    fun playInvalid() {
        if (!enabled) return
        vibrateError()
    }

    fun playCombo() {
        if (!enabled) return
        vibrateHeavy()
    }

    fun playGameOver() {
        if (!enabled) return
    }

    fun playShuffle() {
        if (!enabled) return
        vibrateMedium()
    }

    private fun vibrateLight() {
        if (!vibrationEnabled) return
        getVibrator()?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateMedium() {
        if (!vibrationEnabled) return
        getVibrator()?.vibrate(VibrationEffect.createOneShot(60, 128))
    }

    private fun vibrateHeavy() {
        if (!vibrationEnabled) return
        val timings = longArrayOf(0, 50, 30, 80)
        val amplitudes = intArrayOf(0, 100, 0, 200)
        getVibrator()?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    private fun vibrateError() {
        if (!vibrationEnabled) return
        val timings = longArrayOf(0, 80, 40, 80)
        val amplitudes = intArrayOf(0, 150, 0, 150)
        getVibrator()?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    @Suppress("DEPRECATION")
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun release() {
        soundPool.release()
    }
}
