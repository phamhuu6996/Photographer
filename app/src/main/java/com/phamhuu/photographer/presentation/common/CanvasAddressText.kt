package com.phamhuu.photographer.presentation.common

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.data.renderer.AddTextService

@Composable
fun CanvasAddressText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    borderColor: Color = Color.Black
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { AddressOverlayConstants.TEXT_SIZE.toPx() }
    
    // Use fixed canvas size to ensure text never gets cut off
    val canvasWidth = 180.dp // Fixed width with enough space for text
    val canvasHeight = 800.dp  // Fixed height all screen height
    
    Canvas(
        modifier = modifier.size(
            width = canvasWidth,
            height = canvasHeight
        )
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        // Use AddTextService for preview rendering
        AddTextService.renderAddressForPreview(
            canvas = canvas,
            address = text,
            canvasWidth = size.width,
            textSizePx = textSizePx
        )
    }
}

@Composable
fun CanvasAddressOverlay(
    locationInfo: com.phamhuu.photographer.models.LocationInfo?,
    isLoading: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    when {
        isLoading -> {
            CanvasAddressText(
                text = "Getting location...",
                modifier = modifier
            )
        }
        error != null -> {
            CanvasAddressText(
                text = "Location unavailable",
                textColor = Color.Red,
                modifier = modifier
            )
        }
        locationInfo != null -> {
            val formattedAddress = AddressTextUtils.formatAddress(locationInfo.address)
            CanvasAddressText(
                text = formattedAddress,
                modifier = modifier
            )
        }
        else -> {
            CanvasAddressText(
                text = "No location",
                textColor = Color.Gray,
                modifier = modifier
            )
        }
    }
}


