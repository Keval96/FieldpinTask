package com.keval.fieldpintask.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val email: String, val displayName: String)

@Serializable
data class PointDto(val lat: Double, val lng: Double)

/** [version] is the server version the client last saw when sent, and the new one when received. */
@Serializable
data class SiteDto(
    val id: String,
    val name: String,
    val category: String,
    val boundary: List<PointDto>,
    val areaSqM: Double,
    val perimeterM: Double,
    val status: String,
    val lastInspectionAt: Long?,
    val version: Int,
    val updatedAt: Long,
)

@Serializable
data class InspectionDto(
    val id: String,
    val siteId: String,
    val checkInAt: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val condition: String,
    val notes: String,
    val photoCount: Int,
)

@Serializable
data class SyncRequest(val sites: List<SiteDto>, val inspections: List<InspectionDto>)

@Serializable
data class SyncResponse(
    val sites: List<SiteDto>,
    val conflictedSiteIds: List<String>,
    val acceptedInspectionIds: List<String>,
)
