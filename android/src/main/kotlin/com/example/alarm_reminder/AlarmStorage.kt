package com.example.alarm_reminder

import android.content.Context

object AlarmStorage {
    private const val prefsName = "alarm_reminder_prefs"
    private const val keyAlarmIds = "alarm_ids"

    fun save(context: Context, payload: AlarmPayload) {
        val prefs = prefs(context)
        val ids = prefs.getStringSet(keyAlarmIds, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.add(payload.id.toString())

        prefs.edit()
            .putString(alarmKey(payload.id), payload.toJson())
            .putStringSet(keyAlarmIds, ids)
            .apply()
    }

    fun remove(context: Context, id: Int) {
        val prefs = prefs(context)
        val ids = prefs.getStringSet(keyAlarmIds, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.remove(id.toString())

        prefs.edit()
            .remove(alarmKey(id))
            .putStringSet(keyAlarmIds, ids)
            .apply()
    }

    fun get(context: Context, id: Int): AlarmPayload? {
        val value = prefs(context).getString(alarmKey(id), null) ?: return null
        return runCatching { AlarmPayload.fromJson(value) }.getOrNull()
    }

    fun getAll(context: Context): List<AlarmPayload> {
        val prefs = prefs(context)
        val ids = prefs.getStringSet(keyAlarmIds, emptySet()).orEmpty()
        return ids.mapNotNull { id ->
            val numericId = id.toIntOrNull() ?: return@mapNotNull null
            get(context, numericId)
        }
    }

    fun clear(context: Context) {
        val prefs = prefs(context)
        val ids = prefs.getStringSet(keyAlarmIds, emptySet()).orEmpty()
        val editor = prefs.edit()
        ids.forEach { id ->
            editor.remove(alarmKey(id.toInt()))
        }
        editor.remove(keyAlarmIds).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun alarmKey(id: Int) = "alarm_$id"
}
