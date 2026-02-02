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
    // Buffer dùng lại giữa các frame
    private var tightRgba: ByteArray? = null
    private var tightCapacity = 0

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

    private fun copyTightRgba(imageProxy: ImageProxy): ByteArray {
        val plane = imageProxy.planes[0]
        val srcBuffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride // RGBA = 4
        val width = imageProxy.width
        val height = imageProxy.height

        val rowBytes = width * pixelStride
        val neededBytes = rowBytes * height

        // Chỉ cấp phát lại khi thật sự cần
        if (tightRgba == null || tightCapacity < neededBytes) {
            tightRgba = ByteArray(neededBytes)
            tightCapacity = neededBytes
        }

        val out = tightRgba!!

        // 🚀 FAST PATH: Không có padding
        if (rowStride == rowBytes) {
            srcBuffer.position(0)
            srcBuffer.get(out, 0, neededBytes)
            return out
        }

        // 🧩 Có padding cuối mỗi dòng → copy từng dòng
        var srcOffset = 0
        var dstOffset = 0

        for (row in 0 until height) {
            srcBuffer.position(srcOffset)
            srcBuffer.get(out, dstOffset, rowBytes)

            srcOffset += rowStride
            dstOffset += rowBytes
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

        // ✅ Lấy RGBA chuẩn, không padding
        val rgbaBytes = copyTightRgba(imageProxy)

        // Có thể đóng imageProxy NGAY sau khi copy xong để tránh đầy buffer CameraX
        imageProxy.close()

        // ✅ Rotate bằng GPUPixel (giữ nguyên cách bạn đang dùng)
        val rotatedData = GPUPixel.rotateRgbaImage(
            rgbaBytes,
            width,
            height,
            rotation
        )

        val outWidth = if (rotation == 90 || rotation == 270) height else width
        val outHeight = if (rotation == 90 || rotation == 270) width else height

        // ✅ Face detect
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

        // ✅ Đưa vào pipeline GPUPixel
        mSourceRawData?.ProcessData(
            rotatedData,
            outWidth,
            outHeight,
            outWidth * 4,
            GPUPixelSourceRawData.FRAME_TYPE_RGBA
        )

        // ✅ Lấy output từ GPU
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
