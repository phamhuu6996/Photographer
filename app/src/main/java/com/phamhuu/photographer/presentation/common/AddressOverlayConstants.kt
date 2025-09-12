package com.phamhuu.photographer.presentation.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AddressOverlayConstants {
    // Text styling
    val TEXT_SIZE = 12.sp
    val TEXT_COLOR = Color.White
    val BORDER_COLOR = Color.Black
    val FONT_WEIGHT = FontWeight.Bold
    
    // Layout
    val PADDING = 12.dp
    val MAX_LINES = 3
    const val MAX_WIDTH_RATIO = 0.4f // 40% of container width
    const val LINE_HEIGHT_MULTIPLIER = 1.2f
    
    // For bitmap rendering
    const val TEXT_SIZE_RATIO = 0.025f // Relative to bitmap width
    const val STROKE_WIDTH = 4f
}
