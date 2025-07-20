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

    private val filterRenderer: FilterRenderer

    init {
        setEGLContextClientVersion(2)
        filterRenderer = FilterRenderer()
        setRenderer(filterRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY

        println("🔥 CameraGLSurfaceView initialized")
    }

    // ✅ Check if renderer is ready for operations
    fun isRendererReady(): Boolean {
        return filterRenderer.isReady()
    }
    
    // ✅ Simplified image update - FilterRenderer handles ImageProxy closing
    fun updateImage(imageProxy: ImageProxy) {
        filterRenderer.updateImage(imageProxy)
        requestRender()
    }
    
    fun setImageFilter(filter: ImageFilter) {
        queueEvent {
            try {
                println("🔥 Setting filter: ${filter.displayName}")
                filterRenderer.setFilter(filter)
                requestRender()
            } catch (e: Exception) {
                e.printStackTrace()
                println("❌ Error setting filter: ${e.message}")
            }
        }
    }
    
    fun captureFilteredImage(callback: (Bitmap) -> Unit) {
        queueEvent {
            try {
                filterRenderer.captureFilteredImage { bitmap ->
                    // Post callback to main thread
                    post { 
                        try {
                            callback(bitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                requestRender()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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