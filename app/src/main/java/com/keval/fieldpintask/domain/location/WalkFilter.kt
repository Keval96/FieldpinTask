package com.keval.fieldpintask.domain.location

import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng

/** Decides which GPS fixes become boundary corners while walking a site. */
object WalkFilter {
    const val MAX_ACCURACY_M = 20f
    const val MIN_SPACING_M = 5.0

    fun accept(fix: DeviceLocation, last: LatLng?): Boolean {
        if (fix.accuracyM > MAX_ACCURACY_M) return false
        return last == null || GeoMath.distanceMeters(last, fix.latLng) >= MIN_SPACING_M
    }
}
