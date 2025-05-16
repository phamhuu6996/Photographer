package com.phamhuu.photographer.presentation.camera

import LocalNavController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phamhuu.photographer.presentation.common.CameraControls
import com.phamhuu.photographer.presentation.common.InitCameraPermission
import com.phamhuu.photographer.presentation.common.ResolutionControl
import com.phamhuu.photographer.presentation.common.SlideVertically
import com.phamhuu.photographer.presentation.filament.FilamentSurfaceView
import org.koin.androidx.compose.koinViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = koinViewModel<CameraViewModel>()
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val cameraState = viewModel.cameraState.collectAsStateWithLifecycle()
    val models3D = remember { listOf("models/glasses.glb") }
    val navController = LocalNavController.current

    InitCameraPermission({
        viewModel.startCamera(context, lifecycleOwner, previewView)
        viewModel.checkGalleryContent(context)
    }, context)

    DisposableEffect(Unit) {
        viewModel.setupMediaPipe()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onresume(context, lifecycleOwner, previewView)
                }

                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onPause()
                }

                else -> {}
            }
        }
        // Add the observer
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures(panZoomLock = true) { centroid, pan, zoomChange, rotation ->
                    viewModel.getCameraPointerInput(centroid, pan, zoomChange, rotation)
                }

            },
        contentAlignment = Alignment.TopStart // Align content to top start

    ) {

//        FilamentSurfaceView(
//            context = context,
//            lifecycle = lifecycleOwner.lifecycle,
//        )
        AndroidView(
            factory = {
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        CameraControls(
            onCaptureClick = { viewModel.takePhoto(context) },
            onVideoClick = { viewModel.startRecording(context) },
            onStopRecord = { viewModel.stopRecording() },
            onChangeCamera = { viewModel.changeCamera(context, lifecycleOwner, previewView) },
            onChangeCaptureOrVideo = { value ->
                viewModel.changeCaptureOrVideo(
                    value,
                    context,
                    lifecycleOwner,
                    previewView
                )
            },
            onChangeFlashMode = { viewModel.setFlashMode() },
            onShowGallery = { navController.navigate("gallery") },
            isCapture = cameraState.value.setupCapture,
            isRecording = cameraState.value.isRecording,
            modifier = Modifier.align(Alignment.BottomCenter),
            fileUri = cameraState.value.fileUri,
            flashMode = cameraState.value.flashMode,
        )

        // Hiệu ứng hiện slider khi vuốt lên
        AnimatedVisibility(
            visible = cameraState.value.isBrightnessVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            SlideVertically(cameraState.value.brightness,
                { brightness -> viewModel.setBrightness(brightness) })
        }
        if (cameraState.value.enableSelectResolution)
            ResolutionControl(
                viewModel = viewModel,
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                showSelectResolution = cameraState.value.showBottomSheetSelectResolution,
                resolution = cameraState.value.captureResolution,
            )
    }
}

