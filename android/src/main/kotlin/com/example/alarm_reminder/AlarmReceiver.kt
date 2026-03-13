package com.example.alarm_reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val payloadIntent = intent ?: return
        val payload = AlarmScheduler.alarmPayloadFromIntent(payloadIntent)
        AlarmStorage.remove(context, payload.id)
        AlarmScheduler.showAlarmPresentation(context, payload)
    }
}
