# Architecture

- Structure: Single-activity app in `app/`, Navigation Compose for screens.
- Layers:
  - Presentation: Compose UI and view models (AndroidX), injected via Koin.
  - Domain: simple use-cases/business logic (often in view models).
  - Data: Room for local persistence; Coil for image loading.
- Camera pipeline:
  - CameraX preview/capture → optional GPU filter (`gpupixel`) → optional MediaPipe tasks → display/output.
- Rendering:
  - Filament for 3D/glTF assets in `assets/`.
- Threading/Perf:
  - Use CameraX executors; avoid UI thread blocking; reuse GL contexts where possible.
