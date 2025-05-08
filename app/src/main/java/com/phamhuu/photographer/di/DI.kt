package com.phamhuu.photographer.di

import FilamentHelper
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.presentation.camera.CameraViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory { FaceLandmarkerHelper(context = androidContext(), resultFlow = MutableStateFlow(null)) }
    factory { FilamentHelper() }
    viewModel { CameraViewModel(get<FaceLandmarkerHelper>()) }
}