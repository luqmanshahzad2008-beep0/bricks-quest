package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.R

class SoundManager(context: Context) {
    var isSoundEnabled by mutableStateOf(true)
    var isHapticsEnabled by mutableStateOf(true)

    private val soundPool: SoundPool
    private val soundIds: Map<String, Int>

    // Pentatonic scale pitch multipliers for musical combo chains
    private val comboScale = floatArrayOf(
        1.000f, // Root (E5)
        1.122f, // F#5
        1.260f, // G#5
        1.414f, // B5
        1.587f, // C#6
        1.782f, // E6
        2.000f  // Octave
    )

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(32)
            .setAudioAttributes(audioAttributes)
            .build()

        soundIds = emptyMap()
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var lastHitHapticTime = 0L
    private var lastBreakHapticTime = 0L
    private var lastExplosionHapticTime = 0L
    private var lastHitSoundTime = 0L
    private var lastBreakSoundTime = 0L
    private var lastLaserSoundTime = 0L
    private var lastBombSoundTime = 0L
    private var lastBounceSoundTime = 0L

    fun playHitSound(comboIndex: Int) {
        if (!isSoundEnabled) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastHitSoundTime < 16L) return
        lastHitSoundTime = now
        val id = soundIds["hit"] ?: return
        val noteIdx = (comboIndex.coerceAtLeast(0)) % comboScale.size
        val octaveShift = (comboIndex / comboScale.size).coerceAtMost(2) * 0.14f
        val pitch = (comboScale[noteIdx] + octaveShift).coerceIn(0.75f, 2.0f)
        soundPool.play(id, 0.90f, 0.90f, 1, 0, pitch)
    }

    fun playBounceSound() {
        if (!isSoundEnabled) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastBounceSoundTime < 35L) return
        lastBounceSoundTime = now
        val id = soundIds["click"] ?: return
        soundPool.play(id, 0.35f, 0.35f, 1, 0, 1.35f)
    }

    fun playBreakSound() {
        if (!isSoundEnabled) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastBreakSoundTime < 28L) return
        lastBreakSoundTime = now
        val id = soundIds["break"] ?: return
        val randomPitch = 1.0f + (Math.random().toFloat() * 0.20f)
        soundPool.play(id, 1.0f, 1.0f, 2, 0, randomPitch)
    }

    fun playCollectBallSound() {
        if (!isSoundEnabled) return
        val id = soundIds["collect"] ?: return
        soundPool.play(id, 1.0f, 1.0f, 3, 0, 1.25f)
    }

    fun playLaserSound() {
        if (!isSoundEnabled) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastLaserSoundTime < 35L) return
        lastLaserSoundTime = now
        val id = soundIds["laser"] ?: return
        val pitch = 0.95f + (Math.random().toFloat() * 0.18f)
        soundPool.play(id, 1.0f, 1.0f, 3, 0, pitch)
    }

    fun playBombSound() {
        if (!isSoundEnabled) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastBombSoundTime < 60L) return
        lastBombSoundTime = now
        val id = soundIds["bomb"] ?: return
        soundPool.play(id, 1.0f, 1.0f, 3, 0, 0.92f)
    }

    fun playLaunchSound() {
        if (!isSoundEnabled) return
        val id = soundIds["launch"] ?: return
        soundPool.play(id, 0.70f, 0.70f, 1, 0, 1.05f)
    }

    fun playVictorySound() {
        if (!isSoundEnabled) return
        val id = soundIds["victory"] ?: return
        soundPool.play(id, 1f, 1f, 3, 0, 1.0f)
    }

    fun playClickSound() {
        if (!isSoundEnabled) return
        val id = soundIds["click"] ?: return
        soundPool.play(id, 0.75f, 0.75f, 1, 0, 1.0f)
    }

    fun triggerHitHaptic() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastHitHapticTime < 50L) return
        lastHitHapticTime = now
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(12)
            }
        } catch (_: Exception) {}
    }

    fun triggerBreakHaptic() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastBreakHapticTime < 60L) return
        lastBreakHapticTime = now
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(35)
            }
        } catch (_: Exception) {}
    }

    fun triggerExplosionHaptic() {
        if (!isHapticsEnabled || vibrator?.hasVibrator() != true) return
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastExplosionHapticTime < 100L) return
        lastExplosionHapticTime = now
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (_: Exception) {}
    }
}
