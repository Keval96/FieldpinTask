package com.keval.fieldpintask.data.repository

import androidx.room.withTransaction
import com.keval.fieldpintask.data.local.FieldPinDatabase
import com.keval.fieldpintask.data.local.toDomain
import com.keval.fieldpintask.data.local.toEntity
import com.keval.fieldpintask.domain.model.Inspection
import com.keval.fieldpintask.domain.model.SyncState
import com.keval.fieldpintask.domain.repository.InspectionRepository
import com.keval.fieldpintask.domain.repository.NewInspection
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class InspectionRepositoryImpl @Inject constructor(
    private val database: FieldPinDatabase,
) : InspectionRepository {

    override fun observeForSite(siteId: String): Flow<List<Inspection>> =
        database.inspectionDao().observeForSite(siteId).map { rows -> rows.map { it.toDomain() } }

    // The inspection and the site edit land together so a crash never leaves them out of step.
    override suspend fun submit(inspection: NewInspection) {
        database.withTransaction {
            database.inspectionDao().insert(
                Inspection(
                    id = UUID.randomUUID().toString(),
                    siteId = inspection.siteId,
                    checkInAt = inspection.checkInAt,
                    location = inspection.location,
                    accuracyM = inspection.accuracyM,
                    condition = inspection.condition,
                    notes = inspection.notes.trim(),
                    photoPaths = inspection.photoPaths,
                    syncState = SyncState.PENDING,
                ).toEntity(),
            )
            val site = database.siteDao().getById(inspection.siteId) ?: return@withTransaction
            database.siteDao().upsert(
                site.copy(
                    lastInspectionAt = inspection.checkInAt,
                    status = inspection.siteStatus.name,
                    updatedAt = System.currentTimeMillis(),
                    syncState = SyncState.PENDING.name,
                ),
            )
        }
    }
}
