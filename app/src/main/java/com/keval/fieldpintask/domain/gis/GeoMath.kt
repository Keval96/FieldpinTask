package com.keval.fieldpintask.domain.gis

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geodesic helpers that work on the earth's surface rather than on flat x/y.
 * The earth is modelled as a sphere (mean radius); for site-sized polygons the error
 * versus the WGS84 ellipsoid is well below 1 %.
 */
object GeoMath {
    const val EARTH_RADIUS_M = 6_371_008.8
    const val SQUARE_METERS_PER_ACRE = 4_046.8564224

    private const val METERS_PER_DEGREE = EARTH_RADIUS_M * PI / 180.0

    /** Great-circle distance in metres (haversine). */
    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(h)))
    }

    /** Perimeter of the closed ring in metres. Returns 0 for fewer than 2 points. */
    fun perimeterMeters(ring: List<LatLng>): Double {
        if (ring.size < 2) return 0.0
        return ring.indices.sumOf { i -> distanceMeters(ring[i], ring[(i + 1) % ring.size]) }
    }

    /**
     * Area of the ring in square metres on the sphere (Chamberlain-Duquette):
     * A = R^2 / 2 * |sum((lng2 - lng1) * (2 + sin(lat1) + sin(lat2)))|.
     * Independent of winding order. Returns 0 for fewer than 3 points.
     */
    fun areaSquareMeters(ring: List<LatLng>): Double {
        if (ring.size < 3) return 0.0
        var sum = 0.0
        for (i in ring.indices) {
            val p1 = ring[i]
            val p2 = ring[(i + 1) % ring.size]
            sum += wrapRadians(Math.toRadians(p2.longitude - p1.longitude)) *
                (2 + sin(Math.toRadians(p1.latitude)) + sin(Math.toRadians(p2.latitude)))
        }
        return abs(sum) * EARTH_RADIUS_M * EARTH_RADIUS_M / 2
    }

    fun squareMetersToAcres(squareMeters: Double): Double = squareMeters / SQUARE_METERS_PER_ACRE

    /**
     * Ray-casting point-in-polygon test. Works for concave polygons; the ring is implicitly closed.
     * Uses lat/lng as planar coordinates, which is exact enough for polygons far smaller than the
     * planet and does not misbehave away from the equator, as only comparisons are made.
     * Points exactly on an edge are not guaranteed either way; callers should treat GPS
     * accuracy as the real tolerance.
     */
    fun isInside(point: LatLng, ring: List<LatLng>): Boolean {
        if (ring.size < 3) return false
        var inside = false
        var j = ring.lastIndex
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[j]
            val crossesLatitude = (a.latitude > point.latitude) != (b.latitude > point.latitude)
            if (crossesLatitude) {
                val lngAtCrossing = (b.longitude - a.longitude) *
                    (point.latitude - a.latitude) / (b.latitude - a.latitude) + a.longitude
                if (point.longitude < lngAtCrossing) inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Shortest distance in metres from [point] to the polygon boundary, or 0 when inside.
     * Edges are measured in a local flat projection centred on the point, which is accurate
     * for the short distances involved.
     */
    fun distanceToPolygonMeters(point: LatLng, ring: List<LatLng>): Double {
        if (ring.isEmpty()) return Double.POSITIVE_INFINITY
        if (isInside(point, ring)) return 0.0
        if (ring.size == 1) return distanceMeters(point, ring[0])
        val cosLat = cos(Math.toRadians(point.latitude))
        fun x(p: LatLng) = wrapDegrees(p.longitude - point.longitude) * cosLat * METERS_PER_DEGREE
        fun y(p: LatLng) = (p.latitude - point.latitude) * METERS_PER_DEGREE
        return ring.indices.minOf { i ->
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            distanceToSegmentFromOrigin(x(a), y(a), x(b), y(b))
        }
    }

    private fun distanceToSegmentFromOrigin(ax: Double, ay: Double, bx: Double, by: Double): Double {
        val dx = bx - ax
        val dy = by - ay
        val lengthSquared = dx * dx + dy * dy
        if (lengthSquared == 0.0) return hypot(ax, ay)
        val t = max(0.0, min(1.0, -(ax * dx + ay * dy) / lengthSquared))
        return hypot(ax + t * dx, ay + t * dy)
    }

    private fun wrapRadians(angle: Double): Double = when {
        angle > PI -> angle - 2 * PI
        angle < -PI -> angle + 2 * PI
        else -> angle
    }

    private fun wrapDegrees(angle: Double): Double = when {
        angle > 180 -> angle - 360
        angle < -180 -> angle + 360
        else -> angle
    }
}
