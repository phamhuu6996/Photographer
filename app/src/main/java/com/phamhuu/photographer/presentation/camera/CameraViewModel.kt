package com.phamhuu.photographer.presentation.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.net.Uri
import android.util.Size
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamhuu.photographer.contants.Contants
import com.phamhuu.photographer.presentation.utils.Gallery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraViewModel : ViewModel() {
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState = _cameraState.asStateFlow()
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null

    @OptIn(ExperimentalCamera2Interop::class)
    fun getCameraId(): String? {
        val cameraInfo = camera?.cameraInfo ?: return null
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        return camera2Info.cameraId
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun getSupportedResolutions(context: Context, oldsMap: Map<String, Array<Size>>?) : Map<String, Array<Size>>? {
        val cameraId = getCameraId()
        if((cameraId == null) || oldsMap?.get(cameraId) != null) {
            return null
        }
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)
        if(!sizes.isNullOrEmpty()) {
            return mapOf(cameraId to sizes)
        }
        return null
    }

    fun getResolutionsWithCameraCurrent(): Array<Size>? {
        val cameraId = getCameraId()
        val sizesMap = cameraState.value.resolutionsMap
        if((cameraId == null) || sizesMap?.get(cameraId) == null) {
            return null
        }
        val sizes = sizesMap.get(cameraId)
        return sizes
    }

    private fun resolutionStrategy(size: Size?): ResolutionStrategy {
        if (size == null)
            return ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY
        return ResolutionStrategy(
            size,
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
    }

    private fun resolutionSelector(size: Size?): ResolutionSelector {
        val ratio = size?.width?.toFloat()?.div(size.height.toFloat()) ?: 0f
        val aspectRatio = when {
            ratio >= 1.77 -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            else -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        }
        return ResolutionSelector.Builder().setResolutionStrategy(resolutionStrategy(size))
            .setAspectRatioStrategy(
                aspectRatio
            ).build()
    }

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        size: Size? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Cấu hình preview
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            // Cấu hình chụp ảnh
            imageCapture = ImageCapture.Builder().setResolutionSelector(resolutionSelector(size)).build()
            setResolution(size)

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
                    CameraSelector.Builder().requireLensFacing(cameraState.value.lensFacing).build(),
                    preview,
                    imageCapture,
                    videoCapture
                )
                cameraControl = camera?.cameraControl

                val resolutionsMap =
                    getSupportedResolutions(context, _cameraState.value.resolutionsMap)
                if (resolutionsMap != null) {
                    _cameraState.value = _cameraState.value.copy(resolutionsMap = resolutionsMap)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Không thể khởi động camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(context))

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

    fun setBrightness(brightness: Float) {
        _cameraState.value = _cameraState.value.copy(brightness = brightness)
        cameraControl?.let { control ->
            val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange ?: return
            val index = (brightness * (range.upper - range.lower)).toInt() + range.lower
            control.setExposureCompensationIndex(index)
        }    }

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
        var zoomDetector = zoomChange
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

    fun changeShowSelectResolution(value: Boolean) {
        _cameraState.value = _cameraState.value.copy(showSelectResolution = value)
    }

    fun setResolution(size: Size?) {
        _cameraState.value = _cameraState.value.copy(resolution = size)
    }
}

data class CameraState(
    val isRecording: Boolean = false,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val brightness: Float = 0.5f,
    val color: Float = 1f,
    val contrast: Float = 1f,
    val zoomState: Float = 1f,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val fileUri: Uri? = null,
    val resolutionsMap: Map<String, Array<Size>>? = null,
    val showSelectResolution: Boolean = false,
    val resolution: Size? = null
)