package com.project.maru.experience

import java.time.LocalDate

data class Experience(
    val title: String,
    val location: String,
    val date: String,
    val category: String,

    val imageResId: Int? = null,

    val imageUrl: String? = null,

    val targetAge: String = "",

    val region: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

// 여러 검색어는 모두 일치해야 하며 공백과 영문 대소문자는 구분하지 않는다.
internal fun Experience.matchesSearch(query: String): Boolean {
    val fields = listOf(title, location, region, category, targetAge)
        .map { it.replace(Regex("\\s+"), "") }
    return query.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .all { word -> fields.any { it.contains(word, ignoreCase = true) } }
}

fun isSuitableForAge5To8(targetAge: String): Boolean {

    val target = targetAge.replace(" ", "").lowercase()

    if (target.isBlank() || "성인전용" in target || "성인만" in target) {
        return false
    }

    val minimumAgePattern =
        Regex("""(?:만)?(\d{1,2})세(?:\([^)]*\))?(?:이상|부터)""")

    val minimumAges = minimumAgePattern.findAll(target)
        .mapNotNull { match ->
            match.groupValues[1].toIntOrNull()
        }
        .toList()

    if (minimumAges.isNotEmpty()) {
        return minimumAges.max() <= 8
    }

    if ("초등학생이상" in target) {
        return true
    }

    if ("누구나" in target || "전체관람가" in target) {
        return true
    }

    return false
}
