package com.phamhuu.photographer.services.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import com.phamhuu.photographer.services.renderer.AddTextService
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig as EGLConfigLegacy
import javax.microedition.khronos.opengles.GL10

class FilterRenderer() : GLSurfaceView.Renderer {
    private var mTextureID = -1
    private var mProgramHandle = 0
    private var mPositionHandle = 0
    private var mTextureCoordHandle = 0
    private var mTextureSamplerHandle = 0

    private var mTextureData: ByteBuffer? = null
    private var mTextureWidth = 0
    private var mTextureHeight = 0
    private var mRotation = 0
    private var mIsFrontCamera = false
    private var mTextureNeedsUpdate = false
    private val mLock = Any()

    private val recordingManager = RecordingManager()
    private var mIsRecording = false
    private var mVideoFile: File? = null
    private var mTextOverlay: (() -> String?)? = null

    private var mOverlayBitmapCache: Bitmap? = null
    private var mOverlayCanvasCache: android.graphics.Canvas? = null
    private var mOverlayLastText: String? = null
    private var mOverlayLastWidth: Int = 0
    private var mOverlayLastHeight: Int = 0

    val vertexShaderCode = """attribute vec2 aPosition;
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
  gl_Position = vec4(aPosition, 0.0, 1.0);
  vTextureCoord = aTextureCoord;
}"""

    val fragmentShaderCode = """precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTexture;
void main() {
  gl_FragColor = texture2D(uTexture, vTextureCoord);
}"""

