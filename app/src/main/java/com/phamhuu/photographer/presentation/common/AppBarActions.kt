package com.phamhuu.photographer.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageMode

/**
 * AppBarActions - Các action buttons có thể tái sử dụng cho AppBar
 */

/**
 * ShareActionButton - IconButton cho chia sẻ
 */
@Composable
fun ShareActionButton(
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    PressableContent(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share",
            tint = tint
        )
    }
}

/**
 * DeleteActionButton - IconButton cho xóa
 */
@Composable
fun DeleteActionButton(
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    PressableContent(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = tint
        )
    }
}

/**
 * MoreActionMenuButton - IconButton mở menu dropdown
 */
@Composable
fun MoreActionMenuButton(
    onOpenMenu: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    PressableContent(onClick = onOpenMenu) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "More options",
            tint = tint
        )
    }
}

/**
 * SelectionDropdownMenu - Menu dropdown cho selection mode với Select All/Unselect All
 */
@Composable
fun SelectionDropdownMenu(
    onSelectAll: () -> Unit,
    onUnSelectAll: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    MoreActionMenuButton(onOpenMenu = { showMenu = true })
    
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Chọn tất cả") },
            leadingIcon = {
                ImageCustom(
                    id = R.drawable.check_all,
                    imageMode = ImageMode.MEDIUM
                )
            },
            onClick = {
                showMenu = false
                onSelectAll()
            }
        )
        
        DropdownMenuItem(
            text = { Text("Bỏ tất cả") },
            leadingIcon = {
                ImageCustom(
                    id = R.drawable.uncheck_all,
                    imageMode = ImageMode.MEDIUM
                )
            },
            onClick = {
                showMenu = false
                onUnSelectAll()
            }
        )
    }
}

/**
 * GalleryDropdownMenu - Menu dropdown cho Gallery với các tùy chọn cài đặt
 */
@Composable
fun GalleryDropdownMenu() {
    var showMenu by remember { mutableStateOf(false) }
    
    MoreActionMenuButton(onOpenMenu = { showMenu = true })
    
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        // TODO: Add menu items
    }
}

