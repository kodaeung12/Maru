package com.project.maru.experience.list

private val ageRange = Regex("""(?:만)?(\d{1,2})(?:세)?[~～〜–-](?:만)?(\d{1,2})세""")
private val ageLimit = Regex("""(?:만)?(\d{1,2})세(?:\([^)]*\))?(이상|부터|이하|미만|초과|까지)?""")
private val generalAudience = listOf("누구나", "전체관람가", "전연령", "모든연령", "초등학생이상")
private val adultOnly = listOf("성인전용", "성인만", "성인대상")
private val childAudience = listOf("유아", "어린이", "아동", "미취학", "초등")

fun isSuitableForAge5To8(targetAge: String): Boolean {
    val target = targetAge.filterNot(Char::isWhitespace)
    if (target.isBlank() || target == "성인" || adultOnly.any { it in target }) return false

    if ("성인누구나" in target && childAudience.none { it in target }) return false

    return explicitFamilyAges(target)?.isNotEmpty() ?: generalAudience.any { it in target }
}

internal fun explicitFamilyAges(target: String): List<Int>? {
    val range = ageRange.find(target)
    if (range != null) {
        val first = range.groupValues[1].toInt()
        val last = range.groupValues[2].toInt()
        return (5..8).filter { it in first..last }
    }

    val limits = ageLimit.findAll(target).toList()
    if (limits.isEmpty()) return null
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
