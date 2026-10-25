package com.bookflow.reader.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HapticHelper {
    
    private val hapticScope = CoroutineScope(Dispatchers.Default)
    private var lastPencilHapticTime = 0L

    private var cachedVibrator: Vibrator? = null

    private fun getVibrator(context: Context): Vibrator {
        cachedVibrator?.let { return it }
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        cachedVibrator = v
        return v
    }
    
    /** Light tick — tool switch, color pick, radial menu wedge hover */
    fun tick(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(8)
        }
    }
    
    /** Medium click — bookmark toggle, undo/redo */
    fun click(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(18)
        }
    }
    
    /** Heavy thud — delete, clear, scratch-out */
    fun heavyClick(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(40)
        }
    }
    
    /** Double tick — lasso selection confirmed */
    fun doubleTick(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 10, 35, 10), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    /**
     * Tactile Material Haptic: Gritty micro-texture vibrations as a pencil or fountain pen moves across paper.
     * Throttled to ~55ms intervals to generate a continuous tactile paper sensation.
     */
    fun pencilGrittyTexture(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastPencilHapticTime < 55L) return
        lastPencilHapticTime = now

        val appContext = context.applicationContext
        hapticScope.launch {
            try {
                val vibrator = getVibrator(appContext)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(6, 45))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(6)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Tactile Material Haptic: Deep, solid resonant thud when an eraser deletes a stroke.
     */
    fun eraserThud(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(45, 180))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(45)
        }
    }

    /**
     * Tactile Material Haptic: Crisp, energetic double-pop click when a hand-drawn stroke snaps into geometry.
     */
    fun shapeSnapPop(context: Context) {
        val vibrator = getVibrator(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 14, 22, 18), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(28)
        }
    }
}
