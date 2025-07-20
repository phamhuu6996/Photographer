package com.phamhuu.photographer.presentation.gallery

import android.net.Uri

data class GalleryUiState(
    val isLoading: Boolean = false,
    val images: List<GalleryItem> = emptyList(),
    val error: String? = null
)

data class GalleryItem(
    val uri: Uri,
    val resourceUri: Any? = null
) 