package com.project.maru
import org.junit.Assert.*
import org.junit.Test
class ScheduleTest {
    private fun item(minutes: Int) = Schedule("id","title","",200000000L,minutes,"revision")
    @Test fun reminderTimes() {
        assertEquals(200000000L,item(0).alarmAt)
        assertEquals(196400000L,item(60).alarmAt)
        assertEquals(113600000L,item(1440).alarmAt)
        assertNull(item(-1).alarmAt)
    }
    @Test fun earlyAndDisabledAlarmsAreIgnored() {
        assertFalse(item(0).canDeliver(199999999L))
        assertFalse(item(-1).canDeliver(200000000L))
    }
    @Test fun dueAlarmIsDeliveredButVeryOldAlarmIsIgnored() {
        assertTrue(item(0).canDeliver(200000000L))
        assertTrue(item(60).canDeliver(196400000L))
        assertFalse(item(0).canDeliver(286400000L))
    }
}
