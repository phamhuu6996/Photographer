# Architecture

## Structure
- **Single-activity app** in `app/` with Navigation Compose for screens
- **MVVM Feature-first** architecture with state-only ViewModels
- **Dependency Injection** via Koin

## Layers
- **Presentation**: 
  - `presentation/{feature}/{ui,vm}/` - Compose UI screens and ViewModels
  - State-only MVVM: ViewModels expose `StateFlow<UiState>` and public functions
  - No UiEvent/UiEffect patterns
- **Domain**: 
  - Use cases and business logic
  - Repository interfaces
- **Data**: 
  - `data/repository/` - Repository implementations
  - `data/model/` - Data models and state classes
  - Room for local persistence; Coil for image loading
- **Services**: 
  - `services/gl/` - OpenGL rendering (FilterRenderer, RecordingManager)
  - `services/gpu/` - GPU processing (GPUPixelHelper)
  - `services/filament/` - 3D rendering (FilamentHelper, Manager3DHelper)
  - `services/renderer/` - Text overlay (AddTextService)

## Camera Pipeline
- **CameraX** preview/capture → **GPU filter** (`gpupixel`) → **MediaPipe** face detection → **OpenGL** rendering → display/output
- **Address overlay** integration via `LocationRepository` cache
- **Recording** with MediaCodec/MediaMuxer for video encoding

## Rendering
- **Filament** for 3D/glTF assets in `assets/`
- **OpenGL ES 2.0** for camera frame processing and filters
- **Canvas** for text overlay rendering

## Performance
- **Repository cache** for per-frame address reads (O(1))
- **Bitmap cache** for overlay rendering to prevent GC churn
- CameraX executors; avoid UI thread blocking; reuse GL contexts
