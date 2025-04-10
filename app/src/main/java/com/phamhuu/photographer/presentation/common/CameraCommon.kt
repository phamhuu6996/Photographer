package com.phamhuu.photographer.presentation.common

import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size

fun loadImagesFromGallery(context: Context): List<Uri> {
    val images = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri =
                Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
        }
    }
    return images
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
    } else {
        val filePath = getFilePathFromUri(context, uri)
        ThumbnailUtils.createImageThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
    }
}

fun getFilePathFromUri(context: Context, uri: Uri): String {
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(columnIndex)
    }
    throw IllegalArgumentException("Invalid URI")
}