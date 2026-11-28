package com.project.maru.experience.recommendation

import com.project.maru.experience.Experience
import com.project.maru.experience.list.explicitFamilyAges

import java.util.Locale

internal object SimilarExperienceRecommender {
    private val recencyWeights = listOf(1.0, 0.9, 0.8, 0.7, 0.6)
    private val wordPattern = Regex("[\\p{L}\\p{N}]+")

    fun recommend(
        history: List<RecentExperienceStore.ViewedExperience>,
        candidates: List<Experience>
    ): List<Experience> {
        if (history.isEmpty()) return emptyList()

        val viewedKeys = history.map { RecentExperienceStore.identityOf(it.experience) }.toSet()
        val recent = history.sortedByDescending { it.viewedAt }
            .distinctBy { RecentExperienceStore.identityOf(it.experience) }
            .zip(recencyWeights)

        return candidates.asSequence()
            .filterNot { RecentExperienceStore.identityOf(it) in viewedKeys }
            .map { candidate ->
                val score = recent.maxOf { (viewed, weight) ->
                    similarityScore(viewed.experience, candidate) * weight
                }
                candidate to score
            }
            .filter { (_, score) -> score > 0.0 }
            .sortedWith(compareByDescending<Pair<Experience, Double>> { it.second }
                .thenBy { it.first.title }
                .thenBy { it.first.location }
                .thenBy { it.first.date })
            .map { it.first }
            .distinctBy(RecentExperienceStore::identityOf)
            .take(5)
            .toList()
    }

    fun recommendForEvent(current: Experience, candidates: List<Experience>): List<Experience> {
        val currentKey = RecentExperienceStore.identityOf(current)
        return candidates.asSequence()
            .filterNot { RecentExperienceStore.identityOf(it) == currentKey }
            .map { it to similarityScore(current, it) }
            .filter { it.second > 0.0 }
            .sortedWith(compareByDescending<Pair<Experience, Double>> { it.second }
                .thenBy { it.first.title }
                .thenBy { it.first.location }
                .thenBy { it.first.date })
            .map { it.first }
            .distinctBy(RecentExperienceStore::identityOf)
            .take(5)
            .toList()
    }

    fun similarityScore(viewed: Experience, candidate: Experience): Double {
        val category = viewed.category.trim()
        val categoryScore = if (category.isNotEmpty() && category != "기타" &&
            category == candidate.category.trim()
        ) 40 else 0

        val viewedWords = keywords(viewed.title)
        val candidateWords = keywords(candidate.title)
        val allWords = viewedWords union candidateWords
        val keywordScore = if (allWords.isEmpty()) 0.0 else
            30.0 * (viewedWords intersect candidateWords).size / allWords.size

        val region = viewed.region.trim()
        val regionScore = if (region.isNotEmpty() && region == candidate.region.trim()) 15 else 0

        val viewedAges = eligibleAges(viewed.targetAge)
        val candidateAges = eligibleAges(candidate.targetAge)
        val ageScore = if (viewedAges.any { it in candidateAges }) 15 else 0

        return categoryScore + keywordScore + regionScore + ageScore
    }

    private fun keywords(title: String): Set<String> =
        wordPattern.findAll(title.lowercase(Locale.ROOT)).map { it.value }.toSet()

    private fun eligibleAges(targetAge: String): List<Int> {
        val target = targetAge.filterNot(Char::isWhitespace)
        if ("성인전용" in target || "성인만" in target) return emptyList()
        return explicitFamilyAges(target).orEmpty()
    }
}
