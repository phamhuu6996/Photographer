package com.phamhuu.photographer.presentation.camera

import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.phamhuu.photographer.presentation.common.ImageMode
import com.phamhuu.photographer.presentation.common.InitCameraPermission
import com.phamhuu.photographer.presentation.common.SlideVertically
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraState = viewModel.cameraState.collectAsState()
    var isBrightnessVisible by remember { mutableStateOf(false) }
    val offsetY = remember { Animatable(0f) }

    InitCameraPermission({
        viewModel.startCamera(context, lifecycleOwner, previewView)
    }, context)

    Box(
        modifier = Modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        viewModel.viewModelScope.launch {
                            if (offsetY.value < -50) isBrightnessVisible = true // Vuốt lên
                            if (offsetY.value > 50) isBrightnessVisible = false // Vuốt xuống
                            offsetY.snapTo(0f)
                        } // Reset vị trí kéo
                    },
                    onVerticalDrag = { _, dragAmount ->
                        viewModel.viewModelScope.launch {
                            offsetY.snapTo(offsetY.value + dragAmount)
                        } // Reset vị trí kéo
                    }
                )
            }
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        CameraControls(
            onCaptureClick = { viewModel.takePhoto(context) },
            onVideoClick = { viewModel.startRecording(context) },
            onStopRecord = { viewModel.stopRecording() },
            isRecording = cameraState.value.isRecording,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Row(
            modifier = Modifier
                .padding(top = 20.dp).fillMaxSize(),
            horizontalArrangement = Arrangement.Absolute.Right
        ) {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                onClick = {
                viewModel.setFlashMode()
            }) {
                ImageCustom(
                    id = flashModeToIcon(cameraState.value.flashMode),
                )
            }
        }

        // Hiệu ứng hiện slider khi vuốt lên
        AnimatedVisibility(
            visible = isBrightnessVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            SlideVertically(cameraState.value.brightness,
                { brightness -> viewModel.setBrightness(brightness) })
        }
    }
}

private fun flashModeToIcon(flashMode: Int): Int {
    return when (flashMode) {
        ImageCapture.FLASH_MODE_OFF -> R.drawable.flash_off
        ImageCapture.FLASH_MODE_ON -> R.drawable.flash_on
        ImageCapture.FLASH_MODE_AUTO -> R.drawable.auto_flash
        else -> R.drawable.flash_off
    }
}