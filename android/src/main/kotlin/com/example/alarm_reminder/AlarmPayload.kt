package com.example.alarm_reminder

import org.json.JSONObject

data class AlarmPayload(
    val id: Int,
    val triggerAtMillis: Long,
    val title: String,
    val body: String,
    val dismissLabel: String?,
) {
    fun toJson(): String {
        return JSONObject()
            .put("id", id)
            .put("triggerAtMillis", triggerAtMillis)
            .put("title", title)
            .put("body", body)
            .put("dismissLabel", dismissLabel)
            .toString()
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "triggerAtMillis" to triggerAtMillis,
            "title" to title,
            "body" to body,
            "dismissLabel" to dismissLabel,
        )
    }

    companion object {
        fun fromJson(value: String): AlarmPayload {
            val json = JSONObject(value)
            return AlarmPayload(
                id = json.getInt("id"),
                triggerAtMillis = json.getLong("triggerAtMillis"),
                title = json.getString("title"),
                body = json.getString("body"),
                dismissLabel = json.optString("dismissLabel").ifBlank { null },
            )
        }

        fun fromMap(map: Map<*, *>): AlarmPayload {
            val id = (map["id"] as Number?)?.toInt()
                ?: throw IllegalArgumentException("Alarm id is required")
            val triggerAtMillis = (map["triggerAtMillis"] as Number?)?.toLong()
                ?: throw IllegalArgumentException("triggerAtMillis is required")
            val title = map["title"] as String?
                ?: throw IllegalArgumentException("title is required")
            val body = map["body"] as String?
                ?: throw IllegalArgumentException("body is required")
            val dismissLabel = (map["dismissLabel"] as String?)?.ifBlank { null }

            return AlarmPayload(
                id = id,
                triggerAtMillis = triggerAtMillis,
                title = title,
                body = body,
                dismissLabel = dismissLabel,
            )
        }
    }
}
