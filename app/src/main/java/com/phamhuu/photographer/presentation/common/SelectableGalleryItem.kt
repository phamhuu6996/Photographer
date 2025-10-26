package com.phamhuu.photographer.presentation.common

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageMode

/**
 * GalleryItem - Component gallery item có thể sử dụng cho cả normal mode và selection mode
 * 
 * Chức năng:
 * 1. Hiển thị ảnh/video với hoặc không có checkbox selection
 * 2. Hỗ trợ tap và long press gestures
 * 3. Visual feedback khi được chọn (nếu ở selection mode)
 * 4. Tự động ẩn/hiện checkbox dựa trên mode
 * 
 * @author Pham Huu
 * @version 2.0
 * @since 2024
 */
@Composable
fun GalleryItem(
    galleryItem: Any? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onItemClick: () -> Unit,
    onLongPress: () -> Unit = {},
    width: Dp
) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onItemClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        // Image
        AsyncImageCustom(
            imageSource = galleryItem,
            size = width
        )
        
        // Video indicator
        if (galleryItem !is Uri) {
            ImageCustom(
                id = R.drawable.start_record,
                modifier = Modifier.align(Alignment.Center),
                imageMode = ImageMode.MEDIUM,
                color = Color.White
            )
        }
        
        // Selection checkbox - chỉ hiển thị khi ở selection mode
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.Blue else Color.White.copy(alpha = 0.7f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) Color.Blue else Color.Gray,
                        shape = CircleShape
                    )
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp).align(Alignment.Center)
                    )
                }
            }
        }
    }
}