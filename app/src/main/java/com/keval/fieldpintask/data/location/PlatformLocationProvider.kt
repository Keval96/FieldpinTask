package com.keval.fieldpintask.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.keval.fieldpintask.domain.location.DeviceLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** GPS-only fallback for devices without Google Play Services, where Fused Location is unavailable. */
@Singleton
class PlatformLocationProvider @Inject constructor(@ApplicationContext context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission")
    fun updates(): Flow<DeviceLocation> = callbackFlow {
        // All four callbacks are overridden because the last three are abstract below API 30.
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { trySend(location.toDeviceLocation()) }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_MS, 0f, listener, Looper.getMainLooper())
        awaitClose { manager.removeUpdates(listener) }
    }

    private companion object {
        const val INTERVAL_MS = 1_000L
    }
}
