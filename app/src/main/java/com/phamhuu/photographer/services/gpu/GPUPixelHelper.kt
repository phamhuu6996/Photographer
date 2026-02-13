package com.phamhuu.photographer.services.gpu

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.phamhuu.photographer.contants.BeautySettings
import com.phamhuu.photographer.services.gl.CameraGLSurfaceView
import com.pixpark.gpupixel.FaceDetector
import com.pixpark.gpupixel.GPUPixel
import com.pixpark.gpupixel.GPUPixelFilter
import com.pixpark.gpupixel.GPUPixelSinkRawData
import com.pixpark.gpupixel.GPUPixelSourceRawData
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
    private var hasFaceDetected: Boolean = false
    private var currentBeautySettings: BeautySettings? = null
    
    // Reusable buffer để tránh OOM - chỉ allocate 1 lần
    private var reusableCleanBuffer: ByteArray? = null
    private var lastBufferSize = 0

    fun initGpuPixel(context: Context, gLSurfaceView: CameraGLSurfaceView) {
        this.glSurfaceView = gLSurfaceView
        GPUPixel.Init(context)
        mSourceRawData = GPUPixelSourceRawData.Create()
        mBeautyFilter = GPUPixelFilter.Create(GPUPixelFilter.BEAUTY_FACE_FILTER)
        mFaceReshapeFilter = GPUPixelFilter.Create(GPUPixelFilter.FACE_RESHAPE_FILTER)
        mLipstickFilter = GPUPixelFilter.Create(GPUPixelFilter.LIPSTICK_FILTER)
        mSinkRawData = GPUPixelSinkRawData.Create()
        mFaceDetector = FaceDetector.Create()
        mSourceRawData!!.AddSink(mLipstickFilter)
        mLipstickFilter!!.AddSink(mBeautyFilter)
        mBeautyFilter!!.AddSink(mFaceReshapeFilter)
        mFaceReshapeFilter!!.AddSink(mSinkRawData)
    }

    fun handleImageAnalytic(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (glSurfaceView == null) return
        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height
        val planes = imageProxy.planes[0]
        
        // Remove padding và get clean data (reuse buffer)
        val rgbaBytes = removePaddingAndGetCleanData(planes.buffer, width, height, planes.rowStride)
        
        // Rotate image (không còn padding)
        val rotatedData = GPUPixel.rotateRgbaImage(rgbaBytes, width, height, rotation)
        val outWidth = if ((rotation == 90 || rotation == 270)) height else width
        val outHeight = if ((rotation == 90 || rotation == 270)) width else height
        val outStride = outWidth * 4  // Không còn padding
        
        // Face detection
        val landmarks = mFaceDetector!!.detect(
            rotatedData, outWidth, outHeight,
            outStride, FaceDetector.GPUPIXEL_MODE_FMT_VIDEO,
            FaceDetector.GPUPIXEL_FRAME_TYPE_RGBA
        )
        val previousFaceState = hasFaceDetected
        if (landmarks != null && landmarks.isNotEmpty()) {
            hasFaceDetected = true
            mFaceReshapeFilter!!.SetProperty("face_landmark", landmarks)
            mLipstickFilter!!.SetProperty("face_landmark", landmarks)
        } else {
            hasFaceDetected = false
        }
        if (previousFaceState != hasFaceDetected && currentBeautySettings != null) {
            applyBeautySettings(currentBeautySettings!!)
        }
        
        // Process with GpuPixel
        mSourceRawData!!.ProcessData(
            rotatedData, outWidth, outHeight, outStride,
            GPUPixelSourceRawData.FRAME_TYPE_RGBA
        )
        val processedRgba = mSinkRawData!!.GetRgbaBuffer()
        if (processedRgba != null) {
            val rgbaWidth = mSinkRawData!!.GetWidth()
            val rgbaHeight = mSinkRawData!!.GetHeight()
            glSurfaceView?.filterRenderer?.updateTextureData(
                processedRgba,
                rgbaWidth,
                rgbaHeight,
                isFrontCamera,
                0
            )
            glSurfaceView?.requestRender()
        }
    }
    
    /**
     * Remove padding từ camera buffer và return clean data
     * Reuse buffer để tránh OOM
     */
    private fun removePaddingAndGetCleanData(
        buffer: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        stride: Int
    ): ByteArray {
        val pixelStride = width * 4  // RGBA = 4 bytes per pixel
        
        return if (stride != pixelStride) {
            // Có padding - cần remove
            val requiredSize = height * pixelStride
            
            // Reuse buffer nếu size không đổi, chỉ allocate khi cần
            if (reusableCleanBuffer == null || lastBufferSize != requiredSize) {
                reusableCleanBuffer = ByteArray(requiredSize)
                lastBufferSize = requiredSize
            }
            
            val cleanData = reusableCleanBuffer!!
            
            // Copy từng dòng, loại bỏ padding
            for (row in 0 until height) {
                val srcOffset = row * stride
                val dstOffset = row * pixelStride
                buffer.position(srcOffset)
                buffer.get(cleanData, dstOffset, pixelStride)
            }
            cleanData
        } else {
            // Không có padding - đọc trực tiếp
            val requiredSize = buffer.remaining()
            
            // Reuse buffer
            if (reusableCleanBuffer == null || lastBufferSize != requiredSize) {
                reusableCleanBuffer = ByteArray(requiredSize)
                lastBufferSize = requiredSize
            }
            
            buffer.get(reusableCleanBuffer!!)
            reusableCleanBuffer!!
        }
    }

    suspend fun captureFilteredBitmap(): Bitmap? = suspendCancellableCoroutine { continuation ->
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
        // Clear reusable buffer
        reusableCleanBuffer = null
        lastBufferSize = 0
        
        if (mFaceDetector != null) {
            mFaceDetector!!.destroy()
            mFaceDetector = null
        }
        if (mBeautyFilter != null) {
            mBeautyFilter!!.Destroy()
            mBeautyFilter = null
        }
        if (mFaceReshapeFilter != null) {
            mFaceReshapeFilter!!.Destroy()
            mFaceReshapeFilter = null
        }
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

    fun updateBeautySettings(settings: BeautySettings) {
        currentBeautySettings = settings
        applyBeautySettings(settings)
    }

    private fun applyBeautySettings(settings: BeautySettings) {
        try {
            mBeautyFilter?.SetProperty("skin_smoothing", settings.skinSmoothing)
            mBeautyFilter?.SetProperty("whiteness", settings.whiteness)
            if (hasFaceDetected) {
                mFaceReshapeFilter?.SetProperty("thin_face", settings.thinFace)
                mFaceReshapeFilter?.SetProperty("big_eye", settings.bigEye)
                mLipstickFilter?.SetProperty("blend_level", settings.blendLevel)
            } else {
                mFaceReshapeFilter?.SetProperty("thin_face", 0f)
                mFaceReshapeFilter?.SetProperty("big_eye", 0f)
                mLipstickFilter?.SetProperty("blend_level", 0f)
            }
        } catch (e: Exception) {
        }
    }

    fun isFaceDetected(): Boolean = hasFaceDetected
}
