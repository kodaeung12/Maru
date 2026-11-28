package com.project.maru.experience

import com.project.maru.experience.location.EventCoordinates

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
    val endDate: LocalDate? = null,
    val coordinates: EventCoordinates? = null,
    val feeLabel: String = "",
    val sourceCategory: String = ""
)

internal fun Experience.matchesSearch(query: String): Boolean {
    val fields = listOf(title, location, region, category, targetAge)
        .map { it.replace(Regex("\\s+"), "") }
    return query.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .all { word -> fields.any { it.contains(word, ignoreCase = true) } }
}
