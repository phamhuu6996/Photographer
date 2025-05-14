package com.phamhuu.photographer.models.ar

import com.google.android.filament.utils.Float3

data class FilamentModelManager(
    var first: Float3? = null,
    var second: Float3? = null,
    var modelInstance: ModelInstance? = null,
    var modelUrl: String
)