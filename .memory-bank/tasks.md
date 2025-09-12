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
- Permissions and LocationRepository
- Basic overlay with AddressOverlay composable  
- Photo capture integration with PhotoCaptureService
- Simple toggle icon for enable/disable
- Location state unified into CameraUiState
- Basic unit tests

✅ **Completed Medium Priority:**
- DI integration with Koin
- Single unified state object (CameraUiState)
- EXIF metadata writing
- Error handling and fallbacks

🚧 **Partially Completed Low Priority:**
- Basic video service structure (metadata only)
- Location permission handling
- Address formatting options

📋 **Remaining Tasks:**
- Real-time video overlay during recording
- Advanced performance optimization  
- Comprehensive integration tests

🎯 **Current Implementation (AddTextService - Data Layer):**
- **Service:** AddTextService in data/renderer for all text rendering
- **Photo Capture:** renderAddressToPhoto() for bitmap output
- **Video Recording:** renderAddressToVideo() for frame-by-frame overlay
- **Preview:** renderAddressForPreview() for Compose Canvas
- **Paint Objects:** Shared createTextPaint() and createStrokePaint()
- **Text Processing:** Shared wrapText() and drawAddressText() logic
- **Architecture:** Data layer service used by presentation and services
- **Default:** Location enabled, TOP_RIGHT, FULL address
- **Photos/Videos:** Follow main toggle
