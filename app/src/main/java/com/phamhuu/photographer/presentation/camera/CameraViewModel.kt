package com.phamhuu.photographer.presentation.camera

import Manager3DHelper
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.Log
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
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.domain.usecase.GetFirstGalleryItemUseCase
import com.phamhuu.photographer.domain.usecase.SavePhotoUseCase
import com.phamhuu.photographer.domain.usecase.TakePhotoUseCase
import com.phamhuu.photographer.enums.ImageFilter
import com.phamhuu.photographer.enums.RatioCamera
import com.phamhuu.photographer.enums.TimerDelay
import com.phamhuu.photographer.enums.TypeModel3D
import com.phamhuu.photographer.presentation.utils.CameraGLSurfaceView
import com.phamhuu.photographer.presentation.utils.FilterRenderer
import com.phamhuu.photographer.presentation.utils.GPUPixelHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class CameraViewModel(
    private val faceLandmarkerHelper: FaceLandmarkerHelper,
    private val manager3DHelper: Manager3DHelper,
    private val takePhotoUseCase: TakePhotoUseCase,
    private val savePhotoUseCase: SavePhotoUseCase,
    private val getFirstGalleryItemUseCase: GetFirstGalleryItemUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()
    
    // Camera related variables
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var gPUPixelHelper: GPUPixelHelper? = null

    private val isProcessingImage = AtomicBoolean(false)

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
            .setAspectRatioStrategy(ratio.ratio).build()
    }

    fun  setFilterHelper(glSurfaceView: CameraGLSurfaceView, context: Context) {
        Log.d("CameraViewModel", "Setting up FilterHelper with GLSurfaceView and FilterRenderer")
        this.gPUPixelHelper = GPUPixelHelper().apply {
            initGpuPixel(context, glSurfaceView)
        }
    }

    // ✅ Thread-safe ImageProxy processing
    private fun handleImageAnalyzerFrame(imageProxy: ImageProxy) {
        try {
            if(!isProcessingImage.compareAndSet(false, true)) {
                // If already processing, close the ImageProxy to prevent memory leaks
                return
            }
            if (uiState.value.currentFilter != ImageFilter.NONE) {
                gPUPixelHelper?.handleImageAnalytic(
                    imageProxy,
                    uiState.value.lensFacing == CameraSelector.LENS_FACING_FRONT,
                )
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        isProcessingImage.set(false)
    }

    // Legacy method cho normal PreviewView (backward compatibility)
    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val previewBuilder = Preview.Builder()
            val imageCaptureBuilder = ImageCapture.Builder()
            val imageAnalyzerBuilder = ImageAnalysis.Builder()
            
            if (uiState.value.setupCapture) {
                val resolutionSelect = resolutionSelector(uiState.value.ratioCamera)
                previewBuilder.setResolutionSelector(resolutionSelect)
                imageCaptureBuilder.setResolutionSelector(resolutionSelect)
                imageAnalyzerBuilder.setResolutionSelector(resolutionSelect)
            }

            val preview = previewBuilder.build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            imageCapture = imageCaptureBuilder.build()

            imageAnalyzer = imageAnalyzerBuilder
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(Dispatchers.Default.asExecutor()) { image ->
                        handleImageAnalyzerFrame(image)
                        image.close()
                    }
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.Builder().requireLensFacing(uiState.value.lensFacing).build(),
                    preview,
                    imageCapture,
                    videoCapture,
                    imageAnalyzer,
                )
                cameraControl = camera?.cameraControl

            } catch (e: Exception) {
                updateError("Không thể khởi động camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // ================ FILTER MANAGEMENT ================
    
    fun setImageFilter(filter: ImageFilter) {
        viewModelScope.launch {
            try {
                // ✅ Immediate UI feedback with loading state
                _uiState.value = _uiState.value.copy(
                    currentFilter = filter,
                    isLoading = true
                )

                println("🔥 CameraViewModel: Setting filter to ${filter.displayName}")

                // ✅ Apply filter to GLSurfaceView if available
//                glSurfaceView?.setImageFilter(filter)

                // ✅ Shorter processing delay for better UX
                delay(300)

                // ✅ Update loading state
                _uiState.value = _uiState.value.copy(isLoading = false)

                println("🔥 CameraViewModel: Filter ${filter.displayName} applied successfully")

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    currentFilter = ImageFilter.NONE // Fallback to no filter on error
                )
                updateError("Failed to apply filter: ${e.message}")
                println("❌ CameraViewModel: Filter application failed: ${e.message}")
            }
        }
    }

    // ================ PHOTO CAPTURE ================
    
    fun takePhoto(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                if (uiState.value.currentFilter != ImageFilter.NONE) {
                    // Capture filtered image từ GLSurfaceView
                    captureFilteredPhoto(context)
                } else {
                    // Use normal camera capture
                    val photoFileResult = takePhotoUseCase()
                    if (photoFileResult.isSuccess) {
                        val photoFile = photoFileResult.getOrThrow()
                        captureImageToFile(context, photoFile)
                    } else {
                        updateError("Failed to create photo file")
                    }
                }
            } catch (e: Exception) {
                updateError("Photo capture failed: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    private suspend fun captureFilteredPhoto(context: Context) {
        val photoFileResult = takePhotoUseCase()
        if (photoFileResult.isFailure) {
            updateError("Failed to create photo file")
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        
        val photoFile = photoFileResult.getOrThrow()
        
        // Capture filtered frame từ GLSurfaceView
//        glSurfaceView?.captureFilteredImage { bitmap ->
//            viewModelScope.launch {
//                try {
//                    // Save filtered bitmap to file
//                    saveBitmapToFile(bitmap, photoFile)
//
//                    // Save to gallery
//                    val saveResult = savePhotoUseCase(photoFile)
//                    if (saveResult.isSuccess) {
//                        val uri = saveResult.getOrThrow()
//                        _uiState.value = _uiState.value.copy(fileUri = uri, isLoading = false)
//                    } else {
//                        updateError("Failed to save photo to gallery")
//                    }
//                } catch (e: Exception) {
//                    updateError("Failed to save filtered photo: ${e.message}")
//                } finally {
//                    _uiState.value = _uiState.value.copy(isLoading = false)
//                }
//            }
//        }
    }
    
    private suspend fun saveBitmapToFile(bitmap: Bitmap, file: File) = withContext(Dispatchers.IO) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
    }

    private fun captureImageToFile(context: Context, photoFile: File) {
        val imageCapture = imageCapture ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        val saveResult = savePhotoUseCase(photoFile)
                        if (saveResult.isSuccess) {
                            val uri = saveResult.getOrThrow()
                            _uiState.value = _uiState.value.copy(fileUri = uri, isLoading = false)
                        }
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    updateError("Photo capture failed: ${exc.message}")
                }
            }
        )
    }

    // ================ OTHER CAMERA FUNCTIONS ================

    fun setFlashMode() {
        val newFlashMode = when (uiState.value.flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }
        _uiState.value = _uiState.value.copy(flashMode = newFlashMode)
        imageCapture?.flashMode = newFlashMode
        cameraControl?.enableTorch(newFlashMode == ImageCapture.FLASH_MODE_ON)
    }

    fun setRatioCamera(ratioCamera: RatioCamera, context: Context, lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        _uiState.value = _uiState.value.copy(ratioCamera = ratioCamera)
        startCamera(context, lifecycleOwner, previewView)
    }

    fun setTimerDelay(timerDelay: TimerDelay) {
        _uiState.value = _uiState.value.copy(timerDelay = timerDelay)
    }

    fun setBrightness(brightness: Float) {
        _uiState.value = _uiState.value.copy(brightness = brightness)
        cameraControl?.let { control ->
            val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange ?: return
            val index = (brightness * (range.upper - range.lower)).toInt() + range.lower
            control.setExposureCompensationIndex(index)
        }
    }

    fun changeShowBrightness(isBrightnessVisible: Boolean) {
        _uiState.value = _uiState.value.copy(isBrightnessVisible = isBrightnessVisible)
    }

    fun checkGalleryContent() {
        viewModelScope.launch {
            val result = getFirstGalleryItemUseCase()
            if (result.isSuccess) {
                val uri = result.getOrThrow()
                _uiState.value = _uiState.value.copy(fileUri = uri)
            }
        }
    }

    fun changeCaptureOrVideo(value: Boolean, context: Context, lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        _uiState.value = _uiState.value.copy(setupCapture = value)
        startCamera(context, lifecycleOwner, previewView)
    }

    fun changeZoom(zoomChange: Float) {
        val maxZoom: Float = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
        val minZoom: Float = camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
        var zoomDetector = zoomChange * uiState.value.zoomState
        if (zoomDetector > maxZoom) {
            zoomDetector = maxZoom
        } else if (zoomDetector < minZoom) {
            zoomDetector = minZoom
        }
        _uiState.value = _uiState.value.copy(zoomState = zoomDetector)
        cameraControl?.setZoomRatio(zoomDetector)
    }

    fun changeCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        val lensFacing = if (uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.value = _uiState.value.copy(lensFacing = lensFacing)
//        glSurfaceView?.changeCamera(lensFacing == CameraSelector.LENS_FACING_FRONT)
        startCamera(context, lifecycleOwner, previewView)
    }

    private fun changePan(value: Float) {
        _uiState.value = _uiState.value.copy(offsetY = value)
    }

    fun getCameraPointerInput(centroid: Offset, pan: Offset, zoomChange: Float, rotation: Float) {
        changeZoom(zoomChange)
        changePan(pan.y)
        
        if (uiState.value.offsetY <= -50f) {
            changeShowBrightness(true)
            changePan(0f)
        }

        if (uiState.value.offsetY >= 50f) {
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
                isFrontCamera = uiState.value.lensFacing == CameraSelector.LENS_FACING_FRONT
            )
        } catch (e: Exception) {
            // Handle silently
        }
    }

    fun setupMediaPipe() {
        viewModelScope.launch {
            faceLandmarkerHelper.setupFaceLandmarker()
        }
    }

    fun onResume(context: Context, lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        if (faceLandmarkerHelper.isClose()) {
            startCamera(context, lifecycleOwner, previewView)
            faceLandmarkerHelper.setupFaceLandmarker()
        }
    }

    fun onPause() {
        faceLandmarkerHelper.clearFaceLandmarker()
    }

    override fun onCleared() {
        super.onCleared()
//        glSurfaceView?.release()
//        glSurfaceView = null
        gPUPixelHelper?.onDestroy();
    }

    private fun listenMediaPipe() {
        viewModelScope.launch {
            faceLandmarkerHelper.resultFlow.collect { state ->
                if (state?.result == null) {
                    manager3DHelper.updateModelWithLandmark(null)
                    _uiState.value = _uiState.value.copy(landmarkResult = null)
                } else {
                    manager3DHelper.updateModelWithLandmark(state.result)
                    _uiState.value = _uiState.value.copy(landmarkResult = state)
                }
            }
        }
    }

    fun selectModel3D(context: Context, typeModel3D: TypeModel3D = TypeModel3D.GLASSES) {
        manager3DHelper.selectModel3D(context, typeModel3D)
    }

    private fun updateError(message: String) {
        _uiState.value = _uiState.value.copy(error = message, isLoading = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // TODO: Implement video recording methods with use cases
    fun startRecording(context: Context) {
        // Implementation using RecordVideoUseCase
    }

    fun stopRecording() {
        recording?.stop()
        _uiState.value = _uiState.value.copy(isRecording = false)
    }
}