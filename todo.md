# MVVM Feature-first Migration Todo

## Preparation
- [ ] Create target directories if missing:
  - `app/src/main/java/com/phamhuu/photographer/presentation/camera/ui`
  - `app/src/main/java/com/phamhuu/photographer/presentation/camera/vm`
  - `app/src/main/java/com/phamhuu/photographer/presentation/gallery/ui`
  - `app/src/main/java/com/phamhuu/photographer/presentation/gallery/vm`
  - `app/src/main/java/com/phamhuu/photographer/presentation/image_view/ui`
  - `app/src/main/java/com/phamhuu/photographer/presentation/image_view/vm`
  - `app/src/main/java/com/phamhuu/photographer/presentation/timer/ui`
  - `app/src/main/java/com/phamhuu/photographer/presentation/timer/vm`
  - `app/src/main/java/com/phamhuu/photographer/presentation/settings/ui` (placeholder)
  - `app/src/main/java/com/phamhuu/photographer/presentation/settings/vm` (placeholder)
  - `app/src/main/java/com/phamhuu/photographer/services/gl`
  - `app/src/main/java/com/phamhuu/photographer/services/renderer`
  - `app/src/main/java/com/phamhuu/photographer/services/gpu`
  - `app/src/main/java/com/phamhuu/photographer/services/mediapipe`
  - `app/src/main/java/com/phamhuu/photographer/services/filament`
  - `app/src/main/java/com/phamhuu/photographer/common/constants`
  - `app/src/main/java/com/phamhuu/photographer/common/utils`
  - `app/src/main/java/com/phamhuu/photographer/common/extensions` (placeholder)

## Presentation: Split by feature (ui/vm)
- [ ] Camera
  - Move `presentation/camera/CameraScreen.kt` → `presentation/camera/ui/CameraScreen.kt`
  - Move `presentation/camera/CameraViewModel.kt` → `presentation/camera/vm/CameraViewModel.kt`
  - Move `presentation/camera/CameraUiState.kt` → `presentation/camera/vm/CameraUiState.kt`
  - Update package declarations accordingly.
- [ ] Gallery
  - Move `presentation/gallery/GalleryScreen.kt` → `presentation/gallery/ui/GalleryScreen.kt`
  - Move `presentation/gallery/GalleryViewModel.kt` → `presentation/gallery/vm/GalleryViewModel.kt`
  - Move `presentation/gallery/GalleryUiState.kt` → `presentation/gallery/vm/GalleryUiState.kt`
  - Update package declarations accordingly.
- [ ] Image View (Large Image)
  - Move `presentation/image_view/LargeImageViewModel.kt` → `presentation/image_view/vm/LargeImageViewModel.kt`
  - Ensure any `LargeImageScreen.kt` (if exists) is under `presentation/image_view/ui/`.
  - Update package declarations accordingly.
- [ ] Timer
  - Move `presentation/timer/TimerViewModel.kt` → `presentation/timer/vm/TimerViewModel.kt`
  - Ensure any Timer UI composables are under `presentation/timer/ui/`.
  - Update package declarations accordingly.
- [ ] Filament
  - Move `presentation/filament/FilamentViewModel.kt` → `presentation/filament/vm/FilamentViewModel.kt`
  - Ensure Filament-related UI (if any) is under `presentation/filament/ui/`.
  - Update package declarations accordingly.
- [ ] Common presentation utilities
  - Keep UI-only helpers like `UiConfig.kt` and `CompositionLocal.kt` under `presentation/common/` (create if needed) or move them to `common/utils` if they are non-UI.

## Services: Platform/infra
- [ ] GL
  - Move `presentation/utils/FilterRenderer.kt` → `services/gl/FilterRenderer.kt`
  - Move `presentation/utils/RecordingManager.kt` → `services/gl/RecordingManager.kt`
  - Move `presentation/utils/CameraGLSurfaceView.kt` → `services/gl/CameraGLSurfaceView.kt`
