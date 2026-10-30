package com.keval.fieldpintask.ui.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.location.LocationProvider
import com.keval.fieldpintask.domain.repository.SiteRepository
import com.keval.fieldpintask.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MapUiState(val sites: List<Site> = emptyList(), val selected: Site? = null)

@HiltViewModel
class MapViewModel @Inject constructor(
    siteRepository: SiteRepository,
    private val locationProvider: LocationProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** Site passed in from the list, used once to frame the camera. */
    val initialSiteId: String? = savedStateHandle[Routes.ARG_SITE_ID]

    private val selectedId = MutableStateFlow(initialSiteId)

    val state: StateFlow<MapUiState> = combine(siteRepository.observeSites(), selectedId) { sites, id ->
        MapUiState(sites, sites.firstOrNull { it.id == id })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MapUiState())

    fun locationUpdates(): Flow<DeviceLocation> = locationProvider.updates()

    fun onMapTapped(point: LatLng) {
        // Smallest polygon wins so a site nested inside a bigger one stays tappable.
        val hit = state.value.sites.filter { GeoMath.isInside(point, it.boundary) }.minByOrNull { it.areaSqM }
        selectedId.update { hit?.id }
    }

    fun clearSelection() = selectedId.update { null }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
