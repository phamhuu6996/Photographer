package com.phamhuu.photographer.presentation.utils

import android.R.attr.height
import android.R.attr.width
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.camera.core.ImageProxy
import com.phamhuu.photographer.domain.model.BeautySettings
import com.pixpark.gpupixel.FaceDetector
import com.pixpark.gpupixel.GPUPixel
import com.pixpark.gpupixel.GPUPixelFilter
import com.pixpark.gpupixel.GPUPixelSinkRawData
import com.pixpark.gpupixel.GPUPixelSourceRawData
import com.pixpark.gpupixel.GPUPixelSourceImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException



class GPUPixelHelper {

    private var mSourceRawData: GPUPixelSourceRawData? = null
    private var mBeautyFilter: GPUPixelFilter? = null
    private var mFaceReshapeFilter: GPUPixelFilter? = null
    private var mLipstickFilter: GPUPixelFilter? = null
    private var mFaceDetector: FaceDetector? = null
    private var mSinkRawData: GPUPixelSinkRawData? = null
    var glSurfaceView: CameraGLSurfaceView? = null
    
    // Track face detection state
    private var hasFaceDetected: Boolean = false
    
    // Store current beauty settings to re-apply when face detection changes
    private var currentBeautySettings: BeautySettings? = null

    fun initGpuPixel(context: Context, gLSurfaceView: CameraGLSurfaceView) {
        this.glSurfaceView = gLSurfaceView
        GPUPixel.Init(context)

        // Create GPUPixel processing chain
        mSourceRawData = GPUPixelSourceRawData.Create()

        // Create filters
        mBeautyFilter = GPUPixelFilter.Create(GPUPixelFilter.BEAUTY_FACE_FILTER)
        mFaceReshapeFilter = GPUPixelFilter.Create(GPUPixelFilter.FACE_RESHAPE_FILTER)
        mLipstickFilter = GPUPixelFilter.Create(GPUPixelFilter.LIPSTICK_FILTER)

        // Create output sink
        mSinkRawData = GPUPixelSinkRawData.Create()

        // Initialize face detection
        mFaceDetector = FaceDetector.Create()

        mSourceRawData!!.AddSink(mLipstickFilter)
        mLipstickFilter!!.AddSink(mBeautyFilter)
        mBeautyFilter!!.AddSink(mFaceReshapeFilter)
        mFaceReshapeFilter!!.AddSink(mSinkRawData)
    }

    fun handleImageAnalytic(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if(glSurfaceView == null) return
        // Calculate rotation using GPUPixel
        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height

        // Rotate RGBA data using GPUPixel

        val buffer = imageProxy.planes[0].buffer
        val rgbaBytes = ByteArray(buffer.remaining())

        // ✅ Try to process real camera data, fallback to test pattern
        buffer.get(rgbaBytes)

        val rotatedData = GPUPixel.rotateRgbaImage(rgbaBytes, width, height, rotation)


        // Width and height may be swapped after rotation
        val outWidth = if ((rotation == 90 || rotation == 270)) height else width
        val outHeight = if ((rotation == 90 || rotation == 270)) width else height


        // Perform face detection (using rotated data)
        val landmarks = mFaceDetector!!.detect(
            rotatedData, outWidth, outHeight,
            outWidth * 4, FaceDetector.GPUPIXEL_MODE_FMT_VIDEO,
            FaceDetector.GPUPIXEL_FRAME_TYPE_RGBA
        )

        val previousFaceState = hasFaceDetected
        
        if (landmarks != null && landmarks.isNotEmpty()) {
            Log.d("Face landmarks", "Face landmarks detected: " + landmarks.size)
            hasFaceDetected = true
            mFaceReshapeFilter!!.SetProperty("face_landmark", landmarks)
            mLipstickFilter!!.SetProperty("face_landmark", landmarks)
        } else {
            // No face detected
            hasFaceDetected = false
            Log.d("Face landmarks", "No face detected - disabling face-dependent effects")
        }
        
        // Re-apply beauty settings if face detection state changed
        if (previousFaceState != hasFaceDetected && currentBeautySettings != null) {
            Log.d("GPUPixelHelper", "Face detection state changed, re-applying beauty settings")
            applyBeautySettings(currentBeautySettings!!)
        }


        // Process the rotated RGBA data with GPUPixelSourceRawData
        mSourceRawData!!.ProcessData(
            rotatedData, outWidth, outHeight, outWidth * 4,
            GPUPixelSourceRawData.FRAME_TYPE_RGBA
        )

        // Get processed RGBA data
        val processedRgba = mSinkRawData!!.GetRgbaBuffer()

        // Set texture data and request redraw
        if (processedRgba != null) {
            val rgbaWidth = mSinkRawData!!.GetWidth()
            val rgbaHeight = mSinkRawData!!.GetHeight()
            glSurfaceView?.filterRenderer?.updateTextureData(processedRgba, rgbaWidth, rgbaHeight, isFrontCamera, 0)
            glSurfaceView?.requestRender()
        }
    }

