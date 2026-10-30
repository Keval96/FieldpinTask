package com.keval.fieldpintask.data.camera

import com.keval.fieldpintask.domain.gis.LatLng
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** The lines burned into every inspection photo. */
object StampText {

    fun lines(
        siteName: String,
        location: LatLng,
        accuracyM: Float,
        timeMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<String> {
        val lat = hemisphere(location.latitude, "N", "S")
        val lng = hemisphere(location.longitude, "E", "W")
        val date = SimpleDateFormat("dd MMM yyyy, HH:mm z", Locale.ENGLISH).apply { this.timeZone = timeZone }
        return listOf(
            siteName,
            "$lat, $lng ±${Math.round(accuracyM)} m",
            date.format(timeMillis),
        )
    }

    private fun hemisphere(value: Double, positive: String, negative: String): String =
        String.format(Locale.ENGLISH, "%.5f° %s", Math.abs(value), if (value >= 0) positive else negative)
}
