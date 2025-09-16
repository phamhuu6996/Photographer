package com.phamhuu.photographer.presentation.gallery.ui

import LocalNavController
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import com.phamhuu.photographer.contants.SnackbarType
import com.phamhuu.photographer.presentation.common.SnackbarManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageMode
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.gallery.vm.GalleryViewModel
import org.koin.androidx.compose.koinViewModel
import singleShotClick

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
        val width = maxWidth / 2

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ImageCustom(
                id = R.drawable.back,
                imageMode = ImageMode.SMALL,
                color = Color.White,
                modifier = Modifier
                    .padding(all = 16.dp)
                    .singleShotClick { navController.popBackStack() }
            )

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color.White
                    )
                }

                uiState.error != null -> {
                    Text(
                        text = "No images found",
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = width),
                    ) {
                        items(uiState.images) { galleryItem ->
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .border(width = 1.dp, Color.Gray)
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
                                    Text(
                                        text = "VIDEO",
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color(0x66000000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
