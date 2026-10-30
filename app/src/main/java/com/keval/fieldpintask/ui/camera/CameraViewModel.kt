package com.keval.fieldpintask.ui.camera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.data.PhotoStore
import com.keval.fieldpintask.data.camera.PhotoStamper
import com.keval.fieldpintask.data.camera.StampText
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.location.LocationProvider
import com.keval.fieldpintask.domain.repository.SiteRepository
import com.keval.fieldpintask.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CameraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    siteRepository: SiteRepository,
    private val locationProvider: LocationProvider,
    private val photoStore: PhotoStore,
    private val stamper: PhotoStamper,
) : ViewModel() {
    private val siteId: String = checkNotNull(savedStateHandle[Routes.ARG_SITE_ID])

    val siteName: StateFlow<String> = siteRepository.observeSite(siteId)
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    fun locationUpdates(): Flow<DeviceLocation> = locationProvider.updates()

    fun newPhotoFile(): File = photoStore.newPhotoFile()

    suspend fun stamp(file: File, location: DeviceLocation) {
        val lines = StampText.lines(siteName.value, location.latLng, location.accuracyM, System.currentTimeMillis())
        stamper.stamp(file, lines)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
