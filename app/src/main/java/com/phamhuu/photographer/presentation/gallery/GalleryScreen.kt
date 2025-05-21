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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.R
import com.phamhuu.photographer.enums.ImageMode
import com.phamhuu.photographer.presentation.common.AsyncImageCustom
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.utils.Gallery
import com.phamhuu.photographer.presentation.utils.Gallery.getAllImagesAndVideosFromGallery

@Composable
fun GalleryScreen() {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val images = remember { getAllImagesAndVideosFromGallery(context) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val width = maxWidth / 2  // Lấy chiều rộng của Box

        Column(
            modifier = Modifier.fillMaxSize()

        ) {
            ImageCustom(id = R.drawable.back,
                imageMode = ImageMode.MEDIUM,
                color = Color.White,
                modifier = Modifier
                    .padding(all = 16.dp)
                    .clickable { navController.popBackStack() })
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = width),
            ) {
                items(images.size) { index ->

                    val uri = images[index]
                    val imageSource = Gallery.getResourceUri(context, uri)

                    AsyncImageCustom(
                        imageSource = imageSource,
                        modifier = Modifier
                            .padding(4.dp)
                            .border(width = 1.dp, Color.Gray)
                            .clickable {
                                if (imageSource is Uri) {
                                    val arg = Uri.encode(imageSource.toString())
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