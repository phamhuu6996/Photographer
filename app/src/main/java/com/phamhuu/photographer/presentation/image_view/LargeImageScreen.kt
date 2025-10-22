package com.phamhuu.photographer.presentation.gallery

import LocalNavController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.BackImageCustom
import com.phamhuu.photographer.presentation.image_view.LargeImageViewModel

@Composable
fun LargeImageScreen(imageUri: String) {
    val navController = LocalNavController.current
    val viewModel: LargeImageViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setImageUri(imageUri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (state.imageUri != null)
            AsyncImageCustom(
                imageSource = state.imageUri,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
                isDecoration = false
            )

        BackImageCustom(modifier = Modifier.align(Alignment.TopStart)) {
            navController.popBackStack()
        }
    }
}