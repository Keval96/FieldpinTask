package com.keval.fieldpintask.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val boundary: String,
    val areaSqM: Double,
    val perimeterM: Double,
    val status: String,
    val lastInspectionAt: Long?,
    val version: Int,
    val updatedAt: Long,
    val syncState: String,
)

@Entity(tableName = "inspections", indices = [Index("siteId")])
data class InspectionEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val checkInAt: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val condition: String,
    val notes: String,
    val photoPaths: String,
    val syncState: String,
)
