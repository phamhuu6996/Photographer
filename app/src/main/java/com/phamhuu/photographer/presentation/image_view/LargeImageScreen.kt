package com.phamhuu.photographer.presentation.gallery

import LocalNavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.common.ImageMode

@Composable
fun LargeImageScreen(imageUri: String) {
    val painter: Painter = rememberImagePainter(data = imageUri)
    val navController = LocalNavController.current

    Column {
        Spacer(modifier = Modifier.height(8.dp))
        ImageCustom(id = R.drawable.back,
            imageMode = ImageMode.LARGE,
            color = Color.Black,
            modifier = Modifier.clickable { navController.popBackStack()})
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
        )
    }
}