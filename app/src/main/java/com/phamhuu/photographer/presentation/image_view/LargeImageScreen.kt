package com.phamhuu.photographer.presentation.gallery

import LocalNavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import com.phamhuu.photographer.R
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.common.ImageMode
import com.phamhuu.photographer.presentation.image_view.LargeImageViewModel
import com.phamhuu.photographer.presentation.utils.Permission

@Composable
fun LargeImageScreen(imageUri: String) {
    val navController = LocalNavController.current
    val viewModel : LargeImageViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setImageUri(imageUri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.imageUri != null)
            AsyncImageCustom(
                imageSource = state.imageUri,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )
        ImageCustom(id = R.drawable.back,
            imageMode = ImageMode.MEDIUM,
            color = Color.White,
            modifier = Modifier
                .padding(all = 16.dp)
                .align(Alignment.TopStart)
                .clickable { navController.popBackStack() })
    }
}