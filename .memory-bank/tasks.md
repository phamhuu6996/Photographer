# Tasks

- Add/adjust filters: update GPU Pixel configs; test performance on real devices.
- Camera stability: handle lifecycle, permissions, preview surface size, black screen fixes.
- ML features: configure MediaPipe models (`.task`), manage frame conversion efficiently.
- 3D rendering: load `.glb` assets via Filament; manage renderer lifecycle.
- Navigation/State: keep composables small; hoist state to view models.
- Perf tips: avoid unnecessary allocations in frame loop; prefer immutable data for Compose.

## Location address overlay - Detailed Task Breakdown

### Phase 1: Foundation & Permissions
1.1. Add location dependencies to `build.gradle.kts`:
- Google Play Services Location
- Geocoder (or use built-in Android Geocoder)

1.2. Add manifest permissions to `AndroidManifest.xml`:
- `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`
- Consider `ACCESS_BACKGROUND_LOCATION` if needed

1.3. Update existing permission handler:
- Add location permissions to existing `InitCameraPermission`
- Update permissions array to include `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`
- Leverage existing permission handling infrastructure

### Phase 2: Location Service Layer
2.1. Create location data models:
```kotlin
data class LocationInfo(
    val latitude: Double,
    val longitude: Double, 
    val address: String,
    val timestamp: Long
)
```

2.2. Create `LocationRepository` in `data/` layer:
- Interface and implementation
- Use FusedLocationProviderClient
- Handle location updates and caching

2.3. Create `GeocodingService`:
- Convert lat/lng to readable address
- Handle offline/error cases with fallback text
- Cache recent addresses to avoid API spam

### Phase 3: Location ViewModel & State Management
3.1. Create `LocationViewModel`:
- Manage location state with StateFlow
- Handle permissions state
- Integrate with Koin DI

3.2. Add location state to camera:
- Extend existing camera ViewModel or create shared state
- Location updates only when camera active

### Phase 4: UI Overlay Implementation
4.1. Create `AddressOverlay` composable:
- Small widget following decomposition rule
- Configurable position (top-left, bottom-left, etc.)
- Styled text with background/shadow for readability

4.2. Integrate into camera preview:
- Add overlay to `CameraControls` or camera screen
- Position absolute over preview surface
- Handle orientation changes

### Phase 5: Capture Integration
5.1. Photo capture with address:
- Modify capture logic to embed address
- Option 1: Draw text on bitmap before save
- Option 2: Write to EXIF metadata
- Option 3: Both approaches

5.2. Video recording with address:
- Real-time overlay: Draw text frame-by-frame during recording
- Post-processing: Add text overlay after recording
- Consider performance impact

### Phase 6: Settings & Configuration
6.1. Add settings screen/dialog:
- Toggle enable/disable address overlay
- Choose overlay position
- Choose address format (full/short)

6.2. Persist settings:
- Use DataStore or SharedPreferences
- Set reasonable defaults

### Phase 7: Error Handling & Edge Cases
7.1. Handle permission denied:
- Show fallback UI
- Option to manually enable in settings
- Graceful degradation

7.2. Handle no GPS/network:
- Show "Location unavailable" text
- Retry mechanism
- Offline caching of last known location

7.3. Handle geocoding failures:
- Fallback to coordinates display
- Rate limiting protection
- Network timeout handling

### Phase 8: Testing & Polish
8.1. Unit tests:
- LocationRepository tests
- GeocodingService tests
- ViewModel state tests

8.2. Integration tests:
- Permission flow tests
- Camera capture with address tests

8.3. Performance testing:
- Memory usage with continuous location updates
- Battery impact assessment
- UI responsiveness during geocoding

### Implementation Status:
✅ **Completed High Priority:**
- Permissions and LocationRepository (clean interface)
- Address overlay with simplified CanvasAddressText/CanvasAddressOverlay
- Photo capture integration with AddTextService
- Location toggle integrated into CameraControls with on/off icons
- Location state unified into CameraUiState (isLocationEnabled)
- Basic unit tests
- Code consolidation and cleanup

✅ **Completed Medium Priority:**
- DI integration with Koin
- Single unified state object (CameraUiState)
- EXIF metadata writing
- Constants consolidation in Contants.kt
- AddTextService self-contained (no external dependencies)
- UI components simplified and optimized

