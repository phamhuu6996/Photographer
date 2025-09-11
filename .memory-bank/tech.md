# Technology Stack

## Core Technologies

### Android Platform
- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 28 (Android 9.0, API level 28)
- **Compile SDK**: 34
- **Java Compatibility**: Java 17
- **Kotlin Version**: 1.9.0

### UI Framework
- **Jetpack Compose**: 2025.02.00 BOM
  - `androidx.compose.ui`: 1.7.8
  - `androidx.compose.material3`: 1.0.1
  - `androidx.compose.ui:ui-tooling-preview`: 1.3.3
- **Navigation**: Jetpack Navigation Compose 2.8.8
- **Lifecycle**: ViewModel Compose 2.7.0, Runtime Compose 2.6.0

### Camera & Media
- **CameraX**: 1.4.1 (complete suite)
  - `camera-core`, `camera-camera2`, `camera-lifecycle`
  - `camera-video`, `camera-view`, `camera-extensions`
- **OpenGL ES**: 2.0+ for real-time filtering
- **MediaCodec**: Hardware-accelerated video encoding (H264)
- **MediaMuxer**: Audio/video stream multiplexing
- **AudioRecord**: Microphone audio capture

### Computer Vision & AR
- **Google MediaPipe**: 0.20230731
  - Face landmark detection
  - Real-time face tracking
- **Google Filament**: 1.36.0
  - 3D model rendering engine
  - PBR (Physically Based Rendering)
  - GLTF model support

### Image Processing
- **GPUPixel**: Custom AAR library (`gpupixel-release.aar`)
  - GPU-accelerated image processing
  - OpenGL ES filter pipeline
- **GLSL Shaders**: Custom fragment shaders for filters
- **Coil**: 2.5.0 for image loading and caching

### Dependency Injection
- **Koin**: 3.4.0
  - `koin-android`: Core Android integration
  - `koin-androidx-compose`: Compose integration
  - Constructor injection pattern

### Build System
- **Gradle**: 8.5.0
- **Android Gradle Plugin**: 8.5.0
- **Kotlin Compiler Extension**: 1.5.1

## Development Setup

### Prerequisites
```bash
# Required tools
Android Studio Hedgehog (2023.1.1) or later
Java 17 JDK
Android SDK API 28-34
Git

# Recommended
Android Emulator with API 28+ 
Physical device for camera testing
GPU-enabled emulator for OpenGL testing
```

### Project Setup
```bash
# Clone repository
git clone <repository-url>
cd Photographer

# Sync Gradle dependencies
./gradlew clean build

# Install debug build
./gradlew installDebug
```

### Key Configuration Files

#### `gradle/libs.versions.toml`
- Centralized version management
- All library versions defined here
- Referenced in module build files

#### `app/build.gradle.kts`
- Main module configuration
- Compose setup with compiler extension
- Custom AAR integration (`gpupixel-release.aar`)

#### `local.properties`
- SDK path configuration
- Local development settings
- Not tracked in version control

## Technical Constraints

### Performance Requirements
- **Frame Rate**: Maintain 30+ FPS during filtered preview
- **Memory Usage**: Stay under 200MB during normal operation
- **Battery Impact**: Comparable to native camera apps
- **Startup Time**: Camera ready within 2 seconds

### Device Requirements
- **OpenGL ES**: 2.0+ support (hardware requirement)
- **Camera**: Camera2 API support or CameraX compatibility
- **Memory**: Minimum 3GB RAM recommended for smooth operation
- **Storage**: 100MB+ free space for app and temporary files
- **Audio**: Microphone access for video recording

### API Limitations
- **Camera Access**: Single camera instance (no simultaneous multi-camera)
- **OpenGL Context**: Single context shared between preview and recording
- **MediaCodec**: Hardware encoder availability varies by device
- **File Access**: Scoped storage restrictions on Android 10+

## Dependencies Overview

### Core Android Dependencies
```kotlin
// Essential Android libraries
androidx-core-ktx = "1.10.1"
androidx-appcompat = "1.6.1" 
androidx-activity-compose = "1.6.1"
material = "1.10.0"
```

### Camera & Media Stack
```kotlin
// CameraX complete suite
androidx-camera-* = "1.4.1"

// Custom GPU processing
gpupixel-release.aar (local)

// 3D rendering
filament-android = "1.36.0"
filament-utils-android = "1.36.0"
gltfio-android = "1.36.0"

// Computer vision
tasks-vision = "0.20230731"
```

### UI & Navigation
```kotlin
// Compose BOM manages all Compose versions
androidx-compose-bom = "2025.02.00"

// Navigation
androidx-navigation-compose = "2.8.8"
androidx-navigation-runtime-ktx = "2.8.8"

// Image loading
coil-compose = "2.5.0"
```

### Testing Framework
```kotlin
// Unit testing
junit = "4.13.2"

// Android testing
androidx-junit = "1.1.5"
androidx-espresso-core = "3.5.1"
```

## Tool Usage Patterns

### Development Workflow
1. **Code**: Android Studio with Kotlin
2. **Build**: Gradle with version catalogs
3. **Test**: Unit tests + instrumented tests
4. **Debug**: Android Studio debugger + GPU debugging tools
5. **Profile**: Android Studio profiler for performance analysis

### OpenGL Development
- **Shader Development**: External GLSL editor + Android integration
- **Debugging**: RenderDoc for frame analysis (when available)
- **Testing**: Multiple devices for compatibility testing

### Camera Testing
- **Emulator**: Limited camera functionality, basic UI testing
- **Physical Devices**: Required for camera, OpenGL, and performance testing
- **Various Resolutions**: Test different screen sizes and camera capabilities

### Performance Monitoring
- **Memory**: Android Studio Memory Profiler
- **GPU**: GPU Inspector (when available)
- **Battery**: Battery Historian
- **Frame Rate**: On-device FPS monitoring

## External Integrations

### Google Services
- **MediaPipe**: Face detection models downloaded at runtime
- **Filament**: 3D rendering engine with asset pipeline

### Custom Libraries
- **GPUPixel**: Proprietary image processing library
  - Located in `app/libs/gpupixel-release.aar`
  - Provides OpenGL ES filter implementations
  - Interface through JNI for native processing

### System Integrations
- **MediaStore**: Gallery access and content insertion
- **Camera HAL**: Hardware abstraction layer through CameraX
- **OpenGL ES**: Direct GPU access for real-time processing
- **AudioRecord**: Low-level audio capture for video recording

## Build Configuration

### Debug vs Release
```kotlin
buildTypes {
    debug {
        // Debug optimizations
        isDebuggable = true
        isMinifyEnabled = false
    }
    
    release {
        // Production optimizations
        isMinifyEnabled = false  // Disabled for now
        proguardFiles(...)
    }
}
```

### Proguard/R8 Configuration
- Currently disabled (`isMinifyEnabled = false`)
- ProGuard rules in `proguard-rules.pro`
- Will need configuration for OpenGL and MediaPipe when enabled

### Signing Configuration
- Debug signing with default debug keystore
- Release signing configuration needed for distribution
