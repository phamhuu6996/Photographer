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
    private var rgbaBuffer: ByteArray? = null

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

    private fun extractTightRgba(imageProxy: ImageProxy): ByteArray {
        val plane = imageProxy.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride // = 4 (RGBA)
        val width = imageProxy.width
        val height = imageProxy.height

        val requiredSize = width * height * pixelStride
        if (rgbaBuffer == null || rgbaBuffer!!.size != requiredSize) {
            rgbaBuffer = ByteArray(requiredSize)
        }
        val out = rgbaBuffer!!

        // Nếu stride khít thì copy 1 phát là xong
        if (rowStride == width * pixelStride) {
            buffer.position(0)
            buffer.get(out, 0, requiredSize)
            return out
        }

        // Có padding → copy từng dòng bỏ padding
        var offset = 0
        for (row in 0 until height) {
            buffer.position(row * rowStride)
            buffer.get(out, offset, width * pixelStride)
            offset += width * pixelStride
        }
        return out
    }


    fun handleImageAnalytic(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        if (glSurfaceView == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height

        // ✅ BƯỚC 1: RGBA TIGHT (fix sọc ngang)
        val rgbaBytes = extractTightRgba(imageProxy)

        // ✅ BƯỚC 2: Rotate cho đúng hướng camera
        val rotatedData = GPUPixel.rotateRgbaImage(
            rgbaBytes,
            width,
            height,
            rotation
        )

        val outWidth = if (rotation == 90 || rotation == 270) height else width
        val outHeight = if (rotation == 90 || rotation == 270) width else height

        // ✅ BƯỚC 3: Detect face
        val landmarks = mFaceDetector?.detect(
            rotatedData,
            outWidth,
            outHeight,
            outWidth * 4,
            FaceDetector.GPUPIXEL_MODE_FMT_VIDEO,
            FaceDetector.GPUPIXEL_FRAME_TYPE_RGBA
        )

        val previousFaceState = hasFaceDetected
        if (landmarks != null && landmarks.isNotEmpty()) {
            hasFaceDetected = true
            mFaceReshapeFilter?.SetProperty("face_landmark", landmarks)
            mLipstickFilter?.SetProperty("face_landmark", landmarks)
        } else {
            hasFaceDetected = false
        }

        if (previousFaceState != hasFaceDetected && currentBeautySettings != null) {
            applyBeautySettings(currentBeautySettings!!)
        }

        // ✅ BƯỚC 4: Đưa vào GPUPixel pipeline
        mSourceRawData?.ProcessData(
            rotatedData,
            outWidth,
            outHeight,
            outWidth * 4,
            GPUPixelSourceRawData.FRAME_TYPE_RGBA
        )

        // ✅ BƯỚC 5: Lấy ảnh đã filter từ GPU
        val processedRgba = mSinkRawData?.GetRgbaBuffer()
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
