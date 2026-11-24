package com.project.maru

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventReviewIdTest {
    @Test fun sameEventHasSameIdAcrossScreens() {
        assertEquals(eventReviewId(" 과학 체험 ", "서울", "2026-11-23"), eventReviewId("과학 체험", "서울", "2026-11-23"))
        assertTrue(eventReviewId("제목/슬래시", "서울", "날짜").matches(Regex("[0-9a-f]{64}")))
    }
    @Test fun differentDatesAndPlacesHaveSeparateReviews() {
        val id = eventReviewId("과학", "서울", "2026-11-23")
        assertNotEquals(id, eventReviewId("과학", "부산", "2026-11-23"))
        assertNotEquals(id, eventReviewId("과학", "서울", "2026-11-24"))
    }
    @Test fun fieldsCannotCollideByConcatenation() {
        assertNotEquals(eventReviewId("ab", "c", "d"), eventReviewId("a", "bc", "d"))
    }
}
