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

/**
 * FilterRenderer - OpenGL ES 2.0 Renderer cho Camera với Filter Effects
 * 
 * Class này chịu trách nhiệm:
 * 1. Render ảnh camera lên OpenGL Surface với các filter effects
 * 2. Xử lý rotation và mirroring cho camera trước/sau
 * 3. Quản lý texture và shader cho rendering
 * 4. Hỗ trợ capture ảnh đã được filter
 * 5. Tích hợp với RecordingManager để ghi video có filter
 * 
 * Kiến trúc:
 * - Sử dụng OpenGL ES 2.0 với vertex/fragment shader
 * - Texture coordinates được tính toán động dựa trên rotation
 * - Vertex coordinates được điều chỉnh để maintain aspect ratio
 * - Thread-safe với synchronized blocks cho texture updates
 * 
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
class FilterRenderer() : GLSurfaceView.Renderer {
    // ===================== OPENGL SHADER VARIABLES =====================
    /** OpenGL texture ID cho camera frame */
    private var mTextureID = -1
    
    /** OpenGL shader program handle */
    private var mProgramHandle = 0
    
    /** Vertex shader attribute handle cho position */
    private var mPositionHandle = 0
    
    /** Vertex shader attribute handle cho texture coordinates */
    private var mTextureCoordHandle = 0
    
    /** Fragment shader uniform handle cho texture sampler */
    private var mTextureSamplerHandle = 0

    // ===================== TEXTURE MANAGEMENT =====================
    /** ByteBuffer chứa texture data từ camera */
    private var mTextureData: ByteBuffer? = null
    
    /** Chiều rộng của texture (camera frame width) */
    private var mTextureWidth = 0
    
    /** Chiều cao của texture (camera frame height) */
    private var mTextureHeight = 0
    
    /** Góc xoay của ảnh (0, 90, 180, 270 degrees) */
    private var mRotation = 0
    
    /** Flag xác định có phải camera trước không (cần mirror) */
    private var mIsFrontCamera = false
    
    /** Flag báo hiệu texture cần được update */
    private var mTextureNeedsUpdate = false
    
    /** Lock object cho thread safety */
    private val mLock = Any()

    // ===================== RECORDING MANAGEMENT =====================
    /** Manager xử lý video/audio recording */
    private val recordingManager = RecordingManager()
    
    /** Flag xác định có đang recording không */
    private var mIsRecording = false
    
    /** File video output cho recording */
    private var mVideoFile: File? = null

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
    

    // ===================== GEOMETRY DATA =====================
    /** 
     * Vertex coordinates cho quad (4 đỉnh của hình chữ nhật)
     * Format: [x1, y1, x2, y2, x3, y3, x4, y4]
     * Tạo hình chữ nhật từ -1 đến 1 (normalized device coordinates)
     */
    private val VERTICES = floatArrayOf(
        -1.0f, -1.0f,  // Bottom left
        1.0f, -1.0f,   // Bottom right
        -1.0f, 1.0f,   // Top left
        1.0f, 1.0f     // Top right
    )

    // ===================== TEXTURE COORDINATES FOR ROTATION =====================
    /** Texture coordinates cho 0° rotation (không xoay) */
    private val TEXTURE_COORDS_0 = floatArrayOf(
        0.0f, 1.0f,  // Bottom left
        1.0f, 1.0f,  // Bottom right
        0.0f, 0.0f,  // Top left
        1.0f, 0.0f   // Top right
    )

    /** Texture coordinates cho 90° clockwise rotation */
    private val TEXTURE_COORDS_90 = floatArrayOf(
        0.0f, 0.0f,  // Bottom left
        0.0f, 1.0f,  // Bottom right
        1.0f, 0.0f,  // Top left
        1.0f, 1.0f   // Top right
    )

    /** Texture coordinates cho 180° clockwise rotation */
    private val TEXTURE_COORDS_180 = floatArrayOf(
        1.0f, 0.0f,  // Bottom left
        0.0f, 0.0f,  // Bottom right
        1.0f, 1.0f,  // Top left
        0.0f, 1.0f   // Top right
    )

    /** Texture coordinates cho 270° clockwise rotation */
    private val TEXTURE_COORDS_270 = floatArrayOf(
        1.0f, 1.0f,  // Bottom left
        1.0f, 0.0f,  // Bottom right
        0.0f, 1.0f,  // Top left
        0.0f, 0.0f   // Top right
    )

    /** Texture coordinates cho front camera mirroring - 0° rotation */
    private val TEXTURE_COORDS_MIRROR_0 = floatArrayOf(
        1.0f, 1.0f,  // Bottom left (mirrored)
        0.0f, 1.0f,  // Bottom right (mirrored)
        1.0f, 0.0f,  // Top left (mirrored)
        0.0f, 0.0f   // Top right (mirrored)
    )

    /** Texture coordinates hiện tại đang sử dụng (được cập nhật theo rotation) */
    private var TEXTURE_COORDS = TEXTURE_COORDS_0

    // ===================== BUFFER MANAGEMENT =====================
    /** FloatBuffer chứa vertex coordinates */
    private var mVertexBuffer: FloatBuffer? = null
    
    /** FloatBuffer chứa texture coordinates */
    private var mTexCoordBuffer: FloatBuffer? = null

    // ===================== VIEW PROPERTIES =====================
    /** Chiều rộng của view (OpenGL surface width) */
    private var mViewWidth = 0
    
    /** Chiều cao của view (OpenGL surface height) */
    private var mViewHeight = 0

    /**
     * Callback được gọi khi OpenGL surface được tạo lần đầu
     * 
     * Chức năng:
     * 1. Khởi tạo OpenGL state (background color, viewport)
     * 2. Tạo và compile vertex/fragment shader
     * 3. Tạo OpenGL program và link shaders
     * 4. Lấy attribute/uniform locations
     * 5. Tạo texture object cho camera frame
     * 6. Khởi tạo vertex/texture coordinate buffers
     * 
     * @param gl OpenGL context (legacy, không sử dụng)
     * @param config EGL configuration (legacy, không sử dụng)
     */
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

    /**
     * Cập nhật texture coordinates dựa trên góc xoay và loại camera
     * 
     * Logic:
     * - Camera trước: Cần mirror ngang (đảo U coordinate) để hiển thị đúng
     * - Camera sau: Không cần mirror
     * - Rotation: 0°, 90°, 180°, 270° với texture coordinates tương ứng
     * 
     * @param rotation Góc xoay (0, 90, 180, 270 degrees)
     */
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

    /**
     * Callback được gọi khi surface size thay đổi
     * 
     * Chức năng:
     * 1. Cập nhật OpenGL viewport
     * 2. Lưu trữ view dimensions
     * 3. Tính toán lại vertex coordinates để maintain aspect ratio
     * 
     * @param gl OpenGL context (legacy, không sử dụng)
     * @param width Chiều rộng mới của surface
     * @param height Chiều cao mới của surface
     */
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

    /**
     * Cập nhật vertex coordinates để maintain aspect ratio của video
     * 
     * Logic:
     * 1. Tính aspect ratio của view và texture
     * 2. Nếu texture rộng hơn view: điều chỉnh height (letterbox)
     * 3. Nếu texture cao hơn view: điều chỉnh width (pillarbox)
     * 4. Cập nhật vertex buffer với coordinates mới
     * 
     * Điều này đảm bảo video không bị stretch/distort khi hiển thị
     */
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

        /**
         * TÍNH TOÁN VERTEX COORDINATES ĐỂ MAINTAIN ASPECT RATIO
         * 
         * Mục đích: Đảm bảo video không bị stretch/distort khi hiển thị
         * 
         * Logic:
         * 1. So sánh aspect ratio của texture (video) và view (screen)
         * 2. Nếu texture rộng hơn view: điều chỉnh height (letterbox)
         * 3. Nếu texture cao hơn view: điều chỉnh width (pillarbox)
         * 4. Giữ nguyên aspect ratio gốc của video
         * 
         * Vertex format: [x1, y1, x2, y2, x3, y3, x4, y4]
         * - Bottom left: [0, 1]
         * - Bottom right: [2, 3] 
         * - Top left: [4, 5]
         * - Top right: [6, 7]
         */
        val adjustedVertices = FloatArray(8)

        if (textureAspectRatio > viewAspectRatio) {
            /**
             * CASE 1: TEXTURE RỘNG HƠN VIEW (LANDSCAPE VIDEO TRÊN PORTRAIT SCREEN)
             * 
             * Ví dụ: Video 16:9 trên màn hình 9:16
             * - textureAspectRatio = 16/9 = 1.78
             * - viewAspectRatio = 9/16 = 0.56
             * - textureAspectRatio > viewAspectRatio ✓
             * 
             * Giải pháp: Điều chỉnh height (tạo letterbox)
             * - Giữ width = [-1, 1] (full width)
             * - Điều chỉnh height = [-heightRatio, heightRatio]
             * - heightRatio = viewAspectRatio / textureAspectRatio
             * 
             * Kết quả: Video hiển thị với black bars trên/dưới
             */
            val heightRatio = viewAspectRatio / textureAspectRatio
            
            // Bottom left: (-1, -heightRatio)
            adjustedVertices[0] = -1.0f // Bottom left x
            adjustedVertices[1] = -heightRatio // Bottom left y
            
            // Bottom right: (1, -heightRatio)  
            adjustedVertices[2] = 1.0f // Bottom right x
            adjustedVertices[3] = -heightRatio // Bottom right y
            
            // Top left: (-1, heightRatio)
            adjustedVertices[4] = -1.0f // Top left x
            adjustedVertices[5] = heightRatio // Top left y
            
            // Top right: (1, heightRatio)
            adjustedVertices[6] = 1.0f // Top right x
            adjustedVertices[7] = heightRatio // Top right y
            
        } else {
            /**
             * CASE 2: TEXTURE CAO HƠN VIEW (PORTRAIT VIDEO TRÊN LANDSCAPE SCREEN)
             * 
             * Ví dụ: Video 9:16 trên màn hình 16:9
             * - textureAspectRatio = 9/16 = 0.56
             * - viewAspectRatio = 16/9 = 1.78
             * - textureAspectRatio < viewAspectRatio ✓
             * 
             * Giải pháp: Điều chỉnh width (tạo pillarbox)
             * - Giữ height = [-1, 1] (full height)
             * - Điều chỉnh width = [-widthRatio, widthRatio]
             * - widthRatio = textureAspectRatio / viewAspectRatio
             * 
             * Kết quả: Video hiển thị với black bars trái/phải
             */
            val widthRatio = textureAspectRatio / viewAspectRatio
            
            // Bottom left: (-widthRatio, -1)
            adjustedVertices[0] = -widthRatio // Bottom left x
            adjustedVertices[1] = -1.0f // Bottom left y
            
            // Bottom right: (widthRatio, -1)
            adjustedVertices[2] = widthRatio // Bottom right x
            adjustedVertices[3] = -1.0f // Bottom right y
            
            // Top left: (-widthRatio, 1)
            adjustedVertices[4] = -widthRatio // Top left x
            adjustedVertices[5] = 1.0f // Top left y
            
            // Top right: (widthRatio, 1)
            adjustedVertices[6] = widthRatio // Top right x
            adjustedVertices[7] = 1.0f // Top right y
        }

        /**
         * CẬP NHẬT VERTEX BUFFER VỚI COORDINATES MỚI
         * 
         * Chức năng:
         * 1. Tạo ByteBuffer với size = 8 floats * 4 bytes = 32 bytes
         * 2. Set byte order = native (little-endian trên Android)
         * 3. Wrap thành FloatBuffer
         * 4. Put adjusted vertices vào buffer
         * 5. Reset position về 0 để ready cho rendering
         * 
         * Lưu ý: 
         * - allocateDirect() tạo native memory (faster)
         * - ByteOrder.nativeOrder() đảm bảo compatibility
         * - position(0) reset để đọc từ đầu buffer
         */
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

    /**
     * Callback được gọi mỗi frame để render
     * 
     * Chức năng:
     * 1. Cập nhật texture data nếu cần (từ camera frame)
     * 2. Render lên screen (main surface)
     * 3. Render lên encoder surface nếu đang recording
     * 4. Drain encoder data nếu đang recording
     * 
     * @param gl OpenGL context (legacy, không sử dụng)
     */
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
     * Render filtered content lên target framebuffer
     * 
     * Chức năng:
     * 1. Bind target framebuffer (0 = screen, khác = offscreen)
     * 2. Set viewport và clear buffer
     * 3. Use shader program
     * 4. Set vertex và texture coordinates
     * 5. Bind texture và draw quad
     * 6. Disable vertex arrays
     * 
     * @param framebuffer Target framebuffer ID (0 = screen)
     * @param width Chiều rộng render area
     * @param height Chiều cao render area
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


    /**
     * Cập nhật texture data từ camera frame và xử lý rotation
     * 
     * Chức năng:
     * 1. Cập nhật texture data từ camera frame
     * 2. Xử lý thay đổi kích thước texture
     * 3. Cập nhật texture coordinates nếu rotation/camera thay đổi
     * 4. Cập nhật vertex coordinates để maintain aspect ratio
     * 
     * Thread-safe: Sử dụng synchronized block
     * 
     * @param data ByteArray chứa RGBA pixel data từ camera
     * @param width Chiều rộng của camera frame
     * @param height Chiều cao của camera frame
     * @param isFrontCamera True nếu là camera trước (cần mirror)
     * @param rotation Góc xoay (0, 90, 180, 270 degrees)
     */
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

    /**
     * Compile OpenGL shader từ source code
     * 
     * Chức năng:
     * 1. Tạo shader object
     * 2. Set source code
     * 3. Compile shader
     * 4. Check compilation status
     * 5. Return shader ID hoặc 0 nếu lỗi
     * 
     * @param type Shader type (GL_VERTEX_SHADER hoặc GL_FRAGMENT_SHADER)
     * @param shaderCode Source code của shader
     * @return Shader ID nếu thành công, 0 nếu lỗi
     */
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
     * Capture ảnh đã được filter từ OpenGL framebuffer
     * 
     * Chức năng:
     * 1. Đọc pixel data từ OpenGL framebuffer
     * 2. Tạo Bitmap từ pixel data
     * 3. Flip vertical (OpenGL lưu top-bottom, Android cần bottom-top)
     * 
     * Lưu ý: Phải gọi trong OpenGL context (trong onDrawFrame)
     * 
     * @return Bitmap đã được filter và flip đúng hướng
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

    /**
     * Flip bitmap theo chiều dọc
     * 
     * OpenGL lưu pixel data từ top-bottom, nhưng Android Bitmap cần bottom-top
     * 
     * @param src Bitmap gốc
     * @return Bitmap đã được flip vertical
     */
    private fun flipVertical(src: Bitmap): Bitmap {
        val matrix = android.graphics.Matrix().apply { preScale(1f, -1f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, false)
    }

    // ================ FILTERED VIDEO RECORDING ================
    
    /**
     * Bắt đầu ghi video với filter effects
     * 
     * Chức năng:
     * 1. Set recording state và video file
     * 2. Delegate cho RecordingManager để setup encoders
     * 3. Sử dụng view dimensions làm recording dimensions
     * 
     * @param videoFile File output cho video
     * @param callback Callback trả về kết quả (true = thành công, false = lỗi)
     */
    fun startFilteredVideoRecording(videoFile: File, callback: (Boolean) -> Unit) {
        mVideoFile = videoFile
        mIsRecording = true
        recordingManager.startFilteredVideoRecording(videoFile, mViewWidth, mViewHeight, callback)
    }
    

    

    /**
     * Dừng filtered video recording
     * 
     * Chức năng:
     * 1. Set recording state = false
     * 2. Delegate cho RecordingManager để stop và cleanup
     * 
     * @param callback Callback trả về kết quả và file video
     */
    fun stopFilteredVideoRecording(callback: (Boolean, File?) -> Unit) {
        mIsRecording = false
        recordingManager.stopFilteredVideoRecording(callback)
    }

    /**
     * Release tất cả OpenGL resources
     * 
     * Chức năng:
     * 1. Stop recording nếu đang active
     * 2. Delete OpenGL program
     * 3. Delete OpenGL texture
     * 
     * Phải gọi khi destroy renderer để tránh memory leak
     */
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