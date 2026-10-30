package com.keval.fieldpintask.data.camera

import com.keval.fieldpintask.domain.gis.LatLng
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class StampTextTest {
    private val ist = TimeZone.getTimeZone("Asia/Kolkata")
    private val tenFortyFour = Calendar.getInstance(ist).apply { set(2026, Calendar.OCTOBER, 27, 10, 44, 0) }.timeInMillis

    @Test fun `stamp has site name, coordinates with accuracy and date time`() {
        val lines = StampText.lines("Lake View Park", LatLng(28.6139, 77.2090), 6.2f, tenFortyFour, ist)
        assertEquals(
            listOf("Lake View Park", "28.61390° N, 77.20900° E ±6 m", "27 Oct 2026, 10:44 IST"),
            lines,
        )
    }

    @Test fun `southern and western hemispheres use S and W`() {
        val lines = StampText.lines("Site", LatLng(-33.8688, -70.6693), 12f, tenFortyFour, ist)
        assertEquals("33.86880° S, 70.66930° W ±12 m", lines[1])
    }
}
