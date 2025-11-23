package com.phamhuu.photographer.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageMode

@Composable
fun DetectGestures(
    isBrightnessVisible: Boolean,
    brightness: Float,
    changeBrightness: (Float) -> Unit,
    isZoomVisible: Boolean,
    zoomState: Float,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = isBrightnessVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            SlideVertically(
                brightness,
                { brightness -> changeBrightness(brightness) }
            )
        }

        AnimatedVisibility(
            visible = isZoomVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ImageCustom(
                    id = R.drawable.zoom,
                    imageMode = ImageMode.MEDIUM,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${"%.1f".format(zoomState)}x",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                )
            }
        }
    }
}