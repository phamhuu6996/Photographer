package com.phamhuu.photographer.domain.model

/**
 * BeautySettings - Data class cho các thông số beauty filter
 * 
 * Chứa 5 thông số chính:
 * - skinSmoothing: Làm mịn da (0.0 - 1.0)
 * - whiteness: Làm trắng da (0.0 - 1.0)  
 * - thinFace: Thu gọn mặt (0.0 - 0.1)
 * - bigEye: To mắt (0.0 - 0.2)
 * - blendLevel: Độ pha trộn son môi (0.0 - 1.0)
 * 
 * @author Pham Huu
 * @version 1.0
 * @since 2024
 */
data class BeautySettings(
    val skinSmoothing: Float = 3f / 10.0f,  // 0.3
    val whiteness: Float = 3f / 10.0f,      // 0.3
    val thinFace: Float = 3f / 160.0f,      // 0.01875
    val bigEye: Float = 3f / 40.0f,         // 0.075
    val blendLevel: Float = 1f / 10.0f      // 0.3
) {
    /**
     * Validate ranges cho tất cả parameters
     */
    fun validate(): BeautySettings {
        return copy(
            skinSmoothing = skinSmoothing.coerceIn(0f, 1f),
            whiteness = whiteness.coerceIn(0f, 1f),
            thinFace = thinFace.coerceIn(0f, 0.1f),
            bigEye = bigEye.coerceIn(0f, 0.2f),
            blendLevel = blendLevel.coerceIn(0f, 1f)
        )
    }
    
    /**
     * Reset về default values
     */
    companion object {
        fun default() = BeautySettings()
        
        // Min/Max ranges for UI sliders
        const val SKIN_SMOOTHING_MIN = 0f
        const val SKIN_SMOOTHING_MAX = 1f
        const val WHITENESS_MIN = 0f
        const val WHITENESS_MAX = 1f
        const val THIN_FACE_MIN = 0f
        const val THIN_FACE_MAX = 0.1f
        const val BIG_EYE_MIN = 0f
        const val BIG_EYE_MAX = 0.2f
        const val BLEND_LEVEL_MIN = 0f
        const val BLEND_LEVEL_MAX = 1f
    }
}
