package com.project.maru.experience.recommendation

import com.project.maru.experience.location.EventCoordinates
import com.project.maru.experience.Experience
import com.project.maru.experience.ExperiencePeriod
import com.project.maru.experience.list.isSuitableForAge5To8

import android.content.Context
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class HomeExperienceRecommendationState(
    val personalized: Boolean,
    val events: List<Experience>
)

private const val HOME_LIMIT = 5

internal fun eligibleHomeExperiences(events: List<Experience>, today: LocalDate): List<Experience> {
    val remaining = ExperiencePeriod(today, LocalDate.MAX)
    return events.filter { event ->
        event.title.isNotBlank() && event.location.isNotBlank() && event.date.isNotBlank() &&
            isSuitableForAge5To8(event.targetAge) && remaining.includes(event)
    }.distinctBy(RecentExperienceStore::identityOf)
}

internal fun homeExperienceRecommendations(
    history: List<RecentExperienceStore.ViewedExperience>,
    candidates: List<Experience>,
    today: LocalDate
): HomeExperienceRecommendationState {
    val eligible = eligibleHomeExperiences(candidates, today)
    val similar = SimilarExperienceRecommender.recommend(history, eligible)
    if (similar.isNotEmpty()) return HomeExperienceRecommendationState(true, similar)

    val basic = eligible.sortedWith(
        compareBy<Experience> { it.startDate?.coerceAtLeast(today) ?: LocalDate.MAX }
            .thenBy { it.title }.thenBy { it.location }.thenBy { it.date }
    ).take(HOME_LIMIT)
    return HomeExperienceRecommendationState(false, basic)
}

internal object HomeExperienceRecommendationStore {
    private const val FILE = "home_experience_recommendations"
    private const val KEY = "last_recommendations"

    data class Saved(val state: HomeExperienceRecommendationState, val lastView: Long) {
        fun restore(history: List<RecentExperienceStore.ViewedExperience>, today: LocalDate): HomeExperienceRecommendationState? {
            if (state.personalized && history.isEmpty()) return null
            val valid = eligibleHomeExperiences(state.events, today)
            val events = if (state.personalized) {
                val viewed = history.map { RecentExperienceStore.identityOf(it.experience) }.toSet()
                valid.filterNot { RecentExperienceStore.identityOf(it) in viewed }
            } else valid
            return state.copy(events = events).takeIf { it.events.isNotEmpty() }
        }
    }

    fun write(context: Context, state: HomeExperienceRecommendationState, lastView: Long) {
        val events = JSONArray()
        state.events.take(HOME_LIMIT).forEach { event ->
            events.put(JSONObject().apply {
                put("title", event.title)
                put("location", event.location)
                put("date", event.date)
                put("category", event.category)
                put("imageUrl", event.imageUrl.orEmpty())
                put("targetAge", event.targetAge)
                put("region", event.region)
                put("startDate", event.startDate?.toString().orEmpty())
                put("endDate", event.endDate?.toString().orEmpty())
                put("fee", event.feeLabel)
                event.coordinates?.let { point ->
                    put("lat", point.latitude)
                    put("lng", point.longitude)
                }
            })
        }
        val snapshot = JSONObject().put("personalized", state.personalized)
            .put("lastView", lastView).put("events", events).toString()
        preferences(context).edit().putString(KEY, snapshot).apply()
    }

    fun read(context: Context): Saved? {
        val raw = preferences(context).getString(KEY, null) ?: return null
        val snapshot = try {
            JSONObject(raw)
        } catch (_: JSONException) {
            return null
        }
        val events = snapshot.optJSONArray("events") ?: return null
        val cards = (0 until minOf(events.length(), HOME_LIMIT)).mapNotNull { index ->
            val item = events.optJSONObject(index) ?: return@mapNotNull null
            Experience(
                title = item.optString("title"),
                location = item.optString("location"),
                date = item.optString("date"),
                category = item.optString("category"),
                imageUrl = item.optString("imageUrl").takeIf { it.isNotBlank() },
                targetAge = item.optString("targetAge"),
                region = item.optString("region"),
                startDate = ExperiencePeriod.parseApiDate(item.optString("startDate")),
                endDate = ExperiencePeriod.parseApiDate(item.optString("endDate")),
                coordinates = EventCoordinates.fromApi(item.optString("lat"), item.optString("lng")),
                feeLabel = item.optString("fee")
            )
        }
        return Saved(HomeExperienceRecommendationState(snapshot.optBoolean("personalized"), cards), snapshot.optLong("lastView"))
    }

    private fun preferences(context: Context) = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}
