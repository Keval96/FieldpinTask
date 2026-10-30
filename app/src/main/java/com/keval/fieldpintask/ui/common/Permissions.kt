package com.keval.fieldpintask.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

val LOCATION_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

@Stable
class PermissionRequester(
    val isGranted: Boolean,
    /** True once the system will no longer show the dialog, so only Settings can grant it. */
    val needsSettings: Boolean,
    private val launch: () -> Unit,
    private val settings: () -> Unit,
) {
    /** Asks the system dialog, or opens app settings when the user chose "Don't ask again". */
    fun requestOrOpenSettings() = if (needsSettings) settings() else launch()
}

/**
 * Tracks one runtime permission group. The first request is only made when the caller invokes it,
 * so the rationale can be shown before the system dialog.
 */
@Composable
fun rememberPermissionRequester(permissions: Array<String>): PermissionRequester {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    // Location counts as granted only with the precise permission; approximate cannot geofence.
    val required = permissions.first()

    fun granted() = ContextCompat.checkSelfPermission(context, required) == PackageManager.PERMISSION_GRANTED

    fun needsSettings(): Boolean {
        val activity = context.findActivity() ?: return false
        val askedBefore = prefs.getBoolean(required, false)
        return !granted() && askedBefore && !activity.shouldShowRequestPermissionRationale(required)
    }

    var isGranted by remember { mutableStateOf(granted()) }
    var needsSettings by remember { mutableStateOf(needsSettings()) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        prefs.edit().putBoolean(required, true).apply()
        isGranted = granted()
        needsSettings = needsSettings()
    }

    // The user may come back from Settings with a changed permission.
    LifecycleResumeEffect(Unit) {
        isGranted = granted()
        needsSettings = needsSettings()
        onPauseOrDispose {}
    }

    return PermissionRequester(
        isGranted = isGranted,
        needsSettings = needsSettings,
        launch = { launcher.launch(permissions) },
        settings = { context.openAppSettings() },
    )
}

/** Whether the device-level location switch is on; refreshed when returning from Settings. */
@Composable
fun rememberLocationServicesEnabled(): Boolean {
    val context = LocalContext.current
    fun enabled() = LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java))
    var isEnabled by remember { mutableStateOf(enabled()) }
    LifecycleResumeEffect(Unit) {
        isEnabled = enabled()
        onPauseOrDispose {}
    }
    return isEnabled
}

fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

fun Context.openLocationSettings() {
    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private const val PREFS_NAME = "permission_requests"
