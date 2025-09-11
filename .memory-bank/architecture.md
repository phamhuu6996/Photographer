# System Architecture

## Overall Architecture Pattern

The Photographer app follows **Clean Architecture** principles with **MVVM** presentation pattern, implemented using modern Android development practices.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Presentation Layer                           │
│  ┌─────────────────┐  ┌──────────────────┐  ┌─────────────────┐ │
│  │  Jetpack        │  │    ViewModels    │  │   UI State      │ │
│  │  Compose        │  │  (Camera,        │  │  Management     │ │
│  │  Screens        │  │   Gallery,       │  │                 │ │
│  │                 │  │   Filament)      │  │                 │ │
│  └─────────────────┘  └──────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Domain Layer                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    Use Cases                                │ │
│  │  • TakePhotoUseCase    • RecordVideoUseCase                 │ │
│  │  • SavePhotoUseCase    • SaveVideoUseCase                   │ │
│  │  • GetFirstGalleryItemUseCase                               │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Data Layer                                  │
│  ┌──────────────────┐           ┌──────────────────────────────┐ │
│  │   Repositories   │           │      Data Sources            │ │
│  │  • Camera        │ ◄────────►│  • CameraX API               │ │
│  │  • Gallery       │           │  • MediaStore                │ │
│  └──────────────────┘           │  • File System               │ │
│                                 └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Key Source Code Paths

### Core Application Structure
```
Photographer/app/src/main/java/com/phamhuu/photographer/
├── MainActivity.kt                    # Application entry point
├── Photographer.kt                    # Application class with Koin setup
├── di/
│   └── DI.kt                         # Dependency injection configuration
├── routes/
│   └── Routes.kt                     # Navigation setup
├── presentation/
│   ├── camera/
│   │   ├── CameraScreen.kt           # Main camera UI
│   │   └── CameraViewModel.kt        # Camera business logic
│   ├── gallery/
│   │   ├── GalleryScreen.kt          # Gallery UI
│   │   ├── GalleryViewModel.kt       # Gallery business logic
│   │   └── LargeImageScreen.kt       # Full-screen image viewer
│   ├── common/                       # Shared UI components
│   ├── filament/                     # 3D rendering components
│   └── utils/                        # OpenGL and camera utilities
├── domain/
│   └── usecase/
│       └── CameraUseCase.kt          # Business logic use cases
├── data/
│   └── repository/                   # Data access layer
└── enums/
    └── enums.kt                      # App-wide enumerations
```

### Critical Implementation Paths

#### 1. Camera Preview & Filter Pipeline
```
CameraScreen.kt
    ↓ (setup camera)
CameraViewModel.startCamera()
    ↓ (ImageAnalyzer setup)
ImageProxy → GPUPixelHelper.processImage()
    ↓ (YUV to RGBA conversion)
CameraGLSurfaceView.updateImage()
    ↓ (OpenGL rendering)
FilterRenderer.updateImage()
    ↓ (GLSL shader application)
OpenGL ES Display
```

#### 2. Photo Capture Flow
```
Normal Path:
CameraScreen (capture button) → CameraViewModel.takePhoto() → CameraX ImageCapture → File → Gallery

Filtered Path:
CameraScreen (capture button) → CameraViewModel.takePhoto() → FilterRenderer.captureFilteredBitmap() → OpenGL Render → Bitmap → File → Gallery
```

#### 3. Video Recording Flow
```
Normal Path:
CameraScreen → CameraViewModel.startRecording() → CameraX VideoCapture → MP4 File → Gallery

Filtered Path:
CameraScreen → CameraViewModel.startRecording() → RecordingManager → MediaCodec H264 + AAC → MediaMuxer → MP4 File → Gallery
```

## Component Relationships

### Presentation Layer Components

#### CameraViewModel (Central Orchestrator)
- **Dependencies**: FaceLandmarkerHelper, Manager3DHelper, Use Cases
- **Responsibilities**: 
  - Camera lifecycle management
  - Filter state management
  - Photo/video capture coordination
  - UI state updates
- **Key Methods**: `startCamera()`, `takePhoto()`, `startRecording()`, `setFilter()`

#### CameraScreen (Main UI)
- **Dependencies**: CameraViewModel, Navigation, BeautyAdjustmentPanel
- **Responsibilities**:
  - Always-on filtered camera preview display
  - User interaction handling
  - Permission management
  - UI state rendering
  - Beauty panel integration

### OpenGL ES Filter System

#### FilterRenderer (Core Graphics Engine)
- **File**: `presentation/utils/FilterRenderer.kt`
- **Responsibilities**:
  - OpenGL ES context management
  - GLSL shader compilation and execution
  - Texture management and rendering
  - Frame capture for photos/videos
- **Key Methods**: `updateImage()`, `captureFilteredBitmap()`, `renderToEncoderSurface()`

#### CameraGLSurfaceView (Display Surface)
- **File**: `presentation/utils/CameraGLSurfaceView.kt`
- **Responsibilities**:
  - GLSurfaceView implementation for filtered preview
  - Thread-safe communication with FilterRenderer
  - Video recording integration
- **Key Methods**: `updateImage()`, `startFilteredVideoRecording()`, `release()`

