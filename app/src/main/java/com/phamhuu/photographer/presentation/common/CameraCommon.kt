package com.phamhuu.photographer.presentation.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.camera.CameraViewModel
import com.phamhuu.photographer.presentation.timer.TimerViewModel
import com.phamhuu.photographer.presentation.utils.Permission

@SuppressLint("DefaultLocale")
@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit,
    onVideoClick: () -> Unit,
    onStopRecord: () -> Unit,
    onChangeCamera: () -> Unit,
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
                ImageCustom(
                    id = R.drawable.capture,
                    imageMode = ImageMode.LARGE,
                    color = Color.Black
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
                ImageCustom(
                    id = if (isRecording) R.drawable.stop_record else R.drawable.start_record,
                    imageMode = ImageMode.LARGE,
                    color = Color.Black
                )
            }

            FilledTonalButton(
                onClick = onChangeCamera,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                ImageCustom(
                    id = R.drawable.change_camera,
                    imageMode = ImageMode.LARGE,
                    color = Color.Black
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

@Composable
fun GalleryScreen(viewModel: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val images = remember { loadImagesFromGallery(context) }

    LazyColumn {
        items(images.size, itemContent = { index ->
            ImageItem(images[index])
        })
    }
}

@Composable
fun ImageItem(imageUri: Uri) {
    val context = LocalContext.current
    val bitmap = remember { loadBitmapFromUri(context, imageUri) } ?: return
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    )
}

fun loadImagesFromGallery(context: Context): List<Uri> {
    val images = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
        }
    }
    return images
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
    } else {
        val filePath = getFilePathFromUri(context, uri)
        ThumbnailUtils.createImageThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
    }
}

fun getFilePathFromUri(context: Context, uri: Uri): String {
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(columnIndex)
    }
    throw IllegalArgumentException("Invalid URI")
}