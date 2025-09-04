package com.phamhuu.photographer.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import androidx.camera.core.ImageProxy
import com.phamhuu.photographer.enums.ImageFilter
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CameraGLSurfaceView (
    context: Context,
) : GLSurfaceView(context) {

    val filterRenderer: FilterRenderer

    init {
        setEGLContextClientVersion(2)
        filterRenderer = FilterRenderer()
        setRenderer(filterRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        println("🔥 CameraGLSurfaceView initialized")
    }

    
    /**
     * Bắt đầu ghi filtered video
     */
    fun startFilteredVideoRecording(videoFile: File, callback: (Boolean) -> Unit) {
        queueEvent {
            filterRenderer.startFilteredVideoRecording(videoFile) { success ->
                callback(success)
            }
        }
    }

    /**
     * Dừng ghi filtered video
     */
    fun stopFilteredVideoRecording(callback: (Boolean, File?) -> Unit) {
        queueEvent {
            filterRenderer.stopFilteredVideoRecording { success, file ->
                callback(success, file)
            }
        }
    }

    /**
     * Kiểm tra trạng thái recording
     */
    fun isRecording(): Boolean {
        return filterRenderer.isRecording()
    }

    fun release() {
        queueEvent {
            try {
                filterRenderer.release()
                println("🔥 FilterRenderer released")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
} 