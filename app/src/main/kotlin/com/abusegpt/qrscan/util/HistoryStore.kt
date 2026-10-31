package com.abusegpt.qrscan.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryItem(
    val value: String,
    val timestamp: Long
)

class HistoryStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("qrscan_history", Context.MODE_PRIVATE)

    fun all(): List<HistoryItem> = runCatching {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val value = obj.optString("v")
                if (value.isNotBlank()) add(HistoryItem(value, obj.optLong("t")))
            }
        }
    }.getOrDefault(emptyList())

    fun add(value: String): List<HistoryItem> {
        val current = all().filterNot { it.value == value }
        val updated = (listOf(HistoryItem(value, System.currentTimeMillis())) + current).take(60)
        persist(updated)
        return updated
    }

    fun remove(value: String): List<HistoryItem> {
        val updated = all().filterNot { it.value == value }
        persist(updated)
        return updated
    }

    fun clear(): List<HistoryItem> {
        prefs.edit().remove(KEY).apply()
        return emptyList()
    }

    private fun persist(items: List<HistoryItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().put("v", item.value).put("t", item.timestamp))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "items"
    }
}
