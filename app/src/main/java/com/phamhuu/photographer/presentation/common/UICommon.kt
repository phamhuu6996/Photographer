package com.phamhuu.photographer.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.R

@Composable
fun SlideHorizontal(
    value: Float,
    onExposureChange: (Float) -> Unit,
    firstColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    lastColor: Color = MaterialTheme.colorScheme.primary,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ImageCustom(
            id = R.drawable.brightness,
            color = lastColor,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thanh gradient dưới slider
            CustomPaintSlider(
                firstColor = firstColor, // Màu vàng
                lastColor = lastColor // Màu đỏ
            )

            // Slider tuỳ chỉnh
            CustomSlider(
                value = value,
                onExposureChange = onExposureChange,
            )

        }
    }
}

@Composable
fun SlideVertically(
    value: Float,
    onExposureChange: (Float) -> Unit,
    firstColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    lastColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(modifier = Modifier.rotate(270f)) {
        SlideHorizontal(
            value = value,
            onExposureChange = onExposureChange,
            firstColor = firstColor,
            lastColor = lastColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSlider(
    value: Float,
    onExposureChange: (Float) -> Unit,
    width: Int = 50,
    height: Int = 10,
    widthThumb: Int = 24,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    activeTrackColor: Color = Color.Transparent,
    inactiveTrackColor: Color = Color.Transparent,
) {
    val animatedValue by animateFloatAsState(targetValue = value, label = "")
    Slider(
        value = animatedValue,

        onValueChange = { valueData ->
            onExposureChange(valueData)
        },
        colors = SliderDefaults.colors(
            thumbColor = thumbColor, // Màu của thumb
            activeTrackColor = activeTrackColor, // Ẩn màu mặc định
            inactiveTrackColor = inactiveTrackColor
        ),
        modifier = Modifier
            .width((width + widthThumb).dp)
            .height(height.dp),
        thumb = {
            // Thumb tuỳ chỉnh (hình tròn phát sáng)
            Canvas(modifier = Modifier.size(widthThumb.dp)) {
                drawCircle(
                    color = thumbColor,
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = thumbColor.copy(alpha = 0.5f), // Màu vàng phát sáng
                    radius = size.minDimension / 1.8f
                )
            }
        }
    )
}

@Composable
fun CustomPaintSlider(
    width: Int = 50,
    height: Int = 10,
    firstColor: Color,
    lastColor: Color,
) {
    Canvas(
        modifier = Modifier
            .width(width.dp)
            .height(height.dp)
    ) {
        val gradient = Brush.horizontalGradient(
            colors = listOf(firstColor, lastColor)// Gradient vàng → đỏ
        )
        drawRoundRect(
            brush = gradient,
            size = size,
            cornerRadius = CornerRadius(50f, 50f)
        )
    }
}