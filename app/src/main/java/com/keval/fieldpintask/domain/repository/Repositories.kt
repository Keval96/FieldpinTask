package com.keval.fieldpintask.domain.repository

import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Condition
import com.keval.fieldpintask.domain.model.Inspection
import com.keval.fieldpintask.domain.model.Session
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SiteStatus
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val session: Flow<Session?>
    suspend fun login(email: String, password: String): Result<Session>
    suspend fun logout()
}

interface SiteRepository {
    fun observeSites(): Flow<List<Site>>
    fun observeSite(id: String): Flow<Site?>
    suspend fun seedIfEmpty()
    suspend fun createSite(name: String, category: String, boundary: List<LatLng>): Site
}

data class NewInspection(
    val siteId: String,
    val checkInAt: Long,
    val location: LatLng,
    val accuracyM: Float,
    val condition: Condition,
    val notes: String,
    val photoPaths: List<String>,
    val siteStatus: SiteStatus,
)

interface InspectionRepository {
    fun observeForSite(siteId: String): Flow<List<Inspection>>
    suspend fun submit(inspection: NewInspection)
}

data class SyncSummary(val sitesPushed: Int, val inspectionsPushed: Int, val conflicts: Int)

class OfflineException : Exception("No network connection")

interface SyncRepository {
    val pendingCount: Flow<Int>
    val isOnline: Flow<Boolean>
    suspend fun sync(): Result<SyncSummary>
}
