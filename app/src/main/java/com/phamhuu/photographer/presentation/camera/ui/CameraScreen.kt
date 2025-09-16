package com.phamhuu.photographer.presentation.camera.ui

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phamhuu.photographer.contants.ImageFilter
import com.phamhuu.photographer.presentation.common.CanvasAddressOverlay
import com.phamhuu.photographer.presentation.common.BeautyAdjustmentPanel
import com.phamhuu.photographer.presentation.common.CameraControls
import com.phamhuu.photographer.presentation.common.InitCameraPermission
import com.phamhuu.photographer.presentation.common.SlideVertically
import com.phamhuu.photographer.presentation.filament.FilamentSurfaceView
import com.phamhuu.photographer.services.gl.CameraGLSurfaceView
import com.phamhuu.photographer.presentation.camera.vm.CameraViewModel
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
    
    val filterGLSurfaceView = remember { 
        CameraGLSurfaceView(context)
    }
    
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    InitCameraPermission({
        viewModel.setFilterHelper(filterGLSurfaceView, context)
        viewModel.startCamera(context, lifecycleOwner, previewView)
        viewModel.checkGalleryContent()
        viewModel.checkLocationPermission(context)
    }, context)

    DisposableEffect(Unit) {
        viewModel.setupMediaPipe()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.startCamera(context, lifecycleOwner, previewView)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onPause()
                }
                else -> {}
            }
        }
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
        contentAlignment = Alignment.TopStart
    ) {
        FilamentSurfaceView(
            context = context,
            lifecycle = lifecycleOwner.lifecycle,
        )
        
        val ratio = uiState.value.ratioCamera.toRatio()
        Box(
            modifier = Modifier.aspectRatio(ratio).align(Alignment.Center)
        ) {
            AndroidView(
                factory = { filterGLSurfaceView },
                modifier = Modifier.fillMaxSize()
            )
            
            if (uiState.value.isLocationEnabled) {
                CanvasAddressOverlay(
                    locationInfo = uiState.value.locationState.locationInfo,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }

        CameraControls(
            onCaptureClick = { viewModel.takePhoto(context) },
            onVideoClick = { viewModel.startRecording(context) },
            onStopRecord = { viewModel.stopRecording() },
            onChangeCamera = { 
                viewModel.changeCamera(context, lifecycleOwner, previewView)
            },
            onChangeCaptureOrVideo = { value ->
                viewModel.changeCaptureOrVideo(value, context, lifecycleOwner, previewView)
            },
            onChangeFlashMode = { viewModel.setFlashMode() },
            onShowGallery = { navController.navigate("gallery") },
            isCapture = uiState.value.setupCapture,
            isRecording = uiState.value.isRecording,
            modifier = Modifier.align(Alignment.BottomCenter),
            fileUri = uiState.value.fileUri,
            flashMode = uiState.value.flashMode,
            timeDelay = uiState.value.timerDelay,
            resolution = uiState.value.ratioCamera,
            enableLocation = uiState.value.isLocationEnabled,
            onChangeTimeDelay = { viewModel.setTimerDelay(it) },
            onChangeResolution = { 
                viewModel.setRatioCamera(it, context, lifecycleOwner, previewView)
            },
            onImageFilterSelected = { _ ->
                viewModel.toggleBeautyPanel()
            },
            onChangeLocationToggle = {
                viewModel.toggleLocationEnabled()
            }
        )

        AnimatedVisibility(
            visible = uiState.value.isBrightnessVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            SlideVertically(
                uiState.value.brightness,
                { brightness -> viewModel.setBrightness(brightness) }
            )
        }

        BeautyAdjustmentPanel(
            isVisible = uiState.value.isBeautyPanelVisible,
            beautySettings = uiState.value.beautySettings,
            onSkinSmoothingChange = { viewModel.updateSkinSmoothing(it) },
            onWhitenessChange = { viewModel.updateWhiteness(it) },
            onThinFaceChange = { viewModel.updateThinFace(it) },
            onBigEyeChange = { viewModel.updateBigEye(it) },
            onBlendLevelChange = { viewModel.updateBlendLevel(it) },
            onResetToDefaults = { viewModel.resetBeautySettings() },
            onDismiss = { viewModel.toggleBeautyPanel() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun FilterIndicator(
    filter: ImageFilter,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = "🎨 ${filter.displayName}",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
