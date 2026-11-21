package com.project.maru

import com.project.maru.experience.Experience
import com.project.maru.experience.ExperiencePeriod

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class ExperiencePeriodTest {
    @Test
    fun seoulWeekStartsMondayEvenWhenUtcIsStillSunday() {
        val clock = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneOffset.UTC)
        val monday = ExperiencePeriod.todayInSeoul(clock)

        assertEquals(date("2026-11-23"), monday)
        assertEquals(period("2026-11-23", "2026-11-29"), ExperiencePeriod.weekOf(monday))
        assertEquals(period("2026-11-16", "2026-11-22"), ExperiencePeriod.weekOf(monday.minusDays(1)))
    }

    @Test
    fun monthIncludesLeapDayAndNextMonthCrossesYearBoundary() {
        assertEquals(period("2028-02-01", "2028-02-29"), ExperiencePeriod.monthOf(YearMonth.of(2028, 2)))
        val december = YearMonth.of(2026, 12)
        assertEquals(period("2026-12-01", "2026-12-31"), ExperiencePeriod.monthOf(december))
        assertEquals(period("2027-01-01", "2027-01-31"), ExperiencePeriod.monthOf(december.plusMonths(1)))
    }

    @Test
    fun apiTimestampDatesTakePrecedenceOverDisplayText() {
        val event = event("2026-12-24~2026-12-24").copy(
            startDate = ExperiencePeriod.parseApiDate("2027-02-12 00:00:00.0"),
            endDate = ExperiencePeriod.parseApiDate("2027-02-28 00:00:00.0")
        )

        assertEquals(date("2027-02-12"), event.startDate)
        assertEquals(date("2027-02-28"), event.endDate)
        assertTrue(period("2027-02-28", "2027-02-28").includes(event))
        assertFalse(period("2026-12-24", "2026-12-24").includes(event))
        assertNull(ExperiencePeriod.parseApiDate("2027-02-29 00:00:00.0"))
    }

    @Test
    fun overlappingRangesIncludeBothBoundariesAndEventsSpanningSelection() {
        val selected = period("2026-11-23", "2026-11-29")

        assertTrue(selected.includes(event("2026-11-20~2026-11-23")))
        assertTrue(selected.includes(event("2026-11-29 ~ 2026-12-01")))
        assertTrue(selected.includes(event("2026-11-20~2026-12-01")))
        assertFalse(selected.includes(event("2026-11-20~2026-11-22")))
        assertFalse(selected.includes(event("2026-11-30~2026-12-01")))
    }

    @Test
    fun parsesSingleDisplayDatesAndFallsBackWhenApiDatesAreIncomplete() {
        val selected = period("2026-11-10", "2026-11-10")
        assertTrue(selected.includes(event("2026.11.10").copy(startDate = date("2026-12-24"))))
        assertTrue(selected.includes(event("2026-11-10")))
        assertTrue(selected.includes(event("2026-11-10~2026-11-10")))
    }

    @Test
    fun limitedPeriodExcludesMissingInvalidAndReversedDates() {
        val selected = period("2026-11-01", "2026-11-30")
        assertFalse(selected.includes(event("")))
        assertFalse(selected.includes(event("2026.11.31")))
        assertFalse(selected.includes(event("2026-11-29~2026-11-23")))
    }

    private fun event(date: String) = Experience("행사", "체험관", date, "체험")

    private fun period(start: String, end: String) = ExperiencePeriod(date(start), date(end))

    private fun date(value: String) = LocalDate.parse(value)
}
