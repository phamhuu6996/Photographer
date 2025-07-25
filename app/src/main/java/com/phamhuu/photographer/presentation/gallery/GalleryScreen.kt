package com.phamhuu.photographer.presentation.gallery

import LocalNavController
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.R
import com.phamhuu.photographer.enums.ImageMode
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.ImageCustom
import org.koin.androidx.compose.koinViewModel

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = koinViewModel<GalleryViewModel>()
) {
    val navController = LocalNavController.current
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val width = maxWidth / 2

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Back button
            ImageCustom(
                id = R.drawable.back,
                imageMode = ImageMode.MEDIUM,
                color = Color.White,
                modifier = Modifier
                    .padding(all = 16.dp)
                    .clickable { navController.popBackStack() }
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
                        text = "Error: ${uiState.error}",
                        color = Color.Red,
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
                            AsyncImageCustom(
                                imageSource = galleryItem.resourceUri,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .border(width = 1.dp, Color.Gray)
                                    .clickable {
                                        if (galleryItem.resourceUri is Uri) {
                                            val arg = Uri.encode(galleryItem.resourceUri.toString())
                                            navController.navigate("largeImage/${arg}")
                                        }
                                    },
                                size = width
                            )
                        }
                    }
                }
            }
        }
    }
}