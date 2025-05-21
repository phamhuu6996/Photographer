package com.phamhuu.photographer.presentation.camera

import Manager3DHelper
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Size
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.contants.Contants
import com.phamhuu.photographer.enums.RatioCamera
import com.phamhuu.photographer.enums.TimerDelay
import com.phamhuu.photographer.enums.TypeModel3D
import com.phamhuu.photographer.presentation.utils.Gallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraViewModel(
    private val faceLandmarkerHelper: FaceLandmarkerHelper,
    private val manager3DHelper: Manager3DHelper
) : ViewModel() {
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState = _cameraState.asStateFlow()
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null

    init {
        listenMediaPipe()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun getCameraId(): String? {
        val cameraInfo = camera?.cameraInfo ?: return null
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        return camera2Info.cameraId
    }

    private fun resolutionSelector(ratio: RatioCamera): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                ratio.ratio
            ).build()
    }

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Cấu hình preview
            val previewBuilder = Preview.Builder()
            val imageCaptureBuilder = ImageCapture.Builder()
            val imageAnalyzerBuilder = ImageAnalysis.Builder()
            if (cameraState.value.setupCapture) {
                val resolutionSelect = resolutionSelector(cameraState.value.ratioCamera)
                previewBuilder.setResolutionSelector(resolutionSelect)
                imageCaptureBuilder.setResolutionSelector(resolutionSelect)
                imageAnalyzerBuilder.setResolutionSelector(resolutionSelect)
            }

            val preview =
                previewBuilder.build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            // Cấu hình chụp ảnh
            imageCapture = imageCaptureBuilder.build()

            imageAnalyzer =
                imageAnalyzerBuilder
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        it.setAnalyzer(Dispatchers.Default.asExecutor()) { image ->
                            detectFace(image)
                        }
                    }

            // Cấu hình quay video
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                // Kết nối camera với lifecycle
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(cameraState.value.lensFacing)
                        .build(),
                    preview,
                    imageCapture,
                    videoCapture,
                    imageAnalyzer,
                )
                cameraControl = camera?.cameraControl

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Không thể khởi động camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(context))

        selectModel3D(context, "models/glasses.glb")

    }

    fun setFlashMode() {
        val newFlashMode = when (cameraState.value.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        _cameraState.value = _cameraState.value.copy(flashMode = newFlashMode)
        imageCapture?.flashMode = newFlashMode
        cameraControl?.enableTorch(newFlashMode == ImageCapture.FLASH_MODE_ON)
    }

    fun setRatioCamera(ratioCamera: RatioCamera, context: Context,
                       lifecycleOwner: LifecycleOwner,
                       previewView: PreviewView,) {
        _cameraState.value = _cameraState.value.copy(ratioCamera = ratioCamera)
        startCamera(context, lifecycleOwner, previewView)
    }

    fun setTimerDelay(timerDelay: TimerDelay) {
        _cameraState.value = _cameraState.value.copy(timerDelay = timerDelay)
    }

    fun setBrightness(brightness: Float) {
        _cameraState.value = _cameraState.value.copy(brightness = brightness)
        cameraControl?.let { control ->
            val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange ?: return
            val index = (brightness * (range.upper - range.lower)).toInt() + range.lower
            control.setExposureCompensationIndex(index)
        }
    }

    fun changeShowBrightness(isBrightnessVisible: Boolean) {
        _cameraState.value = _cameraState.value.copy(isBrightnessVisible = isBrightnessVisible)
    }

    fun takePhoto(context: Context) {
        val imageCapture = imageCapture ?: return
        val photoFile = createFile(context, Contants.EXT_IMG, header = Contants.IMG_PREFIX)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Gallery.saveImageToGallery(context, photoFile)
                    if (uri != null) {
                        _cameraState.value = _cameraState.value.copy(fileUri = uri)
                    }

                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        context,
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    // Checked permission before start camera screen
    @SuppressLint("MissingPermission")
    fun startRecording(context: Context) {
        val videoCapture = videoCapture ?: return
        val videoFile = createFile(context, Contants.EXT_VID, header = Contants.VID_PREFIX)

        val fileOutputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(context, fileOutputOptions)
            .apply {
                withAudioEnabled()
            }
            .start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _cameraState.value = _cameraState.value.copy(isRecording = true)
                    }

                    is VideoRecordEvent.Finalize -> {
                        _cameraState.value = _cameraState.value.copy(isRecording = false)
                        if (!event.hasError()) {
                            val uri = Gallery.saveVideoToGallery(context, videoFile)
                            if (uri != null) {
                                _cameraState.value = _cameraState.value.copy(fileUri = uri)
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "Video capture failed: ${event.error}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
    }

    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    private fun createFile(context: Context, extension: String, header: String): File {
        val timeStamp =
            SimpleDateFormat(Contants.DATE_TIME_FORMAT, Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(null)
        return File(storageDir, "${header}${timeStamp}.$extension")
    }

    fun changeZoom(zoomChange: Float) {
        val maxZoom: Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
        val minZoom: Float = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
        var zoomDetector = zoomChange * cameraState.value.zoomState
        if (zoomDetector > maxZoom) {
            zoomDetector = maxZoom
        } else if (zoomDetector < minZoom) {
            zoomDetector = minZoom
        }
        _cameraState.value = _cameraState.value.copy(zoomState = zoomDetector)
        cameraControl?.setZoomRatio(zoomDetector)
    }

    fun changeCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val lensFacing = if (cameraState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _cameraState.value = _cameraState.value.copy(lensFacing = lensFacing)
        startCamera(context, lifecycleOwner, previewView)
    }

    fun checkGalleryContent(context: Context) {
        viewModelScope.launch {
            val uri = Gallery.getFirstImageOrVideo(context)
            _cameraState.value = _cameraState.value.copy(fileUri = uri)
        }
    }

    fun changeCaptureOrVideo(
        value: Boolean,
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        _cameraState.value = _cameraState.value.copy(setupCapture = value)
        startCamera(context, lifecycleOwner, previewView)
    }

    private fun changePan(
        value: Float,
    ) {
        _cameraState.value = _cameraState.value.copy(offsetY = value)
    }

    fun getCameraPointerInput(centroid: Offset, pan: Offset, zoomChange: Float, rotation: Float) {
        changeZoom(zoomChange)

        // Change panned
        changePan(pan.y)
        // Show UI if panned up by 50 pixels
        if (cameraState.value.offsetY <= -50f) {
            changeShowBrightness(true)
            changePan(0f)
        }

        // Hide UI if panned down by 50 pixels
        if (cameraState.value.offsetY >= 50f) {
            changeShowBrightness(false)
            changePan(0f)
        }
    }

    private fun detectFace(imageProxy: ImageProxy) {
        if (faceLandmarkerHelper.isClose()) {
            return
        }
        try {
            faceLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = cameraState.value.lensFacing == CameraSelector.LENS_FACING_FRONT
            )
        } catch (e: Exception) {
        }
    }

    fun setupMediaPipe() {
        viewModelScope.launch {
            faceLandmarkerHelper.setupFaceLandmarker()
        }
    }

    fun onresume(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        if (faceLandmarkerHelper.isClose()) {
            startCamera(context, lifecycleOwner, previewView)
            faceLandmarkerHelper.setupFaceLandmarker()
        }
    }

    fun onPause() {
        faceLandmarkerHelper.clearFaceLandmarker()
    }

    private fun listenMediaPipe() {
        viewModelScope.launch {
            faceLandmarkerHelper.resultFlow.collect { state ->
                if (state?.result == null) {
                    manager3DHelper.updateModelWithLandmark(null)
                    _cameraState.value = _cameraState.value.copy(landmarkResult = null)
                } else {
                    manager3DHelper.updateModelWithLandmark(state.result)
                    _cameraState.value = _cameraState.value.copy(landmarkResult = state)
                }
            }
        }
    }

    private fun selectModel3D(
        context: Context,
        path: String,
        typeModel3D: TypeModel3D = TypeModel3D.GLASSES
    ) {
        manager3DHelper.selectModel3D(path, context, typeModel3D)
    }
}

data class CameraState(
    val isRecording: Boolean = false,
    val setupCapture: Boolean = true,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val brightness: Float = 0.5f,
    val isBrightnessVisible: Boolean = false,
    val offsetY: Float = 0f,
    val color: Float = 1f,
    val contrast: Float = 1f,
    val zoomState: Float = 1f,
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val fileUri: Uri? = null,
    val ratioCamera: RatioCamera = RatioCamera.RATIO_3_4,
    val timerDelay: TimerDelay = TimerDelay.OFF,
    val landmarkResult: FaceLandmarkerHelper.ResultBundle? = null,
)