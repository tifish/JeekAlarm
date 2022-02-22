package com.tinyfish.jeekalarm

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.tinyfish.jeekalarm.start.App

object VibrationService {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (App.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            App.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun vibrate(count: Int) {
        val waveList = mutableListOf<Long>(0)
        for (i in 1..count) {
            waveList.add(400)
            waveList.add(1000)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(waveList.toLongArray(), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(waveList.toLongArray(), -1)
        }
    }

    fun stop() {
        vibrator.cancel()
    }

}