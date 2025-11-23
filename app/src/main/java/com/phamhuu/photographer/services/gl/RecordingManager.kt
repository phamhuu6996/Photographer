package com.phamhuu.photographer.services.gl

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
import com.phamhuu.photographer.contants.AudioQuality
import com.phamhuu.photographer.contants.RecordingConstants
import com.phamhuu.photographer.contants.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class RecordingManager {
    private val audioQuality = AudioQuality.HIGH_QUALITY
    private val videoQuality = VideoQuality.HIGH_QUALITY
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
    private var mRecordingWidth = 1920
    private var mRecordingHeight = 1080
    private var mAudioRecord: AudioRecord? = null
    private val mAudioScope = CoroutineScope(Dispatchers.IO)
    private var mAudioJob: Job? = null
    private var mAudioRecordingActive = AtomicBoolean(false)
    private var mAudioEncoderReady = false

    fun startFilteredVideoRecording(
        videoFile: File,
        textureWidth: Int,
        textureHeight: Int
    ): Boolean {
        try {
            mVideoFile = videoFile
            val recordWidth = if (textureWidth > 0) textureWidth else 1920
            val recordHeight = if (textureHeight > 0) textureHeight else 1080
            mRecordingWidth = (recordWidth + 1) and 0xFFFFFFFE.toInt()
            mRecordingHeight = (recordHeight + 1) and 0xFFFFFFFE.toInt()
            Log.d(
                "RecordingManager",
                "Starting filtered recording: ${mRecordingWidth}x${mRecordingHeight}"
            )
            setupVideoEncoder()
            setupAudioEncoder()
            startAudioRecording()
            setupMediaMuxer(videoFile)
            mIsRecording = true
            return true
        } catch (e: Exception) {
            Log.e("RecordingManager", "Failed to start filtered recording: ${e.message}")
            cleanup()
            return false
        }
    }

    private fun setupVideoEncoder() {
        val format = MediaFormat.createVideoFormat(
            RecordingConstants.VIDEO_MIME_TYPE,
            mRecordingWidth,
            mRecordingHeight
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, RecordingConstants.VIDEO_COLOR_FORMAT)
            setInteger(MediaFormat.KEY_BIT_RATE, videoQuality.bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, videoQuality.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, videoQuality.iFrameInterval)
        }
        mVideoEncoder = MediaCodec.createEncoderByType(RecordingConstants.VIDEO_MIME_TYPE)
        mVideoEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mEncoderSurface = mVideoEncoder?.createInputSurface()
        setupEncoderEGL()
        mVideoEncoder?.start()
        Log.d(
            "RecordingManager",
            "MediaCodec video encoder started (${videoQuality.description}: ${mRecordingWidth}x${mRecordingHeight})"
        )
    }

    private fun setupAudioEncoder() {
        val format = MediaFormat.createAudioFormat(
            RecordingConstants.AUDIO_MIME_TYPE,
            audioQuality.sampleRate,
            audioQuality.channelCount
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, audioQuality.aacProfile)
            setInteger(MediaFormat.KEY_BIT_RATE, audioQuality.bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
            setInteger(MediaFormat.KEY_SAMPLE_RATE, audioQuality.sampleRate)
            setInteger(MediaFormat.KEY_CHANNEL_COUNT, audioQuality.channelCount)
        }
        mAudioEncoder = MediaCodec.createEncoderByType(RecordingConstants.AUDIO_MIME_TYPE)
        mAudioEncoder?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mAudioEncoder?.start()
        Log.d("RecordingManager", "MediaCodec audio encoder started (${audioQuality.description})")
    }

    private fun setupMediaMuxer(videoFile: File) {
        mMediaMuxer =
            MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        Log.d("RecordingManager", "MediaMuxer created for: ${videoFile.absolutePath}")
    }

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

    @SuppressLint("MissingPermission")
    private fun startAudioRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            audioQuality.sampleRate,
            audioQuality.channelConfig,
            RecordingConstants.AUDIO_FORMAT
        )
        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.CAMCORDER,
            audioQuality.sampleRate,
            audioQuality.channelConfig,
            RecordingConstants.AUDIO_FORMAT,
            bufferSize * audioQuality.bufferMultiplier
        )
        mAudioRecord?.startRecording()
        mAudioRecordingActive.set(true)
        mAudioJob = mAudioScope.launch {
            processAudioData()
        }
        Log.d("RecordingManager", "AudioRecord started (${audioQuality.description})")
    }

    private suspend fun processAudioData() {
        val buffer = ByteArray(audioQuality.processingBufferSize)
        while (mAudioRecordingActive.get()) {
            try {
                val audioRecord = mAudioRecord ?: break
                val audioEncoder = mAudioEncoder ?: break
                val bytesRead = audioRecord.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    val inputBufferIndex = audioEncoder.dequeueInputBuffer(10000)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = audioEncoder.getInputBuffer(inputBufferIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(buffer, 0, bytesRead)
                        val presentationTimeUs = System.nanoTime() / 1000
                        audioEncoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            bytesRead,
                            presentationTimeUs,
                            0
                        )
                    }
                    drainAudioEncoder()
                } else if (bytesRead == 0) {
                    delay(5)
                } else {
                    Log.e("RecordingManager", "Audio read error: $bytesRead")
                    break
                }
            } catch (e: Exception) {
                Log.e("RecordingManager", "Audio processing error: ${e.message}")
                break
            }
        }
        Log.d("RecordingManager", "Audio processing coroutine ended")
    }

    private fun signalAudioEndOfStream() {
        val encoder = mAudioEncoder ?: return
        val inputBufferIndex = encoder.dequeueInputBuffer(10000)
        if (inputBufferIndex >= 0) {
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

    private fun checkAndStartMuxer() {
        val muxer = mMediaMuxer ?: return
        if (!mMuxerStarted && mVideoTrackIndex >= 0) {
            val hasAudioEncoder = mAudioEncoder != null
            val canStart = if (hasAudioEncoder) {
                mAudioTrackIndex >= 0
            } else {
                true
            }
            if (canStart) {
                muxer.start()
                mMuxerStarted = true
                val mode = if (hasAudioEncoder) "audio+video" else "video-only"
                Log.d("RecordingManager", "MediaMuxer started with $mode tracks")
            }
        }
    }

    private suspend fun stopAudioRecording() {
        mAudioRecordingActive.set(false)
        mAudioJob?.cancel()
        mAudioJob?.join()
        mAudioRecord?.stop()
        mAudioRecord?.release()
        Log.d("RecordingManager", "Audio recording stopped")
    }

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

    fun stopFilteredVideoRecording(callback: (Boolean, File?) -> Unit) {
        try {
            if (!mIsRecording) {
                callback(false, null)
                return
            }
            mIsRecording = false
            runBlocking {
                stopAudioRecording()
            }
            mVideoEncoder?.signalEndOfInputStream()
            if (mAudioEncoder != null && mAudioEncoderReady && mAudioTrackIndex >= 0) {
                signalAudioEndOfStream()
                Log.d("RecordingManager", "Audio end of stream signaled")
            } else {
                Log.d("RecordingManager", "Skipping audio end signal - encoder not ready")
            }
            drainEncoder()
            drainAudioEncoder()
            mVideoEncoder?.stop()
            mVideoEncoder?.release()
            if (mAudioEncoder != null && mAudioEncoderReady) {
                mAudioEncoder?.stop()
                mAudioEncoder?.release()
                Log.d("RecordingManager", "Audio encoder stopped")
            }
            if (mMuxerStarted) {
                mMediaMuxer?.stop()
            }
            mMediaMuxer?.release()
            releaseEncoderEGL()
            cleanup()
            Log.d("RecordingManager", "Filtered video recording stopped successfully")
            callback(true, mVideoFile)
        } catch (e: Exception) {
            Log.e("RecordingManager", "Error stopping filtered recording: ${e.message}")
            cleanup()
            callback(false, null)
        }
    }

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

    private fun cleanup() {
        mVideoEncoder = null
        mAudioEncoder = null
        mMediaMuxer = null
        mEncoderSurface = null
        mEncoderEGLDisplay = null
        mEncoderEGLContext = null
        mEncoderEGLSurface = null
        mAudioRecord = null
        mAudioJob?.cancel()
        mAudioJob = null
        mVideoTrackIndex = -1
        mAudioTrackIndex = -1
        mMuxerStarted = false
        mIsRecording = false
        mAudioRecordingActive.set(false)
        mAudioEncoderReady = false
    }

    fun isRecording(): Boolean = mIsRecording
    fun getEncoderSurface(): Surface? = mEncoderSurface

    fun renderToEncoderSurface(
        renderFunction: (Int, Int) -> Unit
    ) {
        val currentDisplay = EGL14.eglGetCurrentDisplay()
        val currentContext = EGL14.eglGetCurrentContext()
        val currentDrawSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
        val currentReadSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
        EGL14.eglMakeCurrent(
            mEncoderEGLDisplay,
            mEncoderEGLSurface,
            mEncoderEGLSurface,
            mEncoderEGLContext
        )
        renderFunction(mRecordingWidth, mRecordingHeight)
        EGL14.eglSwapBuffers(mEncoderEGLDisplay, mEncoderEGLSurface)
        EGL14.eglMakeCurrent(currentDisplay, currentDrawSurface, currentReadSurface, currentContext)
    }

    fun drainEncoders() {
        drainEncoder()
        drainAudioEncoder()
    }
}
