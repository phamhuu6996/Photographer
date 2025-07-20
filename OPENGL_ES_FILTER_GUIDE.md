# 📋 **OpenGL ES Filter Implementation Guide**

## 🎯 **Tổng quan**
Hướng dẫn step-by-step implement **real-time OpenGL ES filtering** cho camera app, bao gồm **làm mịn ảnh** và **làm trắng**.

---

## 🚀 **Step 1: Tạo Custom GLSurfaceView**

### **File:** `CameraGLSurfaceView.kt`
```kotlin
class CameraGLSurfaceView : GLSurfaceView, SurfaceTexture.OnFrameAvailableListener {
    
    init {
        setEGLContextClientVersion(2)
        renderer = CameraRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }
    
    // Key methods:
    fun setImageFilter(filter: ImageFilter)
    fun captureFilteredImage(callback: (Bitmap) -> Unit)
    fun getCameraSurface(): Surface?
}
```

**🔧 Functionality:**
- **OpenGL ES 2.0** context
- **Real-time shader switching**
- **Bitmap capture** từ filtered texture
- **Camera surface** cho CameraX integration

---

## 🎨 **Step 2: GLSL Shaders cho Effects**

### **File:** `enums.kt` - Shader Constants

#### **Basic Filters:**
```glsl
// Black & White
const val BLACK_WHITE_SHADER = """
    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    gl_FragColor = vec4(gray, gray, gray, color.a);
"""

// Vintage với Vignette
const val VINTAGE_SHADER = """
    // Sepia tone + vignette effect
    vec3 sepia = vec3(
        gray * 0.393 + gray * 0.769 + gray * 0.189,
        gray * 0.349 + gray * 0.686 + gray * 0.168,
        gray * 0.272 + gray * 0.534 + gray * 0.131
    );
    vec2 position = vTextureCoord - vec2(0.5);
    float vignette = 1.0 - dot(position, position) * 1.4;
    sepia *= vignette;
"""
```

#### **✨ Beauty Filters:**

**🌟 Làm mịn ảnh (Gaussian Blur):**
```glsl
const val SMOOTH_SHADER = """
    // 3x3 Gaussian blur kernel
    float kernel[9];
    kernel[0] = 1.0/16.0; kernel[1] = 2.0/16.0; kernel[2] = 1.0/16.0;
    kernel[3] = 2.0/16.0; kernel[4] = 4.0/16.0; kernel[5] = 2.0/16.0;
    kernel[6] = 1.0/16.0; kernel[7] = 2.0/16.0; kernel[8] = 1.0/16.0;
    
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            sum += texture2D(uTexture, vTextureCoord + offset) * kernel[index];
        }
    }
    
    // Mix với original cho subtle smoothing
    gl_FragColor = mix(color, sum, 0.6);
"""
```

**💎 Làm trắng da (Skin Whitening):**
```glsl
const val WHITENING_SHADER = """
    // Skin tone detection
    float skinTone = 0.0;
    if (color.r > 0.4 && color.g > 0.2 && color.b > 0.1 && 
        color.r > color.b && color.g > color.b) {
        skinTone = 1.0;
    }
    
    // Whitening effect cho skin areas
    vec3 whitened = color.rgb + vec3(0.15, 0.1, 0.05) * skinTone;
    whitened *= 1.1; // Brightness boost
    whitened = clamp(whitened, 0.0, 1.0);
"""
```

---

## 📱 **Step 3: Camera Integration**

### **CameraViewModel.kt**
```kotlin
// OpenGL ES camera setup
fun startCameraWithGL(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraGLSurfaceView: CameraGLSurfaceView
) {
    glSurfaceView = cameraGLSurfaceView
    
    preview.setSurfaceProvider { surfaceRequest ->
        val surface = cameraGLSurfaceView.getCameraSurface()
        surfaceRequest.provideSurface(surface, executor) { }
    }
}

// Real-time filter application
fun setImageFilter(filter: ImageFilter) {
    glSurfaceView?.setImageFilter(filter)
    _uiState.value = _uiState.value.copy(currentFilter = filter)
}
```

### **Filtered Photo Capture:**
```kotlin
fun takePhoto(context: Context) {
    if (currentFilter != ImageFilter.NONE) {
        // Capture từ OpenGL ES texture
        glSurfaceView?.captureFilteredImage { bitmap ->
            saveBitmapToFile(bitmap, photoFile)
        }
    } else {
        // Normal camera capture
        imageCapture.takePicture(...)
    }
}
```

