package com.phamhuu.photographer.presentation.image_view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LargeImageState(
    val imageUri: String? = null
)

class LargeImageViewModel : ViewModel() {
    private val _state = MutableStateFlow(LargeImageState())
    val state = _state.asStateFlow()

    fun setImageUri(uri: String) {
        viewModelScope.launch {
            _state.value = LargeImageState(imageUri = uri)
        }
    }
}