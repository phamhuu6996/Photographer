# 🎨 AddTextService - Data Layer Architecture

## **✅ Problem Solved:**
Preview, photo output, và video recording có cùng 1 service vẽ text, đảm bảo 100% identical rendering.

## **🏗️ Architecture:**

### **📦 AddTextService (Data Layer - Core Service):**
```kotlin
object AddTextService {
    // Shared Paint creation
    fun createTextPaint(textSizePx: Float, color: Int): Paint
    fun createStrokePaint(textSizePx: Float, color: Int): Paint
    
    // Shared text processing
    fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String>
    fun drawAddressText(canvas, lines, startX, startY, textPaint, strokePaint, lineHeight, rightAlign)
    
    // Specialized rendering methods
    fun renderAddressToPhoto(canvas, address, bitmapWidth, bitmapHeight)
    fun renderAddressToVideo(canvas, address, frameWidth, frameHeight) 
    fun renderAddressForPreview(canvas, address, canvasWidth, canvasHeight, textSizePx)
}
```

### **🖼️ Preview (CanvasAddressText):**
```kotlin
@Composable
fun CanvasAddressText() {
    Canvas {
        // Use specialized preview method
        AddTextService.renderAddressForPreview(
            canvas = drawContext.canvas.nativeCanvas,
            address = text,
            canvasWidth = size.width,
            canvasHeight = size.height,
            textSizePx = textSizePx
        )
    }
}
```

### **📸 Photo Output (PhotoCaptureService):**
```kotlin
private fun addTextOverlay(bitmap: Bitmap, address: String): Bitmap {
    val canvas = Canvas(mutableBitmap)
    
    // Use specialized photo method
    AddTextService.renderAddressToPhoto(
        canvas, address, bitmap.width, bitmap.height
    )
    
    return mutableBitmap
}
```

### **🎥 Video Recording (VideoRecordingService):**
```kotlin
fun renderAddressToVideoFrame(canvas: Canvas, address: String, frameWidth: Int, frameHeight: Int) {
    // Use specialized video method
    AddTextService.renderAddressToVideo(
        canvas, address, frameWidth, frameHeight
    )
}
```

## **🎯 Benefits:**

### **1. Code Reuse:**
- ❌ **Before:** 2 separate text rendering implementations
- ✅ **After:** 1 shared renderer cho cả preview và output

### **2. Consistency:**
- ✅ **Same Paint objects** với identical configurations
- ✅ **Same text wrapping** với Paint.measureText() accuracy
- ✅ **Same Canvas drawing** với stroke + fill logic
- ✅ **Same positioning** với right-align calculations

### **3. Maintainability:**
- ✅ **Single source of truth** cho text rendering logic
- ✅ **Easier updates** - chỉ cần sửa 1 chỗ
- ✅ **Consistent behavior** across all components

### **4. Performance:**
- ✅ **Reduced code duplication** 
- ✅ **Shared Paint object creation** logic
- ✅ **Consistent memory usage** patterns

## **📁 File Structure:**
```
data/renderer/
└── AddTextService.kt           # 🎨 Core text rendering service (Data Layer)

data/service/
├── PhotoCaptureService.kt      # 📸 Photo output (uses AddTextService)
└── VideoRecordingService.kt    # 🎥 Video output (uses AddTextService)

presentation/common/
├── CanvasAddressText.kt        # 🖼️ Preview Canvas component (uses AddTextService)
├── AddressOverlayConstants.kt  # 📐 Shared styling constants
└── AddressTextUtils.kt         # 🔧 Text formatting utilities
```

## **🎯 Result:**
**Preview, photo output, và video recording bây giờ sử dụng cùng 1 AddTextService → 100% identical rendering!**

- ✅ **Photos**: `renderAddressToPhoto()` cho bitmap processing
- ✅ **Videos**: `renderAddressToVideo()` cho frame-by-frame overlay  
- ✅ **Preview**: `renderAddressForPreview()` cho Compose Canvas
- ✅ **Data Layer**: Proper separation of concerns - text rendering là data processing
- ✅ **Consistent**: Same Paint objects, text wrapping, và positioning logic

User sẽ thấy exactly những gì sẽ được ghi lên cả ảnh và video - perfect consistency! 🎉
