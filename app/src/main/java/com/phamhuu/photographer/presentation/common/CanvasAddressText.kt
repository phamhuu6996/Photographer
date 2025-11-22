package com.phamhuu.photographer.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.contants.Constants
import com.phamhuu.photographer.data.model.LocationInfo
import com.phamhuu.photographer.services.renderer.AddTextService

@Composable
fun CanvasAddressText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { Constants.TEXT_SIZE.toPx() }
    
    // Use fixed canvas size to ensure text never gets cut off
    val canvasWidth = 180.dp // Fixed width with enough space for text
    val canvasHeight = 80.dp  // Fixed height all screen height
    
    Canvas(
        modifier = modifier.size(
            width = canvasWidth,
            height = canvasHeight
        )
    ) {
        val canvas = drawContext.canvas.nativeCanvas
        
        // Use AddTextService for preview rendering with theme colors
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
    locationInfo: LocationInfo?,
    modifier: Modifier
) {
    if(locationInfo != null) {
        CanvasAddressText(
            text = locationInfo.address,
            modifier = modifier
        )
    }
    else {
        Spacer(modifier)
    }
}


