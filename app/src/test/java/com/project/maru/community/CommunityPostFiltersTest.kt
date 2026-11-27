package com.project.maru.community

import org.junit.Assert.assertEquals
import org.junit.Test

class CommunityPostFiltersTest {
    private val posts = listOf(
        CommunityPost(id = "a", category = "지역정보", title = "외출 준비", content = "물티슈 챙기기", createdAt = 3, likeCount = 2),
        CommunityPost(id = "b", category = "꿀팁정보", title = "외출 준비", content = "물티슈 챙기기", createdAt = 5, likeCount = 2),
        CommunityPost(id = "c", category = "육아정보", title = "외출 준비", content = "건강 정보", createdAt = 7, likeCount = 9)
    )

    @Test fun searchCategoryAndPopularityWorkTogetherIncludingLegacyCategory() {
        assertEquals(listOf("b", "a"), CommunityPostFilters.apply(posts, "꿀팁정보", "외출 물티슈", true).map { it.id })
    }

    @Test fun latestAndPopularityHaveDifferentOrders() {
        assertEquals(listOf("c", "b", "a"), CommunityPostFilters.apply(posts, "전체", "", false).map { it.id })
        assertEquals(listOf("c", "b", "a"), CommunityPostFilters.apply(posts, "전체", "", true).map { it.id })
        assertEquals(listOf("a", "c", "b"), CommunityPostFilters.apply(posts.map {
            if (it.id == "a") it.copy(likeCount = 20) else it
        }, "전체", "", true).map { it.id })
    }

    @Test fun searchIncludesContentAndReturnsAnEmptyListWhenThereIsNoMatch() {
        assertEquals(listOf("c"), CommunityPostFilters.apply(posts, "전체", "건강", false).map { it.id })
        assertEquals(emptyList<CommunityPost>(), CommunityPostFilters.apply(posts, "육아정보", "물티슈", false))
    }
}