- [ ] Renderer (Canvas text overlay)
  - Create `services/renderer/AddTextService.kt` and migrate any text-on-canvas logic there (if currently embedded in `FilterRenderer` or other utils).
- [ ] GPU helpers
  - Move `presentation/utils/GPUPixelHelper.kt` → `services/gpu/GPUPixelHelper.kt`
- [ ] MediaPipe
  - Move `presentation/utils/MediaPipeHelper.kt` → `services/mediapipe/MediaPipeHelper.kt`
- [ ] Filament / 3D
  - Move `presentation/utils/FilamentHelper.kt` → `services/filament/FilamentHelper.kt`
  - Move `presentation/utils/Manager3DHelper.kt` → `services/filament/Manager3DHelper.kt`

## Data: Repositories and datasources
- [ ] Repositories (leave in place if already correct)
  - Verify the following exist under `data/repository/`:
    - `CameraRepository.kt` + `CameraRepositoryImpl.kt`
    - `GalleryRepository.kt` + `GalleryRepositoryImpl.kt`
    - `RecordingRepository.kt` + `RecordingRepositoryImpl.kt` (if missing, add to orchestrate services/gl)
    - `MediaRepository.kt` + `MediaRepositoryImpl.kt` (if missing)
    - `LocationRepository.kt` + `LocationRepositoryImpl.kt` (cache `latestAddressText` + Flow)
- [ ] Datasources
  - Create `data/datasource/media/MediaStoreDataSource.kt` (if logic is in repo impls, extract)
  - Create `data/datasource/location/FusedLocationDataSource.kt` (wrap FusedLocationProvider)
  - Create `data/datasource/camera/CameraDataSource.kt` (wrap CameraX interactions)

## Common: utilities and constants
- [ ] Move `presentation/utils/Permission.kt` → `common/utils/Permission.kt`
- [ ] Move `presentation/utils/Gallery.kt` → `common/utils/Gallery.kt`
- [ ] Move `presentation/utils/RecordingConstants.kt` → `common/constants/RecordingConstants.kt`
- [ ] Ensure `common/constants/constants.kt` (typo-fixed to `constants`) for other shared constants.

## DI wiring (Koin)
- [ ] Update `di/` modules to new package paths for moved classes.
- [ ] Provide singletons/factories for:
  - Repositories (as above)
  - Services: `FilterRenderer`, `RecordingManager`, `AddTextService`, helpers
  - ViewModels per feature

## Code updates after moves
- [ ] Update package declarations in each moved file to match new path.
- [ ] Update all imports across the codebase to the new package paths.
- [ ] In `FilterRenderer`, accept `textProvider: () -> String?` to read from `LocationRepositoryImpl.latestAddressText` and maintain internal bitmap cache per `tasks.md`.
- [ ] Ensure ViewModels are state-only: expose `StateFlow<UiState>` and public methods; remove UiEvent/UiEffect if present.
- [ ] Ensure per-frame operations do not read from ViewModel; only from repo cache or `textProvider`.

## Testing and validation
- [ ] Build project to surface missing imports and fix all references.
- [ ] Run unit tests or add tests:
  - `LocationRepositoryImpl` cache + Flow behavior
  - `CameraViewModel` state transitions
- [ ] Manual QA Flows (from `tasks.md`):
  - Start/stop recording with overlay text from repo cache
  - Capture image with optional overlay
  - Gallery listing and preview

## Optional follow-ups
- [ ] Add `services/renderer/AddTextService.kt` implementation for canvas text rendering used by both video and photo flows.
- [ ] Add `RecordingRepository` to orchestrate `FilterRenderer` + `RecordingManager` per sequences in `tasks.md`.

## Notes
- Keep indentation and existing formatting; only touch edited lines.
- Prefer composition and small widgets; extract `presentation/camera/ui/components/` as needed (e.g., `ShutterButton.kt`, `CanvasAddressOverlay.kt`).
