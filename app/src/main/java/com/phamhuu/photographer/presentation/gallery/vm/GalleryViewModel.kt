package com.phamhuu.photographer.presentation.gallery.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamhuu.photographer.contants.Constants
import com.phamhuu.photographer.data.repository.GalleryRepository
import com.phamhuu.photographer.data.model.GalleryItemModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val galleryRepository: GalleryRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState = _uiState.asStateFlow()
    
    init {
        loadInitial()
    }
    
    private fun setError(message: String?) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val page = galleryRepository.getImagesAndVideos(limit = Constants.MAX_RECORD_LOAD_MORE, after = null)
                val items = page.items.map { uri ->
                    GalleryItemModel(uri = uri, resourceUri = galleryRepository.getResourceUri(uri))
                }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    images = items,
                    galleryPageModel = page,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                setError(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.galleryPageModel?.canLoadMore != true) return
        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true)
            try {
                val page = galleryRepository.getImagesAndVideos(limit = Constants.MAX_RECORD_LOAD_MORE, after = _uiState.value.galleryPageModel?.id)
                val items = page.items.map { uri ->
                    GalleryItemModel(uri = uri, resourceUri = galleryRepository.getResourceUri(uri))
                }
                val newList = currentState.images + items
                _uiState.value = currentState.copy(
                    isLoading = false,
                    images = newList,
                    galleryPageModel = page,
                )
            } catch (e: Exception) {
                _uiState.value = currentState.copy(isLoading = false)
                setError(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun refreshGallery() {
        loadInitial()
    }
}
