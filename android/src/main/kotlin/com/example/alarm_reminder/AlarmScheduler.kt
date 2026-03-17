package com.example.alarm_reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object AlarmScheduler {
    const val channelId = "alarm_reminder_channel"
    const val actionDismissAlarm = "com.example.alarm_reminder.DISMISS_ALARM"
    private const val notificationOffset = 90_000

    fun scheduleAlarm(context: Context, payload: AlarmPayload) {
        if (payload.triggerAtMillis <= System.currentTimeMillis()) {
            throw IllegalArgumentException("triggerAtMillis must be in the future")
        }

        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            throw IllegalStateException("Exact alarms are not allowed on this device/app")
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            payload.triggerAtMillis,
            alarmPendingIntent(context, payload),
        )
        AlarmStorage.save(context, payload)
    }

    fun cancelAlarm(context: Context, id: Int) {
        val payload = AlarmStorage.get(context, id)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(alarmPendingIntent(context, payload ?: fallbackPayload(id)))
        AlarmStorage.remove(context, id)
        stopAlarmPresentation(context, id)
    }

    fun cancelAll(context: Context) {
        AlarmStorage.getAll(context).forEach { payload ->
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(alarmPendingIntent(context, payload))
            stopAlarmPresentation(context, payload.id)
        }
        AlarmStorage.clear(context)
    }

    fun rescheduleAll(context: Context) {
        val existing = AlarmStorage.getAll(context)
        existing.forEach { payload ->
            if (payload.triggerAtMillis <= System.currentTimeMillis()) {
                AlarmStorage.remove(context, payload.id)
                return@forEach
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                payload.triggerAtMillis,
                alarmPendingIntent(context, payload),
            )
        }
    }

    fun showAlarmPresentation(context: Context, payload: AlarmPayload) {
        createNotificationChannel(context)
        AlarmRingingController.start(context)

        val title = payload.title.ifBlank { localizedString(context, R.string.alarm_default_title) }
        val body = payload.body.ifBlank { localizedString(context, R.string.alarm_default_body) }
        val dismissLabel = payload.dismissLabel?.ifBlank { null }
            ?: localizedString(context, R.string.alarm_dismiss)

        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("id", payload.id)
            putExtra("title", title)
            putExtra("body", body)
            putExtra("dismissLabel", dismissLabel)
            putExtra("triggerAtMillis", payload.triggerAtMillis)
        }
        val dismissIntent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = actionDismissAlarm
            putExtra("id", payload.id)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            payload.id,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            payload.id,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, dismissLabel, dismissPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(payload.id), notification)
    }

    fun stopAlarmPresentation(context: Context, id: Int) {
        AlarmRingingController.stop(context)
        NotificationManagerCompat.from(context).cancel(notificationId(id))
    }

    fun getStatus(context: Context): Map<String, Any?> {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarms = AlarmStorage.getAll(context).sortedBy { it.triggerAtMillis }
        val nextScheduledAtMillis = alarms.firstOrNull()?.triggerAtMillis

        return mapOf(
            "notificationsGranted" to NotificationManagerCompat.from(context).areNotificationsEnabled(),
            "exactAlarmAllowed" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            },
            "pendingAlarmCount" to alarms.size,
            "pendingAlarmIds" to alarms.map { it.id },
            "nextScheduledAtMillis" to nextScheduledAtMillis,
        )
    }

    fun openExactAlarmSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:${context.packageName}"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            channelId,
            "Alarm reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Channel for full-screen alarm reminders"
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun alarmPayloadFromIntent(intent: Intent): AlarmPayload {
        return AlarmPayload(
            id = intent.getIntExtra("id", 0),
            triggerAtMillis = intent.getLongExtra("triggerAtMillis", 0L),
            title = intent.getStringExtra("title").orEmpty(),
            body = intent.getStringExtra("body").orEmpty(),
            dismissLabel = intent.getStringExtra("dismissLabel"),
        )
    }

    private fun alarmPendingIntent(context: Context, payload: AlarmPayload): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            payload.toMap().forEach { (key, value) ->
                when (value) {
                    is Int -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                    is String -> putExtra(key, value)
                }
            }
        }
        return PendingIntent.getBroadcast(
            context,
            payload.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun fallbackPayload(id: Int) = AlarmPayload(
        id = id,
        triggerAtMillis = 0L,
        title = "",
        body = "",
        dismissLabel = null,
    )

    private fun notificationId(id: Int) = notificationOffset + id

    private fun localizedString(context: Context, @StringRes resId: Int): String {
        return context.getString(resId)
    }
}