    /**
     * ✅ Thread-safe capture using queueEvent
     */
    suspend fun captureFilteredBitmap(): Bitmap? = suspendCancellableCoroutine { continuation ->
        // ✅ CRITICAL: Must run on GL thread
        glSurfaceView?.queueEvent {
            try {
                val bitmap = glSurfaceView?.filterRenderer?.captureFilteredImage()
                glSurfaceView?.post { continuation.resume(bitmap) }
            } catch (e: Exception) {
                glSurfaceView?.post { continuation.resumeWithException(e) }
            }
        }
    }

    fun onDestroy() {

        // Release face detector
        if (mFaceDetector != null) {
            mFaceDetector!!.destroy()
            mFaceDetector = null
        }
        //
        // Release GPUPixel resources
        if (mBeautyFilter != null) {
            mBeautyFilter!!.Destroy()
            mBeautyFilter = null
        }
        if (mFaceReshapeFilter != null) {
            mFaceReshapeFilter!!.Destroy()
            mFaceReshapeFilter = null
        }

        // Release GPUPixel resources
        if (mLipstickFilter != null) {
            mLipstickFilter!!.Destroy()
            mLipstickFilter = null
        }

        if (mSourceRawData != null) {
            mSourceRawData!!.Destroy()
            mSourceRawData = null
        }

        if (mSinkRawData != null) {
            mSinkRawData!!.Destroy()
            mSinkRawData = null
        }

        glSurfaceView?.release()
    }
    
    /**
     * Update beauty settings for all filters
     * 
     * @param settings BeautySettings object containing all beauty parameters
     */
    fun updateBeautySettings(settings: BeautySettings) {
        // Store current settings for re-application when face detection changes
        currentBeautySettings = settings
        applyBeautySettings(settings)
    }
    
    /**
     * Apply beauty settings with face detection logic
     */
    private fun applyBeautySettings(settings: BeautySettings) {
        try {
            // Always apply general beauty effects (không cần face detection)
            mBeautyFilter?.SetProperty("skin_smoothing", settings.skinSmoothing)
            mBeautyFilter?.SetProperty("whiteness", settings.whiteness)
            
            // Only apply face-dependent effects when face is detected
            if (hasFaceDetected) {
                // Update face reshape filter properties
                mFaceReshapeFilter?.SetProperty("thin_face", settings.thinFace)
                mFaceReshapeFilter?.SetProperty("big_eye", settings.bigEye)
                
                // Update lipstick filter properties
                mLipstickFilter?.SetProperty("blend_level", settings.blendLevel)
                
                Log.d("GPUPixelHelper", "Beauty settings applied with face effects: $settings")
            } else {
                // Disable face-dependent effects
                mFaceReshapeFilter?.SetProperty("thin_face", 0f)
                mFaceReshapeFilter?.SetProperty("big_eye", 0f)
                mLipstickFilter?.SetProperty("blend_level", 0f)
                
                Log.d("GPUPixelHelper", "Beauty settings applied without face effects (no face detected)")
            }
        } catch (e: Exception) {
            Log.e("GPUPixelHelper", "Error applying beauty settings: ${e.message}")
        }
    }
    
    /**
     * Get current face detection state
     * @return true if face is currently detected
     */
    fun isFaceDetected(): Boolean = hasFaceDetected
}