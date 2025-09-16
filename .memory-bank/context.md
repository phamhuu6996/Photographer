# Context

## Project Overview
- **Package**: `com.phamhuu.photographer`
- **Architecture**: MVVM Feature-first with state-only ViewModels
- **Platform**: Android with Jetpack Compose UI

## Documentation
- Notable docs: `OPENGL_ES_FILTER_GUIDE.md`, `IMAGEANALYZER_FILTER_SOLUTION.md`, `BLACK_SCREEN_FIX_SUMMARY.md`, `CONCURRENCY_FIX_SUMMARY.md`, `ADDRESS_OVERLAY_INTEGRATION.md`, `SHARED_RENDERER_SUMMARY.md`
- Architecture refactoring: `.memory-bank/tasks.md` (updated with current status)

## Assets
- Multiple `.glb` models and a `.task` file for MediaPipe
- 3D assets in `assets/` directory

## Key Features
- **Camera Screen**: `presentation/camera/ui/CameraScreen.kt` with controls
- **Gallery Screen**: `presentation/gallery/ui/GalleryScreen.kt`
- **Location Overlay**: Integrated with address display via `services/renderer/AddTextService.kt`
- **3D Rendering**: Filament integration for glTF models
- **GPU Filters**: GPUPixel integration for real-time camera effects

## Current Structure
- **Presentation**: Feature-based with `ui/` and `vm/` separation
- **Services**: Platform/infrastructure components (`gl/`, `gpu/`, `filament/`, `renderer/`)
- **Data**: Repository pattern with `LocationRepository`, `CameraRepository`, `GalleryRepository`
- **Constants**: Centralized in `contants/` (enums, BeautySettings, etc.)

## Build Configuration
- **Gradle**: Kotlin DSL, AGP 8.5
- **Dependencies**: Local GPU filter AAR, Google Play Services Location, CameraX, MediaPipe
- **DI**: Koin for dependency injection
