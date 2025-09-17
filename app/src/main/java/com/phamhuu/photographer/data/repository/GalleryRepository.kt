package com.phamhuu.photographer.data.repository

import android.content.Context
import android.net.Uri
import com.phamhuu.photographer.data.model.GalleryPageModel
import com.phamhuu.photographer.services.android.Gallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface GalleryRepository {
    suspend fun getFirstImageOrVideo(): Uri?
    suspend fun getResourceUri(imageUri: Uri): Any?

    suspend fun getImagesAndVideos(limit: Int, after: Long? = null): GalleryPageModel
}

class GalleryRepositoryImpl(
    private val context: Context
) : GalleryRepository {
    
    override suspend fun getFirstImageOrVideo(): Uri? = withContext(Dispatchers.IO) {
        Gallery.getFirstImageOrVideo(context)
    }
    
    override suspend fun getResourceUri(imageUri: Uri): Any? = withContext(Dispatchers.IO) {
        Gallery.getResourceUri(context, imageUri)
    }

    override suspend fun getImagesAndVideos(limit: Int, after: Long?): GalleryPageModel = withContext(Dispatchers.IO) {
        Gallery.getImagesAndVideos(context, limit, after)
    }
} 