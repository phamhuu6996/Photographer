package com.phamhuu.photographer.presentation.camera

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.domain.model.BeautySettings
import com.phamhuu.photographer.enums.ImageFilter
import com.phamhuu.photographer.enums.RatioCamera
import com.phamhuu.photographer.enums.TimerDelay
import com.phamhuu.photographer.models.LocationInfo
import com.phamhuu.photographer.models.LocationState

data class CameraUiState(
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
    val isLoading: Boolean = false,
    val currentFilter: ImageFilter = ImageFilter.BEAUTY, // Changed from NONE to BEAUTY for always-on filter
    val beautySettings: BeautySettings = BeautySettings.default(), // New beauty settings
    val isBeautyPanelVisible: Boolean = false, // For beauty adjustment panel visibility
    
    // Location state
    val locationState: LocationState = LocationState(),
    val isLocationEnabled: Boolean = true // Default enabled
) 