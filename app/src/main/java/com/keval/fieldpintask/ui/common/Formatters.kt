package com.keval.fieldpintask.ui.common

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

fun formatArea(squareMeters: Double, acres: Double): String {
    val sqm = NumberFormat.getIntegerInstance().format(squareMeters.toLong())
    return "$sqm m² (${String.format(Locale.getDefault(), "%.2f", acres)} ac)"
}

fun formatDistance(meters: Double): String =
    if (meters < 1_000) "${meters.toLong()} m" else String.format(Locale.getDefault(), "%.1f km", meters / 1_000)

fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

fun formatTime(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))
