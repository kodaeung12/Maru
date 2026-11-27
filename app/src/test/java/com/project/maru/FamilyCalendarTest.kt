package com.project.maru
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth
class FamilyCalendarTest {
    @Test fun sundayStartsFirstColumn() {
        val cells=calendarCells(YearMonth.of(2026,11))
        assertEquals(1,cells.first()!!.dayOfMonth)
        assertEquals(30,cells.filterNotNull().size)
        assertEquals(0,cells.size%7)
    }
    @Test fun leapDayAndEmptyCellsAreCorrect() {
        val month=YearMonth.of(2028,2)
        val cells=calendarCells(month)
        assertEquals(29,cells.filterNotNull().size)
        assertEquals(2,cells.indexOfFirst { it!=null })
        assertEquals(month.atEndOfMonth(),cells.filterNotNull().last())
    }
    @Test fun sixWeekMonthHasEveryDayOnce() {
        val month=YearMonth.of(2026,8)
        val cells=calendarCells(month)
        assertEquals(42,cells.size)
        assertEquals((1..31).toList(),cells.filterNotNull().map { it.dayOfMonth })
    }
}
