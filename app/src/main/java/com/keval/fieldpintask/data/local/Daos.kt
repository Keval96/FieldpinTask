package com.keval.fieldpintask.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    fun observeById(id: String): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getById(id: String): SiteEntity?

    @Query("SELECT COUNT(*) FROM sites")
    suspend fun count(): Int

    @Query("SELECT * FROM sites WHERE syncState = 'PENDING'")
    suspend fun pending(): List<SiteEntity>

    @Query("SELECT COUNT(*) FROM sites WHERE syncState = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Upsert
    suspend fun upsert(site: SiteEntity)

    @Upsert
    suspend fun upsertAll(sites: List<SiteEntity>)

    @Query("DELETE FROM sites")
    suspend fun clear()
}

@Dao
interface InspectionDao {
    @Query("SELECT * FROM inspections WHERE siteId = :siteId ORDER BY checkInAt DESC")
    fun observeForSite(siteId: String): Flow<List<InspectionEntity>>

    @Query("SELECT * FROM inspections WHERE syncState = 'PENDING'")
    suspend fun pending(): List<InspectionEntity>

    @Query("SELECT COUNT(*) FROM inspections WHERE syncState = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inspection: InspectionEntity)

    @Query("UPDATE inspections SET syncState = 'SYNCED' WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("DELETE FROM inspections")
    suspend fun clear()
}
