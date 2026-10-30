package com.keval.fieldpintask.ui.inspection

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.data.AttachmentStore
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.location.LocationProvider
import com.keval.fieldpintask.domain.model.Condition
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.domain.repository.InspectionRepository
import com.keval.fieldpintask.domain.repository.NewInspection
import com.keval.fieldpintask.domain.repository.SiteRepository
import com.keval.fieldpintask.domain.usecase.CheckInResult
import com.keval.fieldpintask.domain.usecase.CheckInUseCase
import com.keval.fieldpintask.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CheckInState {
    data object Locating : CheckInState
    data class Outside(val distanceM: Double) : CheckInState
    data class CheckedIn(val atMillis: Long, val location: LatLng, val accuracyM: Float) : CheckInState
}

data class InspectionUiState(
    val site: Site? = null,
    val checkIn: CheckInState = CheckInState.Locating,
    val condition: Condition = Condition.FAIR,
    val status: SiteStatus = SiteStatus.ACTIVE,
    val notes: String = "",
    val photos: List<String> = emptyList(),
    val attachments: List<String> = emptyList(),
    val isSubmitting: Boolean = false,
) {
    val canSubmit: Boolean get() = site != null && checkIn is CheckInState.CheckedIn && !isSubmitting
}

@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val siteRepository: SiteRepository,
    private val inspectionRepository: InspectionRepository,
    private val locationProvider: LocationProvider,
    private val checkInUseCase: CheckInUseCase,
    private val attachmentStore: AttachmentStore,
) : ViewModel() {
    val siteId: String = checkNotNull(savedStateHandle[Routes.ARG_SITE_ID])

    private val _state = MutableStateFlow(InspectionUiState())
    val state: StateFlow<InspectionUiState> = _state.asStateFlow()

    private val submittedEvents = Channel<Unit>(Channel.BUFFERED)
    val submitted: Flow<Unit> = submittedEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            val site = siteRepository.observeSite(siteId).filterNotNull().first()
            _state.update { it.copy(site = site, status = site.status) }
        }
    }

    fun addAttachment(uri: Uri) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { attachmentStore.import(uri) } ?: return@launch
            _state.update { it.copy(attachments = it.attachments + path) }
        }
    }

    fun removeAttachment(path: String) = _state.update { it.copy(attachments = it.attachments - path) }

    fun addPhoto(path: String) = _state.update { it.copy(photos = it.photos + path) }

    fun locationUpdates(): Flow<DeviceLocation> = locationProvider.updates()

    /** Check-in is decided once; later GPS drift inside or outside does not undo it. */
    fun onLocation(location: DeviceLocation?) {
        val current = _state.value
        val site = current.site ?: return
        if (location == null || current.checkIn is CheckInState.CheckedIn) return
        val next = when (val result = checkInUseCase(site, location.latLng)) {
            CheckInResult.Allowed ->
                CheckInState.CheckedIn(System.currentTimeMillis(), location.latLng, location.accuracyM)
            is CheckInResult.Outside -> CheckInState.Outside(result.distanceM)
        }
        _state.update { it.copy(checkIn = next) }
    }

    fun onConditionChange(value: Condition) = _state.update { it.copy(condition = value) }

    fun onStatusChange(value: SiteStatus) = _state.update { it.copy(status = value) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value) }

    fun removePhoto(path: String) = _state.update { it.copy(photos = it.photos - path) }

    fun submit() {
        val current = _state.value
        val checkIn = current.checkIn as? CheckInState.CheckedIn ?: return
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            inspectionRepository.submit(
                NewInspection(
                    siteId = siteId,
                    checkInAt = checkIn.atMillis,
                    location = checkIn.location,
                    accuracyM = checkIn.accuracyM,
                    condition = current.condition,
                    notes = current.notes,
                    photoPaths = current.photos,
                    attachmentPaths = current.attachments,
                    siteStatus = current.status,
                ),
            )
            submittedEvents.send(Unit)
        }
    }
}
