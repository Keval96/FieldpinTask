package com.keval.fieldpintask.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class AuthState { Loading, SignedOut, SignedIn }

@HiltViewModel
class AppViewModel @Inject constructor(authRepository: AuthRepository) : ViewModel() {
    val authState: StateFlow<AuthState> = authRepository.session
        .map { if (it == null) AuthState.SignedOut else AuthState.SignedIn }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)
}
