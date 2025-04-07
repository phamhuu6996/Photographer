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
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.common.CameraControls
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.common.InitCameraPermission
import com.phamhuu.photographer.presentation.common.ResolutionControl
import com.phamhuu.photographer.presentation.common.SlideVertically
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

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
        FaceLandmarkOverlay(
            modifier = Modifier.fillMaxSize(),
            faceLandmarkerResult = cameraState.value.landmarkResult?.result,
            imageWidth = cameraState.value.landmarkResult?.inputImageWidth ?: 1,
            imageHeight = cameraState.value.landmarkResult?.inputImageHeight ?: 1,
        )
    }
}

@Composable
fun FaceLandmarkOverlay(
    modifier: Modifier = Modifier,
    faceLandmarkerResult: FaceLandmarkerResult?,
    imageWidth: Int = 1,
    imageHeight: Int = 1,
) {
    // Khởi tạo các đối tượng Paint
    val linePaint = remember { Paint() }
    val pointPaint = remember { Paint() }

    // Khởi tạo cấu hình cho Paint
    LaunchedEffect(Unit) {
        initPaints(linePaint, pointPaint)
    }

    // Canvas composable để vẽ các landmark và connector
    Canvas(modifier = modifier.fillMaxSize()) {
        // Nếu không có kết quả hoặc danh sách landmarks rỗng thì không vẽ gì (tương đương với hàm clear)
        if (faceLandmarkerResult?.faceLandmarks().isNullOrEmpty()) {
            return@Canvas
        }

        // Tính toán scaleFactor dựa trên kích thước canvas và kích thước hình ảnh
        val scaleFactor =
            min(size.width * 1f / imageWidth.toFloat(), size.height * 1f / imageHeight.toFloat())

        // Tính toán kích thước hình ảnh sau khi scale và tính toán offset để căn giữa
        val scaledImageWidth = imageWidth * scaleFactor
        val scaledImageHeight = imageHeight * scaleFactor
        val offsetX = (size.width - scaledImageWidth) / 2f
        val offsetY = (size.height - scaledImageHeight) / 2f

        // Vẽ landmarks và connectors cho từng khuôn mặt
        faceLandmarkerResult?.faceLandmarks()?.forEach { landmarks ->
            drawFaceLandmarks(
                faceLandmarks = landmarks,
                offsetX = offsetX,
                offsetY = offsetY,
                scaleFactor = scaleFactor,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                pointPaint = pointPaint
            )
            drawFaceConnectors(
                faceLandmarks = landmarks,
                offsetX = offsetX,
                offsetY = offsetY,
                scaleFactor = scaleFactor,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                linePaint = linePaint
            )
        }
    }
}

private fun initPaints(linePaint: Paint, pointPaint: Paint) {
    // Bạn có thể thay đổi màu sắc theo ý thích
    linePaint.color = Color.Blue
    linePaint.strokeWidth = 8f
    linePaint.style = PaintingStyle.Stroke

    pointPaint.color = Color.Yellow
    pointPaint.strokeWidth = 4f
    pointPaint.style = PaintingStyle.Fill
}

private fun DrawScope.drawFaceLandmarks(
    faceLandmarks: List<NormalizedLandmark>,
    offsetX: Float,
    offsetY: Float,
    scaleFactor: Float,
    imageWidth: Int,
    imageHeight: Int,
    pointPaint: Paint
) {
    faceLandmarks.forEach { landmark ->
        val x = landmark.x() * imageWidth * scaleFactor + offsetX
        val y = landmark.y() * imageHeight * scaleFactor + offsetY
        drawCircle(color = pointPaint.color, center = Offset(x, y), radius = pointPaint.strokeWidth)
    }
}

private fun DrawScope.drawFaceConnectors(
    faceLandmarks: List<NormalizedLandmark>,
    offsetX: Float,
    offsetY: Float,
    scaleFactor: Float,
    imageWidth: Int,
    imageHeight: Int,
    linePaint: Paint
) {
    FaceLandmarker.FACE_LANDMARKS_CONNECTORS.filterNotNull().forEach { connector ->
        val startLandmark = faceLandmarks.getOrNull(connector.start())
        val endLandmark = faceLandmarks.getOrNull(connector.end())
        if (startLandmark != null && endLandmark != null) {
            val startX = startLandmark.x() * imageWidth * scaleFactor + offsetX
            val startY = startLandmark.y() * imageHeight * scaleFactor + offsetY
            val endX = endLandmark.x() * imageWidth * scaleFactor + offsetX
            val endY = endLandmark.y() * imageHeight * scaleFactor + offsetY

            drawLine(
                color = linePaint.color,
                strokeWidth = linePaint.strokeWidth,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
            )
        }
    }
}
