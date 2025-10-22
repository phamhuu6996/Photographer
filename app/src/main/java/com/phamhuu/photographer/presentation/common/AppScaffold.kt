package com.phamhuu.photographer.presentation.common

import android.annotation.SuppressLint
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable

/**
 * AppScaffold - Scaffold với Snackbar global cho toàn app
 * 
 * Chức năng:
 * 1. Wrap toàn bộ app với Scaffold
 * 2. Cung cấp SnackbarHostState global
 * 3. Init SnackbarManager với state và scope
 * 4. Hiển thị Snackbar khi có data
 * 
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppScaffold(
    content: @Composable () -> Unit
) {
        Scaffold(
            snackbarHost = {
                SnackbarManager.snackbarHostState?.let { SnackbarHost(hostState = it) }
            },
        ) {  padding ->
            content()
        }
}
