package com.keval.fieldpintask.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keval.fieldpintask.R
import com.keval.fieldpintask.data.camera.StampText
import com.keval.fieldpintask.ui.common.CAMERA_PERMISSIONS
import com.keval.fieldpintask.ui.common.LOCATION_PERMISSIONS
import com.keval.fieldpintask.ui.common.PermissionCard
import com.keval.fieldpintask.ui.common.rememberPermissionRequester
import java.io.File
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(onPhotoTaken: (String) -> Unit, onBack: () -> Unit, viewModel: CameraViewModel = hiltViewModel()) {
    val cameraPermission = rememberPermissionRequester(CAMERA_PERMISSIONS)
    val locationPermission = rememberPermissionRequester(LOCATION_PERMISSIONS)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermission.isGranted) {
            CameraContent(viewModel, locationGranted = locationPermission.isGranted, onPhotoTaken = onPhotoTaken)
        } else {
            PermissionCard(
                title = R.string.camera_permission_title,
                message = R.string.camera_permission_message,
                requester = cameraPermission,
                modifier = Modifier.safeDrawingPadding().padding(16.dp).align(Alignment.Center),
            )
        }
        IconButton(onClick = onBack, modifier = Modifier.safeDrawingPadding().padding(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
        }
    }
}

@Composable
private fun CameraContent(viewModel: CameraViewModel, locationGranted: Boolean, onPhotoTaken: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val siteName by viewModel.siteName.collectAsStateWithLifecycle()

    val locationFlow = remember(locationGranted) { if (locationGranted) viewModel.locationUpdates() else emptyFlow() }
    val location by locationFlow.collectAsStateWithLifecycle(initialValue = null)

    val controller = remember { LifecycleCameraController(context) }
    LaunchedEffect(controller) { controller.bindToLifecycle(lifecycleOwner) }

    var flashOn by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { PreviewView(it).apply { this.controller = controller } },
            modifier = Modifier.fillMaxSize(),
        )

        location?.let { fix ->
            val lines = StampText.lines(siteName, fix.latLng, fix.accuracyM, System.currentTimeMillis())
            Column(
                Modifier.align(Alignment.BottomStart).safeDrawingPadding().padding(start = 16.dp, bottom = 150.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp)).padding(10.dp),
            ) {
                lines.forEach { Text(it, color = Color.White, style = MaterialTheme.typography.bodySmall) }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().safeDrawingPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (location == null) {
                Text(stringResource(R.string.camera_waiting_gps), color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
            if (failed) {
                Text(stringResource(R.string.camera_failed), color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }) { Icon(Icons.Filled.FlipCameraAndroid, contentDescription = stringResource(R.string.camera_flip), tint = Color.White) }

                ShutterButton(enabled = location != null && !isSaving) {
                    val fix = location ?: return@ShutterButton
                    isSaving = true
                    failed = false
                    val file = viewModel.newPhotoFile()
                    takePicture(context, controller, file, onSaved = {
                        scope.launch {
                            viewModel.stamp(file, fix)
                            onPhotoTaken(file.absolutePath)
                        }
                    }, onError = { isSaving = false; failed = true })
                }

                IconButton(onClick = {
                    flashOn = !flashOn
                    controller.imageCaptureFlashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                }) {
                    Icon(
                        if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = stringResource(R.string.camera_flash),
                        tint = Color.White,
                    )
                }
            }
        }
        if (isSaving) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(76.dp).border(4.dp, Color.White, CircleShape).padding(8.dp).clip(CircleShape)
            .background(if (enabled) Color.White else Color.Gray)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

private fun takePicture(
    context: Context,
    controller: LifecycleCameraController,
    file: File,
    onSaved: () -> Unit,
    onError: () -> Unit,
) {
    controller.takePicture(
        ImageCapture.OutputFileOptions.Builder(file).build(),
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSaved()
            override fun onError(exception: ImageCaptureException) = onError()
        },
    )
}
