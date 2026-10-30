package com.keval.fieldpintask.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v2 adds the files an inspector attaches to an inspection. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inspections ADD COLUMN attachmentPaths TEXT NOT NULL DEFAULT ''")
    }
}