### GPUPixelHelper
- **Package**: `presentation.utils.GPUPixelHelper`
- **Responsibilities**: OpenGL ES filter management, beauty effects, face detection integration
- **Key Methods**:
  - `initGpuPixel()` - Initialize filter chain and face detection
  - `handleImageAnalytic()` - Process camera frames with filters and face detection
  - `updateBeautySettings()` - Apply beauty parameters with face detection logic
  - `isFaceDetected()` - Query current face detection state
- **Filter Chain**: 
  - `mSourceRawData` → `mBeautyFilter` → `mFaceReshapeFilter` → `mLipstickFilter` → `mSinkRawData`
- **Face Detection Integration**: Uses native FaceDetector for landmark detection
- **Smart Beauty Logic**: 
  - **Face-Independent Effects**: `skin_smoothing`, `whiteness` - always applied
  - **Face-Dependent Effects**: `thin_face`, `big_eye`, `blend_level` - only when face detected
  - **State Tracking**: Monitors face detection changes and auto re-applies settings
  - **Real-time Updates**: Automatically adjusts effects when face enters/leaves frame
- **Recent Improvements**: Simplified API, removed unused individual setters, added state management

### Video Recording System

#### RecordingManager (Advanced Recording Pipeline)
- **File**: `presentation/utils/RecordingManager.kt`
- **Responsibilities**:
  - MediaCodec encoder setup (H264 video, AAC audio)
  - MediaMuxer stream multiplexing
  - EGL context management for off-screen rendering
  - Audio capture and processing
- **Key Methods**: `startFilteredVideoRecording()`, `renderToEncoderSurface()`, `drainEncoders()`

### 3D AR System

#### FilamentHelper (3D Rendering Engine)
- **Responsibilities**: Google Filament integration for 3D model rendering
- **Integration**: Works with MediaPipe face detection for AR positioning

#### Manager3DHelper (3D Model Management)
- **Responsibilities**: 3D model loading, positioning, and lifecycle management

### Beauty Adjustment System

#### BeautyAdjustmentPanel (Custom UI Component)
- **File**: `presentation/common/BeautyAdjustmentPanel.kt`
- **Dependencies**: BeautySettings, SlideHorizontal components from UICommon
- **Responsibilities**:
  - 5-parameter beauty slider controls
  - Real-time value updates with normalization
  - Tap-to-dismiss functionality
  - Transparent overlay design
- **Key Features**: Custom SlideHorizontal integration, gradient tracks, compact design

#### BeautySlideHorizontal (Wrapper Component)
- **Responsibilities**: Value range normalization for beauty parameters
- **Integration**: Uses CustomSlider and CustomPaintSlider from UICommon.kt
- **Features**: 8dp track thickness, gradient backgrounds, smooth animations

### Data Layer

#### CameraRepository & CameraRepositoryImpl
- **Responsibilities**: File creation, gallery saving, camera configuration
- **Methods**: `createImageFile()`, `createVideoFile()`, `saveImageToGallery()`, `saveVideoToGallery()`

#### GalleryRepository & GalleryRepositoryImpl
- **Responsibilities**: Gallery content access and management
- **Methods**: `getFirstImageOrVideo()`, gallery content queries

## Design Patterns in Use

### 1. MVVM (Model-View-ViewModel)
- **ViewModels**: Manage UI state and business logic
- **Views**: Jetpack Compose screens for UI rendering
- **Models**: Data classes and repository interfaces

### 2. Repository Pattern
- Abstract data access through repository interfaces
- Implementation hiding behind interfaces
- Clean separation between domain and data layers

### 3. Use Case Pattern
- Single responsibility business logic operations
- Clear API for complex operations
- Testable business logic isolation

### 4. Dependency Injection (Koin)
- Constructor injection for all dependencies
- Single source of truth for object creation
- Easy testing and mocking

### 5. Observer Pattern
- StateFlow/Flow for reactive UI updates
- Lifecycle-aware data observation
- Unidirectional data flow

## Technical Decisions

### Camera Framework: CameraX
- **Reasoning**: Modern, lifecycle-aware, consistent API across devices
- **Benefits**: Automatic handling of device-specific camera quirks
- **Trade-offs**: Less low-level control than Camera2 API

### Graphics: OpenGL ES 2.0
- **Reasoning**: Wide device compatibility, mature ecosystem
- **Benefits**: Hardware-accelerated real-time filtering
- **Trade-offs**: More complex than higher-level graphics APIs

### UI Framework: Jetpack Compose
- **Reasoning**: Modern declarative UI, better performance, easier maintenance
- **Benefits**: Type-safe UI, powerful composition, material design integration
- **Trade-offs**: Learning curve, some advanced features still in development

### Architecture: Clean Architecture + MVVM
- **Reasoning**: Separation of concerns, testability, maintainability
- **Benefits**: Clear boundaries, easy testing, scalable codebase
- **Trade-offs**: Initial complexity, more boilerplate code

### Video Encoding: MediaCodec + MediaMuxer
- **Reasoning**: Hardware-accelerated encoding, format flexibility
- **Benefits**: High-quality output, efficient processing
- **Trade-offs**: Complex implementation, device-specific behavior
