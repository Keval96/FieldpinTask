package com.keval.fieldpintask.di

import com.keval.fieldpintask.data.location.AdaptiveLocationProvider
import com.keval.fieldpintask.domain.location.LocationProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds abstract fun locationProvider(impl: AdaptiveLocationProvider): LocationProvider
}
