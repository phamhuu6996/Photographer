package com.phamhuu.photographer.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.phamhuu.photographer.contants.Contants
import com.phamhuu.photographer.services.renderer.AddTextService
import com.phamhuu.photographer.services.android.GalleryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface CameraRepository {
    suspend fun saveImageToGallery(photoFile: File): Uri?
    suspend fun createImageFile(): File
    suspend fun createVideoFile(): File
    suspend fun saveVideoToGallery(videoFile: File): Uri?
    suspend fun addAddressCapture(bitmap: Bitmap, address: String): Bitmap
}

class CameraRepositoryImpl(
    private val context: Context,
) : CameraRepository {
    
    override suspend fun saveImageToGallery(photoFile: File): Uri? = withContext(Dispatchers.IO) {
        GalleryService.saveImageToGallery(context, photoFile)
    }
    
    override suspend fun createImageFile(): File = withContext(Dispatchers.IO) {
        createFile(Contants.EXT_IMG, Contants.IMG_PREFIX)
    }
    
    override suspend fun createVideoFile(): File = withContext(Dispatchers.IO) {
        createFile(Contants.EXT_VID, Contants.VID_PREFIX)
    }
    
    override suspend fun saveVideoToGallery(videoFile: File): Uri? = withContext(Dispatchers.IO) {
        GalleryService.saveVideoToGallery(context, videoFile)
    }
    
    private fun createFile(extension: String, prefix: String): File {
        val timeStamp = SimpleDateFormat(Contants.DATE_TIME_FORMAT, Locale.getDefault()).format(Date())
        val fileName = "$prefix$timeStamp.$extension"
        return File(context.getExternalFilesDir(null), fileName)
    }

    override suspend fun addAddressCapture(bitmap: Bitmap, address: String): Bitmap {
       return withContext(Dispatchers.IO) {
           AddTextService.addTextOverlay(bitmap, address)
        }
    }
} 