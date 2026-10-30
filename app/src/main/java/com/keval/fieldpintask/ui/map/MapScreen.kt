package com.keval.fieldpintask.ui.map

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keval.fieldpintask.R
import com.keval.fieldpintask.domain.gis.GeoMath
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.ui.common.LOCATION_PERMISSIONS
import com.keval.fieldpintask.ui.common.LocationOffCard
import com.keval.fieldpintask.ui.common.PermissionCard
import com.keval.fieldpintask.ui.common.color
import com.keval.fieldpintask.ui.common.contactPickerIntent
import com.keval.fieldpintask.ui.common.dialIntent
import com.keval.fieldpintask.ui.common.formatArea
import com.keval.fieldpintask.ui.common.formatDistance
import com.keval.fieldpintask.ui.common.label
import com.keval.fieldpintask.ui.common.readPickedContact
import com.keval.fieldpintask.ui.common.rememberLocationServicesEnabled
import com.keval.fieldpintask.ui.common.rememberPermissionRequester
import com.keval.fieldpintask.ui.sites.PendingBadge
import kotlinx.coroutines.flow.emptyFlow

private val SHEET_PEEK_HEIGHT = 300.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onStartInspection: (String) -> Unit, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberPermissionRequester(LOCATION_PERMISSIONS)
    val locationServicesOn = rememberLocationServicesEnabled()

    // Collecting with the lifecycle stops GPS updates whenever the screen is not visible.
    val canLocate = permission.isGranted && locationServicesOn
    val locationFlow = remember(canLocate) { if (canLocate) viewModel.locationUpdates() else emptyFlow() }
    val userLocation by locationFlow.collectAsStateWithLifecycle(initialValue = null)

    var camera by remember { mutableStateOf<CameraCommand?>(null) }
    LaunchedEffect(state.sites.isNotEmpty()) {
        if (state.sites.isEmpty() || camera != null) return@LaunchedEffect
        val focus = state.sites.firstOrNull { it.id == viewModel.initialSiteId }
        camera = CameraCommand(CameraTarget.Fit((focus?.boundary ?: state.sites.flatMap { it.boundary })))
    }

    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val selected = state.selected

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (selected != null) SHEET_PEEK_HEIGHT else 0.dp,
        sheetSwipeEnabled = selected != null,
        sheetContent = {
            if (selected != null) {
                SiteSheet(
                    site = selected,
                    location = userLocation,
                    onClose = viewModel::clearSelection,
                    onStartInspection = { onStartInspection(selected.id) },
                    onCaretakerPicked = { name, phone -> viewModel.setCaretaker(selected.id, name, phone) },
                )
            } else {
                Box(Modifier.height(1.dp))
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            FieldMap(
                modifier = Modifier.fillMaxSize(),
                sites = state.sites,
                selectedSiteId = selected?.id,
                userLocation = userLocation,
                cameraCommand = camera,
                onMapClick = viewModel::onMapTapped,
            )
            Column(Modifier.padding(inner).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !permission.isGranted -> PermissionCard(
                        title = R.string.map_permission_title,
                        message = R.string.map_permission_message,
                        requester = permission,
                    )
                    !locationServicesOn -> LocationOffCard()
                }
            }
            FloatingActionButton(
                onClick = {
                    val here = userLocation
                    if (here != null) camera = CameraCommand(CameraTarget.Center(here.latLng))
                    else if (!permission.isGranted) permission.requestOrOpenSettings()
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(inner).padding(16.dp),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = stringResource(R.string.my_location))
            }
        }
    }
}

@Composable
private fun SiteSheet(
    site: Site,
    location: DeviceLocation?,
    onClose: () -> Unit,
    onStartInspection: () -> Unit,
    onCaretakerPicked: (String, String) -> Unit,
) {
    val context = LocalContext.current
    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.data?.let { uri -> readPickedContact(context, uri)?.let { onCaretakerPicked(it.name, it.phone) } }
    }
    val distance = location?.let { GeoMath.distanceToPolygonMeters(it.latLng, site.boundary) }
    val isInside = distance == 0.0
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(site.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${site.category} · ${stringResource(site.status.label)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = site.status.color,
                )
            }
            if (site.isPending) PendingBadge()
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close)) }
        }
        Text(stringResource(R.string.sheet_area, formatArea(site.areaSqM, site.areaAcres)))
        Text(
            when {
                distance == null -> stringResource(R.string.sheet_distance_unknown)
                isInside -> stringResource(R.string.sheet_inside)
                else -> stringResource(R.string.sheet_distance, formatDistance(distance))
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                site.caretakerName?.let { stringResource(R.string.caretaker_value, it, site.caretakerPhone.orEmpty()) }
                    ?: stringResource(R.string.caretaker_none),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!site.caretakerPhone.isNullOrBlank()) {
                TextButton(onClick = { context.startActivity(dialIntent(site.caretakerPhone)) }) { Text(stringResource(R.string.caretaker_call)) }
            }
            TextButton(onClick = { contactPicker.launch(contactPickerIntent()) }) {
                Text(stringResource(if (site.caretakerName == null) R.string.caretaker_set else R.string.caretaker_change))
            }
        }
        Button(onClick = onStartInspection, enabled = isInside, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(stringResource(R.string.start_inspection))
        }
        if (!isInside) {
            Text(
                stringResource(R.string.checkin_disabled_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
