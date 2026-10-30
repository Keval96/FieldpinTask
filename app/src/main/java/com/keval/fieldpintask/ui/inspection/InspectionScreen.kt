package com.keval.fieldpintask.ui.inspection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keval.fieldpintask.R
import com.keval.fieldpintask.domain.model.Condition
import com.keval.fieldpintask.domain.model.SiteStatus
import com.keval.fieldpintask.ui.common.LOCATION_PERMISSIONS
import com.keval.fieldpintask.ui.common.LocationOffCard
import com.keval.fieldpintask.ui.common.PermissionCard
import com.keval.fieldpintask.ui.common.PhotoThumbnail
import com.keval.fieldpintask.ui.common.color
import com.keval.fieldpintask.ui.common.formatDistance
import com.keval.fieldpintask.ui.common.formatTime
import com.keval.fieldpintask.ui.common.label
import com.keval.fieldpintask.ui.common.rememberLocationServicesEnabled
import com.keval.fieldpintask.ui.common.rememberPermissionRequester
import kotlinx.coroutines.flow.emptyFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionScreen(
    onBack: () -> Unit,
    onAddPhoto: () -> Unit,
    newPhoto: String? = null,
    onPhotoConsumed: () -> Unit = {},
    viewModel: InspectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberPermissionRequester(LOCATION_PERMISSIONS)
    val locationServicesOn = rememberLocationServicesEnabled()
    val canLocate = permission.isGranted && locationServicesOn
    val locationFlow = remember(canLocate) { if (canLocate) viewModel.locationUpdates() else emptyFlow() }
    val location by locationFlow.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(location) { viewModel.onLocation(location) }
    LaunchedEffect(newPhoto) { newPhoto?.let { viewModel.addPhoto(it); onPhotoConsumed() } }
    LaunchedEffect(viewModel) { viewModel.submitted.collect { onBack() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.site?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                !permission.isGranted -> PermissionCard(
                    title = R.string.checkin_permission_title,
                    message = R.string.checkin_permission_message,
                    requester = permission,
                )
                !locationServicesOn -> LocationOffCard()
                else -> CheckInBanner(state.checkIn)
            }

            Section(R.string.inspection_condition) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Condition.entries.forEach { option ->
                        FilterChip(
                            selected = state.condition == option,
                            onClick = { viewModel.onConditionChange(option) },
                            label = { Text(stringResource(option.label())) },
                        )
                    }
                }
            }

            Section(R.string.inspection_status) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SiteStatus.entries) { option ->
                        FilterChip(
                            selected = state.status == option,
                            onClick = { viewModel.onStatusChange(option) },
                            label = { Text(stringResource(option.label)) },
                        )
                    }
                }
            }

            Section(R.string.inspection_photos, count = state.photos.size) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.photos, key = { it }) { path ->
                        Box {
                            PhotoThumbnail(path, Modifier.size(88.dp))
                            IconButton(onClick = { viewModel.removePhoto(path) }, modifier = Modifier.align(Alignment.TopEnd).size(28.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_photo), tint = Color.White)
                            }
                        }
                    }
                    item {
                        Box(
                            Modifier.size(88.dp)
                                .clickable(onClick = onAddPhoto)
                                .then(Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Card(Modifier.fillMaxSize(), shape = RoundedCornerShape(12.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.AddAPhoto, contentDescription = stringResource(R.string.add_photo))
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.inspection_notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = viewModel::submit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (state.isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.inspection_submit))
            }
        }
    }
}

@Composable
private fun CheckInBanner(checkIn: CheckInState) {
    val container: Color
    val text: String
    when (checkIn) {
        CheckInState.Locating -> {
            container = MaterialTheme.colorScheme.surfaceVariant
            text = stringResource(R.string.checkin_locating)
        }
        is CheckInState.Outside -> {
            container = MaterialTheme.colorScheme.errorContainer
            text = stringResource(R.string.checkin_outside, formatDistance(checkIn.distanceM))
        }
        is CheckInState.CheckedIn -> {
            container = Color(0xFFCDEBD6)
            text = stringResource(R.string.checkin_done, formatTime(checkIn.atMillis), checkIn.accuracyM.toInt())
        }
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container, contentColor = Color(0xFF1B1B1F))) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Section(title: Int, count: Int? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (count == null) stringResource(title) else "${stringResource(title)} ($count)",
            style = MaterialTheme.typography.titleSmall,
        )
        content()
    }
}

private fun Condition.label(): Int = when (this) {
    Condition.GOOD -> R.string.condition_good
    Condition.FAIR -> R.string.condition_fair
    Condition.POOR -> R.string.condition_poor
}
