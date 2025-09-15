# Context

- Package: `com.phamhuu.photographer`
- Notable docs: `OPENGL_ES_FILTER_GUIDE.md`, `IMAGEANALYZER_FILTER_SOLUTION.md`, `BLACK_SCREEN_FIX_SUMMARY.md`, `CONCURRENCY_FIX_SUMMARY.md`, `ADDRESS_OVERLAY_INTEGRATION.md`, `SHARED_RENDERER_SUMMARY.md`.
- Assets: multiple `.glb` models and a `.task` file for MediaPipe.
- Key screen: camera with controls (see `presentation/common/CameraControls.kt`).
- Location overlay: integrated with address display (see `data/renderer/AddTextService.kt`).
- Constants: centralized in `contants/Contants.kt`.
- Build: Gradle Kotlin DSL, AGP 8.5; includes local GPU filter AAR and Google Play Services Location.
