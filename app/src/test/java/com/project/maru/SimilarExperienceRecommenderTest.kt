package com.project.maru

import com.project.maru.experience.Experience
import com.project.maru.experience.RecentExperienceStore
import com.project.maru.experience.SimilarExperienceRecommender

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarExperienceRecommenderTest {
    @Test
    fun combinesCategoryKeywordsRegionAndAge() {
        val viewed = event("도자기 만들기", age = "만 5~7세")
        val candidate = event("도자기, 만들기 공방 체험", age = "만 7세 이상")

        assertEquals(85.0, SimilarExperienceRecommender.similarityScore(viewed, candidate), 0.001)
        assertEquals(70.0, SimilarExperienceRecommender.similarityScore(
            viewed, candidate.copy(targetAge = "만 8세 이상")
        ), 0.001)
    }

    @Test
    fun missingOrUnspecifiedAgeAddsNoPoints() {
        val viewed = event("도자기 만들기")
        val candidate = event("도자기 만들기 공방 체험", age = "만 6세 이상")

        assertEquals(70.0, SimilarExperienceRecommender.similarityScore(viewed, candidate), 0.001)
        assertEquals(70.0, SimilarExperienceRecommender.similarityScore(
            viewed.copy(targetAge = "누구나"), candidate
        ), 0.001)
    }

    @Test
    fun excludesViewedEventsAndDuplicateCandidatesAndCapsResultsAtFive() {
        val viewed = event("Ceramic Workshop")
        val viewedAgain = viewed.copy(title = " ceramic   workshop ")
        val candidates = (1..6).map { event("후보 $it") }
        val duplicate = candidates.first().copy(location = " 체험관 ")

        val result = SimilarExperienceRecommender.recommend(
            listOf(view(viewed, 1)), listOf(viewedAgain, duplicate) + candidates
        )

        assertEquals(candidates.take(5).map { it.title }, result.map { it.title })
    }

    @Test
    fun newerViewsRankHigherAndOnlyFiveViewsContribute() {
        val history = (1..6).map { index ->
            view(event("조회$index").copy(category = "분류$index", region = ""), index.toLong())
        }
        val oldCandidate = history[1].experience.copy(title = "가 후보")
        val newCandidate = history.last().experience.copy(title = "나 후보")
        val ignoredCandidate = history.first().experience.copy(title = "다 후보")

        assertEquals(listOf(newCandidate, oldCandidate), SimilarExperienceRecommender.recommend(
            history, listOf(oldCandidate, ignoredCandidate, newCandidate)
        ))
    }

    @Test
    fun emptyHistoryProducesNoRecommendations() {
        assertTrue(SimilarExperienceRecommender.recommend(emptyList(), listOf(event("후보"))).isEmpty())
    }

    private fun event(title: String, age: String = "") =
        Experience(title, "체험관", "2026-11-21", "체험", targetAge = age, region = "종로구")

    private fun view(event: Experience, time: Long) = RecentExperienceStore.ViewedExperience(event, time)
}
