package com.phamhuu.photographer.presentation.common

import android.annotation.SuppressLint
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamhuu.photographer.R
import com.phamhuu.photographer.enums.ImageMode
import com.phamhuu.photographer.enums.RatioCamera
import com.phamhuu.photographer.enums.TimerDelay
import com.phamhuu.photographer.presentation.timer.TimerViewModel
import com.phamhuu.photographer.presentation.utils.Gallery

@SuppressLint("DefaultLocale")
@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit,
    onVideoClick: () -> Unit,
    onStopRecord: () -> Unit,
    onChangeCamera: () -> Unit,
    onChangeCaptureOrVideo: (Boolean) -> Unit,
    onShowGallery: (() -> Unit)? = null,
    onChangeFlashMode: () -> Unit,
    isRecording: Boolean = false,
    fileUri: Uri? = null,
    isCapture: Boolean = true,
    flashMode: Int,
    timeDelay: TimerDelay = TimerDelay.OFF,
    resolution: RatioCamera = RatioCamera.RATIO_3_4,
    onChangeTimeDelay: (TimerDelay) -> Unit,
    onChangeResolution: (RatioCamera) -> Unit,
) {
    val timerViewModel: TimerViewModel = viewModel()
    val state = timerViewModel.elapsedTime.collectAsState()
    val showTimerOptions = remember { mutableStateOf(false) }
    val currentAspectRatio = remember { mutableStateOf("4:3") }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Top control row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5F))
                .padding(top = 50.dp, start = 16.dp, end = 16.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash button
            ImageCustom(
                id = flashModeToIcon(flashMode),
                imageMode = ImageMode.MEDIUM,
                color = Color.White,
                modifier = Modifier.clickable { onChangeFlashMode() }
            )

            // Timer button
            ImageCustom(
                id = timeDelay.toIcon(),
                imageMode = ImageMode.MEDIUM,
                color = Color.White,
                modifier = Modifier.clickable {
                    onChangeTimeDelay(timeDelay.next())
                }
            )
            
            // Aspect ratio button
            ImageCustom(
                id = resolution.toIcon(),
                imageMode = ImageMode.MEDIUM,
                color = Color.White,
                modifier = Modifier.clickable {
                    onChangeResolution(resolution.next())
                }
            )
            
            // Camera switch button
            ImageCustom(
                id = R.drawable.change_camera,
                imageMode = ImageMode.MEDIUM,
                color = Color.White,
                modifier = Modifier.clickable { onChangeCamera() }
            )
        }

        if (isRecording) {
            Text(
                text = timerViewModel.timeDisplayRecord(state.value),
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5F))
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                CameraExtensionControl(
                    title = "Ảnh",
                    callBack = { onChangeCaptureOrVideo(true) },
                    selected = isCapture
                )
                Spacer(modifier = Modifier.width(10.dp))
                CameraExtensionControl(
                    title = "Video",
                    callBack = { onChangeCaptureOrVideo(false) },
                    selected = !isCapture
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (fileUri != null)
                    AsyncImageCustom(
                        imageSource = Gallery.getResourceUri(LocalContext.current, fileUri),
                        modifier.clickable {
                            if (onShowGallery != null) {
                                onShowGallery()
                            }
                        },
                        size = 48.dp,
                    )
                else
                    Spacer(
                        modifier = Modifier
                            .height(48.dp)
                            .width(48.dp)
                    )

                if (isCapture)
                    ImageCustom(
                        id = R.drawable.capture,
                        imageMode = ImageMode.LARGE,
                        color = Color.White,
                        modifier = Modifier.clickable { onCaptureClick() }
                    )
                else
                    ImageCustom(
                        id = if (isRecording) R.drawable.stop_record else R.drawable.start_record,
                        imageMode = ImageMode.LARGE,
                        color = Color.White,
                        modifier = Modifier.clickable {
                            if (isRecording) {
                                onStopRecord()
                                timerViewModel.stopTimer()
                            } else {
                                onVideoClick()
                                timerViewModel.startTimer()
                            }
                        }
                    )

                ImageCustom(
                    id = R.drawable.change_camera,
                    imageMode = ImageMode.LARGE,
                    color = Color.White,
                    modifier = Modifier.clickable { onChangeCamera() }
                )
            }
        }
    }
}

@Composable
fun CameraExtensionControl(
    callBack: () -> Unit,
    selected: Boolean,
    title: String,

    ) {
    Text(
        text = title,
        modifier = Modifier.clickable { callBack() },
        color = Color.White,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
}

private fun flashModeToIcon(flashMode: Int): Int {
    return when (flashMode) {
        ImageCapture.FLASH_MODE_OFF -> R.drawable.flash_off
        ImageCapture.FLASH_MODE_ON -> R.drawable.flash_on
        ImageCapture.FLASH_MODE_AUTO -> R.drawable.auto_flash
        else -> R.drawable.flash_off
    }
}