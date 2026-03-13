package com.example.alarm_reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object AlarmRingingController {
    private var ringtone: Ringtone? = null

    fun start(context: Context) {
        stop(context)

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val nextRingtone = RingtoneManager.getRingtone(context, uri)
        nextRingtone.audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            nextRingtone.isLooping = true
        }

        ringtone = nextRingtone
        nextRingtone.play()
        vibrate(context)
    }

    fun stop(context: Context) {
        ringtone?.stop()
        ringtone = null
        val vibrator = context.getSystemService(Vibrator::class.java)
        vibrator?.cancel()
    }

    private fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 400, 250, 600), 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 250, 600), 0)
        }
    }
}
