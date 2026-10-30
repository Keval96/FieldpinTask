package com.keval.fieldpintask.data.local

import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Condition
import com.keval.fieldpintask.domain.model.Inspection
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.domain.model.SyncState

private const val POINT_SEPARATOR = ";"
private const val PATH_SEPARATOR = "\n"

internal fun encodeBoundary(boundary: List<LatLng>): String =
    boundary.joinToString(POINT_SEPARATOR) { "${it.latitude},${it.longitude}" }

internal fun decodeBoundary(encoded: String): List<LatLng> =
    if (encoded.isBlank()) emptyList() else encoded.split(POINT_SEPARATOR).map {
        val (lat, lng) = it.split(",")
        LatLng(lat.toDouble(), lng.toDouble())
    }

private fun decodePaths(encoded: String): List<String> = if (encoded.isEmpty()) emptyList() else encoded.split(PATH_SEPARATOR)

internal fun SiteEntity.toDomain() = Site(
    id = id,
    name = name,
    category = category,
    boundary = decodeBoundary(boundary),
    areaSqM = areaSqM,
    perimeterM = perimeterM,
    status = SiteStatus.valueOf(status),
    lastInspectionAt = lastInspectionAt,
    version = version,
    updatedAt = updatedAt,
    syncState = SyncState.valueOf(syncState),
    caretakerName = caretakerName,
    caretakerPhone = caretakerPhone,
)

internal fun Site.toEntity() = SiteEntity(
    id = id,
    name = name,
    category = category,
    boundary = encodeBoundary(boundary),
    areaSqM = areaSqM,
    perimeterM = perimeterM,
    status = status.name,
    lastInspectionAt = lastInspectionAt,
    version = version,
    updatedAt = updatedAt,
    syncState = syncState.name,
    caretakerName = caretakerName,
    caretakerPhone = caretakerPhone,
)

internal fun InspectionEntity.toDomain() = Inspection(
    id = id,
    siteId = siteId,
    checkInAt = checkInAt,
    location = LatLng(latitude, longitude),
    accuracyM = accuracyM,
    condition = Condition.valueOf(condition),
    notes = notes,
    photoPaths = decodePaths(photoPaths),
    syncState = SyncState.valueOf(syncState),
    attachmentPaths = decodePaths(attachmentPaths),
)

internal fun Inspection.toEntity() = InspectionEntity(
    id = id,
    siteId = siteId,
    checkInAt = checkInAt,
    latitude = location.latitude,
    longitude = location.longitude,
    accuracyM = accuracyM,
    condition = condition.name,
    notes = notes,
    photoPaths = photoPaths.joinToString(PATH_SEPARATOR),
    syncState = syncState.name,
    attachmentPaths = attachmentPaths.joinToString(PATH_SEPARATOR),
)
