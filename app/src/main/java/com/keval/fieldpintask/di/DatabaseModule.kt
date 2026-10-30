package com.keval.fieldpintask.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.keval.fieldpintask.data.local.FieldPinDatabase
import com.keval.fieldpintask.data.local.InspectionDao
import com.keval.fieldpintask.data.local.MIGRATION_1_2
import com.keval.fieldpintask.data.local.SiteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): FieldPinDatabase =
        Room.databaseBuilder(context, FieldPinDatabase::class.java, "fieldpin.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun siteDao(database: FieldPinDatabase): SiteDao = database.siteDao()

    @Provides
    fun inspectionDao(database: FieldPinDatabase): InspectionDao = database.inspectionDao()

    @Provides
    @Singleton
    fun sessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("session") }
}
