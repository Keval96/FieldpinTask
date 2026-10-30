package com.keval.fieldpintask.domain.gis

/** A WGS84 coordinate in decimal degrees. */
data class LatLng(val latitude: Double, val longitude: Double) {
    init {
        require(latitude in -90.0..90.0) { "Latitude out of range: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude out of range: $longitude" }
    }
}
