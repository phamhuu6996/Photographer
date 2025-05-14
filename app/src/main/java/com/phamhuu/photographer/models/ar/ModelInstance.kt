package com.phamhuu.photographer.models.ar

import com.google.android.filament.gltfio.FilamentAsset

data class ModelInstance(
    val asset: FilamentAsset,
    val rootEntity: Int,
    val width: Float = 1f,
)