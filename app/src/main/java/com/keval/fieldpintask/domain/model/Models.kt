package com.keval.fieldpintask.domain.model

import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng

enum class SiteStatus { ACTIVE, NEEDS_REPAIR, CLOSED }

enum class Condition { GOOD, FAIR, POOR }

enum class SyncState { SYNCED, PENDING }

data class Site(
    val id: String,
    val name: String,
    val category: String,
    val boundary: List<LatLng>,
    val areaSqM: Double,
    val perimeterM: Double,
    val status: SiteStatus,
    val lastInspectionAt: Long?,
    val version: Int,
    val updatedAt: Long,
    val syncState: SyncState,
) {
    val areaAcres: Double get() = GeoMath.squareMetersToAcres(areaSqM)
    val isPending: Boolean get() = syncState == SyncState.PENDING
}

data class Inspection(
    val id: String,
    val siteId: String,
    val checkInAt: Long,
    val location: LatLng,
    val accuracyM: Float,
    val condition: Condition,
    val notes: String,
    val photoPaths: List<String>,
    val syncState: SyncState,
    val attachmentPaths: List<String> = emptyList(),
)

data class Session(val token: String, val email: String, val displayName: String)

val SITE_CATEGORIES = listOf("Park", "Lake", "Water tank", "Solar installation", "Heritage", "Stadium", "Ground", "Other")
