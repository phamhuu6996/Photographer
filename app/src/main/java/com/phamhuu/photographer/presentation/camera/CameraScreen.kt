package com.phamhuu.photographer.presentation.camera

import FilamentHelper
import LocalNavController
import RenderableModel
import android.content.Context
import android.graphics.PixelFormat
import android.view.Surface
import android.view.SurfaceView
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.filament.Engine
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.phamhuu.photographer.presentation.common.CameraControls
import com.phamhuu.photographer.presentation.common.InitCameraPermission
import com.phamhuu.photographer.presentation.common.ResolutionControl
import com.phamhuu.photographer.presentation.common.SlideVertically
import kotlin.math.min
import android.graphics.Color.TRANSPARENT

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val cameraState = viewModel.cameraState.collectAsState()
    var offsetY = remember { 0f }
    val navController = LocalNavController.current

    InitCameraPermission({
        viewModel.startCamera(context, lifecycleOwner, previewView)
        viewModel.checkGalleryContent(context)
    }, context)

    DisposableEffect(Unit) {
        viewModel.setupMediaPipe(context)
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
        FilamentSurfaceView(
            context = context,
            lifecycle = lifecycleOwner.lifecycle,
            resultBundle = cameraState.value.landmarkResult,
        )
        AndroidView(
            factory = {

                previewView },
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

@Composable
fun FilamentSurfaceView(
    context: Context,
    lifecycle: Lifecycle,
    resultBundle: FaceLandmarkerHelper.ResultBundle?,
    modelPath: String = "models/glasses.glb",
) {
    val surfaceView = remember { SurfaceView(context) }
    val filamentHelper = remember { FilamentHelper(context, surfaceView, lifecycle) }

    // Load model kính chỉ 1 lần
    val glassesBuffer = remember {
        filamentHelper.loadGlbAssetFromAssets(modelPath)
    }

    // Load model khi khởi tạo
    LaunchedEffect(glassesBuffer) {
        glassesBuffer.let {
            val initialTransform = floatArrayOf(0f, 0f, -3f)

            filamentHelper.loadModels(
                listOf(RenderableModel(it, initialTransform))
            )
        }
    }

    // Cập nhật transform mỗi lần nhận result mới
    LaunchedEffect(resultBundle) {
        val transforms = filamentHelper.extractGlassesTransform(resultBundle) ?: return@LaunchedEffect
        filamentHelper.updateModelPositionsAndScales(listOf(transforms))
    }

    AndroidView(factory = { surfaceView })
}

