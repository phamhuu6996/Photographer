package com.phamhuu.photographer.services.gl

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import java.io.File

@SuppressLint("ViewConstructor")
class CameraGLSurfaceView(
    context: Context,
    val filterRenderer: FilterRenderer
) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(2)
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
