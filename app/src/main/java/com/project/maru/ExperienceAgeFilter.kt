package com.project.maru

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
