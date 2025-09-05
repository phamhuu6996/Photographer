package com.phamhuu.photographer.presentation.utils

import android.annotation.SuppressLint
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
import android.util.Log
import android.view.Surface
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class RecordingManager {
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

    /**
     * Bắt đầu ghi video với filter sử dụng MediaCodec + MediaMuxer
     */
    fun startFilteredVideoRecording(videoFile: File, textureWidth: Int, textureHeight: Int, callback: (Boolean) -> Unit) {
        try {
            mVideoFile = videoFile
            
            // Sử dụng texture dimensions thực tế
            val recordWidth = if (textureWidth > 0) textureWidth else 1920
            val recordHeight = if (textureHeight > 0) textureHeight else 1080
            
            // Ensure even dimensions for H264
            mRecordingWidth = (recordWidth + 1) and 0xFFFFFFFE.toInt()
            mRecordingHeight = (recordHeight + 1) and 0xFFFFFFFE.toInt()
            
            Log.d("RecordingManager", "Starting filtered recording: ${mRecordingWidth}x${mRecordingHeight}")
            
            // 1. Setup video encoder (always)
            setupVideoEncoder() // H264 video encoder
            
            // 2. Try setup audio encoder (fallback to video-only if fail)
            try {
                setupAudioEncoder() // AAC audio encoder
                startAudioRecording()
                Log.d("RecordingManager", "Audio setup successful")
            } catch (e: Exception) {
                Log.w("RecordingManager", "Audio setup failed, video-only mode: ${e.message}")
                mAudioEncoder = null
            }
            
            // 3. Setup MediaMuxer
            setupMediaMuxer(videoFile)
            
            mIsRecording = true
            callback(true)
            
        } catch (e: Exception) {
            Log.e("RecordingManager", "Failed to start filtered recording: ${e.message}")
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
        Log.d("RecordingManager", "MediaCodec video encoder started")
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
        
        Log.d("RecordingManager", "MediaCodec audio encoder started")
    }
    
    
    /**
     * Setup MediaMuxer
     */
    private fun setupMediaMuxer(videoFile: File) {
        mMediaMuxer = MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        Log.d("RecordingManager", "MediaMuxer created for: ${videoFile.absolutePath}")
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
        
        Log.d("RecordingManager", "Encoder EGL context setup complete")
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
        
        Log.d("RecordingManager", "AudioRecord started")
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
                Log.e("RecordingManager", "Audio processing error: ${e.message}")
                break
            }
        }
        
        Log.d("RecordingManager", "Audio processing thread ended")
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
                Log.d("RecordingManager", "Audio end of stream buffer queued")
            }
        } catch (e: Exception) {
            Log.e("RecordingManager", "Error queuing audio end buffer: ${e.message}")
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
                            Log.d("RecordingManager", "Audio track added to muxer")
                            
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
                                Log.e("RecordingManager", "Error writing audio data: ${e.message}")
                            }
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("RecordingManager", "Error draining audio encoder: ${e.message}")
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
                Log.d("RecordingManager", "MediaMuxer started with $mode tracks")
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
            Log.d("RecordingManager", "Audio recording stopped")
        } catch (e: Exception) {
            Log.e("RecordingManager", "Error stopping audio: ${e.message}")
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
                            Log.d("RecordingManager", "Video track added to muxer")
                            
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
                                Log.e("RecordingManager", "Error writing video data: ${e.message}")
                            }
                        }
                        encoder.releaseOutputBuffer(encoderStatus, false)
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("RecordingManager", "Error draining video encoder: ${e.message}")
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
                Log.e("RecordingManager", "Error signaling video end: ${e.message}")
            }
            
            // Signal audio end với empty buffer + END_OF_STREAM flag
            if (mAudioEncoder != null && mAudioEncoderReady && mAudioTrackIndex >= 0) {
                try {
                    signalAudioEndOfStream()
                    Log.d("RecordingManager", "Audio end of stream signaled")
                } catch (e: Exception) {
                    Log.e("RecordingManager", "Error signaling audio end: ${e.message}")
                }
            } else {
                Log.d("RecordingManager", "Skipping audio end signal - encoder not ready")
            }
            
            // Drain remaining data from both encoders
            drainEncoder()
            drainAudioEncoder()
            
            // Stop and release both encoders
            try {
                mVideoEncoder?.stop()
                mVideoEncoder?.release()
            } catch (e: Exception) {
                Log.e("RecordingManager", "Error stopping video encoder: ${e.message}")
            }
            
            // Chỉ stop audio encoder nếu đã ready
            if (mAudioEncoder != null && mAudioEncoderReady) {
                try {
                    mAudioEncoder?.stop()
                    mAudioEncoder?.release()
                    Log.d("RecordingManager", "Audio encoder stopped")
                } catch (e: Exception) {
                    Log.e("RecordingManager", "Error stopping audio encoder: ${e.message}")
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
            
            Log.d("RecordingManager", "Filtered video recording stopped successfully")
            callback(true, mVideoFile)
            
        } catch (e: Exception) {
            Log.e("RecordingManager", "Error stopping filtered recording: ${e.message}")
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
            Log.e("RecordingManager", "Error releasing encoder EGL: ${e.message}")
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

    /**
     * Get encoder surface
     */
    fun getEncoderSurface(): Surface? = mEncoderSurface

    /**
     * Render to encoder surface
     */
    fun renderToEncoderSurface(renderFunction: (Int, Int) -> Unit) {
        try {
            // Save current EGL state
            val currentDisplay = EGL14.eglGetCurrentDisplay()
            val currentContext = EGL14.eglGetCurrentContext()
            val currentDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
            val currentReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
            
            // Make encoder surface current
            EGL14.eglMakeCurrent(mEncoderEGLDisplay, mEncoderEGLSurface, mEncoderEGLSurface, mEncoderEGLContext)
            
            // Render filtered content
            renderFunction(mRecordingWidth, mRecordingHeight)
            
            // Set presentation time for MediaCodec
            EGL14.eglSwapBuffers(mEncoderEGLDisplay, mEncoderEGLSurface)
            
            // Restore original EGL state
            EGL14.eglMakeCurrent(currentDisplay, currentDrawSurface, currentReadSurface, currentContext)
            
        } catch (e: Exception) {
            Log.e("RecordingManager", "Error rendering to encoder surface: ${e.message}")
        }
    }

    /**
     * Drain encoders
     */
    fun drainEncoders() {
        drainEncoder() // Drain video encoder
        drainAudioEncoder() // Drain audio encoder
    }
}
