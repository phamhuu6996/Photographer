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
