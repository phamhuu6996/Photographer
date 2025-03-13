package com.phamhuu.photographer.presentation.common

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phamhuu.photographer.R
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
    onShowGallery: (() -> Unit) ?= null,
    isRecording: Boolean = false,
    fileUri: Uri? = null,
) {
    val timerViewModel: TimerViewModel = viewModel()
    val state = timerViewModel.elapsedTime.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
        .padding(16.dp)
    ) {
        if (isRecording) {
            Text(
                text = timerViewModel.timeDisplayRecord(state.value),
                color = Color.Red,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImageCustom(
                imageSource = Gallery.getResourceUri(LocalContext.current, fileUri),
                modifier.clickable {
                    if (fileUri != null && onShowGallery != null) {
                        onShowGallery()
                    }
                })

            ImageCustom(
                id = R.drawable.capture,
                imageMode = ImageMode.LARGE,
                color = Color.Black,
                modifier = Modifier.clickable { onCaptureClick() }
            )

            ImageCustom(
                id = if (isRecording) R.drawable.stop_record else R.drawable.start_record,
                imageMode = ImageMode.LARGE,
                color = Color.Black,
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
                color = Color.Black,
                modifier = Modifier.clickable { onChangeCamera() }
            )
        }
    }
}