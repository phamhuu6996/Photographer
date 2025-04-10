package com.phamhuu.photographer.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun AsyncImageCustom(
    imageSource: Any?, // Can be a URL, File, or URI
    modifier: Modifier = Modifier,
    size: Dp? = null,
    color: Color = Color.Black,
    contentScale: ContentScale = ContentScale.FillWidth
) {
    var customModifier = modifier
    if (size != null)
        customModifier = modifier.size(size)
    if (imageSource == null)
        Spacer(modifier = customModifier)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageSource)
            .crossfade(true)
            .build(),
        contentDescription = "Image from source",
        modifier = customModifier.background(color),
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