✅ **Completed Low Priority:**
- Address formatting moved to AddTextService
- Removed redundant files (AddressTextUtils.kt, AddressOverlayConstants.kt)
- Removed unused constants and methods
- Canvas size optimization (180x80dp for preview)

📋 **Remaining Tasks:**
- Real-time video overlay during recording
- Advanced performance optimization  
- Comprehensive integration tests

🎯 **Current Implementation (Updated):**

**AddTextService (Data Layer - Consolidated):**
- **Service:** AddTextService in data/renderer - self-contained text rendering service
- **Photo Capture:** renderAddressToPhoto() for bitmap output
- **Video Recording:** renderAddressToVideo() for frame-by-frame overlay  
- **Preview:** renderAddressForPreview() for Compose Canvas with extra right padding
- **Paint Objects:** Shared createTextPaint() with Paint.Align.RIGHT
- **Text Processing:** Built-in wrapText(), drawAddressText(), and formatAddress() methods
- **Architecture:** Single service handles all text rendering - no external dependencies

**Location Toggle (UI Layer):**
- **Integration:** Built into CameraControls top row with other camera controls
- **Icons:** R.drawable.location_on / R.drawable.location_off based on state
- **State:** CameraUiState.isLocationEnabled (default: true)
- **Handler:** onChangeLocationToggle calls viewModel.toggleLocationEnabled()
- **Conditional Display:** Address overlay only shows when isLocationEnabled = true
- **Position:** Top-right aligned with proper padding for readability

**UI Components (Simplified):**
- **CanvasAddressText:** Simple text renderer - fixed 180x80dp canvas size
- **CanvasAddressOverlay:** Minimal wrapper - shows text or Spacer based on locationInfo
- **Constants:** All address constants moved to Contants.kt (TEXT_SIZE, MAX_LINES, etc.)
- **Removed:** AddressTextUtils.kt (functionality moved to AddTextService)
- **Removed:** AddressOverlayConstants.kt (moved to Contants.kt)

**Location Repository (Clean):**
- **Interface:** Minimal 3 methods - getCurrentLocation(), getLastKnownLocation(), stopLocationUpdates()
- **Implementation:** LocationRepositoryImpl with FusedLocationProviderClient + Geocoder
- **State Management:** Direct access via CameraUiState (no separate StateFlow accessors)
- **DI:** Single LocationRepository instance in Koin appModule

## Video Text Overlay - Detailed Task Breakdown

### 🎯 **Goal:** Implement real-time address overlay on video recording output

### 📋 **Current State Analysis:**
- ✅ **RecordingManager**: Complete OpenGL-based video recording with MediaCodec + MediaMuxer
- ✅ **AddTextService**: Text rendering service with `renderAddressToVideo()` method 
- ✅ **FilterRenderer**: OpenGL rendering pipeline for beauty filters
- ✅ **Location Service**: Address data available via `CameraUiState.locationState.locationInfo`
- ❌ **Integration**: Text overlay not integrated into video recording pipeline
- ❌ **Real-time**: No frame-by-frame text rendering during recording

### 🔍 **Technical Approach Options:**

#### **Option 1: OpenGL Shader Integration (Recommended)**
**Pros:** Real-time, GPU-accelerated, consistent with existing filter pipeline
**Cons:** Complex OpenGL text rendering, shader programming required

#### **Option 2: Post-Processing with FFmpeg**
**Pros:** Easier text rendering, proven solution
**Cons:** Additional dependency, CPU-intensive, processing delay

#### **Option 3: Canvas Overlay on Encoder Surface**
**Pros:** Reuse existing AddTextService, Android Canvas API
**Cons:** CPU-intensive, potential performance issues

### 🎯 **Recommended Implementation: Option 3 (Canvas + Encoder Surface)**

### Phase 1: Encoder Surface Text Integration
**1.1. Modify RecordingManager.renderToEncoderSurface()**
```kotlin
// Current signature
fun renderToEncoderSurface(renderFunction: (Int, Int) -> Unit)

// New signature  
fun renderToEncoderSurface(
    renderFunction: (Int, Int) -> Unit,
    overlayFunction: ((Canvas, Int, Int) -> Unit)? = null
)
```

**1.2. Create Canvas from Encoder Surface**
```kotlin
// In renderToEncoderSurface method
if (overlayFunction != null) {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    overlayFunction(canvas, width, height)
    // Draw bitmap to OpenGL texture
    // Render texture to encoder surface
}
```

