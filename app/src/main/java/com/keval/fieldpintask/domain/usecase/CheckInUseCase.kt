package com.keval.fieldpintask.domain.usecase

import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Site
import javax.inject.Inject

sealed interface CheckInResult {
    data object Allowed : CheckInResult
    data class Outside(val distanceM: Double) : CheckInResult
}

/** Check-in is allowed only when the device is inside the site boundary. */
class CheckInUseCase @Inject constructor() {
    operator fun invoke(site: Site, location: LatLng): CheckInResult =
        if (GeoMath.isInside(location, site.boundary)) {
            CheckInResult.Allowed
        } else {
            CheckInResult.Outside(GeoMath.distanceToPolygonMeters(location, site.boundary))
        }
}
