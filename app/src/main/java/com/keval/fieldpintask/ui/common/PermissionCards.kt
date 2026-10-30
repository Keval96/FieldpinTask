package com.keval.fieldpintask.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keval.fieldpintask.R

/** Explains why a permission is needed and offers Allow, or Settings after "Don't ask again". */
@Composable
fun PermissionCard(
    @StringRes title: Int,
    @StringRes message: Int,
    requester: PermissionRequester,
    modifier: Modifier = Modifier,
) {
    NoticeCard(
        title = title,
        message = if (requester.needsSettings) R.string.permission_denied_settings else message,
        actionLabel = if (requester.needsSettings) R.string.open_settings else R.string.allow,
        onAction = requester::requestOrOpenSettings,
        modifier = modifier,
    )
}

/** Shown when permission is granted but the device location switch is off. */
@Composable
fun LocationOffCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    NoticeCard(
        title = R.string.gps_off_title,
        message = R.string.gps_off_message,
        actionLabel = R.string.turn_on_location,
        onAction = context::openLocationSettings,
        modifier = modifier,
    )
}

@Composable
private fun NoticeCard(
    @StringRes title: Int,
    @StringRes message: Int,
    @StringRes actionLabel: Int,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onAction) { Text(stringResource(actionLabel)) }
            }
        }
    }
}
