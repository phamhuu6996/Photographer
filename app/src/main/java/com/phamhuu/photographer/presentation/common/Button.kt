package com.phamhuu.photographer.presentation.common

import android.util.Size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun FilledTonalButtonCustom(
    callBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    size: Dp = 48.dp,
    color: Color = Color.Transparent

) {
    FilledTonalButton(
        onClick = callBack,
        colors = ButtonDefaults.buttonColors(
            containerColor = color
        ),
        modifier = Modifier.size(size).then(modifier)
    ) {
        content()
    }
}