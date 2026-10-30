package com.keval.fieldpintask.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keval.fieldpintask.ui.login.LoginScreen
import com.keval.fieldpintask.ui.navigation.AppNavHost

/** The session decides which graph is shown, so logout needs no manual back-stack handling. */
@Composable
fun AppRoot(viewModel: AppViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when (authState) {
        AuthState.Loading -> Unit
        AuthState.SignedOut -> LoginScreen()
        AuthState.SignedIn -> AppNavHost()
    }
}
