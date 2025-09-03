package com.phamhuu.photographer.presentation.utils

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import androidx.camera.core.ImageProxy
import com.phamhuu.photographer.enums.ImageFilter
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