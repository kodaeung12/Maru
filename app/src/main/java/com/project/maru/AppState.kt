package com.project.maru   // ← 내 프로젝트 패키지명과 다르면 이 줄만 바꿔주세요

// 화면을 옮겨 다녀도 기억해야 하는 값들
object AppState {
    var notificationOn = true                        // 알림 ON/OFF
    val likedCards = booleanArrayOf(false, false, false)  // 추천 체험 카드 3개의 하트
}
