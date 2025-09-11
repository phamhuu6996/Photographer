package com.phamhuu.photographer.enums

import androidx.camera.core.resolutionselector.AspectRatioStrategy
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.common.PopupItemData

enum class ImageMode(val size: Int) {
    SMALL(20), MEDIUM(30), LARGE(40)
}

enum class TypeModel3D(val displayName: String, val iconRes: Int) {
    GLASSES("Kính", R.drawable.ic_glasses),
    HAT("Mũ", R.drawable.ic_hat);
    
    fun toPopupItemData(): PopupItemData {
        return PopupItemData(
            id = ordinal,
            title = displayName,
            iconRes = iconRes
        )
    }
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
    RATIO_1_1(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY),
    RATIO_9_16(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY),
    RATIO_3_4(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY);

    fun next(): RatioCamera {
        return when (this) {
            RATIO_1_1 -> RATIO_9_16
            RATIO_9_16 -> RATIO_3_4
            RATIO_3_4 -> RATIO_1_1
        }
    }

    fun toIcon(): Int {
        return when (this) {
            RATIO_1_1 -> R.drawable.resolution11
            RATIO_9_16 -> R.drawable.resolution916
            RATIO_3_4 -> R.drawable.resolution34
        }
    }

    fun toRatio() : Float {
        return when (this) {
            RATIO_1_1 -> 1f
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

