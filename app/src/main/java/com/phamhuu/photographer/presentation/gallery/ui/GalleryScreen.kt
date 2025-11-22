package com.phamhuu.photographer.presentation.gallery.ui

import LocalNavController
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.contants.SnackbarType
import com.phamhuu.photographer.presentation.common.GalleryAppBar
import com.phamhuu.photographer.presentation.common.GalleryItem
import com.phamhuu.photographer.presentation.common.SnackbarManager
import com.phamhuu.photographer.presentation.gallery.vm.GalleryViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = koinViewModel<GalleryViewModel>()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
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
            .background(MaterialTheme.colorScheme.background)

    ) {
        val width = this.maxWidth / 2

        Box{
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                GalleryAppBar(
                    isSelectionMode = uiState.isSelectionMode,
                    onBackClick = { navController.popBackStack() },
                    onCancel = { viewModel.exitSelectionMode() },
                    selectedCount = uiState.selectedItems.size,
                    onSelectAll = { viewModel.selectAllItems() },
                    onUnSelectAll = { viewModel.clearSelection() },
                    onShare = { viewModel.shareSelectedItems(context) },
                    onDelete = { viewModel.deleteSelectedItems(context) }
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    itemsIndexed(
                        uiState.images,
                        key = { _, galleryItem -> galleryItem.uri }
                    ) { index, galleryItem ->
                        if (index >= uiState.images.size - 4) {
                            viewModel.loadMore()
                        }

                        val itemKey = galleryItem.uri.toString()
                        val isSelected = uiState.selectedItems.contains(itemKey)

                        GalleryItem(
                            galleryItem = galleryItem.resourceUri,
                            isSelectionMode = uiState.isSelectionMode,
                            isSelected = isSelected,
                            onItemClick = {
                                if (uiState.isSelectionMode) {
                                    // Trong selection mode, click vào item sẽ toggle selection
                                    viewModel.toggleItemSelection(itemKey)
                                } else {
                                    // Normal mode, click để mở chi tiết
                                    if (galleryItem.resourceUri is Uri) {
                                        val arg = Uri.encode(galleryItem.uri.toString())
                                        navController.navigate("largeImage/${arg}")
                                    } else {
                                        val arg = Uri.encode(galleryItem.uri.toString())
                                        navController.navigate("video/${arg}")
                                    }
                                }
                            },
                            onLongPress = {
                                // Toggle selection (sẽ tự động bắt đầu selection mode nếu chưa có)
                                viewModel.toggleItemSelection(itemKey)
                            },
                            width = width
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
