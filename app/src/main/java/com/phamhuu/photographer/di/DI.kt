package com.phamhuu.photographer.di

import FilamentHelper
import FilamentViewModel
import com.phamhuu.photographer.services.filament.Manager3DHelper
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.mediapipe.examples.facelandmarker.FaceLandmarkerHelper
import com.phamhuu.photographer.data.repository.CameraRepository
import com.phamhuu.photographer.data.repository.CameraRepositoryImpl
import com.phamhuu.photographer.data.repository.GalleryRepository
import com.phamhuu.photographer.data.repository.GalleryRepositoryImpl
import com.phamhuu.photographer.data.repository.LocationRepository
import com.phamhuu.photographer.data.repository.LocationRepositoryImpl
import com.phamhuu.photographer.domain.usecase.AddTextCaptureUseCase
import com.phamhuu.photographer.domain.usecase.GetFirstGalleryItemUseCase
import com.phamhuu.photographer.domain.usecase.RecordVideoUseCase
import com.phamhuu.photographer.domain.usecase.SavePhotoUseCase
import com.phamhuu.photographer.domain.usecase.SaveVideoUseCase
import com.phamhuu.photographer.domain.usecase.TakePhotoUseCase
import com.phamhuu.photographer.presentation.camera.vm.CameraViewModel
import com.phamhuu.photographer.presentation.gallery.vm.GalleryViewModel
import com.phamhuu.photographer.presentation.video.vm.VideoPlayerViewModel
import com.phamhuu.photographer.services.gl.CameraGLSurfaceView
import com.phamhuu.photographer.services.gl.FilterRenderer

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
    single<LocationRepository> { 
        LocationRepositoryImpl(
            androidContext(),
            LocationServices.getFusedLocationProviderClient(androidContext()),
            Geocoder(androidContext())
        )
    }
    factory { FilterRenderer() }
    factory { CameraGLSurfaceView(androidContext(), get()) }
    
    // Use Cases
    factory { TakePhotoUseCase(get()) }
    factory { SavePhotoUseCase(get()) }
    factory { RecordVideoUseCase(get()) }
    factory { SaveVideoUseCase(get()) }
    factory { GetFirstGalleryItemUseCase(get()) }
    factory { AddTextCaptureUseCase(get()) }
    
    // ViewModels
    viewModel { FilamentViewModel(get<FilamentHelper>()) }
    viewModel { CameraViewModel(get<FaceLandmarkerHelper>(), get<Manager3DHelper>(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { GalleryViewModel(get()) }
    viewModel { VideoPlayerViewModel() }
}