package com.project.maru.experience

import java.util.Locale

internal object SimilarExperienceRecommender {
    private val recencyWeights = listOf(1.0, 0.9, 0.8, 0.7, 0.6)
    private val wordPattern = Regex("[\\p{L}\\p{N}]+")
    private val ageRange = Regex("(?:만)?(\\d{1,2})(?:세)?[~～〜–-](?:만)?(\\d{1,2})세")
    private val ageLimit = Regex("(?:만)?(\\d{1,2})세(?:\\([^)]*\\))?(이상|부터|이하|미만|초과|까지)?")

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
        val range = ageRange.find(target)
        if (range != null) {
            val first = range.groupValues[1].toInt()
            val last = range.groupValues[2].toInt()
            return (5..8).filter { it in first..last }
        }

        val limits = ageLimit.findAll(target).toList()
        if (limits.isEmpty()) return emptyList()

        return (5..8).filter { age ->
            limits.all { limit ->
                val boundary = limit.groupValues[1].toInt()
                when (limit.groupValues[2]) {
                    "이상", "부터" -> age >= boundary
                    "이하", "까지" -> age <= boundary
                    "미만" -> age < boundary
                    "초과" -> age > boundary
                    else -> age == boundary
                }
            }
        }
    }
}
