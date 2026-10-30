package com.keval.fieldpintask.data.repository

import androidx.room.withTransaction
import com.keval.fieldpintask.data.local.FieldPinDatabase
import com.keval.fieldpintask.data.remote.FieldPinApi
import com.keval.fieldpintask.data.remote.SyncRequest
import com.keval.fieldpintask.data.remote.toDto
import com.keval.fieldpintask.data.remote.toEntity
import com.keval.fieldpintask.data.sync.NetworkMonitor
import com.keval.fieldpintask.domain.repository.OfflineException
import com.keval.fieldpintask.domain.repository.SyncRepository
import com.keval.fieldpintask.domain.repository.SyncSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val api: FieldPinApi,
    private val database: FieldPinDatabase,
    private val networkMonitor: NetworkMonitor,
) : SyncRepository {
    private val mutex = Mutex()

    override val pendingCount: Flow<Int> = combine(
        database.siteDao().observePendingCount(),
        database.inspectionDao().observePendingCount(),
    ) { sites, inspections -> sites + inspections }

    override val isOnline: Flow<Boolean> = networkMonitor.online

    override suspend fun sync(): Result<SyncSummary> = mutex.withLock {
        if (!networkMonitor.isOnline()) return Result.failure(OfflineException())
        try {
            Result.success(push())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun push(): SyncSummary {
        val pendingSites = database.siteDao().pending()
        val pendingInspections = database.inspectionDao().pending()
        val sentAt = pendingSites.associate { it.id to it.updatedAt }

        val response = api.sync(
            SyncRequest(pendingSites.map { it.toDto() }, pendingInspections.map { it.toDto() }),
        )

        database.withTransaction {
            database.inspectionDao().markSynced(response.acceptedInspectionIds)
            // A row edited while the request was in flight stays pending and goes out next time.
            val merged = response.sites.filter { server ->
                val local = database.siteDao().getById(server.id)
                local == null || sentAt[server.id] == local.updatedAt || local.syncState != "PENDING"
            }
            database.siteDao().upsertAll(merged.map { it.toEntity() })
        }
        return SyncSummary(pendingSites.size, pendingInspections.size, response.conflictedSiteIds.size)
    }
}