---

## 🎯 **Step 4: UI Integration**

### **CameraScreen.kt**
```kotlin
@Composable
fun CameraScreen() {
    val cameraGLSurfaceView = remember { CameraGLSurfaceView(context) }
    
    // Real-time filter updates
    LaunchedEffect(uiState.value.currentFilter) {
        cameraGLSurfaceView.setImageFilter(uiState.value.currentFilter)
    }
    
    // Use GLSurfaceView thay vì PreviewView
    AndroidView(factory = { cameraGLSurfaceView })
}
```

### **Filter Selection UI:**
```kotlin
CameraControls(
    onImageFilterSelected = { filter ->
        viewModel.setImageFilter(filter) // ✅ Real-time!
    },
    currentFilter = uiState.value.currentFilter
)
```

---

## 🛠️ **Step 5: Advanced Features**

### **1. Multi-pass Rendering:**
```kotlin
// Combine multiple effects
class MultiPassRenderer {
    fun applyFilters(filters: List<ImageFilter>) {
        for (filter in filters) {
            renderPass(filter)
        }
    }
}
```

### **2. Custom Filter Parameters:**
```glsl
// Adjustable filter strength
uniform float uFilterStrength;
gl_FragColor = mix(originalColor, filteredColor, uFilterStrength);
```

### **3. Face-aware Filtering:**
```kotlin
// Apply filters only to face regions
fun detectFaceRegions(): List<Rect> { /* MediaPipe */ }
fun applyFaceFilters(faceRegions: List<Rect>) { /* Target filtering */ }
```

---

## 📊 **Available Filters**

| Filter | Effect | Use Case | Shader Type |
|--------|--------|----------|-------------|
| **SMOOTH** | Gaussian blur smoothing | Skin beautification | Multi-sample |
| **WHITENING** | Skin tone brightening | Beauty enhancement | Conditional |
| **VINTAGE** | Sepia + vignette | Artistic effect | Complex |
| **BLACK_WHITE** | Grayscale conversion | Classic look | Simple |
| **COOL/WARM** | Color temperature | Mood adjustment | Color transform |
| **BRIGHT** | Brightness boost | Exposure fix | Linear |

---

## ⚡ **Performance Tips**

### **Optimization Strategies:**
1. **Shader Compilation Caching**
2. **Texture Reuse**
3. **Efficient Buffer Management**
4. **Thread-safe Operations**

### **Memory Management:**
```kotlin
override fun onCleared() {
    glSurfaceView?.release() // ✅ Cleanup
    filterHelper.release()
}
```

---

## 🎨 **Adding New Filters**

### **3 Simple Steps:**

**1. Write GLSL Shader:**
```glsl
const val NEW_FILTER_SHADER = """
    precision mediump float;
    varying vec2 vTextureCoord;
    uniform samplerExternalOES uTexture;
    
    void main() {
        vec4 color = texture2D(uTexture, vTextureCoord);
        // Your effect here
        gl_FragColor = color;
    }
"""
```

**2. Add to Enum:**
```kotlin
enum class ImageFilter {
    NEW_FILTER("New Effect", R.drawable.icon, NEW_FILTER_SHADER)
}
```

**3. Test:** Filter tự động xuất hiện trong UI!

---

## 🔧 **Technical Architecture**

### **Data Flow:**
```
Camera → SurfaceTexture → OpenGL ES → Fragment Shader → Filtered Output
                                    ↓
                            Real-time Preview + Photo Capture
```

### **Threading Model:**
- **UI Thread:** Filter selection, state management
- **GL Thread:** Shader compilation, rendering
- **Camera Thread:** Frame capture, analysis

---

## 🚀 **Benefits của Implementation**

✅ **Real-time Performance:** GPU acceleration  
✅ **WYSIWYG:** Preview = captured image  
✅ **Professional Quality:** GLSL shader effects  
✅ **Extensible:** Easy to add new filters  
✅ **Memory Efficient:** Proper resource management  
✅ **Cross-platform:** Standard OpenGL ES  

---

## 📱 **Usage Instructions**

1. **Select Filter:** Tap Effects button → Choose filter
2. **Real-time Preview:** See immediate effect in camera
3. **Take Photo:** Filtered image automatically saved
4. **Perfect Consistency:** Preview matches captured image!

**🎉 Complete OpenGL ES Filter System Ready!** 