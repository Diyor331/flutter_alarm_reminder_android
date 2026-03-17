package com.example.alarm_reminder

import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.Date

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }

        setContentView(R.layout.activity_alarm)

        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title").orEmpty().ifBlank {
            getString(R.string.alarm_default_title)
        }
        val body = intent.getStringExtra("body").orEmpty().ifBlank {
            getString(R.string.alarm_default_body)
        }
        val dismissLabel = intent.getStringExtra("dismissLabel").orEmpty().ifBlank {
            getString(R.string.alarm_dismiss)
        }
        val triggerAtMillis = intent.getLongExtra("triggerAtMillis", 0L)

        findViewById<TextView>(R.id.alarmSubtitle).apply {
            text = body
            visibility = if (body.isBlank()) View.GONE else View.VISIBLE
        }
        findViewById<TextView>(R.id.alarmTime).text = formatAlarmTime(triggerAtMillis)
        findViewById<TextView>(R.id.alarmTitle).text = title
        findViewById<Button>(R.id.dismissButton).apply {
            text = dismissLabel
            setOnClickListener {
                AlarmScheduler.stopAlarmPresentation(this@AlarmActivity, id)
                finish()
            }
        }
    }

    private fun formatAlarmTime(triggerAtMillis: Long): String {
        val timestamp = if (triggerAtMillis > 0L) triggerAtMillis else System.currentTimeMillis()
        return DateFormat.getTimeFormat(this).format(Date(timestamp))
    }
}
