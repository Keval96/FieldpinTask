package com.keval.fieldpintask.ui.newsite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.location.LocationProvider
import com.keval.fieldpintask.domain.location.WalkFilter
import com.keval.fieldpintask.domain.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewSiteUiState(
    val points: List<LatLng> = emptyList(),
    val areaSqM: Double = 0.0,
    val areaAcres: Double = 0.0,
    val perimeterM: Double = 0.0,
    val crossesItself: Boolean = false,
    val isSaving: Boolean = false,
    val walking: Boolean = false,
) {
    val canSave: Boolean get() = points.size >= MIN_POINTS && !crossesItself && !isSaving
}

private const val MIN_POINTS = 3

@HiltViewModel
class NewSiteViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(NewSiteUiState())
    val state: StateFlow<NewSiteUiState> = _state.asStateFlow()

    private val savedEvents = Channel<Unit>(Channel.BUFFERED)
    val saved: Flow<Unit> = savedEvents.receiveAsFlow()

    /** Existing boundaries, used to frame the map when there is no GPS fix yet. */
    val knownPoints: StateFlow<List<LatLng>> = siteRepository.observeSites()
        .map { sites -> sites.flatMap { it.boundary } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun locationUpdates(): Flow<DeviceLocation> = locationProvider.updates()

    fun addPoint(point: LatLng) = update { it + point }

    fun toggleWalk() = _state.update { it.copy(walking = !it.walking) }

    /** In walk mode every good-enough fix becomes a corner; poor or too-close fixes are dropped. */
    fun onWalkFix(fix: DeviceLocation?) {
        val current = _state.value
        if (fix == null || !current.walking) return
        if (WalkFilter.accept(fix, current.points.lastOrNull())) addPoint(fix.latLng)
    }

    fun undo() = update { it.dropLast(1) }

    fun clear() = update { emptyList() }

    fun save(name: String, category: String) {
        val current = _state.value
        if (!current.canSave || name.isBlank()) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            siteRepository.createSite(name, category, current.points)
            savedEvents.send(Unit)
        }
    }

    private fun update(transform: (List<LatLng>) -> List<LatLng>) {
        _state.update { current ->
            val points = transform(current.points)
            val area = GeoMath.areaSquareMeters(points)
            current.copy(
                points = points,
                areaSqM = area,
                areaAcres = GeoMath.squareMetersToAcres(area),
                perimeterM = GeoMath.perimeterMeters(points),
                crossesItself = GeoMath.isSelfIntersecting(points),
            )
        }
    }
}
