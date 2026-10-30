package com.keval.fieldpintask.domain.gis

import kotlin.math.cos
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    private val metersPerDegree = GeoMath.EARTH_RADIUS_M * Math.PI / 180.0

    /** Axis-aligned rectangle of the given size in metres, south-west corner at [origin]. */
    private fun rectangle(origin: LatLng, widthM: Double, heightM: Double): List<LatLng> {
        val dLat = heightM / metersPerDegree
        val dLng = widthM / (metersPerDegree * cos(Math.toRadians(origin.latitude)))
        return listOf(
            origin,
            LatLng(origin.latitude, origin.longitude + dLng),
            LatLng(origin.latitude + dLat, origin.longitude + dLng),
            LatLng(origin.latitude + dLat, origin.longitude),
        )
    }

    private val ahmedabad = LatLng(23.0225, 72.5714)
    private val square100 = rectangle(ahmedabad, 100.0, 100.0)

    // ---- area ----

    @Test fun `area of a 100 m square is about 10000 square metres`() {
        assertEquals(10_000.0, GeoMath.areaSquareMeters(square100), 10_000.0 * 0.005)
    }

    @Test fun `area is the same for clockwise and counter-clockwise rings`() {
        assertEquals(
            GeoMath.areaSquareMeters(square100),
            GeoMath.areaSquareMeters(square100.reversed()),
            1e-6,
        )
    }

    @Test fun `area of one degree square at the equator matches the spherical value`() {
        val ring = listOf(LatLng(0.0, 0.0), LatLng(0.0, 1.0), LatLng(1.0, 1.0), LatLng(1.0, 0.0))
        // ~ (111.195 km)^2 * (1 - small curvature term)
        assertEquals(1.2364e10, GeoMath.areaSquareMeters(ring), 1.2364e10 * 0.005)
    }

    @Test fun `area shrinks with latitude for the same degree span`() {
        val equator = listOf(LatLng(0.0, 0.0), LatLng(0.0, 0.1), LatLng(0.1, 0.1), LatLng(0.1, 0.0))
        val north = listOf(LatLng(60.0, 0.0), LatLng(60.0, 0.1), LatLng(60.1, 0.1), LatLng(60.1, 0.0))
        assertEquals(0.5, GeoMath.areaSquareMeters(north) / GeoMath.areaSquareMeters(equator), 0.01)
    }

    @Test fun `area handles a ring crossing the antimeridian`() {
        val ring = listOf(LatLng(0.0, 179.9), LatLng(0.0, -179.9), LatLng(0.1, -179.9), LatLng(0.1, 179.9))
        val expected = GeoMath.areaSquareMeters(
            listOf(LatLng(0.0, 0.0), LatLng(0.0, 0.2), LatLng(0.1, 0.2), LatLng(0.1, 0.0)),
        )
        assertEquals(expected, GeoMath.areaSquareMeters(ring), expected * 0.001)
    }

    @Test fun `area is zero for fewer than three points`() {
        assertEquals(0.0, GeoMath.areaSquareMeters(emptyList()), 0.0)
        assertEquals(0.0, GeoMath.areaSquareMeters(square100.take(2)), 0.0)
    }

    @Test fun `acres conversion uses 4046_86 square metres`() {
        assertEquals(1.0, GeoMath.squareMetersToAcres(4_046.8564224), 1e-9)
        assertEquals(1.59, GeoMath.squareMetersToAcres(6_420.0), 0.01)
    }

    // ---- perimeter and distance ----

    @Test fun `perimeter of a 100 m square is about 400 metres`() {
        assertEquals(400.0, GeoMath.perimeterMeters(square100), 400.0 * 0.002)
    }

    @Test fun `perimeter closes the ring`() {
        val triangle = listOf(LatLng(0.0, 0.0), LatLng(0.0, 0.001), LatLng(0.001, 0.0))
        val open = GeoMath.distanceMeters(triangle[0], triangle[1]) + GeoMath.distanceMeters(triangle[1], triangle[2])
        assertTrue(GeoMath.perimeterMeters(triangle) > open)
    }

    @Test fun `distance between Ahmedabad and Mumbai is about 440 km`() {
        val mumbai = LatLng(19.0760, 72.8777)
        assertEquals(440_000.0, GeoMath.distanceMeters(ahmedabad, mumbai), 10_000.0)
    }

    @Test fun `distance to the same point is zero`() {
        assertEquals(0.0, GeoMath.distanceMeters(ahmedabad, ahmedabad), 1e-9)
    }

    // ---- point in polygon ----

    private val lShape = listOf(
        LatLng(0.0, 0.0), LatLng(0.0, 0.02), LatLng(0.01, 0.02),
        LatLng(0.01, 0.01), LatLng(0.02, 0.01), LatLng(0.02, 0.0),
    )

    @Test fun `centre of the square is inside`() {
        val centre = LatLng(ahmedabad.latitude + 0.00045, ahmedabad.longitude + 0.0005)
        assertTrue(GeoMath.isInside(centre, square100))
    }

    @Test fun `point outside the square is outside`() {
        assertFalse(GeoMath.isInside(LatLng(ahmedabad.latitude + 0.01, ahmedabad.longitude), square100))
        assertFalse(GeoMath.isInside(LatLng(ahmedabad.latitude - 0.0001, ahmedabad.longitude + 0.0004), square100))
    }

    @Test fun `concave notch of an L shape is outside while both arms are inside`() {
        assertFalse(GeoMath.isInside(LatLng(0.015, 0.015), lShape))
        assertTrue(GeoMath.isInside(LatLng(0.005, 0.015), lShape))
        assertTrue(GeoMath.isInside(LatLng(0.015, 0.005), lShape))
    }

    @Test fun `inside test does not depend on winding order`() {
        assertTrue(GeoMath.isInside(LatLng(0.005, 0.015), lShape.reversed()))
        assertFalse(GeoMath.isInside(LatLng(0.015, 0.015), lShape.reversed()))
    }

    @Test fun `degenerate rings contain nothing`() {
        assertFalse(GeoMath.isInside(ahmedabad, emptyList()))
        assertFalse(GeoMath.isInside(ahmedabad, square100.take(2)))
    }

    // ---- distance to polygon ----

    @Test fun `distance to polygon is zero inside`() {
        val centre = LatLng(ahmedabad.latitude + 0.00045, ahmedabad.longitude + 0.0005)
        assertEquals(0.0, GeoMath.distanceToPolygonMeters(centre, square100), 0.0)
    }

    @Test fun `distance to polygon from 100 m north of an edge is about 100 metres`() {
        val north = LatLng(square100[2].latitude + 100.0 / metersPerDegree, ahmedabad.longitude + 0.0005)
        assertEquals(100.0, GeoMath.distanceToPolygonMeters(north, square100), 1.0)
    }

    @Test fun `distance to polygon from a diagonal corner uses the corner`() {
        val ne = square100[2]
        val point = LatLng(ne.latitude + 30.0 / metersPerDegree, ne.longitude + 40.0 / (metersPerDegree * cos(Math.toRadians(ne.latitude))))
        assertEquals(50.0, GeoMath.distanceToPolygonMeters(point, square100), 1.0)
    }
}
