package com.project.maru

import com.project.maru.experience.Experience
import com.project.maru.experience.matchesSearch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperienceSearchTest {
    private val event = Experience("어린이 과학 체험", "서울 과학관", "2026-11-23", "교육", region = "종로구", targetAge = "5세 이상")

    @Test fun searchesTitlePlaceRegionCategoryAndAge() {
        listOf("과학", "과학관", "종로구", "교육", "5세").forEach { assertTrue(event.matchesSearch(it)) }
    }
    @Test fun matchesMultipleWordsAcrossFields() {
        assertTrue(event.matchesSearch("  종로구   과학  "))
        assertFalse(event.matchesSearch("종로구 음악"))
    }
    @Test fun ignoresSpacingInStoredFields() {
        assertTrue(event.matchesSearch("어린이과학"))
    }
    @Test fun ignoresEnglishCase() {
        assertTrue(event.copy(title = "Kids ART").matchesSearch("kids art"))
    }
    @Test fun emptyQueryShowsAllAndUnknownQueryMatchesNothing() {
        assertTrue(event.matchesSearch("  "))
        assertFalse(event.matchesSearch("없는행사"))
    }
}