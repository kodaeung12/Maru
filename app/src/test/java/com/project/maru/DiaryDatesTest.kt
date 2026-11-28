package com.project.maru

import com.project.maru.diary.DiaryDates
import com.project.maru.diary.DiaryMood
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DiaryDatesTest {
    @Test fun weekStartsOnMondayAcrossYearBoundary() {
        assertEquals(LocalDate.of(2025, 12, 29), DiaryDates.weekStart(LocalDate.of(2026, 1, 4)))
        assertEquals(LocalDate.of(2026, 1, 5), DiaryDates.weekStart(LocalDate.of(2026, 1, 5)))
    }
    @Test fun leapMonthAndSixWeekMonthKeepSundayColumns() {
        assertEquals(29, calendarCells(YearMonth.of(2024, 2)).filterNotNull().size)
        val august = calendarCells(YearMonth.of(2026, 8))
        assertEquals(42, august.size)
        assertEquals(LocalDate.of(2026, 8, 1), august[6])
    }
    @Test fun weeklySummaryDoesNotInventAMajority() {
        assertEquals("아직 감정 기록이 없어요", DiaryMood.summary(emptyList()))
        assertEquals("대체로 좋음", DiaryMood.summary(listOf("happy", "good", "tired")))
        assertEquals("대체로 낮음", DiaryMood.summary(listOf("sad", "tired", "neutral")))
        assertEquals("여러 감정이 고르게 나타났어요", DiaryMood.summary(listOf("good", "tired")))
    }
}
