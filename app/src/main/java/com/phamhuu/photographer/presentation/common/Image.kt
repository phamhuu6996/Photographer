package com.phamhuu.photographer.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageMode
import androidx.compose.ui.Alignment

/**
 * Composable chung cho hiệu ứng press (lún xuống khi click)
 * Có thể dùng cho cả ImageCustom và Icon
 */
@Composable
fun PressableContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    
    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .scale(if (isPressed) 0.9f else 1f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun AsyncImageCustom(
    imageSource: Any?, // Can be a URL, File, or URI
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color = MaterialTheme.colorScheme.surface,
    contentScale: ContentScale = ContentScale.FillWidth,
    isDecoration: Boolean = true,
) {
    var customModifier = modifier
    if (size != null)
        customModifier = modifier.size(size)
    if (imageSource == null)
        Spacer(modifier = customModifier)
    val shape = RoundedCornerShape(8.dp)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageSource)
            .crossfade(true)
            .build(),
        contentDescription = "Image from source",
        modifier = if (!isDecoration) customModifier else customModifier
            .clip(shape)
            .background(color)
            .border(width = 1.dp, MaterialTheme.colorScheme.outline, shape),
        contentScale = contentScale
    )
}


@Composable
fun ImageCustom(
    modifier: Modifier = Modifier,
    id: Int,
    contentDescription: String? = null,
    imageMode: ImageMode = ImageMode.MEDIUM,
    color: Color = MaterialTheme.colorScheme.onSurface,
    filter: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    if (onClick != null) {
        PressableContent(
            onClick = onClick,
            size = imageMode.size.dp,
            modifier = modifier
        ) {
            Image(
                painter = painterResource(id = id),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (filter) androidx.compose.ui.graphics.ColorFilter.tint(color) else null
            )
        }
    } else {
        Image(
            painter = painterResource(id = id),
            contentDescription = contentDescription,
            modifier = modifier
                .size(imageMode.size.dp)
                .fillMaxSize(),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color)
        )
    }
}

@Composable
fun BackImageCustom(
    modifier: Modifier? = Modifier,
    color: Color? = null,
    callBack : () -> Unit = {},
) {
    Box(
        modifier = (modifier ?: Modifier)
            .padding(all = 16.dp)
    ){
        ImageCustom(id = R.drawable.back,
            imageMode = ImageMode.MEDIUM,
            color = color ?: MaterialTheme.colorScheme.onSurface,
            onClick = callBack,
            modifier = Modifier
                .padding(all = 4.dp) )
    }
}