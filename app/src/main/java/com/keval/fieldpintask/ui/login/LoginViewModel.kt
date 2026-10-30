package com.keval.fieldpintask.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keval.fieldpintask.BuildConfig
import com.keval.fieldpintask.R
import com.keval.fieldpintask.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @StringRes val error: Int? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun useDemoAccount() = _state.update {
        it.copy(email = BuildConfig.DEMO_EMAIL, password = BuildConfig.DEMO_PASSWORD, error = null)
    }

    fun signIn() {
        val current = _state.value
        if (current.isLoading) return
        if (current.email.isBlank() || current.password.isBlank()) {
            _state.update { it.copy(error = R.string.login_error_empty) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.login(current.email, current.password)
                .onFailure { e ->
                    val message = if (e is HttpException && e.code() == HTTP_UNAUTHORIZED) {
                        R.string.login_error_invalid
                    } else {
                        R.string.login_error_generic
                    }
                    _state.update { it.copy(isLoading = false, error = message) }
                }
            // On success the session flow flips AppRoot to the signed-in graph.
        }
    }

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
