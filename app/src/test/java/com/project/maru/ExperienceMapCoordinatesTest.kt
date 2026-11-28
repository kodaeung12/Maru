package com.project.maru

import com.project.maru.experience.location.EventCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExperienceMapCoordinatesTest {
    @Test fun validatesApiCoordinates() {
        val point = checkNotNull(EventCoordinates.fromApi(" 37.4856046634033 ", "127.028078834421"))
        assertEquals(37.4856046634033, point.latitude, 0.0)
        assertEquals(127.028078834421, point.longitude, 0.0)
        assertNull(EventCoordinates.fromApi(null, "127"))
        assertNull(EventCoordinates.fromApi(" ", "127"))
        assertNull(EventCoordinates.fromApi("NaN", "127"))
        assertNull(EventCoordinates.fromApi("37", "Infinity"))
        assertNull(EventCoordinates.fromApi("127", "37"))
        assertNull(EventCoordinates.fromApi("37", "181"))
        assertNull(EventCoordinates.fromApi("0", "-0"))
    }
}
