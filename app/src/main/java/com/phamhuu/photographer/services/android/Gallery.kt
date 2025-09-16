package com.phamhuu.photographer.services.android

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.annotation.RequiresApi
import com.phamhuu.photographer.presentation.common.getFilePathFromUri
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

    fun saveImageToGallery(context: Context, file: File): Uri? {
        val values = valuesSaveImage(file)
        return saveFileToUri(context, file, values, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    fun saveVideoToGallery(context: Context, file: File): Uri? {
        val values = valuesSaveVideo(file)
        return saveFileToUri(context, file, values, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    }

    private fun saveFileToUri(context: Context, file: File, values: ContentValues, uri: Uri): Uri? {
        val resolver = context.contentResolver
        val uriFile = resolver.insert(uri, values) ?: return null
        resolver.openOutputStream(uriFile).use { outputStream ->
            FileInputStream(file).use { inputStream ->
                inputStream.copyTo(outputStream!!)
            }
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uriFile, values, null, null)
        return uriFile
    }

    private fun getFirstImageFromGallery(context: Context): Uri? {
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        context.contentResolver.query(contentUri, projection, null, null, sortOrder)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val imageId = cursor.getLong(idColumn)
                    return Uri.withAppendedPath(contentUri, imageId.toString())
                }
            }
        return null
    }

    private fun getFirstVideoFromGallery(context: Context): Uri? {
        val contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Video.Media._ID)
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        context.contentResolver.query(contentUri, projection, null, null, sortOrder)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val videoId = cursor.getLong(idColumn)
                    return Uri.withAppendedPath(contentUri, videoId.toString())
                }
            }
        return null
    }

    fun getFirstImageOrVideo(context: Context): Uri? {
        return getFirstImageFromGallery(context) ?: getFirstVideoFromGallery(context)
    }

    fun getAllImagesAndVideosFromGallery(context: Context): List<Uri> {
        val mediaList = mutableListOf<Uri>()
        val contentUri = MediaStore.Files.getContentUri("external")
        val projection =
            arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE)
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        context.contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val typeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val mediaType = cursor.getInt(typeColumn)
                    val uri = when (mediaType) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )
                        else -> null
                    }
                    uri?.let { mediaList.add(it) }
                }
            }
        return mediaList
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
        } else {
            val filePath = getFilePathFromUri(context, uri)
            ThumbnailUtils.createImageThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
        }
    }

    fun getResourceUri(context: Context, uri: Uri): Any? {
        val mimeType = context.contentResolver.getType(uri)
        var imageSource: Any? = uri
        if (mimeType?.startsWith("video") == true) {
            imageSource = loadBitmapFromUri(context, uri)
        }
        return imageSource
    }
}
