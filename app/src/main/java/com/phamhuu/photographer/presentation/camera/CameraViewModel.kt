package com.phamhuu.photographer.presentation.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ScaleGestureDetector
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
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
    var scaleGestureDetector: ScaleGestureDetector? = null
    private var cameraControl: CameraControl? = null

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Cấu hình preview
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            // Cấu hình chụp ảnh
            imageCapture = ImageCapture.Builder().build()

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

            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Không thể khởi động camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(context))
        scaleGestureDetector = getScaleGestureDetector(context)

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
                    Toast.makeText(context, "Photo saved: ${output.savedUri}", Toast.LENGTH_SHORT)
                        .show()
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
                            Toast.makeText(
                                context,
                                "Video saved: ${event.outputResults.outputUri}",
                                Toast.LENGTH_SHORT
                            ).show()
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

    private fun getScaleGestureDetector(context: Context): ScaleGestureDetector {
       return ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {

                val maxZoom: Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
                val minZoom: Float = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                var zoomDetector = detector.scaleFactor
                if (zoomDetector > maxZoom) {
                    zoomDetector = maxZoom
                } else if (zoomDetector < minZoom) {
                    zoomDetector = minZoom
                }
                _cameraState.value = _cameraState.value.copy(zoomState = zoomDetector)
                cameraControl?.setZoomRatio(zoomDetector)
                return true
            }
        })
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

}

data class CameraState(
    val isRecording: Boolean = false,
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    val brightness: Float = 0.5f,
    val color: Float = 1f,
    val contrast: Float = 1f,
    val zoomState: Float = 1f,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val fileUri: Uri? = null
)