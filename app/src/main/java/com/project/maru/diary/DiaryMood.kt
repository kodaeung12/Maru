package com.project.maru.diary

import com.project.maru.R

enum class DiaryMood(val key: String, val label: String, val image: Int, val description: String) {
    HAPPY("happy", "행복", R.drawable.diary_happy, "오늘은 행복했어요"),
    GOOD("good", "좋음", R.drawable.diary_good, "오늘은 기분이 좋았어요"),
    NEUTRAL("neutral", "보통", R.drawable.diary_neutral, "오늘은 평범한 하루였어요"),
    TIRED("tired", "힘듦", R.drawable.diary_tired, "오늘은 조금 힘들었어요"),
    SAD("sad", "속상함", R.drawable.diary_sad, "오늘은 마음이 속상했어요");

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key }
        fun summary(moods: List<String>): String {
            if (moods.isEmpty()) return "아직 감정 기록이 없어요"
            val positive = moods.count { it == "happy" || it == "good" }
            val difficult = moods.count { it == "tired" || it == "sad" }
            return when {
                positive > moods.size / 2 -> "대체로 좋음"
                difficult > moods.size / 2 -> "대체로 낮음"
                else -> "여러 감정이 고르게 나타났어요"
            }
        }
    }
}
