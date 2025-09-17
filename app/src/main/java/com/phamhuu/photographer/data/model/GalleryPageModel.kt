package com.phamhuu.photographer.data.model

import android.net.Uri

data class GalleryPageModel(
    val items: List<Uri>,
    val id: Long? = null,
    val dateAdded: Long? = null,
    val canLoadMore: Boolean,
)
