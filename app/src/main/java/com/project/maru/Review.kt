package com.project.maru

// 리뷰 한 개의 데이터
data class Review(
    val name: String,
    val rating: Int,      // 1 ~ 5
    val date: String,
    val content: String
)

// 서울시 API에는 리뷰가 없어서 임시(더미) 리뷰를 사용
// 나중에 Firebase(Firestore) 등으로 바꾸면 이 부분만 교체하면 됨
val sampleReviews = listOf(
    Review("하늘맘", 5, "2026.10.26", "아이가 체험 부스를 너무 좋아했어요! 다음에도 또 오고 싶어요."),
    Review("준우아빠", 4, "2026.10.26", "공연도 재밌고 체험도 다양해서 하루가 금방 갔어요."),
   )
