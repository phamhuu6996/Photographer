package com.phamhuu.photographer.enums

import androidx.camera.core.resolutionselector.AspectRatioStrategy
import com.phamhuu.photographer.R
import com.phamhuu.photographer.enums.TimerDelay.OFF
import com.phamhuu.photographer.enums.TimerDelay.TEN
import com.phamhuu.photographer.enums.TimerDelay.THREE
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

enum class BeautyEffect(val displayName: String, val iconRes: Int) {
    WHITENING("Làm trắng", R.drawable.ic_beauty_whitening),
    SLIM_FACE("Làm nhỏ mặt", R.drawable.ic_beauty_slim);
    
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
    NONE("Không", R.drawable.ic_filter),
    BEAUTY("Beauty", R.drawable.ic_beauty_whitening);
    
    fun toPopupItemData(): PopupItemData {
        return PopupItemData(
            id = ordinal,
            title = displayName,
            iconRes = iconRes
        )
    }
}

enum class TimerDelay {
    OFF,
    THREE,
    TEN;

    fun next(): TimerDelay {
        return when (this) {
            OFF -> THREE
            THREE -> TEN
            TEN -> OFF
        }
    }

    fun toIcon(): Int {
        return when (this) {
            OFF -> R.drawable.delay0
            THREE -> R.drawable.delay3
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

