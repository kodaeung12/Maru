package com.project.maru.community

// 게시글 아래 comments 컬렉션에 저장하는 댓글
data class CommunityComment(
    var id: String = "",
    var postId: String = "",
    var ownerId: String = "",
    var authorName: String = "",
    var content: String = "",
    var createdAt: Long = 0L
)
