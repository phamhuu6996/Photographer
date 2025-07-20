package com.phamhuu.photographer.enums

import androidx.camera.core.resolutionselector.AspectRatioStrategy
import com.phamhuu.photographer.R
import com.phamhuu.photographer.enums.TimerDelay.OFF
import com.phamhuu.photographer.enums.TimerDelay.TEN
import com.phamhuu.photographer.enums.TimerDelay.THREE
import com.phamhuu.photographer.presentation.common.PopupItemData

enum class ImageMode(val size: Int) {
    SMALL(20), MEDIUM(30), LARGE(40)
}

enum class TypeModel3D(val displayName: String, val iconRes: Int) {
    GLASSES("Kính", R.drawable.ic_glasses),
    HAT("Mũ", R.drawable.ic_hat);
    
    fun toPopupItemData(): PopupItemData {
        return PopupItemData(
            id = ordinal,
            title = displayName,
            iconRes = iconRes
        )
    }
}

enum class BeautyEffect(val displayName: String, val iconRes: Int) {
    WHITENING("Làm trắng", R.drawable.ic_beauty_whitening),
    SLIM_FACE("Làm nhỏ mặt", R.drawable.ic_beauty_slim);
    
    fun toPopupItemData(): PopupItemData {
        return PopupItemData(
            id = ordinal,
            title = displayName,
            iconRes = iconRes
        )
    }
}

// ========== GLSL SHADER CONSTANTS ==========

// Basic filters
const val VINTAGE_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        
        // Vintage effect: sepia + vignette + contrast
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        vec3 sepia = vec3(
            gray * 0.393 + gray * 0.769 + gray * 0.189,
            gray * 0.349 + gray * 0.686 + gray * 0.168,
            gray * 0.272 + gray * 0.534 + gray * 0.131
        );
        
        // Add vignette
        vec2 position = vTextureCoord - vec2(0.5);
        float vignette = 1.0 - dot(position, position) * 1.4;
        sepia *= vignette;
        
        gl_FragColor = vec4(sepia, color.a);
    }
"""

const val BLACK_WHITE_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        gl_FragColor = vec4(gray, gray, gray, color.a);
    }
"""

const val SEPIA_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        vec3 sepia = vec3(
            gray * 0.393 + gray * 0.769 + gray * 0.189,
            gray * 0.349 + gray * 0.686 + gray * 0.168,
            gray * 0.272 + gray * 0.534 + gray * 0.131
        );
        gl_FragColor = vec4(sepia, color.a);
    }
"""

const val COOL_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        // Cool filter: enhance blues, reduce reds
        color.r *= 0.8;
        color.b *= 1.2;
        gl_FragColor = color;
    }
"""

const val WARM_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        // Warm filter: enhance reds/yellows, reduce blues
        color.r *= 1.1;
        color.g *= 1.05;
        color.b *= 0.9;
        gl_FragColor = color;
    }
"""

const val BRIGHT_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        // Brightness filter
        color.rgb *= 1.3;
        gl_FragColor = color;
    }
"""

// ========== BEAUTY FILTERS ==========

const val SMOOTH_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec2 texelSize = vec2(1.0 / 1024.0, 1.0 / 1024.0); // Adjust based on texture size
        vec4 color = texture2D(uTexture, vTextureCoord);
        
        // Gaussian blur kernel for smoothing
        vec4 sum = vec4(0.0);
        float kernel[9];
        kernel[0] = 1.0/16.0; kernel[1] = 2.0/16.0; kernel[2] = 1.0/16.0;
        kernel[3] = 2.0/16.0; kernel[4] = 4.0/16.0; kernel[5] = 2.0/16.0;
        kernel[6] = 1.0/16.0; kernel[7] = 2.0/16.0; kernel[8] = 1.0/16.0;
        
        int index = 0;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                vec2 offset = vec2(float(x), float(y)) * texelSize;
                sum += texture2D(uTexture, vTextureCoord + offset) * kernel[index];
                index++;
            }
        }
        
        // Mix original with blurred for subtle smoothing
        gl_FragColor = mix(color, sum, 0.6);
    }
"""

const val WHITENING_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform sampler2D uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        
        // Skin tone detection (simple approach)
        float skinTone = 0.0;
        if (color.r > 0.4 && color.g > 0.2 && color.b > 0.1 && 
            color.r > color.b && color.g > color.b) {
            skinTone = 1.0;
        }
        
        // Whitening effect for skin areas
        vec3 whitened = color.rgb + vec3(0.15, 0.1, 0.05) * skinTone;
        
        // Brightness boost
        whitened *= 1.1;
        
        // Ensure values don't exceed 1.0
        whitened = clamp(whitened, 0.0, 1.0);
        
        gl_FragColor = vec4(whitened, color.a);
    }
"""

enum class ImageFilter(
    val displayName: String, 
    val iconRes: Int,
    val fragmentShader: String = ""
) {
    NONE("Không filter", R.drawable.ic_filter, ""),
    VINTAGE("Vintage", R.drawable.ic_filter_vintage, VINTAGE_SHADER),
    BLACK_WHITE("Đen trắng", R.drawable.ic_filter_bw, BLACK_WHITE_SHADER),
    SEPIA("Sepia", R.drawable.ic_effects, SEPIA_SHADER),
    COOL("Lạnh", R.drawable.ic_filter, COOL_SHADER),
    WARM("Ấm", R.drawable.ic_filter, WARM_SHADER),
    BRIGHT("Sáng", R.drawable.ic_filter, BRIGHT_SHADER),
    SMOOTH("Làm mịn", R.drawable.ic_beauty_whitening, SMOOTH_SHADER),
    WHITENING("Làm trắng", R.drawable.ic_beauty_slim, WHITENING_SHADER);
    
    fun toPopupItemData(): PopupItemData {
        return PopupItemData(
            id = ordinal,
            title = displayName,
            iconRes = iconRes
        )
    }
}

enum class TimerDelay {
    OFF,
    THREE,
    TEN;

    fun next(): TimerDelay {
        return when (this) {
            OFF -> THREE
            THREE -> TEN
            TEN -> OFF
        }
    }

    fun toIcon(): Int {
        return when (this) {
            OFF -> R.drawable.delay0
            THREE -> R.drawable.delay3
            TEN -> R.drawable.delay10
        }
    }
}

enum class RatioCamera(val ratio: AspectRatioStrategy) {
    RATIO_1_1(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY),
    RATIO_9_16(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY),
    RATIO_3_4(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY);

    fun next(): RatioCamera {
        return when (this) {
            RATIO_1_1 -> RATIO_9_16
            RATIO_9_16 -> RATIO_3_4
            RATIO_3_4 -> RATIO_1_1
        }
    }

    fun toIcon(): Int {
        return when (this) {
            RATIO_1_1 -> R.drawable.resolution11
            RATIO_9_16 -> R.drawable.resolution916
            RATIO_3_4 -> R.drawable.resolution34
        }
    }
}

