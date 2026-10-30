package com.keval.fieldpintask.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SiteEntity::class, InspectionEntity::class], version = 3, exportSchema = true)
abstract class FieldPinDatabase : RoomDatabase() {
    abstract fun siteDao(): SiteDao
    abstract fun inspectionDao(): InspectionDao
}
