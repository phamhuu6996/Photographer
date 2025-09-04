package com.phamhuu.photographer.presentation.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.Surface
import com.pixpark.gpupixel.GPUPixel
import com.pixpark.gpupixel.GPUPixelFilter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig as EGLConfigLegacy
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.thread


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

    // Video + Audio recording với dual MediaCodec + MediaMuxer
    private var mVideoEncoder: MediaCodec? = null
    private var mAudioEncoder: MediaCodec? = null
    private var mMediaMuxer: MediaMuxer? = null
    private var mEncoderSurface: Surface? = null
    private var mEncoderEGLDisplay: EGLDisplay? = null
    private var mEncoderEGLContext: EGLContext? = null
    private var mEncoderEGLSurface: EGLSurface? = null
    private var mIsRecording = false
    private var mVideoFile: File? = null
    private var mVideoTrackIndex = -1
    private var mAudioTrackIndex = -1
    private var mMuxerStarted = false
    
    // Recording dimensions
    private var mRecordingWidth = 1920
    private var mRecordingHeight = 1080
    
    // Audio recording với AudioRecord + MediaCodec
    private var mAudioRecord: AudioRecord? = null
    private var mAudioThread: Thread? = null
    private var mAudioRecordingActive = AtomicBoolean(false)
    private var mAudioEncoderReady = false
    

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
        if (mIsRecording && mEncoderEGLSurface != null) {
            renderToEncoderSurface()
            drainEncoder() // Drain video encoder
            drainAudioEncoder() // Drain audio encoder
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

    /**
     * Render filtered frame to encoder surface
     */
    private fun renderToEncoderSurface() {
        try {
            // Save current EGL state
            val currentDisplay = EGL14.eglGetCurrentDisplay()
            val currentContext = EGL14.eglGetCurrentContext()
            val currentDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
            val currentReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
            
            // Make encoder surface current
            EGL14.eglMakeCurrent(mEncoderEGLDisplay, mEncoderEGLSurface, mEncoderEGLSurface, mEncoderEGLContext)
            
            // Render filtered content
            renderToTarget(0, mRecordingWidth, mRecordingHeight)
            
            // Set presentation time for MediaCodec
            EGL14.eglSwapBuffers(mEncoderEGLDisplay, mEncoderEGLSurface)
            
            // Restore original EGL state
            EGL14.eglMakeCurrent(currentDisplay, currentDrawSurface, currentReadSurface, currentContext)
            
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Error rendering to encoder surface: ${e.message}")
        }
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
        try {
            mVideoFile = videoFile
            
            // Sử dụng texture dimensions thực tế
            val recordWidth = if (mTextureWidth > 0) mTextureWidth else 1920
            val recordHeight = if (mTextureHeight > 0) mTextureHeight else 1080
            
            // Ensure even dimensions for H264
            mRecordingWidth = (recordWidth + 1) and 0xFFFFFFFE.toInt()
            mRecordingHeight = (recordHeight + 1) and 0xFFFFFFFE.toInt()
            
            Log.d("FilterRenderer", "Starting filtered recording: ${mRecordingWidth}x${mRecordingHeight}")
            
            // 1. Setup video encoder (always)
            setupVideoEncoder() // H264 video encoder
            
            // 2. Try setup audio encoder (fallback to video-only if fail)
            try {
                setupAudioEncoder() // AAC audio encoder
                startAudioRecording()
                Log.d("FilterRenderer", "Audio setup successful")
            } catch (e: Exception) {
                Log.w("FilterRenderer", "Audio setup failed, video-only mode: ${e.message}")
                mAudioEncoder = null
            }
            
            // 3. Setup MediaMuxer
            setupMediaMuxer(videoFile)
            
            mIsRecording = true
            callback(true)
            
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Failed to start filtered recording: ${e.message}")
            cleanup()
            callback(false)
        }
    }
    
    /**
     * Setup MediaCodec video encoder
     */
    private fun setupVideoEncoder() {
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mRecordingWidth, mRecordingHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, 0x7F000789) // COLOR_FormatSurface
            setInteger(MediaFormat.KEY_BIT_RATE, 8000000) // 8Mbps
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        
        mVideoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        mVideoEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mEncoderSurface = mVideoEncoder?.createInputSurface()
        
        // Setup EGL surface for encoder
        setupEncoderEGL()
        
        mVideoEncoder?.start()
        Log.d("FilterRenderer", "MediaCodec video encoder started")
    }
    
    /**
     * Setup AAC audio encoder
     */
    private fun setupAudioEncoder() {
        val format = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, 2) // AAC_PROFILE_LC
            setInteger(MediaFormat.KEY_BIT_RATE, 128000)
        }
        
        mAudioEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm")
        mAudioEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mAudioEncoder?.start()
        
        Log.d("FilterRenderer", "MediaCodec audio encoder started")
    }
    
    
    /**
     * Setup MediaMuxer
     */
    private fun setupMediaMuxer(videoFile: File) {
        mMediaMuxer = MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        Log.d("FilterRenderer", "MediaMuxer created for: ${videoFile.absolutePath}")
    }
    
    /**
     * Setup EGL context for encoder surface
     */
    private fun setupEncoderEGL() {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(display, null, 0, null, 0)
        
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        
        val attributes = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE
        )
        
        EGL14.eglChooseConfig(display, attributes, 0, configs, 0, configs.size, numConfigs, 0)
        
        val context = EGL14.eglCreateContext(
            display, configs[0], EGL14.eglGetCurrentContext(), 
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
        )
        
        mEncoderEGLSurface = EGL14.eglCreateWindowSurface(
            display, configs[0], mEncoderSurface, intArrayOf(EGL14.EGL_NONE), 0
        )
        
        mEncoderEGLDisplay = display
        mEncoderEGLContext = context
        
        Log.d("FilterRenderer", "Encoder EGL context setup complete")
    }

    /**
     * Start AudioRecord để lấy PCM data cho audio encoder
     */
    @SuppressLint("MissingPermission")
    private fun startAudioRecording() {
        val sampleRate = 44100
        val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
        val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize * 2 // Double buffer size for safety
        )
        
        mAudioRecord?.startRecording()
        mAudioRecordingActive.set(true)
        
        // Start background thread để process audio data
        mAudioThread = thread {
            processAudioData()
        }
        
        Log.d("FilterRenderer", "AudioRecord started")
    }
    
    /**
     * Process PCM data từ AudioRecord và đẩy vào audio encoder
     */
    private fun processAudioData() {
        val buffer = ByteArray(1024)
        
        while (mAudioRecordingActive.get()) {
            try {
                val audioRecord = mAudioRecord ?: break
                val audioEncoder = mAudioEncoder ?: break
                
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    // Lấy input buffer từ audio encoder
                    val inputBufferIndex = audioEncoder.dequeueInputBuffer(10000) // 10ms timeout
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = audioEncoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(buffer, 0, bytesRead)
                        
                        // Queue PCM data vào encoder
                        val presentationTimeUs = System.nanoTime() / 1000
                        audioEncoder.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, 0)
                    }
                    
                    // Drain audio encoder output
                    drainAudioEncoder()
                }
                
                Thread.sleep(10) // Small delay
                
            } catch (e: Exception) {
                Log.e("FilterRenderer", "Audio processing error: ${e.message}")
                break
            }
        }
        
        Log.d("FilterRenderer", "Audio processing thread ended")
    }

    /**
     * Signal end of stream cho audio encoder với empty buffer
     */
    private fun signalAudioEndOfStream() {
        val encoder = mAudioEncoder ?: return
        
        try {
            val inputBufferIndex = encoder.dequeueInputBuffer(10000) // 10ms timeout
            if (inputBufferIndex >= 0) {
                // Queue empty buffer với END_OF_STREAM flag
                encoder.queueInputBuffer(
                    inputBufferIndex, 
                    0, 
                    0, 
                    System.nanoTime() / 1000, 
                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
                Log.d("FilterRenderer", "Audio end of stream buffer queued")
            }
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Error queuing audio end buffer: ${e.message}")
        }
    }

    /**
     * Drain audio encoder output và add track nếu cần
     */
    private fun drainAudioEncoder() {
        try {
            val encoder = mAudioEncoder ?: return
            val muxer = mMediaMuxer ?: return
            
            val bufferInfo = MediaCodec.BufferInfo()
            
            while (true) {
                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)
                
                when {
                    encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                    encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (mAudioTrackIndex < 0) {
                            val audioFormat = encoder.outputFormat
                            mAudioTrackIndex = muxer.addTrack(audioFormat)
                            mAudioEncoderReady = true
                            Log.d("FilterRenderer", "Audio track added to muxer")
                            
                            // Check if can start muxer
                            checkAndStartMuxer()
                        }
                    }
                    encoderStatus >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                        if (encodedData != null && mMuxerStarted && mAudioTrackIndex >= 0) {
                            try {
                                muxer.writeSampleData(mAudioTrackIndex, encodedData, bufferInfo)
                            } catch (e: Exception) {
                                Log.e("FilterRenderer", "Error writing audio data: ${e.message}")
                            }
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Error draining audio encoder: ${e.message}")
        }
    }

    /**
     * Check và start MediaMuxer khi tracks ready
     */
    private fun checkAndStartMuxer() {
        val muxer = mMediaMuxer ?: return
        
        if (!mMuxerStarted && mVideoTrackIndex >= 0) {
            // Nếu có audio encoder, đợi cả 2 tracks
            // Nếu không có audio encoder, start với video-only
            val hasAudioEncoder = mAudioEncoder != null
            val canStart = if (hasAudioEncoder) {
                mAudioTrackIndex >= 0 // Đợi audio track
            } else {
                true // Video-only, start ngay
            }
            
            if (canStart) {
                muxer.start()
                mMuxerStarted = true
                val mode = if (hasAudioEncoder) "audio+video" else "video-only"
                Log.d("FilterRenderer", "MediaMuxer started with $mode tracks")
            }
        }
    }

    /**
     * Stop audio recording
     */
    private fun stopAudioRecording() {
        try {
            mAudioRecordingActive.set(false)
            mAudioRecord?.stop()
            mAudioRecord?.release()
            mAudioThread?.join(1000) // Wait max 1 second
            Log.d("FilterRenderer", "Audio recording stopped")
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Error stopping audio: ${e.message}")
        }
    }

    /**
     * Drain video encoder output
     */
    private fun drainEncoder() {
        try {
            val encoder = mVideoEncoder ?: return
            val muxer = mMediaMuxer ?: return
            
            val bufferInfo = MediaCodec.BufferInfo()
            
            while (true) {
                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 0)
                
                when {
                    encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER -> break
                    encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (mVideoTrackIndex < 0) {
                            val videoFormat = encoder.outputFormat
                            mVideoTrackIndex = muxer.addTrack(videoFormat)
                            Log.d("FilterRenderer", "Video track added to muxer")
                            
                            // Check if can start muxer
                            checkAndStartMuxer()
                        }
                    }
                    encoderStatus >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                        if (encodedData != null && mMuxerStarted && mVideoTrackIndex >= 0) {
                            try {
                                muxer.writeSampleData(mVideoTrackIndex, encodedData, bufferInfo)
                            } catch (e: Exception) {
                                Log.e("FilterRenderer", "Error writing video data: ${e.message}")
                            }
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Error draining video encoder: ${e.message}")
        }
    }
    

    /**
     * Dừng filtered video recording
     */
    fun stopFilteredVideoRecording(callback: (Boolean, File?) -> Unit) {
        try {
            if (!mIsRecording) {
                callback(false, null)
                return
            }
            
            mIsRecording = false
            
            // Stop audio recording
            stopAudioRecording()
            
            // Signal end of stream to encoders
            try {
                mVideoEncoder?.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.e("FilterRenderer", "Error signaling video end: ${e.message}")
            }
            
            // Signal audio end với empty buffer + END_OF_STREAM flag
            if (mAudioEncoder != null && mAudioEncoderReady && mAudioTrackIndex >= 0) {
                try {
                    signalAudioEndOfStream()
                    Log.d("FilterRenderer", "Audio end of stream signaled")
                } catch (e: Exception) {
                    Log.e("FilterRenderer", "Error signaling audio end: ${e.message}")
                }
            } else {
                Log.d("FilterRenderer", "Skipping audio end signal - encoder not ready")
            }
            
            // Drain remaining data from both encoders
            drainEncoder()
            drainAudioEncoder()
            
            // Stop and release both encoders
            try {
                mVideoEncoder?.stop()
                mVideoEncoder?.release()
            } catch (e: Exception) {
                Log.e("FilterRenderer", "Error stopping video encoder: ${e.message}")
            }
            
            // Chỉ stop audio encoder nếu đã ready
            if (mAudioEncoder != null && mAudioEncoderReady) {
                try {
                    mAudioEncoder?.stop()
                    mAudioEncoder?.release()
                    Log.d("FilterRenderer", "Audio encoder stopped")
                } catch (e: Exception) {
                    Log.e("FilterRenderer", "Error stopping audio encoder: ${e.message}")
                }
            }
            
            // Stop muxer
            if (mMuxerStarted) {
                mMediaMuxer?.stop()
            }
            mMediaMuxer?.release()
            
            // Release EGL resources
            releaseEncoderEGL()
            
            // Cleanup
            cleanup()
            
            Log.d("FilterRenderer", "Filtered video recording stopped successfully")
            callback(true, mVideoFile)
            
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Error stopping filtered recording: ${e.message}")
            cleanup()
            callback(false, null)
        }
    }

    /**
     * Release encoder EGL resources
     */
    private fun releaseEncoderEGL() {
        try {
            mEncoderEGLSurface?.let { surface ->
                EGL14.eglDestroySurface(mEncoderEGLDisplay, surface)
            }
            mEncoderEGLContext?.let { context ->
                EGL14.eglDestroyContext(mEncoderEGLDisplay, context)
            }
            mEncoderEGLDisplay?.let { display ->
                EGL14.eglTerminate(display)
            }
        } catch (e: Exception) {
            Log.e("FilterRenderer", "Error releasing encoder EGL: ${e.message}")
        }
    }

    /**
     * Cleanup all recording resources
     */
    private fun cleanup() {
        mVideoEncoder = null
        mAudioEncoder = null
        mMediaMuxer = null
        mEncoderSurface = null
        mEncoderEGLDisplay = null
        mEncoderEGLContext = null
        mEncoderEGLSurface = null
        mAudioRecord = null
        mAudioThread = null
        mVideoTrackIndex = -1
        mAudioTrackIndex = -1
        mMuxerStarted = false
        mIsRecording = false
        mAudioRecordingActive.set(false)
        mAudioEncoderReady = false
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = mIsRecording

    fun release() {
        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 