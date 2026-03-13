package com.example.alarm_reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AlarmScheduler.actionDismissAlarm) {
            return
        }

        val id = intent.getIntExtra("id", 0)
        AlarmScheduler.stopAlarmPresentation(context, id)
    }
}
