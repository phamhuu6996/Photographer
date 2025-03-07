package com.phamhuu.photographer.presentation.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

object Gallery {

    private fun valuesSaveImage(file: File): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    private fun valuesSaveVideo(file: File): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
    }

    fun saveImageToGallery(context: Context, file: File) {
        // Create a ContentValues object to store the image metadata
        val values = valuesSaveImage(file)
        saveFileToUri(context, file, values, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    fun saveVideoToGallery(context: Context, file: File) {
        // Create a ContentValues object to store the video metadata
        val values = valuesSaveVideo(file)
        saveFileToUri(context, file, values, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    }

    private fun saveFileToUri(context: Context, file: File, values: ContentValues, uri: Uri) {
        // Get the content resolver
        val resolver = context.contentResolver

        // Insert the image metadata into the MediaStore and get the URI
        val uri = resolver.insert(uri, values) ?: return

        // Open an output stream to the URI and copy the file contents
        resolver.openOutputStream(uri).use { outputStream ->
            FileInputStream(file).use { inputStream ->
                inputStream.copyTo(outputStream!!)
            }
        }

        // Update the image metadata to mark it as not pending
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
}