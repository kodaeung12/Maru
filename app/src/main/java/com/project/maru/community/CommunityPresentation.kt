package com.project.maru.community

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommunityPresentation {
    fun time(createdAt: Long): String {
        if (createdAt <= 0) return "작성 시간 없음"
        val minutes = ((System.currentTimeMillis() - createdAt).coerceAtLeast(0) / 60000)
        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            minutes < 1440 -> "${minutes / 60}시간 전"
            minutes < 10080 -> "${minutes / 1440}일 전"
            else -> SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(createdAt))
        }
    }
}
