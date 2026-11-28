package com.project.maru

object ProfileAvatar {
    fun image(gender: String?): Int = when (gender) {
        "female", "여성", "여자" -> R.drawable.ic_profile_female
        "male", "남성", "남자" -> R.drawable.ic_profile_male
        else -> R.drawable.ic_person
    }
}
