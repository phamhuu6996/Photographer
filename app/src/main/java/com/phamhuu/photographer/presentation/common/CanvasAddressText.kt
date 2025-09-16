package com.phamhuu.photographer.presentation.common

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.contants.Contants
import com.phamhuu.photographer.data.model.LocationInfo
import com.phamhuu.photographer.services.renderer.AddTextService

@Composable
fun CanvasAddressText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { Contants.TEXT_SIZE.toPx() }
    
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


