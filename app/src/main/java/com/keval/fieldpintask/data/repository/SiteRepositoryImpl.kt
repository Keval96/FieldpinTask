package com.keval.fieldpintask.data.repository

import com.keval.fieldpintask.data.local.SiteDao
import com.keval.fieldpintask.data.local.toDomain
import com.keval.fieldpintask.data.local.toEntity
import com.keval.fieldpintask.data.seed.SeedSites
import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.domain.model.SyncState
import com.keval.fieldpintask.domain.repository.SiteRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SiteRepositoryImpl @Inject constructor(private val siteDao: SiteDao) : SiteRepository {

    override fun observeSites(): Flow<List<Site>> = siteDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeSite(id: String): Flow<Site?> = siteDao.observeById(id).map { it?.toDomain() }

    override suspend fun seedIfEmpty() {
        if (siteDao.count() == 0) {
            siteDao.upsertAll(SeedSites.create(System.currentTimeMillis()).map { it.toEntity() })
        }
    }

    override suspend fun createSite(name: String, category: String, boundary: List<LatLng>): Site {
        require(boundary.size >= MIN_POINTS) { "A site needs at least $MIN_POINTS points" }
        val site = Site(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            category = category,
            boundary = boundary,
            areaSqM = GeoMath.areaSquareMeters(boundary),
            perimeterM = GeoMath.perimeterMeters(boundary),
            status = SiteStatus.ACTIVE,
            lastInspectionAt = null,
            version = 0,
            updatedAt = System.currentTimeMillis(),
            syncState = SyncState.PENDING,
        )
        siteDao.upsert(site.toEntity())
        return site
    }

    private companion object {
        const val MIN_POINTS = 3
    }
}
