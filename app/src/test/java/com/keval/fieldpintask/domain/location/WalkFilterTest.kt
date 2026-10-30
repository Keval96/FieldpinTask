package com.keval.fieldpintask.domain.location

import com.keval.fieldpintask.domain.gis.LatLng
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkFilterTest {
    private val origin = LatLng(23.0, 72.0)
    private fun fix(point: LatLng, accuracy: Float) = DeviceLocation(point, accuracy, 0L)

    // 0.0001 degrees of latitude is about 11 m.
    private val elevenMetresNorth = LatLng(23.0001, 72.0)
    private val oneMetreNorth = LatLng(23.00001, 72.0)

    @Test fun `first accurate fix is accepted`() = assertTrue(WalkFilter.accept(fix(origin, 8f), null))

    @Test fun `fix worse than 20 m is ignored`() = assertFalse(WalkFilter.accept(fix(origin, 20.5f), null))

    @Test fun `fix at exactly 20 m is accepted`() = assertTrue(WalkFilter.accept(fix(origin, 20f), null))

    @Test fun `fix too close to the last corner is ignored`() =
        assertFalse(WalkFilter.accept(fix(oneMetreNorth, 5f), origin))

    @Test fun `fix far enough from the last corner is accepted`() =
        assertTrue(WalkFilter.accept(fix(elevenMetresNorth, 5f), origin))

    @Test fun `inaccurate fix is ignored even when far away`() =
        assertFalse(WalkFilter.accept(fix(elevenMetresNorth, 35f), origin))
}
