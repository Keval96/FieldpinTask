package com.keval.fieldpintask.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keval.fieldpintask.R
import com.keval.fieldpintask.ui.common.shareGeoJson

enum class MainTab { Sites, Map }

/** Shared chrome for the two top-level tabs: title bar with sync and sign-out, plus bottom navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messageFlow.collect { message ->
            snackbarHostState.showSnackbar(context.getString(message.resId, *message.args.toTypedArray()))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.exportFlow.collect { shareGeoJson(context, it, context.getString(R.string.export_title)) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (!state.isOnline) {
                        Icon(
                            Icons.Filled.CloudOff,
                            contentDescription = stringResource(R.string.offline),
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    IconButton(onClick = viewModel::exportSites) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.action_export))
                    }
                    IconButton(onClick = viewModel::syncNow, enabled = !state.isSyncing) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            BadgedBox(badge = { if (state.pendingCount > 0) Badge { Text(state.pendingCount.toString()) } }) {
                                Icon(Icons.Filled.Sync, contentDescription = stringResource(R.string.action_sync))
                            }
                        }
                    }
                    IconButton(onClick = { confirmLogout = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.action_logout))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selected == MainTab.Sites,
                    onClick = { onTabSelected(MainTab.Sites) },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_sites)) },
                )
                NavigationBarItem(
                    selected = selected == MainTab.Map,
                    onClick = { onTabSelected(MainTab.Map) },
                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) },
                )
            }
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.logout_title)) },
            text = { Text(stringResource(R.string.logout_message)) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; viewModel.logout() }) {
                    Text(stringResource(R.string.logout_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
