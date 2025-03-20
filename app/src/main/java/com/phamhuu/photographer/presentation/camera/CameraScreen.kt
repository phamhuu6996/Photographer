package com.phamhuu.photographer.presentation.camera

import LocalNavController
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.common.CameraControls
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.common.InitCameraPermission
import com.phamhuu.photographer.presentation.common.ResolutionControl
import com.phamhuu.photographer.presentation.common.SlideVertically
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FIT_CENTER
    } }
    val cameraState = viewModel.cameraState.collectAsState()
    val offsetY = remember { Animatable(0f) }
    val navController = LocalNavController.current

    InitCameraPermission({
        viewModel.startCamera(context, lifecycleOwner, previewView)
        viewModel.checkGalleryContent(context)
    }, context)

    Box(
        modifier = Modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures (panZoomLock = true){ _, _, zoomChange, _ ->
                    viewModel.changeZoom(zoomChange)
                }
                detectVerticalDragGestures(
                    onDragEnd = {
                        viewModel.viewModelScope.launch {
                            if (offsetY.value < -50) viewModel.changeShowBrightness(true) // Vuốt lên
                            if (offsetY.value > 50) viewModel.changeShowBrightness(false)// Vuốt xuống
                            offsetY.snapTo(0f)
                        } // Reset vị trí kéo
                    },
                    onVerticalDrag = { _, dragAmount ->
                        viewModel.viewModelScope.launch {
                            offsetY.snapTo(offsetY.value + dragAmount)
                        } // Reset vị trí kéo
                    }
                )
            },
        contentAlignment = Alignment.TopStart // Align content to top start

    ) {
        AndroidView(
            factory = { previewView },
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
        if(cameraState.value.enableSelectResolution)
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

