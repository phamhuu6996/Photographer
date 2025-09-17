package com.phamhuu.photographer.presentation.gallery.vm

import com.phamhuu.photographer.data.model.GalleryItemModel
import com.phamhuu.photographer.data.model.GalleryPageModel

data class GalleryUiState(
    val isLoading: Boolean = false,
    val images: List<GalleryItemModel> = emptyList(),
    val error: String? = null,
    val galleryPageModel: GalleryPageModel? = null,
)
