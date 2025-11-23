package com.phamhuu.photographer.presentation.gallery.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamhuu.photographer.contants.Constants
import com.phamhuu.photographer.data.model.GalleryItemModel
import com.phamhuu.photographer.data.repository.GalleryRepository
import com.phamhuu.photographer.services.android.DeleteService
import com.phamhuu.photographer.services.android.ShareService
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
                val page = galleryRepository.getImagesAndVideos(
                    limit = Constants.MAX_RECORD_LOAD_MORE,
                    after = null
                )
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
                val page = galleryRepository.getImagesAndVideos(
                    limit = Constants.MAX_RECORD_LOAD_MORE,
                    after = _uiState.value.galleryPageModel?.id
                )
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

    // Selection management functions
    fun toggleItemSelection(itemKey: String) {
        val currentSelected = _uiState.value.selectedItems
        val newSelected = if (itemKey in currentSelected) {
            currentSelected - itemKey
        } else {
            currentSelected + itemKey
        }

        _uiState.value = _uiState.value.copy(
            selectedItems = newSelected,
            isSelectionMode = newSelected.isNotEmpty()
        )
    }

    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedItems = emptySet()
        )
    }

    fun selectAllItems() {
        val allItemKeys = _uiState.value.images.map { it.uri.toString() }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedItems = allItemKeys
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItems = emptySet()
        )
    }

    fun deselectAllItems() {
        _uiState.value = _uiState.value.copy(
            selectedItems = emptySet()
        )
    }

    /**
     * Share các items đã chọn
     */
    fun shareSelectedItems(context: Context) {
        val selectedKeys = _uiState.value.selectedItems
        val selectedImages = _uiState.value.images.filter {
            it.uri.toString() in selectedKeys
        }
        if (selectedImages.isNotEmpty()) {
            val uris = selectedImages.map { galleryItem ->
                galleryItem.uri
            }
            ShareService.multiShare(context, uris)
        }
    }

    /**
     * Xóa thật sự các items đã chọn
     */
    fun deleteSelectedItems(context: Context) {
        val selectedKeys = _uiState.value.selectedItems
        val selectedImages = _uiState.value.images.filter {
            it.uri.toString() in selectedKeys
        }
        if (selectedImages.isNotEmpty()) {
            val uris = selectedImages.map { galleryItem ->
                galleryItem.uri
            }
            val deletedFiles = DeleteService.deleteMultipleMedia(context, uris)
            if (deletedFiles.isNotEmpty()) {
                // Cập nhật UI - loại bỏ các file đã xóa
                val remainingImages = _uiState.value.images.filter {
                    it.uri !in deletedFiles
                }
                _uiState.value = _uiState.value.copy(
                    images = remainingImages,
                    selectedItems = emptySet(),
                    isSelectionMode = false
                )
            }
        }
    }

}
