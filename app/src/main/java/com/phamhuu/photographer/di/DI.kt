package com.phamhuu.photographer.di

import FilamentHelper
import FilamentViewModel
import Manager3DHelper
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.data.repository.CameraRepository
import com.phamhuu.photographer.data.repository.CameraRepositoryImpl
import com.phamhuu.photographer.data.repository.GalleryRepository
import com.phamhuu.photographer.data.repository.GalleryRepositoryImpl
import com.phamhuu.photographer.domain.usecase.GetFirstGalleryItemUseCase
import com.phamhuu.photographer.domain.usecase.RecordVideoUseCase
import com.phamhuu.photographer.domain.usecase.SavePhotoUseCase
import com.phamhuu.photographer.domain.usecase.SaveVideoUseCase
import com.phamhuu.photographer.domain.usecase.TakePhotoUseCase
import com.phamhuu.photographer.presentation.camera.CameraViewModel
import com.phamhuu.photographer.presentation.gallery.GalleryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // MediaPipe and 3D helpers
    factory {
        FaceLandmarkerHelper(
            context = androidContext(),
            resultFlow = MutableStateFlow(null)
        )
    }
    single { FilamentHelper() }
    factory { Manager3DHelper(get<FilamentHelper>()) }
    
    // Repositories
    single<GalleryRepository> { GalleryRepositoryImpl(androidContext()) }
    single<CameraRepository> { CameraRepositoryImpl(androidContext()) }
    
    // Use Cases
    factory { TakePhotoUseCase(get()) }
    factory { SavePhotoUseCase(get()) }
    factory { RecordVideoUseCase(get()) }
    factory { SaveVideoUseCase(get()) }
    factory { GetFirstGalleryItemUseCase(get()) }
    
    // ViewModels
    viewModel { FilamentViewModel(get<FilamentHelper>()) }
    viewModel { CameraViewModel(get<FaceLandmarkerHelper>(), get<Manager3DHelper>(), get(), get(), get(), get(), get()) }
    viewModel { GalleryViewModel(get()) }
}