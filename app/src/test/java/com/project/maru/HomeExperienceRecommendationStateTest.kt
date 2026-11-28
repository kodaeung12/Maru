package com.project.maru

import com.project.maru.experience.Experience
import com.project.maru.experience.recommendation.RecentExperienceStore.ViewedExperience
import com.project.maru.experience.recommendation.homeExperienceRecommendations
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeExperienceRecommendationStateTest {
    private val today = LocalDate.of(2026, 11, 26)

    @Test fun basicRecommendationsAreValidAndStable() {
        val valid = (1..6).map { event("체험 $it", it.toLong()) }
        val candidates = valid.reversed() + valid.first() + event("종료", -1) +
            event("성인", 1).copy(targetAge = "성인만") + event("", 1)
        val state = homeExperienceRecommendations(emptyList(), candidates, today)
        assertFalse(state.personalized)
        assertEquals(valid.take(5), state.events)
        assertEquals(state, homeExperienceRecommendations(emptyList(), candidates.reversed(), today))
    }

    @Test fun personalizesOnlyWhenSimilarEventsExist() {
        val viewed = event("종이 공예", 0)
        val similar = event("종이 공예 클래스", 1)
        val other = event("오케스트라", 1).copy(category = "문화/예술")
        val history = listOf(ViewedExperience(viewed, 1))
        val state = homeExperienceRecommendations(history, listOf(viewed, similar, other, similar), today)
        assertTrue(state.personalized)
        assertEquals(listOf(similar), state.events)
        val fallback = homeExperienceRecommendations(history, listOf(other), today)
        assertFalse(fallback.personalized)
        assertEquals(listOf(other), fallback.events)
    }

    private fun event(title: String, offset: Long): Experience {
        val date = today.plusDays(offset)
        return Experience(title, "체험관", date.toString(), "체험",
            targetAge = "누구나", startDate = date, endDate = date)
    }
}
