package com.phamhuu.photographer.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.phamhuu.photographer.presentation.utils.UiConfig

@Composable
fun FullScreen(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        UiConfig.hideSystemUI(context)
        onDispose {
            UiConfig.showSystemUI(context)
        }
    }

    content()
}