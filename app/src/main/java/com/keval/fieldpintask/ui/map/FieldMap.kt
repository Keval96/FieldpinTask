package com.keval.fieldpintask.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.keval.fieldpintask.BuildConfig
import com.keval.fieldpintask.domain.gis.LatLng
import com.keval.fieldpintask.domain.location.DeviceLocation
import com.keval.fieldpintask.domain.model.Site
import com.keval.fieldpintask.ui.common.color
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng as MapLatLng

sealed interface CameraTarget {
    data class Fit(val points: List<LatLng>) : CameraTarget
    data class Center(val point: LatLng, val zoom: Double = DEFAULT_ZOOM) : CameraTarget
}

/** [token] makes repeated identical requests (e.g. the my-location button) count as new commands. */
data class CameraCommand(val target: CameraTarget, val token: Long = System.nanoTime())

private const val DEFAULT_ZOOM = 16.0
private const val FIT_PADDING_PX = 120
private const val CAMERA_ANIMATION_MS = 600

/** Map with site polygons, the user's position and an optional boundary being drawn. */
@Composable
fun FieldMap(
    modifier: Modifier = Modifier,
    sites: List<Site> = emptyList(),
    selectedSiteId: String? = null,
    userLocation: DeviceLocation? = null,
    draftPoints: List<LatLng> = emptyList(),
    draftValid: Boolean = true,
    cameraCommand: CameraCommand? = null,
    onMapClick: (LatLng) -> Unit = {},
) {
    val mapView = rememberMapViewWithLifecycle()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val currentOnClick by rememberUpdatedState(onMapClick)

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.isRotateGesturesEnabled = false
            m.addOnMapClickListener { point ->
                currentOnClick(LatLng(point.latitude, point.longitude))
                true
            }
            m.setStyle(Style.Builder().fromJson(MapStyle.json(BuildConfig.TILE_URL_TEMPLATE))) { loaded ->
                MapStyle.install(loaded)
                style = loaded
            }
        }
    }

    LaunchedEffect(style, sites, selectedSiteId) {
        style?.let { MapStyle.updateSites(it, sites, selectedSiteId) { site -> site.status.color } }
    }
    LaunchedEffect(style, draftPoints, draftValid) { style?.let { MapStyle.updateDraft(it, draftPoints, draftValid) } }
    LaunchedEffect(style, userLocation) { style?.let { MapStyle.updateUser(it, userLocation) } }
    LaunchedEffect(map, cameraCommand) {
        val target = cameraCommand?.target ?: return@LaunchedEffect
        map?.moveCamera(target)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun MapLibreMap.moveCamera(target: CameraTarget) {
    when (target) {
        is CameraTarget.Center -> animateCamera(
            CameraUpdateFactory.newLatLngZoom(MapLatLng(target.point.latitude, target.point.longitude), target.zoom),
            CAMERA_ANIMATION_MS,
        )
        is CameraTarget.Fit -> {
            if (target.points.size < 2) {
                target.points.firstOrNull()?.let { moveCamera(CameraTarget.Center(it)) }
                return
            }
            val bounds = LatLngBounds.Builder()
                .includes(target.points.map { MapLatLng(it.latitude, it.longitude) })
                .build()
            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, FIT_PADDING_PX), CAMERA_ANIMATION_MS)
        }
    }
}

/** Forwards the host lifecycle to the MapView, which MapLibre requires. */
@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) { destroyed = true; mapView.onDestroy() }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // Leaving the screen while the activity lives: walk the MapView down ourselves.
            if (!destroyed) {
                destroyed = true
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
                mapView.onDestroy()
            }
        }
    }
    return mapView
}
