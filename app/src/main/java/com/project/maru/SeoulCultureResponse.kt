package com.project.maru

data class SeoulCultureResponse(
    val aiViewCulture: SeoulCultureData
)

data class SeoulCultureData(
    val list_total_count: Int,
    val row: List<SeoulCultureEvent>
)

data class SeoulCultureEvent(
    val TITLE: String,
    val DATE: String,
    val PLACE: String,
    val CODENAME: String,
    val GUNAME: String,
    val USE_TRGT: String,
    val MAIN_IMG: String
)
