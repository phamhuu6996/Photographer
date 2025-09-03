package com.phamhuu.photographer.presentation.utils

import android.R.attr.height
import android.R.attr.width
import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.camera.core.ImageProxy
import com.pixpark.gpupixel.FaceDetector
import com.pixpark.gpupixel.GPUPixel
import com.pixpark.gpupixel.GPUPixelFilter
import com.pixpark.gpupixel.GPUPixelSinkRawData
import com.pixpark.gpupixel.GPUPixelSourceRawData


class GPUPixelHelper {

    private var mSourceRawData: GPUPixelSourceRawData? = null
    private var mBeautyFilter: GPUPixelFilter? = null
    private var mFaceReshapeFilter: GPUPixelFilter? = null
    private var mLipstickFilter: GPUPixelFilter? = null
    private var mFaceDetector: FaceDetector? = null
    private var mSinkRawData: GPUPixelSinkRawData? = null
    var glSurfaceView: CameraGLSurfaceView? = null

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

        // Set default parameters
        mBeautyFilter!!.SetProperty("skin_smoothing", 3f / 10.0f)
        mBeautyFilter!!.SetProperty("whiteness", 3f / 10.0f)
        mFaceReshapeFilter!!.SetProperty("thin_face", 3f / 160.0f)
        mFaceReshapeFilter!!.SetProperty("big_eye", 3f / 40.0f)
        mLipstickFilter!!.SetProperty("blend_level", 3f / 10.0f)
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

        if (landmarks != null && landmarks.isNotEmpty()) {
            Log.d("Face landmarks", "Face landmarks detected: " + landmarks.size)
            mFaceReshapeFilter!!.SetProperty("face_landmark", landmarks)
            mLipstickFilter!!.SetProperty("face_landmark", landmarks)
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
    }
}