package com.project.maru.community

// 같은 목록에 검색, 카테고리, 정렬을 함께 적용한다.
internal object CommunityPostFilters {
    fun category(value: String): String = if (value == "지역정보") "꿀팁정보" else value

    fun apply(posts: List<CommunityPost>, selected: String, search: String, popular: Boolean): List<CommunityPost> {
        val words = search.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val result = posts.filter { post ->
            val matchesCategory = selected == "전체" || category(post.category) == selected
            val matchesSearch = words.all { word ->
                post.title.contains(word, ignoreCase = true) || post.content.contains(word, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
        return if (popular) result.sortedWith(compareByDescending<CommunityPost> { it.likeCount }
            .thenByDescending { it.createdAt }.thenBy { it.id })
        else result.sortedWith(compareByDescending<CommunityPost> { it.createdAt }.thenBy { it.id })
    }
}
