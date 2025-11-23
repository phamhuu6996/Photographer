package com.phamhuu.photographer.services.android

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

/**
 * DeleteService - Quản lý việc xóa file media thật sự
 *
 * Chức năng:
 * 1. Xóa file từ storage
 * 2. Xóa entry từ MediaStore database
 * 3. Refresh media scanner
 * 4. Hỗ trợ xóa nhiều file cùng lúc
 *
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
object DeleteService {

    /**
     * Xóa một file media
     */
    fun deleteMedia(context: Context, uri: Uri): Boolean {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val count = contentResolver.delete(uri, null, null)
            return count > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Xóa nhiều file media cùng lúc
     */
    fun deleteMultipleMedia(context: Context, uris: List<Uri>): List<Uri> {
        val deletedFiles = mutableListOf<Uri>()

        uris.forEach { uri ->
            if (deleteMedia(context, uri)) {
                deletedFiles.add(uri)
            }
        }
        return deletedFiles
    }

}
