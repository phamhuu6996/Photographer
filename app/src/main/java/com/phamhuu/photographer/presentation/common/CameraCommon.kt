package com.phamhuu.photographer.presentation.common

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
fun getFilePathFromUri(context: Context, uri: Uri): String {
    val projection = arrayOf(MediaStore.Images.Media.DATA)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor.moveToFirst()
        return cursor.getString(columnIndex)
    }
    throw IllegalArgumentException("Invalid URI")
}