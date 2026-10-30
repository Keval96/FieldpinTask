package com.keval.fieldpintask.di

import com.keval.fieldpintask.data.repository.AuthRepositoryImpl
import com.keval.fieldpintask.data.repository.InspectionRepositoryImpl
import com.keval.fieldpintask.data.repository.SiteRepositoryImpl
import com.keval.fieldpintask.data.repository.SyncRepositoryImpl
import com.keval.fieldpintask.domain.repository.AuthRepository
import com.keval.fieldpintask.domain.repository.InspectionRepository
import com.keval.fieldpintask.domain.repository.SiteRepository
import com.keval.fieldpintask.domain.repository.SyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun auth(impl: AuthRepositoryImpl): AuthRepository
    @Binds abstract fun sites(impl: SiteRepositoryImpl): SiteRepository
    @Binds abstract fun inspections(impl: InspectionRepositoryImpl): InspectionRepository
    @Binds abstract fun sync(impl: SyncRepositoryImpl): SyncRepository
}
