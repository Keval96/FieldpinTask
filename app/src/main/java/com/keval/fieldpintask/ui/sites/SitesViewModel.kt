package com.keval.fieldpintask.ui.sites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.domain.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SitesUiState(
    val sites: List<Site> = emptyList(),
    val query: String = "",
    val statusFilter: SiteStatus? = null,
)

@HiltViewModel
class SitesViewModel @Inject constructor(siteRepository: SiteRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<SiteStatus?>(null)

    val state: StateFlow<SitesUiState> = combine(
        siteRepository.observeSites(),
        query,
        statusFilter,
    ) { sites, q, filter ->
        SitesUiState(
            sites = sites.filter { site ->
                site.name.contains(q.trim(), ignoreCase = true) && (filter == null || site.status == filter)
            },
            query = q,
            statusFilter = filter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SitesUiState())

    fun onQueryChange(value: String) = query.update { value }

    fun onFilterChange(status: SiteStatus?) = statusFilter.update { status }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
