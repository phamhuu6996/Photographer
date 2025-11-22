package com.phamhuu.photographer.presentation.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.phamhuu.photographer.R

// ============================================================================
// BASE COMPONENT - Component cơ bản có thể tái sử dụng
// ============================================================================

/**
 * BaseAppBar - Component AppBar cơ bản có thể tái sử dụng cho các màn hình
 * Sử dụng CenterAlignedTopAppBar từ Material3
 *
 * Chức năng:
 * 1. Hiển thị nút back và tiêu đề ở giữa
 * 2. Có thể tùy chỉnh title và màu sắc
 * 3. Material Design chuẩn
 * 4. Có thể thêm action buttons
 *
 * @author Pham Huu
 * @version 2.0
 * @since 2024
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseAppBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        },
        navigationIcon = {
            BackImageCustom(
                color = titleColor
            ) {
                onBackClick?.invoke()
            }
        },
        actions = {
            actions?.invoke()
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = titleColor,
            navigationIconContentColor = titleColor,
            actionIconContentColor = titleColor
        )
    )
}

// ============================================================================
// GALLERY SCREEN APP BARS
// ============================================================================

/**
 * GalleryAppBar - AppBar chuyên dụng cho Gallery screen
 * Hỗ trợ 2 modes: Normal và Selection
 *
 * @param isSelectionMode true nếu đang ở selection mode
 * @param onBackClick callback khi click nút back (normal mode)
 * @param onCancel callback khi click cancel (selection mode)
 * @param selectedCount số lượng items đã chọn
 * @param onSelectAll callback khi chọn tất cả
 * @param onUnSelectAll callback khi bỏ chọn tất cả
 * @param onShare callback khi share
 * @param onDelete callback khi delete
 */
@Composable
fun GalleryAppBar(
    isSelectionMode: Boolean,
    onBackClick: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    selectedCount: Int = 0,
    onSelectAll: (() -> Unit)? = null,
    onUnSelectAll: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    title: String = stringResource(R.string.gallery)
) {
    if (isSelectionMode) {
        BaseAppBar(
            onBackClick = onCancel,
            title = stringResource(R.string.selected_count, selectedCount),
            actions = {
                if (selectedCount > 0) {
                    onShare?.let { ShareActionButton(onClick = it) }
                    onDelete?.let { DeleteActionButton(onClick = it) }
                }
                
                if (onSelectAll != null && onUnSelectAll != null) {
                    SelectionDropdownMenu(
                        onSelectAll = onSelectAll,
                        onUnSelectAll = onUnSelectAll
                    )
                }
            }
        )
    } else {
        BaseAppBar(
            title = title,
            onBackClick = onBackClick,
            actions = {
                GalleryDropdownMenu()
            }
        )
    }
}

// ============================================================================
// IMAGE/VIDEO SCREEN APP BARS
// ============================================================================

/**
 * ImageVideoViewerAppBar - AppBar chuyên dụng cho Image và Video viewer screens
 * Có nút Share để chia sẻ file
 */
@Composable
fun DetailViewerAppBar(
    title: String,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color? = null, // null = dùng default, có thể override cho video/camera
    actions: @Composable (() -> Unit)? = null
) {
    BaseAppBar(
        title = title,
        onBackClick = onBackClick,
        titleColor = titleColor,
        backgroundColor = backgroundColor ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        actions = {
            ShareActionButton(onClick = onShareClick, tint = titleColor)
            actions?.invoke()
        }
    )
}
