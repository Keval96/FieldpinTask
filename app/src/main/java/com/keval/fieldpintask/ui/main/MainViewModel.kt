package com.keval.fieldpintask.ui.main

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.R
import com.keval.fieldpintask.domain.export.GeoJsonExporter
import com.keval.fieldpintask.domain.repository.AuthRepository
import com.keval.fieldpintask.domain.repository.OfflineException
import com.keval.fieldpintask.domain.repository.SiteRepository
import com.keval.fieldpintask.domain.repository.SyncRepository
import com.keval.fieldpintask.domain.repository.SyncSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Message(@StringRes val resId: Int, val args: List<Any> = emptyList())

data class MainUiState(
    val pendingCount: Int = 0,
    val isOnline: Boolean = true,
    val isSyncing: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val siteRepository: SiteRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)
    private val messages = Channel<Message>(Channel.BUFFERED)
    val messageFlow: Flow<Message> = messages.receiveAsFlow()

    private val exports = Channel<String>(Channel.BUFFERED)
    val exportFlow: Flow<String> = exports.receiveAsFlow()

    fun exportSites() {
        viewModelScope.launch {
            val sites = siteRepository.observeSites().first()
            if (sites.isEmpty()) messages.send(Message(R.string.export_empty)) else exports.send(GeoJsonExporter.export(sites))
        }
    }

    val state: StateFlow<MainUiState> = combine(
        syncRepository.pendingCount,
        syncRepository.isOnline,
        syncing,
    ) { pending, online, isSyncing -> MainUiState(pending, online, isSyncing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MainUiState())

    init {
        viewModelScope.launch { siteRepository.seedIfEmpty() }
        // Push queued changes as soon as connectivity returns while the app is open.
        viewModelScope.launch {
            syncRepository.isOnline.distinctUntilChanged().filter { it }.collect {
                if (state.value.pendingCount > 0) runSync(silent = true)
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch { runSync(silent = false) }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    private suspend fun runSync(silent: Boolean) {
        if (syncing.value) return
        syncing.value = true
        val result = syncRepository.sync()
        syncing.value = false
        result.onSuccess { if (!silent) messages.send(it.toMessage()) }
            .onFailure { e ->
                if (!silent) messages.send(Message(if (e is OfflineException) R.string.sync_offline else R.string.sync_failed))
            }
    }

    private fun SyncSummary.toMessage(): Message = when {
        sitesPushed == 0 && inspectionsPushed == 0 -> Message(R.string.sync_nothing)
        conflicts > 0 -> Message(R.string.sync_done_conflicts, listOf(conflicts))
        else -> Message(R.string.sync_done, listOf(sitesPushed, inspectionsPushed))
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
