package com.phamhuu.photographer.di

import FilamentHelper
import FilamentViewModel
import Manager3DHelper
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.presentation.camera.CameraViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    factory {
        FaceLandmarkerHelper(
            context = androidContext(),
            resultFlow = MutableStateFlow(null)
        )
    }
    single { FilamentHelper() }
    factory { Manager3DHelper(get<FilamentHelper>()) }
    viewModel { FilamentViewModel(get<FilamentHelper>()) }
    viewModel { CameraViewModel(get<FaceLandmarkerHelper>(), get<Manager3DHelper>()) }
}