package com.phamhuu.photographer.services.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.phamhuu.photographer.contants.Constants

/**
 * AddTextService - Handles text rendering for both photo capture and video recording
 *
 * This service provides:
 * 1. Canvas-based text rendering with Paint objects
 * 2. Text wrapping and positioning calculations
 * 3. Consistent styling between preview and output
 * 4. Support for both bitmap (photos) and video frame rendering
 */
object AddTextService {

    /**
     * Creates text paint for address rendering
     */
    fun createTextPaint(
        textSizePx: Float,
        color: Int = Color.White.toArgb(),
        shadowColor: Int = Color.Black.toArgb()
    ): Paint {
        return Paint().apply {
            this.color = color
            textSize = textSizePx
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT  // Use RIGHT align for automatic positioning
            setShadowLayer(6f, 0f, 0f, shadowColor)
        }
    }

    /**
     * Wraps text to fit within specified width using StaticLayout
     * StaticLayout automatically handles text wrapping based on width
     */
    fun wrapText(input: String, paint: Paint, maxWidth: Float): List<String> {
        if (input.isBlank()) {
            return emptyList()
        }
        
        val textPaint = TextPaint(paint)
        val layout = StaticLayout.Builder
            .obtain(input, 0, input.length, textPaint, maxWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()
        
        val lines = mutableListOf<String>()
        for (i in 0 until layout.lineCount) {
            val start = layout.getLineStart(i)
            val end = layout.getLineEnd(i)
            lines.add(input.substring(start, end).trim())
        }
        
        return lines
    }

    /**
     * Draws address text with border on canvas (shared logic for both photo and video)
     */
    fun drawAddressText(
        canvas: Canvas,
        lines: List<String>,
        startX: Float,
        startY: Float,
        textPaint: Paint,
        lineHeight: Float
    ) {
        var currentY = startY

        lines.forEach { line ->
            // When using Paint.Align.RIGHT, startX is where the right edge should be
            // No need for manual calculation since Paint handles the alignment
            canvas.drawText(line, startX, currentY, textPaint)
            currentY += lineHeight
        }
    }

    /**
     * Complete address rendering for photo capture (bitmap output)
     */
    fun renderAddressToPhoto(
        canvas: Canvas,
        address: String,
        bitmapWidth: Int,
    ) {
        val textSizePx = bitmapWidth * Constants.TEXT_SIZE_RATIO
        val textPaint = createTextPaint(textSizePx)

        val maxWidth = bitmapWidth * Constants.MAX_WIDTH_RATIO
        val wrappedLines = wrapText(address, textPaint, maxWidth)

        val padding = bitmapWidth * 0.02f
        val lineHeight = textSizePx * Constants.LINE_HEIGHT_MULTIPLIER

        // TOP_RIGHT positioning
        val startX = bitmapWidth - padding  // Right edge position
        val startY = padding + textSizePx

        drawAddressText(
            canvas = canvas,
            lines = wrappedLines,
            startX = startX,
            startY = startY,
            textPaint = textPaint,
            lineHeight = lineHeight,
        )
    }

    /**
     * Complete address rendering for video recording (frame-by-frame)
     * Similar to photo but optimized for video frame processing
     */
    fun renderAddressToVideo(
        canvas: Canvas,
        address: String,
        frameWidth: Int,
    ) {
        // Use same logic as photo for consistency
        renderAddressToPhoto(canvas, address, frameWidth)
    }

    /**
     * Render address for preview (Compose Canvas)
     * Used by CanvasAddressText composable
     */
    fun renderAddressForPreview(
        canvas: Canvas,
        address: String,
        canvasWidth: Float,
        textSizePx: Float,
        textColor: Int? = null,
        shadowColor: Int? = null
    ) {
        val textPaint = createTextPaint(
            textSizePx,
            color = textColor ?: Color.White.toArgb(),
            shadowColor = shadowColor ?: Color.Black.toArgb()
        )
        val maxWidth = canvasWidth * 0.9f
        val wrappedLines = wrapText(address, textPaint, maxWidth)

        val lineHeight = textSizePx * Constants.LINE_HEIGHT_MULTIPLIER

        // TOP_RIGHT positioning for preview - position text within canvas bounds
        // startX should be where the RIGHT edge of text should be, not where text starts
        val padding = canvasWidth * 0.02f
        val rightPadding = canvasWidth * 0.05f  // More padding for preview
        val startX = canvasWidth - rightPadding  // Right edge position with extra padding
        val startY = padding + textSizePx

        drawAddressText(
            canvas = canvas,
            lines = wrappedLines,
            startX = startX,
            startY = startY,
            textPaint = textPaint,
            lineHeight = lineHeight,
        )
    }

    fun addTextOverlay(
        bitmap: Bitmap,
        address: String
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Use AddTextService for photo capture
        renderAddressToPhoto(
            canvas = canvas,
            address = address,
            bitmapWidth = bitmap.width,
        )

        return mutableBitmap
    }
}
