# Address Overlay Integration Points

## 📸 Photo Output Integration

### Location: `CameraViewModel.saveBitmapToFile()`
**File:** `app/src/main/java/com/phamhuu/photographer/presentation/camera/CameraViewModel.kt`
**Lines:** ~414-430

```kotlin
private suspend fun saveBitmapToFile(bitmap: Bitmap, file: File) = withContext(Dispatchers.IO) {
    // Add address overlay if location is enabled and available
    val finalBitmap = if (showOnPhotos && uiState.value.locationState.locationInfo != null) {
        addAddressOverlayToBitmap(bitmap, uiState.value.locationState.locationInfo!!)
    } else {
        bitmap
    }
    
    FileOutputStream(file).use { out ->
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    finalBitmap.recycle()
}
```

### Helper Method: `addAddressOverlayToBitmap()`
```kotlin
private fun addAddressOverlayToBitmap(bitmap: Bitmap, locationInfo: LocationInfo): Bitmap {
    return photoCaptureService.addAddressToBitmap(
        bitmap = bitmap,
        locationInfo = locationInfo // Position hardcoded as TOP_RIGHT
    )
}
```

### Service: `PhotoCaptureService.addAddressToBitmap()`
**File:** `app/src/main/java/com/phamhuu/photographer/data/service/PhotoCaptureService.kt`
```kotlin
fun addAddressToBitmap(
    bitmap: Bitmap,
    locationInfo: LocationInfo
): Bitmap {
    return addTextOverlay(bitmap, locationInfo.address) // Hardcoded TOP_RIGHT
}
```

## 🎥 Video Output Integration

### Location: `CameraViewModel.stopRecording()`
**File:** `app/src/main/java/com/phamhuu/photographer/presentation/camera/CameraViewModel.kt`
**Lines:** ~784-790

```kotlin
helper.glSurfaceView?.stopFilteredVideoRecording { success: Boolean, videoFile: File? ->
    if (success && videoFile != null) {
        viewModelScope.launch {
            // TODO: Add address overlay to video file if needed
            // For now, just save the video as-is
            saveVideoToGallery(videoFile)
        }
    }
}
```

**Status:** 🚧 **Placeholder** - Real-time video overlay requires more complex implementation

## ⚙️ Configuration

### Settings (Identical Preview & Output):
- **Position:** TOP_RIGHT (preview: Canvas right-align, output: TOP_RIGHT)
- **Format:** FULL address with identical auto line wrapping
- **Style:** White text with black stroke border, no background
- **Text Size:** 12sp preview / 2.5% of bitmap width output (same Paint objects)
- **Max Lines:** 3 lines with same overflow behavior
- **Max Width:** Same pixel calculations for both preview and output
- **Rendering:** Both use Android Canvas with identical Paint configurations
- **Text Wrapping:** Same `wrapTextForCanvas()` logic with Paint.measureText()
- **Preview:** Inside camera frame using CanvasAddressOverlay
- **Photos:** Enabled when `showOnPhotos = true`
- **Videos:** Enabled when `showOnVideos = true` (not implemented yet)

### Conditions:
- Only adds overlay if `isLocationEnabled = true`
- Only adds overlay if `locationInfo != null`
- Uses current location from `uiState.value.locationState.locationInfo`

## 🔄 Flow:

**Preview (Real-time):**
1. **Location updates** → `LocationViewModel` → `CameraUiState`
2. **Canvas rendering** → `CanvasAddressOverlay` → Same Paint objects as output
3. **Text wrapping** → Same `wrapTextForCanvas()` logic as PhotoCaptureService
4. **Right-aligned positioning** → Matches TOP_RIGHT output position

**Photo Output:**
1. **User takes photo** → `takePhoto()` → `captureFromGLSurface()` → `saveBitmapToFile()`
2. **Check conditions** → Location enabled + Location available
3. **Add overlay** → `addAddressOverlayToBitmap()` → `PhotoCaptureService.addAddressToBitmap()`
4. **Draw text** → Canvas drawing with stroke + fill (identical to preview)
5. **Save to file** → Compressed JPEG with address overlay

## 📋 Next Steps for Video:
- Implement real-time overlay during recording
- Or post-process video file after recording
- Add metadata to video file headers
