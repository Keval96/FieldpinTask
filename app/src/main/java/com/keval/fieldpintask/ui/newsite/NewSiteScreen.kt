package com.keval.fieldpintask.ui.newsite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keval.fieldpintask.R
import com.keval.fieldpintask.domain.model.SITE_CATEGORIES
import com.keval.fieldpintask.ui.common.LOCATION_PERMISSIONS
import com.keval.fieldpintask.ui.common.LocationOffCard
import com.keval.fieldpintask.ui.common.PermissionCard
import com.keval.fieldpintask.ui.common.formatArea
import com.keval.fieldpintask.ui.common.rememberLocationServicesEnabled
import com.keval.fieldpintask.ui.common.rememberPermissionRequester
import com.keval.fieldpintask.domain.location.WalkFilter
import com.keval.fieldpintask.ui.map.CameraCommand
import com.keval.fieldpintask.ui.map.CameraTarget
import com.keval.fieldpintask.ui.map.FieldMap
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSiteScreen(onBack: () -> Unit, viewModel: NewSiteViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberPermissionRequester(LOCATION_PERMISSIONS)
    val locationServicesOn = rememberLocationServicesEnabled()
    val canLocate = permission.isGranted && locationServicesOn
    val locationFlow = remember(canLocate) { if (canLocate) viewModel.locationUpdates() else emptyFlow() }
    val location by locationFlow.collectAsStateWithLifecycle(initialValue = null)

    var camera by remember { mutableStateOf<CameraCommand?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Frame the user once on the first fix; after that the map is theirs to pan.
    val hasFix = location != null
    LaunchedEffect(hasFix) { location?.let { camera = CameraCommand(CameraTarget.Center(it.latLng)) } }
    val knownPoints by viewModel.knownPoints.collectAsStateWithLifecycle()
    LaunchedEffect(knownPoints.isNotEmpty()) {
        if (camera == null && knownPoints.isNotEmpty()) camera = CameraCommand(CameraTarget.Fit(knownPoints))
    }
    LaunchedEffect(location) { viewModel.onWalkFix(location) }
    LaunchedEffect(viewModel) { viewModel.saved.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_site_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            FieldMap(
                modifier = Modifier.fillMaxSize(),
                userLocation = location,
                draftPoints = state.points,
                draftValid = !state.crossesItself,
                cameraCommand = camera,
                onMapClick = viewModel::addPoint,
            )
            Column(Modifier.padding(12.dp)) {
                when {
                    !permission.isGranted -> PermissionCard(
                        title = R.string.new_site_permission_title,
                        message = R.string.new_site_permission_message,
                        requester = permission,
                    )
                    !locationServicesOn -> LocationOffCard()
                }
            }
            Surface(
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = androidx.compose.foundation.shape.ZeroCornerSize, bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.new_site_points, state.points.size), style = MaterialTheme.typography.titleMedium)
                    if (state.points.size >= 3) {
                        Text(stringResource(R.string.new_site_area, formatArea(state.areaSqM, state.areaAcres)))
                        Text(stringResource(R.string.new_site_perimeter, state.perimeterM.toLong()))
                    } else {
                        Text(stringResource(R.string.new_site_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    location?.let { Text(stringResource(R.string.new_site_accuracy, it.accuracyM.toInt()), style = MaterialTheme.typography.bodySmall) }
                    FilterChip(
                        selected = state.walking,
                        onClick = viewModel::toggleWalk,
                        enabled = location != null,
                        label = { Text(stringResource(if (state.walking) R.string.walk_mode_on else R.string.walk_mode)) },
                    )
                    val poorFix = location?.let { it.accuracyM > WalkFilter.MAX_ACCURACY_M } == true
                    if (state.walking && poorFix) {
                        Text(stringResource(R.string.walk_poor_fix, WalkFilter.MAX_ACCURACY_M.toInt()), color = MaterialTheme.colorScheme.error)
                    }
                    if (state.crossesItself) {
                        Text(stringResource(R.string.new_site_crossing), color = MaterialTheme.colorScheme.error)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = viewModel::undo, enabled = state.points.isNotEmpty(), modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.undo))
                        }
                        OutlinedButton(
                            onClick = { location?.let { viewModel.addPoint(it.latLng) } },
                            enabled = location != null,
                            modifier = Modifier.weight(1.4f),
                        ) { Text(stringResource(R.string.new_site_add_gps)) }
                        Button(onClick = { showSaveDialog = true }, enabled = state.canSave, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.new_site_save))
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveSiteDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name, category -> showSaveDialog = false; viewModel.save(name, category) },
        )
    }
}

@Composable
private fun SaveSiteDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SITE_CATEGORIES.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_site_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.new_site_name)) },
                    singleLine = true,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SITE_CATEGORIES) { option ->
                        FilterChip(selected = option == category, onClick = { category = option }, label = { Text(option) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, category) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.new_site_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
