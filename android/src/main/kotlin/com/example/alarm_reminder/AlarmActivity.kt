package com.example.alarm_reminder

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity

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
        val title = intent.getStringExtra("title").orEmpty().ifBlank { "Alarm reminder" }
        val body = intent.getStringExtra("body").orEmpty().ifBlank { "Reminder fired." }
        val dismissLabel = intent.getStringExtra("dismissLabel") ?: "Dismiss"

        findViewById<TextView>(R.id.alarmTitle).text = title
        findViewById<TextView>(R.id.alarmSubtitle).text = body
        findViewById<Button>(R.id.dismissButton).apply {
            text = dismissLabel
            setOnClickListener {
                AlarmScheduler.stopAlarmPresentation(this@AlarmActivity, id)
                finish()
            }
        }
    }
}
