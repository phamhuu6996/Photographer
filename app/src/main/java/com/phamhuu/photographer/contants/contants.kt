package com.phamhuu.photographer.contants

import android.media.AudioFormat
import androidx.compose.ui.unit.sp

object Contants {
    const val DATE_TIME_FORMAT = "yyyyMMdd_HHmmss"
    const val IMG_PREFIX = "IMG_"
    const val VID_PREFIX = "VID_"
    const val EXT_IMG = "jpg"
    const val EXT_VID = "mp4"
    const val FORMAT_TIME = "%02d:%02d"
    
    // Address overlay constants
    val TEXT_SIZE = 12.sp
    const val MAX_LINES = 3
    const val MAX_WIDTH_RATIO = 0.4f // 40% of container width
    const val LINE_HEIGHT_MULTIPLIER = 1.2f
    const val TEXT_SIZE_RATIO = 0.025f // Relative to bitmap width

    // Load more
    const val MAX_RECORD_LOAD_MORE = 10
}