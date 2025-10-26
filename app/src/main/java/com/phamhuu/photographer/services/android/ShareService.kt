package com.phamhuu.photographer.services.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import java.io.File

/**
 * ShareService - Quản lý việc share media ra các mạng xã hội
 * 
 * Chức năng:
 * 1. Share hình ảnh/video ra các app khác
 * 2. Hỗ trợ nhiều file cùng lúc
 * 3. Sử dụng FileProvider để share file an toàn
 * 
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
object ShareService {
    
    /**
     * Share nhiều file media (hình ảnh/video) cùng lúc
     */
    fun multiShare(context: Context, uris: List<Uri>) {
        try {

            if (uris.isEmpty()) return
            val mimeType = "*/*"
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Chia sẻ qua...")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Composable để sử dụng ShareService trong Compose
 */
@Composable
fun rememberShareService(): ShareService {
    return remember { ShareService }
}
