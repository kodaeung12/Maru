package com.project.maru.community

data class CommunityPost(
    var id: String = "",
    var category: String = "육아정보",
    var title: String = "",
    var content: String = "",
    var ownerId: String = "",
    var authorName: String = "",
    var createdAt: Long = 0L
)
