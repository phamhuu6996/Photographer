package com.phamhuu.photographer.presentation.filament

import FilamentViewModel
import RenderableModel
import android.content.Context
import android.opengl.Matrix
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.presentation.camera.CameraViewModel
import org.koin.androidx.compose.get
import org.koin.androidx.compose.koinViewModel

@Composable
fun FilamentSurfaceView(
    context: Context,
    lifecycle: Lifecycle,
    matrixList: List<FloatArray>?,
    modelPath: List<String>,
    viewModel: FilamentViewModel = koinViewModel<FilamentViewModel>()
) {
    val surfaceView = remember { SurfaceView(context) }

    viewModel.initModels(lifecycle, surfaceView, context, modelPath)

    // Cập nhật transform mỗi lần nhận result mới
    LaunchedEffect(matrixList) {
        viewModel.extractGlassesTransform(matrixList)
    }

    AndroidView(factory = { surfaceView })
}