package com.keval.fieldpintask.data.location

import android.location.Location
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.location.DeviceLocation

internal fun Location.toDeviceLocation() = DeviceLocation(LatLng(latitude, longitude), accuracy, time)