    private val VERTICES = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        1.0f, 1.0f
    )

    private val TEXTURE_COORDS_0 = floatArrayOf(
        0.0f, 1.0f,
        1.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f
    )
    private val TEXTURE_COORDS_90 = floatArrayOf(
        0.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )
    private val TEXTURE_COORDS_180 = floatArrayOf(
        1.0f, 0.0f,
        0.0f, 0.0f,
        1.0f, 1.0f,
        0.0f, 1.0f
    )
    private val TEXTURE_COORDS_270 = floatArrayOf(
        1.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        0.0f, 0.0f
    )
    private val TEXTURE_COORDS_MIRROR_0 = floatArrayOf(
        1.0f, 1.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        0.0f, 0.0f
    )
    private var TEXTURE_COORDS = TEXTURE_COORDS_0

    private var mVertexBuffer: FloatBuffer? = null
    private var mTexCoordBuffer: FloatBuffer? = null

    private var mViewWidth = 0
    private var mViewHeight = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfigLegacy?) {
        Log.d("TAG", "onSurfaceCreated called")
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        val bb = ByteBuffer.allocateDirect(VERTICES.size * 4)
        bb.order(ByteOrder.nativeOrder())
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer!!.put(VERTICES)
        mVertexBuffer!!.position(0)

        updateTextureCoordinates(0)

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgramHandle = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgramHandle, vertexShader)
        GLES20.glAttachShader(mProgramHandle, fragmentShader)
        GLES20.glLinkProgram(mProgramHandle)

        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition")
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord")
        mTextureSamplerHandle = GLES20.glGetUniformLocation(mProgramHandle, "uTexture")

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        mTextureID = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID)

        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun updateTextureCoordinates(rotation: Int) {
        TEXTURE_COORDS = when (rotation) {
            90 -> if (mIsFrontCamera) TEXTURE_COORDS_270 else TEXTURE_COORDS_90
            180 -> TEXTURE_COORDS_180
            270 -> if (mIsFrontCamera) TEXTURE_COORDS_90 else TEXTURE_COORDS_270
            else -> if (mIsFrontCamera) TEXTURE_COORDS_MIRROR_0 else TEXTURE_COORDS_0
        }
        val bb = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4)
        bb.order(ByteOrder.nativeOrder())
        mTexCoordBuffer = bb.asFloatBuffer()
        mTexCoordBuffer!!.put(TEXTURE_COORDS)
        mTexCoordBuffer!!.position(0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("TAG", "onSurfaceChanged: width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)
        mViewWidth = width
        mViewHeight = height
        if (mTextureWidth > 0 && mTextureHeight > 0) {
            updateVertexCoordinates()
        }
    }

    private fun updateVertexCoordinates() {
        if (mViewWidth <= 0 || mViewHeight <= 0 || mTextureWidth <= 0 || mTextureHeight <= 0) {
            return
        }
        val viewAspectRatio = mViewWidth.toFloat() / mViewHeight
        val textureAspectRatio = if (mRotation == 90 || mRotation == 270) {
            mTextureHeight.toFloat() / mTextureWidth
        } else {
            mTextureWidth.toFloat() / mTextureHeight
        }
        val adjustedVertices = FloatArray(8)
        if (textureAspectRatio > viewAspectRatio) {
            val heightRatio = viewAspectRatio / textureAspectRatio
            adjustedVertices[0] = -1.0f
            adjustedVertices[1] = -heightRatio
            adjustedVertices[2] = 1.0f
            adjustedVertices[3] = -heightRatio
            adjustedVertices[4] = -1.0f
            adjustedVertices[5] = heightRatio
            adjustedVertices[6] = 1.0f
            adjustedVertices[7] = heightRatio
        } else {
            val widthRatio = textureAspectRatio / viewAspectRatio
            adjustedVertices[0] = -widthRatio
            adjustedVertices[1] = -1.0f
            adjustedVertices[2] = widthRatio
            adjustedVertices[3] = -1.0f
            adjustedVertices[4] = -widthRatio
            adjustedVertices[5] = 1.0f
            adjustedVertices[6] = widthRatio
            adjustedVertices[7] = 1.0f
        }
        val bb = ByteBuffer.allocateDirect(adjustedVertices.size * 4)
        bb.order(ByteOrder.nativeOrder())
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer!!.put(adjustedVertices)
        mVertexBuffer!!.position(0)
        Log.d(
            "TAG",
            "Video aspect ratio updated: View=" + viewAspectRatio
                    + ", Texture=" + textureAspectRatio + ", Rotation=" + mRotation
        )
    }

    override fun onDrawFrame(gl: GL10?) {
        Log.d("TAG", "onDrawFrame called")
        
        synchronized(mLock) {
            if (mTextureNeedsUpdate && mTextureData != null) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID)
                GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mTextureWidth,
                    mTextureHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                    mTextureData
                )
                mTextureNeedsUpdate = false
            }
        }

        renderToTarget(0, mViewWidth, mViewHeight)

        if (mIsRecording) {
            recordingManager.renderToEncoderSurface { width, height ->
                renderToTarget(0, width, height)
                renderTextOverlay(width, height)
            }
            recordingManager.drainEncoders()
        }
    }

    private fun renderToTarget(framebuffer: Int, width: Int, height: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(mProgramHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer
        )
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer
        )
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID)
        GLES20.glUniform1i(mTextureSamplerHandle, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle)
    }

    fun updateTextureData(data: ByteArray, width: Int, height: Int, isFrontCamera: Boolean, rotation: Int) {
        synchronized(mLock) {
            val sizeChanged =
                mTextureWidth != width || mTextureHeight != height
            if (mTextureData == null || sizeChanged) {
                mTextureData = ByteBuffer.allocateDirect(width * height * 4)
                mTextureData?.order(ByteOrder.nativeOrder())
                mTextureWidth = width
                mTextureHeight = height
            }

            mTextureData!!.clear()
            mTextureData!!.put(data)
            mTextureData!!.position(0)
            mTextureNeedsUpdate = true

            val rotationChanged = (rotation != mRotation)
            val isFrontCameraChanged = (isFrontCamera != mIsFrontCamera)

            if (rotationChanged || isFrontCameraChanged) {
                mRotation = rotation
                mIsFrontCamera = isFrontCamera
                updateTextureCoordinates(mRotation)
            }

            if (sizeChanged || rotationChanged) {
                updateVertexCoordinates()
            }
        }
    }

    fun compileShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e("TAG", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun captureFilteredImage() : Bitmap {
        val bufferSize = mViewWidth * mViewHeight * 4
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(
            0, 0, mViewWidth, mViewHeight,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, byteBuffer
        )
        val bitmap = Bitmap.createBitmap(mViewWidth, mViewHeight, Bitmap.Config.ARGB_8888)
        byteBuffer.position(0)
        bitmap.copyPixelsFromBuffer(byteBuffer)
        return flipVertical(bitmap)
    }

    private fun flipVertical(src: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply { preScale(1f, -1f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, false)
    }

    private fun renderTextOverlay(
        width: Int,
        height: Int
    ) {
        val textOverlay = mTextOverlay?.invoke()
        if (textOverlay.isNullOrEmpty()) return

        val sizeChanged = (mOverlayLastWidth != width) || (mOverlayLastHeight != height)
        val textChanged = (mOverlayLastText != textOverlay)

        if (mOverlayBitmapCache == null || sizeChanged) {
            mOverlayBitmapCache?.recycle()
            mOverlayBitmapCache = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mOverlayCanvasCache = android.graphics.Canvas(mOverlayBitmapCache!!)
            mOverlayLastWidth = width
            mOverlayLastHeight = height
        }

        if (sizeChanged || textChanged) {
            val canvas = mOverlayCanvasCache ?: return
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            AddTextService.renderAddressToVideo(canvas, textOverlay, width)
            mOverlayLastText = textOverlay
        }

        mOverlayBitmapCache?.let { renderBitmapOverlay(it) }
    }
    
    fun renderBitmapOverlay(bitmap: Bitmap) {
        val overlayTexture = IntArray(1)
        GLES20.glGenTextures(1, overlayTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTexture[0])
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(mProgramHandle)
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        val overlayTexCoordBuffer = createTexCoordBuffer(TEXTURE_COORDS_0)
        GLES20.glVertexAttribPointer(mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, overlayTexCoordBuffer)
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTexture[0])
        GLES20.glUniform1i(mTextureSamplerHandle, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glDeleteTextures(1, overlayTexture, 0)
    }
    
    private fun createTexCoordBuffer(texCoords: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(texCoords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        val buffer = bb.asFloatBuffer()
        buffer.put(texCoords)
        buffer.position(0)
        return buffer
    }
    
    fun startFilteredVideoRecording(
        videoFile: File,
        textOverlay: (() -> String?)? = null,
    ) : Boolean {
        mVideoFile = videoFile
        mTextOverlay = textOverlay
        mIsRecording = true
        return recordingManager.startFilteredVideoRecording(videoFile, mViewWidth, mViewHeight)
    }
    
    fun stopFilteredVideoRecording(callback: (Boolean, File?) -> Unit) {
        mIsRecording = false
        recordingManager.stopFilteredVideoRecording(callback)
    }

    fun release() {
        if (mIsRecording) {
            stopFilteredVideoRecording { _, _ -> }
        }
        mOverlayBitmapCache?.recycle()
        mOverlayBitmapCache = null
        mOverlayCanvasCache = null
        mOverlayLastText = null
        
        if (mProgramHandle != 0) {
            GLES20.glDeleteProgram(mProgramHandle)
            mProgramHandle = 0
        }
        if (mTextureID != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(mTextureID), 0)
            mTextureID = -1
        }
    }
}
