package com.keval.fieldpintask.data.location

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.location.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Fused Location whenever Google Play Services is present, platform GPS otherwise. */
@Singleton
class AdaptiveLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fused: FusedLocationProvider,
    private val platform: PlatformLocationProvider,
) : LocationProvider {

    override fun updates(): Flow<DeviceLocation> {
        val hasPlayServices = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        return if (hasPlayServices) fused.updates() else platform.updates()
    }
}
