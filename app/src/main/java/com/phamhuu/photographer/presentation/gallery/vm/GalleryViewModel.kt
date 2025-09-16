package com.phamhuu.photographer.presentation.gallery.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamhuu.photographer.data.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val galleryRepository: GalleryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        loadGalleryImages()
    }
    
    private fun loadGalleryImages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val imageUris = galleryRepository.getAllImagesAndVideos()
                val galleryItems = imageUris.map { uri ->
                    GalleryItem(
                        uri = uri,
                        resourceUri = galleryRepository.getResourceUri(uri)
                    )
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    images = galleryItems
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }
    
    fun refreshGallery() {
        loadGalleryImages()
    }
}
