package com.project.maru.community

import com.google.firebase.firestore.FirebaseFirestoreException

// 게시글/댓글 경로와 오류 안내를 한 곳에서 관리한다.
object CommunityFirestore {
    const val POSTS = "communityPosts"
    const val COMMENTS = "comments"

    fun errorMessage(error: Exception): String {
        if (error is FirebaseFirestoreException) {
            return when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "접근 권한이 없어요. 로그인 계정과 Firestore 규칙을 확인해 주세요."
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> "로그인이 만료됐어요. 다시 로그인해 주세요."
                FirebaseFirestoreException.Code.UNAVAILABLE -> "서버에 연결하지 못했어요. 인터넷 연결 후 다시 시도해 주세요."
                FirebaseFirestoreException.Code.NOT_FOUND -> "삭제되었거나 존재하지 않는 글이에요."
                else -> "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."
            }
        }
        return "게시글 데이터를 처리하지 못했어요. 다시 시도해 주세요."
    }
}
