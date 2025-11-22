package com.phamhuu.photographer.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.BeautySettings

/**
 * BeautyAdjustmentPanel - Panel điều chỉnh các thông số beauty filter
 * 
 * Chức năng:
 * - 5 slider cho skin_smoothing, whiteness, thin_face, big_eye, blend_level
 * - Real-time preview updates
 * - Reset to defaults button
 * - Smooth animations
 * 
 * @param isVisible Panel visibility state
 * @param beautySettings Current beauty settings
 * @param onSkinSmoothingChange Callback for skin smoothing changes
 * @param onWhitenessChange Callback for whiteness changes
 * @param onThinFaceChange Callback for thin face changes
 * @param onBigEyeChange Callback for big eye changes
 * @param onBlendLevelChange Callback for blend level changes
 * @param onResetToDefaults Callback to reset all values to defaults
 * @param onDismiss Callback to close the panel
 * 
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
@Composable
fun BeautyAdjustmentPanel(
    isVisible: Boolean,
    beautySettings: BeautySettings,
    onSkinSmoothingChange: (Float) -> Unit,
    onWhitenessChange: (Float) -> Unit,
    onThinFaceChange: (Float) -> Unit,
    onBigEyeChange: (Float) -> Unit,
    onBlendLevelChange: (Float) -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        // Background overlay để tap ra ngoài tắt popup
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        ) {
            // Card chính không bị ảnh hưởng bởi click
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.BottomCenter)
                    .clickable(enabled = false) { }, // Prevent click through
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.beauty_adjustment),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row {
                            // Reset button (using text for now)
                            Text(
                                text = stringResource(R.string.reset),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { onResetToDefaults() }
                                    .padding(6.dp)
                            )
                            
                            // Close button (using text for now)
                            Text(
                                text = stringResource(R.string.close),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { onDismiss() }
                                    .padding(6.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Beauty sliders
                    BeautySlider(
                        label = stringResource(R.string.skin_smoothing),
                        value = beautySettings.skinSmoothing,
                        valueRange = BeautySettings.SKIN_SMOOTHING_MIN..BeautySettings.SKIN_SMOOTHING_MAX,
                        onValueChange = onSkinSmoothingChange
                    )
                    
                    BeautySlider(
                        label = stringResource(R.string.whiteness),
                        value = beautySettings.whiteness,
                        valueRange = BeautySettings.WHITENESS_MIN..BeautySettings.WHITENESS_MAX,
                        onValueChange = onWhitenessChange
                    )
                    
                    BeautySlider(
                        label = stringResource(R.string.thin_face),
                        value = beautySettings.thinFace,
                        valueRange = BeautySettings.THIN_FACE_MIN..BeautySettings.THIN_FACE_MAX,
                        onValueChange = onThinFaceChange,
                        requiresFace = true
                    )
                    
                    BeautySlider(
                        label = stringResource(R.string.big_eye),
                        value = beautySettings.bigEye,
                        valueRange = BeautySettings.BIG_EYE_MIN..BeautySettings.BIG_EYE_MAX,
                        onValueChange = onBigEyeChange,
                        requiresFace = true
                    )
                    
                    BeautySlider(
                        label = stringResource(R.string.blend_level),
                        value = beautySettings.blendLevel,
                        valueRange = BeautySettings.BLEND_LEVEL_MIN..BeautySettings.BLEND_LEVEL_MAX,
                        onValueChange = onBlendLevelChange,
                        requiresFace = true
                    )
                }
            }
        }
    }
}

/**
 * BeautySlider - Individual slider component for beauty parameters
 */
@Composable
private fun BeautySlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    requiresFace: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = String.format("%.2f", value),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.End
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Wrapper cho SlideHorizontal với value normalization
        BeautySlideHorizontal(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
        
        Spacer(modifier = Modifier.height(6.dp))
    }
}

/**
 * BeautySlideHorizontal - Wrapper cho SlideHorizontal với value normalization
 */
@Composable
private fun BeautySlideHorizontal(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    // Normalize value to 0.0-1.0 range for SlideHorizontal
    val normalizedValue = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Custom paint background
        CustomPaintSlider(
            width = 200, // Chiều rộng slider
            height = 8,  // Chiều cao track tăng lên 8
            firstColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            lastColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        
        // Custom slider
        CustomSlider(
            value = normalizedValue,
            onExposureChange = { normalizedVal ->
                // Convert back to actual value range
                val actualValue = valueRange.start + (normalizedVal * (valueRange.endInclusive - valueRange.start))
                onValueChange(actualValue)
            },
            width = 200,        // Match paint width
            height = 8,         // Track height tăng lên 8
            widthThumb = 16,    // Small thumb
            thumbColor = MaterialTheme.colorScheme.primary
        )
    }
}

