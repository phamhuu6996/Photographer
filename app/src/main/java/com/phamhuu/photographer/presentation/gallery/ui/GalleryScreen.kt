package com.phamhuu.photographer.presentation.gallery.ui

import LocalNavController
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageMode
import com.phamhuu.photographer.contants.SnackbarType
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.BackImageCustom
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.common.SnackbarManager
import com.phamhuu.photographer.presentation.gallery.vm.GalleryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = koinViewModel<GalleryViewModel>()
) {
    val navController = LocalNavController.current
    val uiState by viewModel.uiState.collectAsState()
    
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            SnackbarManager.show(
                message = error,
                type = SnackbarType.FAIL
            )
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val width = maxWidth / 3

        Box{
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = width),
                ) {
                    itemsIndexed(
                        uiState.images,
                        key = { _, galleryItem -> galleryItem.uri }
                    ) { index, galleryItem ->
                        if (index >= uiState.images.size - 4) {
                            viewModel.loadMore()
                        }
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable {
                                    if (galleryItem.resourceUri is Uri) {
                                        val arg = Uri.encode(galleryItem.resourceUri.toString())
                                        navController.navigate("largeImage/${arg}")
                                    } else {
                                        val arg = Uri.encode(galleryItem.uri.toString())
                                        navController.navigate("video/${arg}")
                                    }
                                }
                        ) {
                            AsyncImageCustom(
                                imageSource = galleryItem.resourceUri,
                                size = width
                            )
                            if (galleryItem.resourceUri !is Uri) {
                                ImageCustom(
                                    id = R.drawable.video,
                                    modifier = Modifier
                                        .align(Alignment.Center),
                                    imageMode = ImageMode.MEDIUM,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            BackImageCustom(modifier = Modifier.align(Alignment.TopStart)) {
                navController.popBackStack()
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
