package com.phamhuu.photographer.services.gl

import android.content.Context
import android.opengl.GLSurfaceView
import java.io.File

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

    fun startFilteredVideoRecording(
        videoFile: File,
        textOverlay: (() -> String?)? = null,
        callback: (Boolean) -> Unit
    ) {
        queueEvent {
           val success = filterRenderer.startFilteredVideoRecording(videoFile, textOverlay)
           callback(success)
        }
    }

    fun stopFilteredVideoRecording(callback: (Boolean, File?) -> Unit) {
        queueEvent {
            filterRenderer.stopFilteredVideoRecording { success, file ->
                callback(success, file)
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
