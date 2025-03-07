package com.phamhuu.photographer.presentation.camera.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.timer.TimerViewModel
import com.phamhuu.photographer.presentation.utils.Permission
import kotlinx.coroutines.delay

@SuppressLint("DefaultLocale")
@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit,
    onVideoClick: () -> Unit,
    onStopRecord: () -> Unit,
    isRecording: Boolean = false,
) {
    val timerViewModel: TimerViewModel = viewModel()
    val state = timerViewModel.elapsedTime.collectAsState()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isRecording) {
            Text(
                text = timerViewModel.timeDisplayRecord(state.value),
                color = Color.Red,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    Column(
        modifier = modifier
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            FilledTonalButton(
                onClick = onCaptureClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.capture),
                    contentDescription = "Capture Photo",
                    modifier = Modifier.size(48.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)

                )
            }

            FilledTonalButton(
                onClick = {
                    if (isRecording) {
                        onStopRecord()
                        timerViewModel.stopTimer()
                    } else {
                        onVideoClick()
                        timerViewModel.startTimer()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
            ) {
                Image(
                    painter = painterResource(id = if (isRecording) R.drawable.stop else R.drawable.record),
                    contentDescription = "Record video",
                    modifier = Modifier.size(48.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)
                )
            }
        }
    }
}

@Composable
fun InitCameraPermission(callback: () -> Unit, context: Context) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            callback()
        } else {
            Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
            Permission.openSettings(context)
        }
    }

    val permissions: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    LaunchedEffect(Unit) {
        if (!Permission.hasPermissions(context, permissions)) {
            permissionLauncher.launch(permissions)
        } else {
            callback()
        }
    }
}