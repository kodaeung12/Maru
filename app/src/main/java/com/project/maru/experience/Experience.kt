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
