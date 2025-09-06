package com.phamhuu.photographer.presentation.utils

import android.media.AudioFormat

/**
 * RecordingConstants - Quản lý tất cả constants cho video/audio recording
 * 
 * Class này chứa tất cả các thông số chất lượng cho:
 * 1. Video encoding (H264)
 * 2. Audio encoding (AAC)
 * 3. Audio recording (PCM)
 * 4. Buffer sizes và performance settings
 *
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
object RecordingConstants {
    
    // ===== BASIC CONSTANTS =====
    
    /** MIME type for AAC audio encoding */
    const val AUDIO_MIME_TYPE = "audio/mp4a-latm"
    
    /** MIME type for H.264 video encoding */
    const val VIDEO_MIME_TYPE = "video/avc"
    
    /** Audio format for recording (16-bit PCM) */
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    
    /** Color format for video encoding (Surface format) */
    const val VIDEO_COLOR_FORMAT = 0x7F000789 // COLOR_FormatSurface
}

// ===== AUDIO QUALITY ENUMS =====

/**
 * Audio quality presets with different sample rates, bit rates, and channel configurations.
 * Each preset is optimized for specific use cases.
 */
enum class AudioQuality(
    val sampleRate: Int,
    val channelCount: Int,
    val bitRate: Int,
    val aacProfile: Int,
    val channelConfig: Int,
    val bufferMultiplier: Int,
    val processingBufferSize: Int,
    val description: String
) {
    LOW_QUALITY(
        sampleRate = 22050,
        channelCount = 1,
        bitRate = 64000,
        aacProfile = 1,
        channelConfig = AudioFormat.CHANNEL_IN_MONO,
        bufferMultiplier = 1,
        processingBufferSize = 1024,
        description = "Low Quality (22.05kHz, Mono, 64kbps)"
    ),

    MEDIUM_QUALITY(
        sampleRate = 44100,
        channelCount = 1,
        bitRate = 128000,
        aacProfile = 2,
        channelConfig = AudioFormat.CHANNEL_IN_MONO,
        bufferMultiplier = 1,
        processingBufferSize = 1024,
        description = "Medium Quality (44.1kHz, Mono, 128kbps)"
    ),

    HIGH_QUALITY(
        sampleRate = 48000,
        channelCount = 2,
        bitRate = 256000,
        aacProfile = 2,
        channelConfig = AudioFormat.CHANNEL_IN_STEREO,
        bufferMultiplier = 2,
        processingBufferSize = 2048,
        description = "High Quality (48kHz, Stereo, 256kbps)"
    ),

    ULTRA_QUALITY(
        sampleRate = 48000,
        channelCount = 2,
        bitRate = 320000,
        aacProfile = 2,
        channelConfig = AudioFormat.CHANNEL_IN_STEREO,
        bufferMultiplier = 2,
        processingBufferSize = 2048,
        description = "Ultra Quality (48kHz, Stereo, 320kbps)"
    )
}

// ===== VIDEO QUALITY ENUMS =====

/**
 * Video quality presets with different bit rates, frame rates, and I-frame intervals.
 * Each preset is optimized for specific use cases and device capabilities.
 */
enum class VideoQuality(
    val bitRate: Int,
    val frameRate: Int,
    val iFrameInterval: Int,
    val description: String
) {
    LOW_QUALITY(
        bitRate = 2000000,
        frameRate = 24,
        iFrameInterval = 3,
        description = "Low Quality (2Mbps, 24fps, I-frame every 3s)"
    ),

    MEDIUM_QUALITY(
        bitRate = 4000000,
        frameRate = 30,
        iFrameInterval = 2,
        description = "Medium Quality (4Mbps, 30fps, I-frame every 2s)"
    ),

    HIGH_QUALITY(
        bitRate = 8000000,
        frameRate = 30,
        iFrameInterval = 2,
        description = "High Quality (8Mbps, 30fps, I-frame every 2s)"
    ),

    ULTRA_QUALITY(
        bitRate = 12000000,
        frameRate = 60,
        iFrameInterval = 1,
        description = "Ultra Quality (12Mbps, 60fps, I-frame every 1s)"
    )
}