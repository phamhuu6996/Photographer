package com.phamhuu.photographer.presentation.filament

import FilamentViewModel
import android.content.Context
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun FilamentSurfaceView(
    context: Context,
    lifecycle: Lifecycle,
    viewModel: FilamentViewModel = koinViewModel<FilamentViewModel>()
) {
    val surfaceView = remember {
        val surfaceView = SurfaceView(context)
        viewModel.initModels(lifecycle, surfaceView)
        surfaceView
    }

    AndroidView(factory = { surfaceView })
}