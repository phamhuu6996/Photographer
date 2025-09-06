package com.phamhuu.photographer.presentation.utils

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig as EGLConfigLegacy
import javax.microedition.khronos.opengles.GL10


// ===================== KIẾN THỨC TỔNG KẾT =====================
// Khi render ảnh camera lên OpenGL:
// 1. Xoay (rotation):
//    - Đổi vị trí các đỉnh (hoặc U/V) để ảnh xoay đúng góc (0, 90, 180, 270)
// 2. Flip (mirror):
//    - Camera trước cần mirror ngang (đảo U)
//    - Camera sau không cần mirror
//    - Lật dọc (đảo V) nếu muốn flip dọc
// =============================================================
class FilterRenderer() : GLSurfaceView.Renderer {
    private var mTextureID = -1
    private var mProgramHandle = 0
    private var mPositionHandle = 0
    private var mTextureCoordHandle = 0
    private var mTextureSamplerHandle = 0

    private var mTextureData: ByteBuffer? = null
    private var mTextureWidth = 0
    private var mTextureHeight = 0
    private var mRotation = 0 // Image rotation angle
    private var mIsFrontCamera = false
    private var mTextureNeedsUpdate = false
    private val mLock = Any()

    // Recording manager
    private val recordingManager = RecordingManager()
    private var mIsRecording = false
    private var mVideoFile: File? = null
    

    // Vertex coordinates
    private val VERTICES = floatArrayOf(
        -1.0f, -1.0f,  // Bottom left
        1.0f, -1.0f,  // Bottom right
        -1.0f, 1.0f,  // Top left
        1.0f, 1.0f // Top right
    )

    // Texture coordinates - default (0° rotation)
    private val TEXTURE_COORDS_0 = floatArrayOf(
        0.0f, 1.0f,  // Bottom left
        1.0f, 1.0f,  // Bottom right
        0.0f, 0.0f,  // Top left
        1.0f, 0.0f // Top right
    )

    // Texture coordinates - 90° clockwise rotation
    private val TEXTURE_COORDS_90 = floatArrayOf(
        0.0f, 0.0f,  // Bottom left
        0.0f, 1.0f,  // Bottom right
        1.0f, 0.0f,  // Top left
        1.0f, 1.0f // Top right
    )

    // Texture coordinates - 180° clockwise rotation
    private val TEXTURE_COORDS_180 = floatArrayOf(
        1.0f, 0.0f,  // Bottom left
        0.0f, 0.0f,  // Bottom right
        1.0f, 1.0f,  // Top left
        0.0f, 1.0f // Top right
    )

    // Texture coordinates - 270° clockwise rotation
    private val TEXTURE_COORDS_270 = floatArrayOf(
        1.0f, 1.0f,  // Bottom left
        1.0f, 0.0f,  // Bottom right
        0.0f, 1.0f,  // Top left
        0.0f, 0.0f // Top right
    )

    // Texture coordinates for front camera mirroring - default (0° rotation)
    private val TEXTURE_COORDS_MIRROR_0 = floatArrayOf(
        1.0f, 1.0f,  // Bottom left
        0.0f, 1.0f,  // Bottom right
        1.0f, 0.0f,  // Top left
        0.0f, 0.0f // Top right
    )

    // Currently used texture coordinates
    private var TEXTURE_COORDS = TEXTURE_COORDS_0

    private var mVertexBuffer: FloatBuffer? = null
    private var mTexCoordBuffer: FloatBuffer? = null

