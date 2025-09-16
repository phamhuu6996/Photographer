package com.phamhuu.photographer.data.repository

import android.content.Context
import android.net.Uri
import com.phamhuu.photographer.services.android.Gallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface GalleryRepository {
    suspend fun getAllImagesAndVideos(): List<Uri>
    suspend fun getFirstImageOrVideo(): Uri?
    suspend fun getResourceUri(imageUri: Uri): Any?
}

class GalleryRepositoryImpl(
    private val context: Context
) : GalleryRepository {
    
    override suspend fun getAllImagesAndVideos(): List<Uri> = withContext(Dispatchers.IO) {
        Gallery.getAllImagesAndVideosFromGallery(context)
    }
    
    override suspend fun getFirstImageOrVideo(): Uri? = withContext(Dispatchers.IO) {
        Gallery.getFirstImageOrVideo(context)
    }
    
    override suspend fun getResourceUri(imageUri: Uri): Any? = withContext(Dispatchers.IO) {
        Gallery.getResourceUri(context, imageUri)
    }
} 