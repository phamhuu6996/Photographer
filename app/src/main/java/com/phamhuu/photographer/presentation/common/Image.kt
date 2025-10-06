package com.phamhuu.photographer.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import singleShotClick

@Composable
fun AsyncImageCustom(
    imageSource: Any?, // Can be a URL, File, or URI
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color = Color.Black,
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
            .border(width = 1.dp, Color.Gray, shape),
        contentScale = contentScale
    )
}


@Composable
fun ImageCustom(
    modifier: Modifier = Modifier,
    id: Int,
    contentDescription: String? = null,
    imageMode: ImageMode = ImageMode.MEDIUM,
    color: Color = Color.Black,
) {
    Image(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = modifier
            .size(imageMode.size.dp)
            .fillMaxSize(),
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color)
    )
}

@Composable
fun BackImageCustom(
    modifier: Modifier? = Modifier,
    callBack : () -> Unit = {},
) {
    ImageCustom(id = R.drawable.back,
        imageMode = ImageMode.LARGE,
        color = Color.White,
        modifier = (modifier ?: Modifier)
            .padding(all = 16.dp)
            .background(Color.Black.copy(0.2f), shape = CircleShape)
            .singleShotClick { callBack() } )
}