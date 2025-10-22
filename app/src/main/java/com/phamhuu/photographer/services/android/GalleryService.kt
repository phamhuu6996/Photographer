package com.phamhuu.photographer.services.android

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import com.phamhuu.photographer.data.model.GalleryPageModel
import com.phamhuu.photographer.presentation.common.getFilePathFromUri
import java.io.File
import java.io.FileInputStream
import androidx.core.graphics.scale

object GalleryService {
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

    fun getFirstImageOrVideo(context: Context): Uri? {
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Video.Media._ID)
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

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(200, 200), null)
        } else {
            val filePath = getFilePathFromUri(context, uri)
            val bitmap = BitmapFactory.decodeFile(filePath)
            val bitmapScale = bitmap.scale(200, 200)
            bitmap.recycle()
            bitmapScale
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

    fun getImagesAndVideos(
        context: Context,
        limit: Int,
        afterId: Long? = null // lastId từ lần trước
    ): GalleryPageModel {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

        val sel =
            StringBuilder("(${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=? )")
        val selArgs = mutableListOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        afterId?.let {
            sel.append(" AND ${MediaStore.Files.FileColumns._ID} < ?")
            selArgs.add(it.toString())
        }

        val args = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns._ID)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, sel.toString())
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selArgs.toTypedArray())
        }

        val uri = MediaStore.Files.getContentUri("external")
        context.contentResolver.query(uri, projection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

            val items = mutableListOf<Uri>()
            var lastId: Long? = null
            var lastDate: Long? = null

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val type = c.getInt(typeCol)
                val date = c.getLong(dateCol)
                val base = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

                items += ContentUris.withAppendedId(base, id)
                lastId = id
                lastDate = date
            }
            Log.d(
                "GalleryViewModel",
                "Queried ${items.size} items, lastId=$lastId, lastDate=$lastDate"
            )
            return GalleryPageModel(
                items = items,
                id = lastId,
                dateAdded = lastDate,
                canLoadMore = items.isNotEmpty()
            )
        }
        return GalleryPageModel(emptyList(), null, null, false)
    }
}
