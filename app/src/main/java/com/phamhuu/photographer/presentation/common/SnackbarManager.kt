package com.phamhuu.photographer.presentation.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import com.phamhuu.photographer.contants.SnackbarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * SnackbarManager - Singleton quản lý Snackbar global
 * 
 * Chức năng:
 * 1. Singleton pattern để truy cập từ bất cứ đâu
 * 2. Quản lý SnackbarHostState và CoroutineScope
 * 3. Hàm show() để hiển thị Snackbar
 * 
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
object SnackbarManager {
    
    // SnackbarHostState và CoroutineScope sẽ được init từ onCreate
    private var _snackbarHostState: SnackbarHostState? = null
    private var _scope: CoroutineScope? = null

    val snackbarHostState: SnackbarHostState? get() = _snackbarHostState

    /**
     * Init SnackbarManager với SnackbarHostState và CoroutineScope
     * Gọi từ onCreate của MainActivity
     */
    fun init(snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
        this._snackbarHostState = snackbarHostState
        this._scope = scope
    }
    
    /**
     * Show Snackbar với SnackbarData
     * 
     * @param data SnackbarData chứa message, type và callback
     */
    fun show(data: SnackbarData) {

        _scope?.launch {
            snackbarHostState?.showSnackbar(
                message = data.message,
                actionLabel = data.type.actionLabel,
                duration = SnackbarDuration.Short,
            )
            data.onActionClick?.invoke()
        }
    }
    
    /**
     * Show Snackbar với message và type
     * 
     * @param message Thông báo hiển thị
     * @param type Loại Snackbar (SUCCESS, FAIL, WARNING, INFO)
     * @param onActionClick Callback khi user click action button (optional)
     */
    fun show(
        message: String,
        type: SnackbarType,
        onActionClick: (() -> Unit)? = null
    ) {
        show(SnackbarData(message, type, onActionClick))
    }

}

data class SnackbarData(
    val message: String,
    val type: SnackbarType,
    val onActionClick: (() -> Unit)? = null
)
