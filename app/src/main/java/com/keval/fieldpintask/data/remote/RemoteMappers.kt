package com.keval.fieldpintask.data.remote

import com.keval.fieldpintask.data.local.InspectionEntity
import com.keval.fieldpintask.data.local.SiteEntity
import com.keval.fieldpintask.data.local.decodeBoundary
import com.keval.fieldpintask.data.local.encodeBoundary
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SyncState

internal fun Site.toDto() = SiteDto(
    id = id,
    name = name,
    category = category,
    boundary = boundary.map { PointDto(it.latitude, it.longitude) },
    areaSqM = areaSqM,
    perimeterM = perimeterM,
    status = status.name,
    lastInspectionAt = lastInspectionAt,
    version = version,
    updatedAt = updatedAt,
)

internal fun SiteEntity.toDto() = SiteDto(
    id = id,
    name = name,
    category = category,
    boundary = decodeBoundary(boundary).map { PointDto(it.latitude, it.longitude) },
    areaSqM = areaSqM,
    perimeterM = perimeterM,
    status = status,
    lastInspectionAt = lastInspectionAt,
    version = version,
    updatedAt = updatedAt,
)

internal fun SiteDto.toEntity() = SiteEntity(
    id = id,
    name = name,
    category = category,
    boundary = encodeBoundary(boundary.map { LatLng(it.lat, it.lng) }),
    areaSqM = areaSqM,
    perimeterM = perimeterM,
    status = status,
    lastInspectionAt = lastInspectionAt,
    version = version,
    updatedAt = updatedAt,
    syncState = SyncState.SYNCED.name,
)

internal fun InspectionEntity.toDto() = InspectionDto(
    id = id,
    siteId = siteId,
    checkInAt = checkInAt,
    latitude = latitude,
    longitude = longitude,
    accuracyM = accuracyM,
    condition = condition,
    notes = notes,
    photoCount = if (photoPaths.isEmpty()) 0 else photoPaths.split("\n").size,
)
