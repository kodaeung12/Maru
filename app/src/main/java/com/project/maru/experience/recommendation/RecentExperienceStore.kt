package com.project.maru.experience.recommendation

import com.project.maru.experience.Experience

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

internal object RecentExperienceStore {
    private const val PREFERENCES_NAME = "recent_experiences"
    private const val HISTORY_KEY = "history"
    private const val MAX_RECORDS = 5
    private val whitespace = Regex("\\s+")

    data class ViewedExperience(val experience: Experience, val viewedAt: Long)

    @Synchronized
    fun recordView(context: Context, experience: Experience) {
        val key = identityOf(experience)
        val history = readRecent(context).filterNot { identityOf(it.experience) == key }
        val updated = listOf(ViewedExperience(experience, System.currentTimeMillis())) + history
        val records = JSONArray()
        updated.take(MAX_RECORDS).forEach { record ->
            val event = record.experience
            records.put(JSONObject().apply {
                put("title", event.title)
                put("location", event.location)
                put("date", event.date)
                put("category", event.category)
                put("imageUrl", event.imageUrl)
                put("targetAge", event.targetAge)
                put("region", event.region)
                put("viewedAt", record.viewedAt)
            })
        }
        preferences(context).edit().putString(HISTORY_KEY, records.toString()).apply()
    }

    fun readRecent(context: Context): List<ViewedExperience> {
        val stored = preferences(context).getString(HISTORY_KEY, null) ?: return emptyList()
        val records = try {
            JSONArray(stored)
        } catch (_: JSONException) {
            return emptyList()
        }

        return (0 until records.length()).mapNotNull { index ->
            val record = records.optJSONObject(index) ?: return@mapNotNull null
            ViewedExperience(
                experience = Experience(
                    title = record.optString("title"),
                    location = record.optString("location"),
                    date = record.optString("date"),
                    category = record.optString("category"),
                    imageUrl = record.optString("imageUrl").takeIf { it.isNotEmpty() },
                    targetAge = record.optString("targetAge"),
                    region = record.optString("region")
                ),
                viewedAt = record.optLong("viewedAt")
            )
        }.take(MAX_RECORDS)
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun identityOf(experience: Experience): List<String> =
        listOf(experience.title, experience.location, experience.date).map { value ->
            value.trim().replace(whitespace, " ").lowercase(Locale.ROOT)
        }
}
