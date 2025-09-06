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

/**
 * RecordingManager - Quản lý Video/Audio Recording với OpenGL Filter
 * 
 * Class này chịu trách nhiệm:
 * 1. Setup và quản lý MediaCodec encoders (H264 video, AAC audio)
 * 2. Quản lý MediaMuxer để multiplex video/audio streams
 * 3. Setup EGL context cho encoder surface (off-screen rendering)
 * 4. Capture audio từ microphone với AudioRecord
 * 5. Xử lý encoding pipeline và data flow
 * 6. Quản lý recording lifecycle (start/stop/cleanup)
 * 
 * Kiến trúc:
 * - Video: Camera → OpenGL Filter → Encoder Surface → MediaCodec → MediaMuxer
 * - Audio: Microphone → AudioRecord → MediaCodec → MediaMuxer
 * - EGL: Separate context cho encoder surface để render off-screen
 * 
 * Thread Safety:
 * - Audio processing chạy trên background thread
 * - Video rendering chạy trên OpenGL thread
 * - AtomicBoolean cho audio recording state
 * 
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
class RecordingManager {
    // ===================== QUALITY SETTINGS =====================
    /** Audio quality setting cho recording instance này */
    private val audioQuality = AudioQuality.HIGH_QUALITY
    
    /** Video quality setting cho recording instance này */
    private val videoQuality = VideoQuality.HIGH_QUALITY
    
    // ===================== VIDEO ENCODING =====================
    /** MediaCodec encoder cho H264 video */
    private var mVideoEncoder: MediaCodec? = null
    
    /** MediaCodec encoder cho AAC audio */
    private var mAudioEncoder: MediaCodec? = null
    
    /** MediaMuxer để multiplex video/audio streams thành MP4 */
    private var mMediaMuxer: MediaMuxer? = null
    
    /** Input surface cho video encoder (từ MediaCodec) */
    private var mEncoderSurface: Surface? = null
    
    // ===================== EGL CONTEXT FOR ENCODER =====================
    /** EGL display cho encoder surface */
    private var mEncoderEGLDisplay: EGLDisplay? = null
    
    /** EGL context cho encoder surface */
    private var mEncoderEGLContext: EGLContext? = null
    
    /** EGL surface cho encoder (off-screen rendering) */
    private var mEncoderEGLSurface: EGLSurface? = null
    
    // ===================== RECORDING STATE =====================
    /** Flag xác định có đang recording không */
    private var mIsRecording = false
    
    /** File output cho video recording */
    private var mVideoFile: File? = null
    
    /** Track index của video trong MediaMuxer */
    private var mVideoTrackIndex = -1
    
    /** Track index của audio trong MediaMuxer */
    private var mAudioTrackIndex = -1
    
    /** Flag xác định MediaMuxer đã start chưa */
    private var mMuxerStarted = false
    
    // ===================== RECORDING DIMENSIONS =====================
    /** Chiều rộng recording (được làm chẵn cho H264) */
    private var mRecordingWidth = 1920
    
    /** Chiều cao recording (được làm chẵn cho H264) */
    private var mRecordingHeight = 1080
    
    // ===================== AUDIO RECORDING =====================
    /** AudioRecord để capture audio từ microphone */
    private var mAudioRecord: AudioRecord? = null
    
    /** Background thread xử lý audio data */
    private var mAudioThread: Thread? = null
    
    /** Atomic flag cho audio recording state (thread-safe) */
    private var mAudioRecordingActive = AtomicBoolean(false)
    
    /** Flag xác định audio encoder đã ready chưa */
    private var mAudioEncoderReady = false

    /**
     * Bắt đầu ghi video với filter effects
     * 
     * Chức năng:
     * 1. Setup video encoder (H264) với input surface
     * 2. Setup audio encoder (AAC) và AudioRecord
     * 3. Setup MediaMuxer để multiplex streams
     * 4. Setup EGL context cho encoder surface
     * 5. Start audio recording thread
     * 
     * Pipeline:
     * - Video: OpenGL → Encoder Surface → MediaCodec → MediaMuxer
     * - Audio: Microphone → AudioRecord → MediaCodec → MediaMuxer
     * 
     * @param videoFile File output cho video (MP4)
     * @param textureWidth Chiều rộng texture từ camera
     * @param textureHeight Chiều cao texture từ camera
     * @param callback Callback trả về kết quả (true = thành công, false = lỗi)
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
            setupAudioEncoder() // AAC audio encoder
            startAudioRecording()
            
            // 2. Setup MediaMuxer
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
     * Setup MediaCodec video encoder (H264)
     * 
     * Chức năng:
     * 1. Tạo MediaFormat với H264 codec
     * 2. Configure encoder với COLOR_FormatSurface
     * 3. Tạo input surface cho encoder
     * 4. Setup EGL context cho surface
     * 5. Start encoder
     * 
     * Lưu ý: Sử dụng COLOR_FormatSurface để render trực tiếp từ OpenGL
     */
    private fun setupVideoEncoder() {
        val format = MediaFormat.createVideoFormat(RecordingConstants.VIDEO_MIME_TYPE, mRecordingWidth, mRecordingHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, RecordingConstants.VIDEO_COLOR_FORMAT) // COLOR_FormatSurface
            setInteger(MediaFormat.KEY_BIT_RATE, videoQuality.bitRate) // Video quality bit rate
            setInteger(MediaFormat.KEY_FRAME_RATE, videoQuality.frameRate) // Video quality frame rate
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoQuality.iFrameInterval) // Video quality I-frame interval
        }
        
        mVideoEncoder = MediaCodec.createEncoderByType(RecordingConstants.VIDEO_MIME_TYPE)
        mVideoEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mEncoderSurface = mVideoEncoder?.createInputSurface()
        
        // Setup EGL surface for encoder
        setupEncoderEGL()
        
        mVideoEncoder?.start()
        Log.d("RecordingManager", "MediaCodec video encoder started (${videoQuality.description}: ${mRecordingWidth}x${mRecordingHeight})")
    }
    
    /**
     * Setup MediaCodec audio encoder (AAC) - High Quality
     * 
     * Chức năng:
     * 1. Tạo MediaFormat với AAC codec
     * 2. Configure encoder với AAC profile
     * 3. Start encoder
     * 
     * Chất lượng High:
     * - Sample Rate: 48000 Hz (Professional standard)
     * - Channels: 2 (Stereo)
     * - Bit Rate: 256 kbps (High quality)
     * - Profile: AAC_LC (Low Complexity)
     * 
     * Lưu ý: Audio data sẽ được feed từ AudioRecord
     */
    private fun setupAudioEncoder() {
        val format = MediaFormat.createAudioFormat(RecordingConstants.AUDIO_MIME_TYPE, audioQuality.sampleRate, audioQuality.channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, audioQuality.aacProfile) // AAC profile from quality setting
            setInteger(MediaFormat.KEY_BIT_RATE, audioQuality.bitRate) // Audio quality bit rate
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0) // Let encoder determine max input size
            setInteger(MediaFormat.KEY_SAMPLE_RATE, audioQuality.sampleRate) // Explicit sample rate
            setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioQuality.channelCount) // Explicit channel count
        }
        
        mAudioEncoder = MediaCodec.createEncoderByType(RecordingConstants.AUDIO_MIME_TYPE)
        mAudioEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mAudioEncoder?.start()
        
        Log.d("RecordingManager", "MediaCodec audio encoder started (${audioQuality.description})")
    }
    
    /**
     * Setup MediaMuxer để multiplex video/audio streams
     * 
     * Chức năng:
     * 1. Tạo MediaMuxer với MP4 output format
     * 2. Set output file path
     * 
     * @param videoFile File output cho video
     */
    private fun setupMediaMuxer(videoFile: File) {
        mMediaMuxer = MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        Log.d("RecordingManager", "MediaMuxer created for: ${videoFile.absolutePath}")
    }
    
    /**
     * Setup EGL context cho encoder surface
     * 
     * Chức năng:
     * 1. Initialize EGL display
     * 2. Choose EGL config
     * 3. Create EGL context
     * 4. Create EGL surface từ encoder surface
     * 
     * Lưu ý: EGL context này dùng để render off-screen lên encoder surface
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
     * Start AudioRecord để capture audio từ microphone - High Quality
     * 
     * Chức năng:
     * 1. Setup AudioRecord với PCM 16-bit, 48kHz, stereo
     * 2. Start recording
     * 3. Start background thread để process audio data
     * 
     * Chất lượng High:
     * - Sample Rate: 48000 Hz (Professional standard)
     * - Channels: Stereo (2 channels)
     * - Bit Depth: 16-bit PCM
     * - Buffer Size: Double minimum size for stability
     * 
     * Thread Safety: Audio processing chạy trên background thread
     * 
     * @SuppressLint("MissingPermission") - Permission được check ở level cao hơn
     */
    @SuppressLint("MissingPermission")
    private fun startAudioRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(audioQuality.sampleRate, audioQuality.channelConfig, RecordingConstants.AUDIO_FORMAT)

        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.CAMCORDER,
            audioQuality.sampleRate,
            audioQuality.channelConfig,
            RecordingConstants.AUDIO_FORMAT,
            bufferSize * audioQuality.bufferMultiplier // Buffer size multiplier from quality setting
        )
        
        mAudioRecord?.startRecording()
        mAudioRecordingActive.set(true)
        
        // Start background thread để process audio data
        mAudioThread = thread {
            processAudioData()
        }
        
        Log.d("RecordingManager", "AudioRecord started (${audioQuality.description})")
    }
    
    /**
     * Process PCM data từ AudioRecord và feed vào audio encoder
     * 
     * Chức năng:
     * 1. Đọc PCM data từ AudioRecord
     * 2. Feed data vào audio encoder input buffer
     * 3. Drain audio encoder output
     * 4. Chạy liên tục cho đến khi stop
     * 
     * Thread: Chạy trên background thread
     * Thread Safety: Sử dụng AtomicBoolean cho state
     */
    private fun processAudioData() {
        val buffer = ByteArray(audioQuality.processingBufferSize) // Buffer size from quality setting
        
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
                        
                        // Queue PCM data vào encoder với timestamp chính xác
                        val presentationTimeUs = System.nanoTime() / 1000
                        audioEncoder.queueInputBuffer(inputBufferIndex, 0, bytesRead, presentationTimeUs, 0)
                    }
                    
                    // Drain audio encoder output
                    drainAudioEncoder()
                } else if (bytesRead == 0) {
                    // No data available, sleep briefly
                    Thread.sleep(5)
                } else {
                    // Error reading audio data
                    Log.e("RecordingManager", "Audio read error: $bytesRead")
                    break
                }
                
            } catch (e: Exception) {
                Log.e("RecordingManager", "Audio processing error: ${e.message}")
                break
            }
        }
        
        Log.d("RecordingManager", "Audio processing thread ended")
    }

    /**
     * Signal end of stream cho audio encoder
     * 
     * Chức năng:
     * 1. Queue empty buffer với END_OF_STREAM flag
     * 2. Báo hiệu audio encoder kết thúc input
     * 
     * Lưu ý: Phải gọi trước khi stop encoder
     */
    private fun signalAudioEndOfStream() {
        val encoder = mAudioEncoder ?: return
        
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
    }

    /**
     * Drain audio encoder output và write vào MediaMuxer
     * 
     * Chức năng:
     * 1. Dequeue output buffer từ audio encoder
     * 2. Add audio track vào MediaMuxer nếu chưa có
     * 3. Write encoded data vào MediaMuxer
     * 4. Release output buffer
     * 
     * Lưu ý: Phải gọi liên tục trong recording loop
     */
    private fun drainAudioEncoder() {
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
                        muxer.writeSampleData(mAudioTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false)
                }
            }
        }
    }

    /**
     * Check và start MediaMuxer khi tracks ready
     * 
     * Logic:
     * 1. Nếu có audio encoder: đợi cả video và audio tracks
     * 2. Nếu không có audio encoder: start với video-only
     * 3. Start MediaMuxer khi đủ tracks
     * 
     * Lưu ý: MediaMuxer chỉ start được 1 lần
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
     * Stop audio recording và cleanup resources
     * 
     * Chức năng:
     * 1. Set audio recording flag = false
     * 2. Stop và release AudioRecord
     * 3. Join audio thread với timeout
     * 
     * Thread Safety: Sử dụng AtomicBoolean để stop thread
     */
    private fun stopAudioRecording() {
        mAudioRecordingActive.set(false)
        mAudioRecord?.stop()
        mAudioRecord?.release()
        mAudioThread?.join(1000) // Wait max 1 second
        Log.d("RecordingManager", "Audio recording stopped")
    }

    /**
     * Drain video encoder output và write vào MediaMuxer
     * 
     * Chức năng:
     * 1. Dequeue output buffer từ video encoder
     * 2. Add video track vào MediaMuxer nếu chưa có
     * 3. Write encoded data vào MediaMuxer
     * 4. Release output buffer
     * 
     * Lưu ý: Phải gọi liên tục trong recording loop
     */
    private fun drainEncoder() {
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
                        muxer.writeSampleData(mVideoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false)
                }
            }
        }
    }
    
    /**
     * Dừng filtered video recording và cleanup tất cả resources
     * 
     * Chức năng:
     * 1. Stop audio recording
     * 2. Signal end of stream cho encoders
     * 3. Drain remaining data từ encoders
     * 4. Stop và release encoders
     * 5. Stop và release MediaMuxer
     * 6. Release EGL resources
     * 7. Cleanup tất cả variables
     * 
     * @param callback Callback trả về kết quả và file video
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
            mVideoEncoder?.signalEndOfInputStream()
            
            // Signal audio end với empty buffer + END_OF_STREAM flag
            if (mAudioEncoder != null && mAudioEncoderReady && mAudioTrackIndex >= 0) {
                signalAudioEndOfStream()
                Log.d("RecordingManager", "Audio end of stream signaled")
            } else {
                Log.d("RecordingManager", "Skipping audio end signal - encoder not ready")
            }
            
            // Drain remaining data from both encoders
            drainEncoder()
            drainAudioEncoder()
            
            // Stop and release both encoders
            mVideoEncoder?.stop()
            mVideoEncoder?.release()
            
            // Chỉ stop audio encoder nếu đã ready
            if (mAudioEncoder != null && mAudioEncoderReady) {
                mAudioEncoder?.stop()
                mAudioEncoder?.release()
                Log.d("RecordingManager", "Audio encoder stopped")
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
     * Release EGL resources cho encoder surface
     * 
     * Chức năng:
     * 1. Destroy EGL surface
     * 2. Destroy EGL context
     * 3. Terminate EGL display
     * 
     * Lưu ý: Phải gọi để tránh memory leak
     */
    private fun releaseEncoderEGL() {
        mEncoderEGLSurface?.let { surface ->
            EGL14.eglDestroySurface(mEncoderEGLDisplay, surface)
        }
        mEncoderEGLContext?.let { context ->
            EGL14.eglDestroyContext(mEncoderEGLDisplay, context)
        }
        mEncoderEGLDisplay?.let { display ->
            EGL14.eglTerminate(display)
        }
    }

    /**
     * Cleanup tất cả recording resources và reset state
     * 
     * Chức năng:
     * 1. Set tất cả variables về null/default values
     * 2. Reset recording state
     * 3. Reset track indices
     * 4. Reset flags
     * 
     * Lưu ý: Được gọi khi error hoặc stop recording
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
     * Check xem có đang recording không
     * 
     * @return true nếu đang recording, false nếu không
     */
    fun isRecording(): Boolean = mIsRecording

    /**
     * Get encoder surface cho video recording
     * 
     * @return Surface cho video encoder, null nếu chưa setup
     */
    fun getEncoderSurface(): Surface? = mEncoderSurface

    /**
     * Render filtered content lên encoder surface
     * 
     * Chức năng:
     * 1. Save current EGL state
     * 2. Make encoder surface current
     * 3. Call render function với recording dimensions
     * 4. Swap buffers để commit frame
     * 5. Restore original EGL state
     * 
     * @param renderFunction Function render với (width, height) parameters
     */
    fun renderToEncoderSurface(renderFunction: (Int, Int) -> Unit) {
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
    }

    /**
     * Drain cả video và audio encoders
     * 
     * Chức năng:
     * 1. Drain video encoder output
     * 2. Drain audio encoder output
     * 
     * Lưu ý: Phải gọi liên tục trong recording loop
     */
    fun drainEncoders() {
        drainEncoder() // Drain video encoder
        drainAudioEncoder() // Drain audio encoder
    }
    
}
