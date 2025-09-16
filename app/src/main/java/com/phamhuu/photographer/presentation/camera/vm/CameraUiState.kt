package com.phamhuu.photographer.presentation.camera.vm

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.CameraSelector
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.contants.BeautySettings
import com.phamhuu.photographer.contants.ImageFilter
import com.phamhuu.photographer.contants.RatioCamera
import com.phamhuu.photographer.contants.TimerDelay
import com.phamhuu.photographer.data.model.LocationState

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
    val currentFilter: ImageFilter = ImageFilter.BEAUTY,
    val beautySettings: BeautySettings = BeautySettings.default(),
    val isBeautyPanelVisible: Boolean = false,

    val locationState: LocationState = LocationState(),
    val isLocationEnabled: Boolean = true
)
