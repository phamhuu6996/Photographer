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
import androidx.compose.runtime.LaunchedEffect
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
import com.phamhuu.photographer.enums.ImageFilter
import com.phamhuu.photographer.presentation.common.CameraControls
import com.phamhuu.photographer.presentation.common.InitCameraPermission
import com.phamhuu.photographer.presentation.common.SlideVertically
import com.phamhuu.photographer.presentation.filament.FilamentSurfaceView
import com.phamhuu.photographer.presentation.utils.CameraGLSurfaceView
import com.phamhuu.photographer.presentation.utils.FilterRenderer
import org.koin.androidx.compose.koinViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = koinViewModel<CameraViewModel>()
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Normal camera preview
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    
    // ✅ CameraGLSurfaceView cho filtering từ ImageAnalyzer data
    val filterGLSurfaceView = remember { 
        CameraGLSurfaceView(context)
    }
    
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalNavController.current

    InitCameraPermission({
        viewModel.setFilterHelper(filterGLSurfaceView, context)
        viewModel.startCamera(context, lifecycleOwner, previewView)
        viewModel.checkGalleryContent()
    }, context)

    DisposableEffect(Unit) {
        viewModel.setupMediaPipe()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // Resume camera
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

    // Show error message if present
    uiState.value.error?.let { error ->
        LaunchedEffect(error) {
            // You can show toast or snackbar here
            // For now, just clear the error after showing
            viewModel.clearError()
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
        
        // ✅ Improved view switching với loading feedback
        when {
            uiState.value.currentFilter != ImageFilter.NONE -> {
                // Filtered camera preview với ImageAnalyzer data
                val ratio = uiState.value.ratioCamera.toRatio()// Default ratio if not set
                AndroidView(
                    factory = { filterGLSurfaceView },
                    modifier = Modifier.aspectRatio(ratio).align(Alignment.Center)
                )
                
                // ✅ Show loading indicator during filter transition
                AnimatedVisibility(
                    visible = uiState.value.isLoading,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "🔄 Applying ${uiState.value.currentFilter.displayName}...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            else -> {
                // Normal camera preview
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Error display
        uiState.value.error?.let { error ->
            Text(
                text = error,
                color = Color.Red,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        // Filter indicator ở top center
        AnimatedVisibility(
            visible = uiState.value.currentFilter != ImageFilter.NONE && !uiState.value.isLoading,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            FilterIndicator(
                filter = uiState.value.currentFilter,
                modifier = Modifier.padding(top = 80.dp)
            )
        }

        CameraControls(
            onCaptureClick = { viewModel.takePhoto(context) },
            onVideoClick = { viewModel.startRecording(context) },
            onStopRecord = { viewModel.stopRecording() },
            onChangeCamera = { 
                // Normal camera change functionality
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
            onChangeTimeDelay = { viewModel.setTimerDelay(it) },
            onChangeResolution = { 
                viewModel.setRatioCamera(it, context, lifecycleOwner, previewView)
            },
            // Bottom navigation callbacks
            onBeautyEffectSelected = { beautyEffect ->
                // TODO: Map BeautyEffect to ImageFilter
            },
            on3DModelSelected = { model3D ->
                viewModel.selectModel3D(context, model3D)
            },
            onImageFilterSelected = { imageFilter ->
                // ✅ Real OpenGL ES filtering với ImageAnalyzer data!
                viewModel.setImageFilter(imageFilter)
            },
            currentFilter = uiState.value.currentFilter
        )

        // Brightness slider with animation
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

