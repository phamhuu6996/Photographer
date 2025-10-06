package com.phamhuu.photographer.contants

import android.media.AudioFormat
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.common.PopupItemData

enum class ImageMode(val size: Int) {
    SMALL(20), MEDIUM(30), LARGE(40)
}

enum class ImageFilter(
    val displayName: String, 
    val iconRes: Int,
) {
    BEAUTY("Beauty", R.drawable.magic);
    
    fun toPopupItemData(): PopupItemData {
        return PopupItemData(
            id = ordinal,
            title = displayName,
            iconRes = iconRes
        )
    }
}

enum class TimerDelay(val millisecond: Long) {
    OFF(0L),
    THREE(3000L),
    FIVE(5000L),
    TEN(10000L);

    fun next(): TimerDelay {
        return when (this) {
            OFF -> THREE
            THREE -> FIVE
            FIVE -> TEN
            TEN -> OFF
        }
    }

    fun toIcon(): Int {
        return when (this) {
            OFF -> R.drawable.delay0
            THREE -> R.drawable.delay3
            FIVE -> R.drawable.delay5
            TEN -> R.drawable.delay10
        }
    }

}

enum class RatioCamera(val ratio: AspectRatioStrategy) {
    RATIO_9_16(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY),
    RATIO_3_4(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY);

    fun next(): RatioCamera {
        return when (this) {
            RATIO_3_4 -> RATIO_9_16
            RATIO_9_16 -> RATIO_3_4
        }
    }

    fun toIcon(): Int {
        return when (this) {
            RATIO_9_16 -> R.drawable.resolution916
            RATIO_3_4 -> R.drawable.resolution34
        }
    }

    fun toRatio() : Float {
        return when (this) {
            RATIO_9_16 -> 9f / 16f
            RATIO_3_4 -> 3f / 4f
        }
    }
}

enum class SnackbarType(val actionLabel: String, val duration: Long) {
    SUCCESS("OK", 2000L),
    FAIL("Dismiss", 4000L),
    WARNING("Got it", 3000L),
    INFO("OK", 2000L)
}

object RecordingConstants {
    const val AUDIO_MIME_TYPE = "audio/mp4a-latm"
    const val VIDEO_MIME_TYPE = "video/avc"
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    const val VIDEO_COLOR_FORMAT = 0x7F000789
}

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
        processingBufferSize = 2048,
        description = "Low Quality (22.05kHz, Mono, 64kbps)"
    ),
    MEDIUM_QUALITY(
        sampleRate = 44100,
        channelCount = 1,
        bitRate = 128000,
        aacProfile = 2,
        channelConfig = AudioFormat.CHANNEL_IN_MONO,
        bufferMultiplier = 1,
        processingBufferSize = 2048,
        description = "Medium Quality (44.1kHz, Mono, 128kbps)"
    ),
    HIGH_QUALITY(
        sampleRate = 48000,
        channelCount = 2,
        bitRate = 256000,
        aacProfile = 2,
        channelConfig = AudioFormat.CHANNEL_IN_STEREO,
        bufferMultiplier = 2,
        processingBufferSize = 4096,
        description = "High Quality (48kHz, Stereo, 256kbps)"
    ),
    ULTRA_QUALITY(
        sampleRate = 48000,
        channelCount = 2,
        bitRate = 320000,
        aacProfile = 2,
        channelConfig = AudioFormat.CHANNEL_IN_STEREO,
        bufferMultiplier = 2,
        processingBufferSize = 4096,
        description = "Ultra Quality (48kHz, Stereo, 320kbps)"
    );
}

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

