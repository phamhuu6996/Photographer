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
 import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageFilter
import com.phamhuu.photographer.contants.ImageMode
import com.phamhuu.photographer.contants.RatioCamera
import com.phamhuu.photographer.contants.TimerDelay
import com.phamhuu.photographer.presentation.timer.TimerViewModel
import com.phamhuu.photographer.services.android.GalleryService

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
    enableLocation: Boolean = false,
    onChangeTimeDelay: (TimerDelay) -> Unit,
    onChangeResolution: (RatioCamera) -> Unit,
    onImageFilterSelected: (ImageFilter) -> Unit = {},
    onChangeLocationToggle: () -> Unit = {},
) {
    val timerViewModel: TimerViewModel = viewModel()
    val state = timerViewModel.elapsedTime.collectAsState()
    val context = LocalContext.current

    val onChangeCameraModifier = remember(onShowGallery) {
        modifier.clickable {
            if (onShowGallery != null) {
                onShowGallery()
            }
        }
    }
    val uriThumbnails = remember(fileUri) {
        fileUri?.let { GalleryService.getResourceUri(context, fileUri) }
    }

    val iconLocation = if(enableLocation) R.drawable.location_on else R.drawable.location_off

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Top control row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5F))
                .padding(top = 50.dp, start = 16.dp, end = 16.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash button - dùng primary khi active (không phải OFF)
            ImageCustom(
                id = flashModeToIcon(flashMode),
                imageMode = ImageMode.MEDIUM,
                color = if (flashMode != ImageCapture.FLASH_MODE_OFF) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                onClick = { onChangeFlashMode() }
            )

            // Timer button - dùng primary khi active (không phải OFF)
            ImageCustom(
                id = timeDelay.toIcon(),
                imageMode = ImageMode.MEDIUM,
                color = if (timeDelay != TimerDelay.OFF) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                onClick = { onChangeTimeDelay(timeDelay.next()) }
            )
            
            // Aspect ratio button - chỉ 16:9 mới dùng màu, 3:4 dùng mặc định
            ImageCustom(
                id = resolution.toIcon(),
                imageMode = ImageMode.MEDIUM,
                color = if (resolution == RatioCamera.RATIO_9_16) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                onClick = { onChangeResolution(resolution.next()) }
            )

            // Location toggle button - dùng primary khi enabled
            ImageCustom(
                id = iconLocation,
                imageMode = ImageMode.MEDIUM,
                color = if (enableLocation) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface,
                onClick = { onChangeLocationToggle() }
            )
            
            // Camera switch button
            ImageCustom(
                id = R.drawable.change_camera,
                imageMode = ImageMode.MEDIUM,
                color = MaterialTheme.colorScheme.onSurface,
                onClick = { onChangeCamera() }
            )
        }

        if (isRecording) {
            Text(
                text = timerViewModel.timeDisplayRecord(state.value),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5F))
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                CameraExtensionControl(
                    title = stringResource(R.string.photo),
                    callBack = { onChangeCaptureOrVideo(true) },
                    selected = isCapture
                )
                Spacer(modifier = Modifier.width(10.dp))
                CameraExtensionControl(
                    title = stringResource(R.string.video),
                    callBack = { onChangeCaptureOrVideo(false) },
                    selected = !isCapture
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (fileUri != null)
                    AsyncImageCustom(
                        imageSource = uriThumbnails,
                        modifier = onChangeCameraModifier,
                        size = 40.dp,
                    )
                else
                    ImageCustom(
                        id = R.drawable.ic_album,
                        imageMode = ImageMode.MEDIUM,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                if (isCapture)
                    ImageCustom(
                        id = R.drawable.capture,
                        imageMode = ImageMode.LARGE,
                        filter = false,
                        onClick = { onCaptureClick() },
                        modifier = Modifier.size(70.dp)
                    )
                else
                    ImageCustom(
                        id = if (isRecording) R.drawable.stop_record else R.drawable.start_record,
                        imageMode = ImageMode.LARGE,
                        filter = false,
                        onClick = {
                            if (isRecording) {
                                onStopRecord()
                                timerViewModel.stopTimer()
                            } else {
                                onVideoClick()
                                timerViewModel.startTimer()
                            }
                        },
                        modifier = Modifier.size(70.dp)
                    )

                // Beauty Adjustment button
                ImageCustom(
                    id = R.drawable.magic,
                    imageMode = ImageMode.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = { 
                        onImageFilterSelected(ImageFilter.BEAUTY) // Triggers beauty panel toggle
                    }
                )
            }
        }
        
        // Filter popup removed - beauty panel is now handled directly in CameraScreen
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
        color = if (selected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurface,
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