package com.phamhuu.photographer.presentation.gallery

import LocalNavController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.DetailViewerAppBar
import com.phamhuu.photographer.services.android.ShareService
import com.phamhuu.photographer.presentation.image_view.LargeImageViewModel
import androidx.core.net.toUri

@Composable
fun LargeImageScreen(imageUri: String) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val viewModel: LargeImageViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setImageUri(imageUri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // AppBar with Share button
        DetailViewerAppBar(
            title = "Image",
            onBackClick = { navController.popBackStack() },
            onShareClick = {
                // Share the image
                ShareService.multiShare(context, listOf(imageUri.toUri()))
            }
        )
        
        // Image content
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (state.imageUri != null)
                AsyncImageCustom(
                    imageSource = state.imageUri,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    isDecoration = false
                )
        }
    }
}