**1.3. Integrate AddTextService**
```kotlin
// Create overlay function in CameraViewModel
private fun createVideoOverlayFunction(): (Canvas, Int, Int) -> Unit = { canvas, width, height ->
    val location = uiState.value.locationState.locationInfo
    if (showOnVideos && location != null) {
        AddTextService.renderAddressToVideo(canvas, location.address, width)
    }
}
```

### Phase 2: Performance Optimization
**2.1. Bitmap Pooling**
- Reuse bitmap objects to avoid GC pressure
- Create bitmap pool for different sizes
- Implement efficient bitmap recycling

**2.2. Text Caching**
- Cache rendered text bitmaps when address doesn't change
- Invalidate cache only when location updates
- Pre-render common text elements

**2.3. Threading Optimization**
- Ensure text rendering happens on background thread
- Use coroutines for async text processing
- Avoid blocking the OpenGL render thread

### Phase 3: Integration Points
**3.1. Update FilterRenderer/CameraGLSurfaceView**
```kotlin
// In startFilteredVideoRecording
recordingManager.renderToEncoderSurface(
    renderFunction = { width, height -> 
        // Existing filter rendering
        renderFilteredFrame(width, height)
    },
    overlayFunction = viewModel.createVideoOverlayFunction()
)
```

**3.2. Update CameraViewModel.stopRecording()**
```kotlin
// Remove TODO comment, add actual implementation
if (success && videoFile != null) {
    // Text overlay now integrated in real-time during recording
    // No post-processing needed
    saveVideoToGallery(videoFile)
}
```

### Phase 4: Configuration & Settings
**4.1. Video Text Settings**
```kotlin
// Add to CameraUiState or separate settings
data class VideoTextSettings(
    val enabled: Boolean = true,
    val position: TextPosition = TextPosition.TOP_RIGHT,
    val opacity: Float = 1.0f,
    val fontSize: Float = 1.0f
)
```

**4.2. Runtime Toggle**
- Respect `CameraUiState.isLocationEnabled` flag
- Allow users to toggle video overlay independently
- Sync with photo overlay settings

### Phase 5: Error Handling & Fallbacks
**5.1. Performance Monitoring**
- Monitor frame rate during recording
- Detect performance degradation
- Fallback to no-overlay if performance issues

**5.2. Memory Management**
- Monitor memory usage during recording
- Implement emergency cleanup if memory low
- Graceful degradation strategies

**5.3. Error Recovery**
- Handle Canvas/Bitmap creation failures
- Continue recording without overlay if text rendering fails
- Log errors for debugging

### Phase 6: Testing & Validation
**6.1. Performance Testing**
- Test on various devices (low-end to high-end)
- Measure impact on recording quality
- Validate frame rate consistency

**6.2. Quality Testing**
- Verify text readability in different lighting
- Test with various address lengths
- Validate text positioning accuracy

**6.3. Integration Testing**
- Test with different video resolutions
- Test with beauty filters enabled
- Test location permission scenarios

### 📊 **Implementation Priority:**

**🔴 High Priority (Core Functionality):**
- Phase 1: Encoder Surface Text Integration
- Phase 3: Integration Points
- Phase 5: Error Handling (basic)

**🟡 Medium Priority (Optimization):**
- Phase 2: Performance Optimization (bitmap pooling, caching)
- Phase 4: Configuration & Settings

**🟢 Low Priority (Polish):**
- Phase 2: Advanced performance optimization
- Phase 6: Comprehensive testing
- Phase 4: Advanced settings UI

### 🎯 **Success Criteria:**
1. ✅ Address text appears on recorded videos when location enabled
2. ✅ Text position matches photo overlay (TOP_RIGHT)
3. ✅ No significant impact on video quality or frame rate
4. ✅ Graceful handling when location unavailable
5. ✅ Memory usage remains stable during long recordings

### 🔧 **Key Files to Modify:**
```
app/src/main/java/com/phamhuu/photographer/
├── presentation/utils/RecordingManager.kt         # Core integration point
├── presentation/utils/CameraGLSurfaceView.kt     # OpenGL integration  
├── presentation/camera/CameraViewModel.kt        # Overlay function creation
├── data/renderer/AddTextService.kt               # Text rendering (already done)
└── presentation/camera/CameraUiState.kt          # Settings (if needed)
```