    // View dimension properties
    private var mViewWidth = 0
    private var mViewHeight = 0


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfigLegacy?) {
        Log.d("TAG", "onSurfaceCreated called")
        // Set background color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Initialize vertex coordinate buffer
        val bb = ByteBuffer.allocateDirect(VERTICES.size * 4)
        bb.order(ByteOrder.nativeOrder())
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer!!.put(VERTICES)
        mVertexBuffer!!.position(0)

        // Initialize texture coordinate buffer - using default texture coordinates
        updateTextureCoordinates(0)

        // Create vertex shader
        val vertexShaderCode = """attribute vec2 aPosition;
attribute vec2 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
  gl_Position = vec4(aPosition, 0.0, 1.0);
  vTextureCoord = aTextureCoord;
}"""

        // Create fragment shader
        val fragmentShaderCode = """precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTexture;
void main() {
  gl_FragColor = texture2D(uTexture, vTextureCoord);
}"""

        // Compile shaders
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // Create program
        mProgramHandle = GLES20.glCreateProgram()
        GLES20.glAttachShader(mProgramHandle, vertexShader)
        GLES20.glAttachShader(mProgramHandle, fragmentShader)
        GLES20.glLinkProgram(mProgramHandle)

        // Get attribute locations
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "aPosition")
        mTextureCoordHandle = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord")
        mTextureSamplerHandle = GLES20.glGetUniformLocation(mProgramHandle, "uTexture")

        // Create texture
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        mTextureID = textures[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID)

        // Set texture parameters
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

    // Update texture coordinates based on rotation angle
    private fun updateTextureCoordinates(rotation: Int) {
        // Mirror only for front camera to correct preview parity
        TEXTURE_COORDS = when (rotation) {
            90 -> if (mIsFrontCamera) TEXTURE_COORDS_270 else TEXTURE_COORDS_90
            180 -> if (mIsFrontCamera) TEXTURE_COORDS_180 else TEXTURE_COORDS_180
            270 -> if (mIsFrontCamera) TEXTURE_COORDS_90 else TEXTURE_COORDS_270
            else -> if (mIsFrontCamera) TEXTURE_COORDS_MIRROR_0 else TEXTURE_COORDS_0
        }
        // Update texture coordinate buffer
        val bb = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4)
        bb.order(ByteOrder.nativeOrder())
        mTexCoordBuffer = bb.asFloatBuffer()
        mTexCoordBuffer!!.put(TEXTURE_COORDS)
        mTexCoordBuffer!!.position(0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("TAG", "onSurfaceChanged: width=$width, height=$height")
        GLES20.glViewport(0, 0, width, height)

        // Store view dimensions for later calculations
        mViewWidth = width
        mViewHeight = height

        // If texture data already exists, update vertex coordinates to match texture aspect
        // ratio
        if (mTextureWidth > 0 && mTextureHeight > 0) {
            updateVertexCoordinates()
        }
    }

    // Update vertex coordinates to maintain video aspect ratio
    private fun updateVertexCoordinates() {
        if (mViewWidth <= 0 || mViewHeight <= 0 || mTextureWidth <= 0 || mTextureHeight <= 0) {
            return
        }

        // Calculate view and texture aspect ratios
        val viewAspectRatio = mViewWidth.toFloat() / mViewHeight

        // Consider rotation
        val textureAspectRatio = if (mRotation == 90 || mRotation == 270) {
            // Width and height are swapped after rotation
            mTextureHeight.toFloat() / mTextureWidth
        } else {
            mTextureWidth.toFloat() / mTextureHeight
        }

        // Set vertex coordinates while maintaining texture aspect ratio
        val adjustedVertices = FloatArray(8)

        if (textureAspectRatio > viewAspectRatio) {
            // Texture is wider than view, adjust height
            val heightRatio = viewAspectRatio / textureAspectRatio
            adjustedVertices[0] = -1.0f // Bottom left x
            adjustedVertices[1] = -heightRatio // Bottom left y
            adjustedVertices[2] = 1.0f // Bottom right x
            adjustedVertices[3] = -heightRatio // Bottom right y
            adjustedVertices[4] = -1.0f // Top left x
            adjustedVertices[5] = heightRatio // Top left y
            adjustedVertices[6] = 1.0f // Top right x
            adjustedVertices[7] = heightRatio // Top right y
        } else {
            // Texture is higher than view, adjust width
            val widthRatio = textureAspectRatio / viewAspectRatio
            adjustedVertices[0] = -widthRatio // Bottom left x
            adjustedVertices[1] = -1.0f // Bottom left y
            adjustedVertices[2] = widthRatio // Bottom right x
            adjustedVertices[3] = -1.0f // Bottom right y
            adjustedVertices[4] = -widthRatio // Top left x
            adjustedVertices[5] = 1.0f // Top left y
            adjustedVertices[6] = widthRatio // Top right x
            adjustedVertices[7] = 1.0f // Top right y
        }

        // Update vertex buffer
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

        // Render to screen
        renderToTarget(0, mViewWidth, mViewHeight)

        // Render to encoder surface if recording
        if (mIsRecording) {
            recordingManager.renderToEncoderSurface { width, height ->
                renderToTarget(0, width, height)
            }
            recordingManager.drainEncoders()
        }
    }

    /**
     * Render filtered content to target framebuffer
     */
    private fun renderToTarget(framebuffer: Int, width: Int, height: Int) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Use shader program
        GLES20.glUseProgram(mProgramHandle)

        // Set vertex coordinates
        GLES20.glVertexAttribPointer(
            mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVertexBuffer
        )
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Set texture coordinates
        GLES20.glVertexAttribPointer(
            mTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexCoordBuffer
        )
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle)

        // Set texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID)
        GLES20.glUniform1i(mTextureSamplerHandle, 0)

        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle)
    }


    // Update texture data and handle rotation
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

            // When rotation changes, update texture coordinates
            val rotationChanged = (rotation != mRotation)
            val isFrontCameraChanged = (isFrontCamera != mIsFrontCamera)

            if (rotationChanged || isFrontCameraChanged) {
                mRotation = rotation
                mIsFrontCamera = isFrontCamera
                updateTextureCoordinates(mRotation)
            }

            // If size or rotation changes, update vertex coordinates to maintain aspect ratio
            if (sizeChanged || rotationChanged) {
                updateVertexCoordinates()
            }
        }
    }

    // Compile shader
    private fun compileShader(type: Int, shaderCode: String): Int {
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

    /**
     * Yêu cầu capture ảnh đã được filter
     *
     * Ảnh sẽ được capture trong onDrawFrame() và gọi callback
     */
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

        // OpenGL lưu top–bottom → cần lật dọc
        return flipVertical(bitmap)
    }

    private fun flipVertical(src: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply { preScale(1f, -1f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, false)
    }

    // ================ FILTERED VIDEO RECORDING ================
    
    /**
     * Bắt đầu ghi video với filter sử dụng MediaCodec + MediaMuxer
     */
    fun startFilteredVideoRecording(videoFile: File, callback: (Boolean) -> Unit) {
        mVideoFile = videoFile
        mIsRecording = true
        recordingManager.startFilteredVideoRecording(videoFile, mTextureWidth, mTextureHeight, callback)
    }
    

    

    /**
     * Dừng filtered video recording
     */
    fun stopFilteredVideoRecording(callback: (Boolean, File?) -> Unit) {
        mIsRecording = false
        recordingManager.stopFilteredVideoRecording(callback)
    }

    fun release() {
        // Stop recording if active
        if (mIsRecording) {
            stopFilteredVideoRecording { _, _ -> }
        }
        
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