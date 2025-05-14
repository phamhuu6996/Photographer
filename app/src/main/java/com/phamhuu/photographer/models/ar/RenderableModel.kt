package com.phamhuu.photographer.models.ar

import com.google.android.filament.utils.Float3
import java.nio.ByteBuffer

data class RenderableModel(
    val buffer: ByteBuffer,
    val initialPosition: Float3
)