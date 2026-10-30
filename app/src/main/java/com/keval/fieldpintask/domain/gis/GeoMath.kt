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

/** Geodesic helpers on a sphere (mean radius), not flat x/y. */
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

    /** Spherical polygon area (Chamberlain-Duquette), any winding order; 0 below 3 points. */
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

    /** Ray-casting point-in-polygon, safe for concave rings. Points exactly on an edge are
     *  undefined, so GPS accuracy is the real tolerance. */
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

    /** Metres from [point] to the nearest edge, 0 when inside. Uses a local flat projection
     *  centred on the point, fine at short range. */
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

    /** True when any two non-adjacent edges of the closed ring cross or touch. */
    fun isSelfIntersecting(ring: List<LatLng>): Boolean {
        val n = ring.size
        if (n < 4) return false
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val adjacent = j == i + 1 || (i == 0 && j == n - 1)
                if (!adjacent && segmentsIntersect(ring[i], ring[(i + 1) % n], ring[j], ring[(j + 1) % n])) return true
            }
        }
        return false
    }

    private fun segmentsIntersect(a: LatLng, b: LatLng, c: LatLng, d: LatLng): Boolean {
        val d1 = cross(c, d, a)
        val d2 = cross(c, d, b)
        val d3 = cross(a, b, c)
        val d4 = cross(a, b, d)
        if (d1 * d2 < 0 && d3 * d4 < 0) return true
        return (d1 == 0.0 && onSegment(c, d, a)) || (d2 == 0.0 && onSegment(c, d, b)) ||
            (d3 == 0.0 && onSegment(a, b, c)) || (d4 == 0.0 && onSegment(a, b, d))
    }

    private fun cross(p: LatLng, q: LatLng, r: LatLng): Double =
        (q.longitude - p.longitude) * (r.latitude - p.latitude) - (q.latitude - p.latitude) * (r.longitude - p.longitude)

    private fun onSegment(p: LatLng, q: LatLng, r: LatLng): Boolean =
        r.longitude in min(p.longitude, q.longitude)..max(p.longitude, q.longitude) &&
            r.latitude in min(p.latitude, q.latitude)..max(p.latitude, q.latitude)

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
