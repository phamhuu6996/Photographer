package com.phamhuu.photographer.presentation.camera.vm

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.phamhuu.photographer.contants.BeautySettings
import com.phamhuu.photographer.contants.Constants
import com.phamhuu.photographer.contants.ImageFilter
import com.phamhuu.photographer.contants.RatioCamera
import com.phamhuu.photographer.contants.SnackbarType
import com.phamhuu.photographer.contants.TimerDelay
import com.phamhuu.photographer.presentation.common.SnackbarManager
import com.phamhuu.photographer.services.gl.CameraGLSurfaceView
import com.phamhuu.photographer.data.repository.LocationRepository
import com.phamhuu.photographer.services.gpu.GPUPixelHelper
import com.phamhuu.photographer.data.repository.CameraRepository
import com.phamhuu.photographer.data.repository.GalleryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class CameraViewModel(
    private val cameraRepository: CameraRepository,
    private val galleryRepository: GalleryRepository,
    private val locationRepository: LocationRepository,
) : ViewModel() {
    companion object {
        const val INTERSTITIAL_AD_FREQUENCY = 5
    }
    
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()
    
    val showOnPhotos get() = uiState.value.isLocationEnabled
    val showOnVideos get() = uiState.value.isLocationEnabled
    
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var gPUPixelHelper: GPUPixelHelper? = null

    private val isProcessingImage = AtomicBoolean(false)
    private var hideZoomJob: Job? = null
    private var hideBrightnessJob: Job? = null

    @OptIn(ExperimentalCamera2Interop::class)
    fun getCameraId(): String? {
        val cameraInfo = camera?.cameraInfo ?: return null
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        return camera2Info.cameraId
    }

    fun checkLocationPermission(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        _uiState.update { 
            it.copy(locationState = it.locationState.copy(hasPermission = hasPermission))
        }

        if (hasPermission && uiState.value.isLocationEnabled) {
            startLocationUpdates()
        }
    }

    fun onLocationPermissionGranted() {
        _uiState.update { 
            it.copy(locationState = it.locationState.copy(hasPermission = true))
        }
        if (uiState.value.isLocationEnabled) {
            startLocationUpdates()
        }
    }

    fun toggleLocationEnabled() {
        val newState = !uiState.value.isLocationEnabled
        _uiState.update { it.copy(isLocationEnabled = newState) }
        
        if (newState && uiState.value.locationState.hasPermission) {
            startLocationUpdates()
        } else {
            stopLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        if (!uiState.value.locationState.hasPermission) return

        _uiState.update { 
            it.copy(locationState = it.locationState.copy(isLoading = true, error = null))
        }

        viewModelScope.launch {
            try {
                val lastKnown = locationRepository.getLastKnownLocation()
                if (lastKnown != null) {
                    _uiState.update { 
                        it.copy(locationState = it.locationState.copy(
                            locationInfo = lastKnown,
                            isLoading = false
                        ))
                    }
                }

                locationRepository.getCurrentLocation()
                    .catch { error ->
                        _uiState.update { 
                            it.copy(locationState = it.locationState.copy(
                                isLoading = false,
                                error = error.message ?: "Unknown location error"
                            ))
                        }
                    }
                    .collect { locationInfo ->
                        _uiState.update { 
                            it.copy(locationState = it.locationState.copy(
                                locationInfo = locationInfo,
                                isLoading = false,
                                error = null
                            ))
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(locationState = it.locationState.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to get location"
                    ))
                }
            }
        }
    }

    private fun stopLocationUpdates() {
        locationRepository.stopLocationUpdates()
        _uiState.update { 
            it.copy(locationState = it.locationState.copy(isLoading = false))
        }
    }

    private fun resolutionSelector(ratio: RatioCamera): ResolutionSelector {
        return ResolutionSelector.Builder()
            .setAspectRatioStrategy(ratio.ratio).build()
    }

    fun  setFilterHelper(glSurfaceView: CameraGLSurfaceView, context: Context) {
        Log.d("CameraViewModel", "Setting up FilterHelper with GLSurfaceView and FilterRenderer")
        this.gPUPixelHelper = GPUPixelHelper().apply {
            viewModelScope.launch(Dispatchers.Default) {
                initGpuPixel(context, glSurfaceView)
            }
        }
    }

    private fun handleImageAnalyzerFrame(imageProxy: ImageProxy) {
        try {
            if(!isProcessingImage.compareAndSet(false, true)) {
                return
            }
            gPUPixelHelper?.handleImageAnalytic(
                imageProxy,
                uiState.value.lensFacing == CameraSelector.LENS_FACING_FRONT,
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
        isProcessingImage.set(false)
    }

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        ratioCamera: RatioCamera? = null,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var setRatio = false

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val previewBuilder = Preview.Builder()
            val imageCaptureBuilder = ImageCapture.Builder()
            val imageAnalyzerBuilder = ImageAnalysis.Builder()

            val currentRatio = ratioCamera ?: uiState.value.ratioCamera

            // Always set resolution selector for both photo and video mode
            val resolutionSelect = resolutionSelector(currentRatio)
            previewBuilder.setResolutionSelector(resolutionSelect)
            imageAnalyzerBuilder.setResolutionSelector(resolutionSelect)

            // Only set resolution selector for ImageCapture when in photo mode
            if (uiState.value.setupCapture) {
                imageCaptureBuilder.setResolutionSelector(resolutionSelect)
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
                        if (ratioCamera != null && !setRatio) {
                            setRatio = true
                            _uiState.value = _uiState.value.copy(ratioCamera = ratioCamera)
                        }
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

                applyBeautySettingsToFilter(_uiState.value.beautySettings)

            } catch (e: Exception) {
                updateError("Không thể khởi động camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setImageFilter(filter: ImageFilter) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    currentFilter = filter,
                    isLoading = true
                )

                delay(300)
                _uiState.value = _uiState.value.copy(isLoading = false)

                SnackbarManager.show(
                    message = "Filter ${filter.displayName} applied successfully",
                    type = SnackbarType.SUCCESS
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
                updateError("Failed to apply filter: ${e.message}")
            }
        }
    }

    fun takePhoto(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val hasFilter = true
                delay(_uiState.value.timerDelay.millisecond)
                capturePhoto(context, hasFilter)
            } catch (e: Exception) {
                updateError("Photo capture failed: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    private suspend fun capturePhoto(context: Context, useFilter: Boolean) {
        val photoFile = cameraRepository.createImageFile()
        
        if (useFilter) {
            captureFromGLSurface(photoFile)
        } else {
            captureFromCamera(photoFile, context)
        }
    }
    
    private suspend fun captureFromGLSurface(photoFile: File) {
        val bitmap = gPUPixelHelper?.captureFilteredBitmap() 
            ?: run {
                updateError("Failed to capture filtered bitmap")
                return
            }
            
        saveBitmapToFile(bitmap, photoFile)
        saveToGallery(photoFile)
    }
    
    private suspend fun saveBitmapToFile(bitmap: Bitmap, file: File) = withContext(Dispatchers.IO) {
        val location = uiState.value.locationState.locationInfo
        val finalBitmap = if (showOnPhotos && location != null) {
            cameraRepository.addAddressCapture(bitmap, location.address)
        } else {
            bitmap
        }
        
        FileOutputStream(file).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        finalBitmap.recycle()
        if (finalBitmap != bitmap) {
            bitmap.recycle()
        }
    }

    private fun captureFromCamera(photoFile: File, context: Context) {
        val imageCapture = imageCapture ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    viewModelScope.launch {
                        saveToGallery(photoFile)
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    updateError("Photo capture failed: ${exc.message}")
                }
            }
        )
    }
    
    private suspend fun saveToGallery(photoFile: File) {
        val uri = cameraRepository.saveImageToGallery(photoFile)
        if (uri != null) {
            incrementMediaCount()
            _uiState.value = _uiState.value.copy(fileUri = uri)
        } else {
            updateError("Failed to save photo to gallery")
        }
    }
    
    private fun incrementMediaCount() {
        val newCount = _uiState.value.mediaCount + 1
        val shouldShowAd = newCount % INTERSTITIAL_AD_FREQUENCY == 0
        _uiState.update { 
            it.copy(
                mediaCount = newCount,
                shouldShowInterstitialAd = shouldShowAd
            )
        }
    }
    
    fun onInterstitialAdShown() {
        _uiState.update { it.copy(shouldShowInterstitialAd = false) }
    }

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

    fun setRatioCamera(ratioCamera: RatioCamera, context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        startCamera(context, lifecycleOwner, previewView, ratioCamera)
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

    fun checkGalleryContent() {
        viewModelScope.launch {
            val uri = galleryRepository.getFirstImageOrVideo()
            if (uri != null) {
                _uiState.value = _uiState.value.copy(fileUri = uri)
            }
        }
    }

    fun changeCaptureOrVideo(value: Boolean, context: Context, lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        _uiState.value = _uiState.value.copy(setupCapture = value)
        startCamera(context, lifecycleOwner, previewView)
    }

    fun toggleBeautyPanel() {
        _uiState.value = _uiState.value.copy(
            isBeautyPanelVisible = !_uiState.value.isBeautyPanelVisible
        )
    }
    
    fun updateBeautySettings(newSettings: BeautySettings) {
        val validatedSettings = newSettings.validate()
        _uiState.value = _uiState.value.copy(beautySettings = validatedSettings)
        applyBeautySettingsToFilter(validatedSettings)
    }
    
    fun updateSkinSmoothing(value: Float) {
        val currentSettings = _uiState.value.beautySettings
        updateBeautySettings(currentSettings.copy(skinSmoothing = value))
    }
    
    fun updateWhiteness(value: Float) {
        val currentSettings = _uiState.value.beautySettings
        updateBeautySettings(currentSettings.copy(whiteness = value))
    }
    
    fun updateThinFace(value: Float) {
        val currentSettings = _uiState.value.beautySettings
        updateBeautySettings(currentSettings.copy(thinFace = value))
    }
    
    fun updateBigEye(value: Float) {
        val currentSettings = _uiState.value.beautySettings
        updateBeautySettings(currentSettings.copy(bigEye = value))
    }
    
    fun updateBlendLevel(value: Float) {
        val currentSettings = _uiState.value.beautySettings
        updateBeautySettings(currentSettings.copy(blendLevel = value))
    }
    
    fun resetBeautySettings() {
        updateBeautySettings(BeautySettings.default())
    }
    
    private fun applyBeautySettingsToFilter(settings: BeautySettings) {
        gPUPixelHelper?.updateBeautySettings(settings)
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
        if(zoomDetector == uiState.value.zoomState) return
        _uiState.value = _uiState.value.copy(zoomState = zoomDetector, isZoomVisible = true)
        cameraControl?.setZoomRatio(zoomDetector)
        hideZoomJob?.cancel()
        hideZoomJob = viewModelScope.launch {
            delay(Constants.HIDE_SYSTEM_UI_DELAY)
            _uiState.value = _uiState.value.copy(isZoomVisible = false)
        }
    }

    fun changeCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        val lensFacing = if (uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.value = _uiState.value.copy(lensFacing = lensFacing)
        startCamera(context, lifecycleOwner, previewView)
    }

    private fun changeShowBrightness(value: Float) {
        if(_uiState.value.isBrightnessVisible) return
        _uiState.value = _uiState.value.copy(offsetY = value)
        if (uiState.value.offsetY <= Constants.PAN_TOP) {
            _uiState.value = _uiState.value.copy(isBrightnessVisible = true)
        }
        hideBrightnessJob?.cancel()
        hideBrightnessJob = viewModelScope.launch {
            delay(Constants.HIDE_SYSTEM_UI_DELAY)
            _uiState.value = _uiState.value.copy(offsetY = 0f, isBrightnessVisible = false)
        }
    }

    fun getCameraPointerInput(centroid: Offset, pan: Offset, zoomChange: Float, rotation: Float) {
        changeZoom(zoomChange)
        changeShowBrightness(pan.y)
    }
    override fun onCleared() {
        super.onCleared()
        gPUPixelHelper?.onDestroy()
        stopLocationUpdates()
    }

    private fun updateError(message: String) {
        SnackbarManager.show(
            message = message,
            type = SnackbarType.FAIL
        )
        _uiState.value = _uiState.value.copy(isLoading = false)
    }

    fun clearError() {
    }
    
    fun startRecording(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val videoFile = cameraRepository.createVideoFile()
                startFilteredRecording(videoFile)
                
            } catch (e: Exception) {
                updateError("Failed to start recording: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
                Log.e("CameraViewModel", "Start recording error: ${e.message}")
            }
        }
    }

    private fun startFilteredRecording(videoFile: File) {
        val gpuPixelHelper = this.gPUPixelHelper
        val glSurfaceView = gpuPixelHelper?.glSurfaceView
        if( glSurfaceView == null) {
            updateError("GL Surface View not initialized")
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        val textOverlay = {
            uiState.value.locationState.locationInfo?.address
        }
        glSurfaceView.startFilteredVideoRecording(videoFile, textOverlay) { success ->
            _uiState.value = _uiState.value.copy(
                isRecording = success,
                isLoading = false
            )
            if (!success) {
                updateError("Failed to start filtered recording")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startNormalRecording(videoFile: File, context: Context) {
        val videoCapture = videoCapture ?: run {
            updateError("Video capture not initialized")
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }

        val outputOptions = androidx.camera.video.FileOutputOptions.Builder(videoFile).build()
        
        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is androidx.camera.video.VideoRecordEvent.Start -> {
                        _uiState.value = _uiState.value.copy(
                            isRecording = true,
                            isLoading = false
                        )
                        Log.d("CameraViewModel", "Normal video recording started")
                    }
                    is androidx.camera.video.VideoRecordEvent.Finalize -> {
                        _uiState.value = _uiState.value.copy(isRecording = false)
                        if (!recordEvent.hasError()) {
                            viewModelScope.launch {
                                saveVideoToGallery(videoFile)
                            }
                            Log.d("CameraViewModel", "Video recording completed successfully")
                        } else {
                            updateError("Recording error: ${recordEvent.error}")
                            Log.e("CameraViewModel", "Recording error: ${recordEvent.error}")
                        }
                    }
                }
            }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                val hasFilter = true 
                
                gPUPixelHelper?.let { helper ->
                    helper.glSurfaceView?.stopFilteredVideoRecording { success: Boolean, videoFile: File? ->
                        _uiState.value = _uiState.value.copy(isRecording = false)
                        
                        if (success && videoFile != null) {
                            viewModelScope.launch {
                                saveVideoToGallery(videoFile)
                            }
                            Log.d("CameraViewModel", "Filtered recording stopped successfully")
                        } else {
                            updateError("Failed to stop filtered recording")
                            Log.e("CameraViewModel", "Filtered recording stop failed")
                        }
                    } ?: run {
                        _uiState.value = _uiState.value.copy(isRecording = false)
                        updateError("GL Surface View not initialized")
                    }
                } ?: run {
                    _uiState.value = _uiState.value.copy(isRecording = false)
                    updateError("Filter helper not initialized")
                }
                
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error stopping recording: ${e.message}")
                updateError("Failed to stop recording: ${e.message}")
                _uiState.value = _uiState.value.copy(isRecording = false)
            }
        }
    }
    
    private suspend fun saveVideoToGallery(videoFile: File) {
        try {
            val uri = cameraRepository.saveVideoToGallery(videoFile)
            if (uri != null) {
                incrementMediaCount()
                _uiState.value = _uiState.value.copy(fileUri = uri)
                Log.d("CameraViewModel", "Video saved to gallery successfully")
            } else {
                updateError("Failed to save video to gallery")
                Log.e("CameraViewModel", "Failed to save video to gallery")
            }
        } catch (e: Exception) {
            updateError("Error saving video: ${e.message}")
            Log.e("CameraViewModel", "Error saving video: ${e.message}")
        }
    }
}
