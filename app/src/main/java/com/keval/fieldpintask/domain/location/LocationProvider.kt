package com.keval.fieldpintask.domain.location

import com.keval.fieldpintask.domain.gis.LatLng
import kotlinx.coroutines.flow.Flow

data class DeviceLocation(val latLng: LatLng, val accuracyM: Float, val timeMillis: Long)

interface LocationProvider {
    /** Cold flow: updates start on collect and stop on cancel, so collectors tie it to the screen lifecycle. */
    fun updates(): Flow<DeviceLocation>
}
