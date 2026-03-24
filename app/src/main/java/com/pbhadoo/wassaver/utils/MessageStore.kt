package com.pbhadoo.wassaver.utils

import android.content.Context
import com.pbhadoo.wassaver.data.model.DeletedMessage
import org.json.JSONArray
import org.json.JSONObject

object MessageStore {
    private const val PREFS_NAME = "deleted_messages"
    private const val KEY_MESSAGES = "messages"
    private const val KEY_CAPTURE_ENABLED = "capture_enabled"

    fun isCaptureEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CAPTURE_ENABLED, false)
    }

    fun setCaptureEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_CAPTURE_ENABLED, enabled).apply()
    }

    fun saveMessage(context: Context, message: DeletedMessage) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = getAllMessages(context).toMutableList()
        existing.add(message)
        // Keep last 1000 messages max
        val trimmed = if (existing.size > 1000) existing.takeLast(1000) else existing
        val arr = JSONArray()
        trimmed.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_MESSAGES, arr.toString()).apply()
    }

    fun getAllMessages(context: Context): List<DeletedMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { DeletedMessage.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getMessagesForSender(context: Context, senderId: String): List<DeletedMessage> {
        return getAllMessages(context).filter { it.senderId == senderId }
            .sortedBy { it.timestamp }
    }

    fun getConversations(context: Context): List<DeletedMessage> {
        // Return latest message per sender
        return getAllMessages(context)
            .groupBy { it.senderId }
            .map { (_, messages) -> messages.maxByOrNull { it.timestamp }!! }
            .sortedByDescending { it.timestamp }
    }

    fun clearChat(context: Context, senderId: String) {
        val remaining = getAllMessages(context).filter { it.senderId != senderId }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        remaining.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_MESSAGES, arr.toString()).apply()
    }

    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_MESSAGES).apply()
    }
}

private fun DeletedMessage.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("senderId", senderId)
    put("senderName", senderName)
    put("text", text)
    put("timestamp", timestamp)
    put("isGroup", isGroup)
    put("groupName", groupName ?: "")
    put("packageName", packageName)
}

private fun DeletedMessage.Companion.fromJson(obj: JSONObject): DeletedMessage = DeletedMessage(
    id = obj.getLong("id"),
    senderId = obj.getString("senderId"),
    senderName = obj.getString("senderName"),
    text = obj.getString("text"),
    timestamp = obj.getLong("timestamp"),
    isGroup = obj.getBoolean("isGroup"),
    groupName = obj.getString("groupName").ifEmpty { null },
    packageName = obj.getString("packageName")
)
