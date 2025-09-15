# Photographer

A modern Android camera app with real-time OpenGL ES filters, 3D models, and advanced video recording capabilities.

## Features

- 📸 **Photo Capture**: Normal and filtered photo capture
- 🎥 **Video Recording**: With and without real-time filters
- 🎨 **Real-time Filters**: OpenGL ES-based image processing
- 🎭 **3D Models**: Filament-based 3D model rendering
- 📱 **Modern UI**: Jetpack Compose with Material Design 3
- 🔧 **Advanced Recording**: MediaCodec + MediaMuxer for filtered videos

## Architecture

### 1. Filter Preview Flow

```mermaid
graph TD
    A[Camera Start] --> B[CameraX Setup]
    B --> C[ImageAnalyzer Setup]
    C --> D[ImageProxy Processing]
    D --> E{Filter Selected?}
    E -->|No| F[Normal PreviewView]
    E -->|Yes| G[CameraGLSurfaceView]
    G --> H[GPUPixelHelper]
    H --> I[FilterRenderer]
    I --> J[OpenGL ES Processing]
    J --> K[Filtered Preview Display]
    F --> L[Standard Camera Preview]
    
    style A fill:#e1f5fe
    style G fill:#f3e5f5
    style I fill:#fff3e0
    style K fill:#e8f5e8
```

### 2. Photo Capture Flow

```mermaid
graph TD
    A[Take Photo Button] --> B[CameraViewModel.takePhoto]
    B --> C{Has Filter?}
    C -->|No| D[Normal Capture Path]
    C -->|Yes| E[Filtered Capture Path]
    
    D --> D1[ImageCapture.takePicture]
    D1 --> D2[CameraX API]
    D2 --> D3[Save to File]
    D3 --> D4[Save to Gallery]
    
    E --> E1[GPUPixelHelper.captureFilteredBitmap]
    E1 --> E2[FilterRenderer Capture]
    E2 --> E3[OpenGL ES Render]
    E3 --> E4[Bitmap Generation]
    E4 --> E5[Save to File]
    E5 --> E6[Save to Gallery]
    
    D4 --> F[Photo Saved Successfully]
    E6 --> F
    
    style A fill:#e1f5fe
    style D fill:#e3f2fd
    style E fill:#f3e5f5
    style F fill:#e8f5e8
```

### 3. Video Recording Flow

```mermaid
graph TD
    A[Start Recording Button] --> B[CameraViewModel.startRecording]
    B --> C{Has Filter?}
    C -->|No| D[Normal Recording Path]
    C -->|Yes| E[Filtered Recording Path]
    
    D --> D1[CameraX VideoCapture]
    D1 --> D2[withAudioEnabled]
    D2 --> D3[FileOutputOptions]
    D3 --> D4[Start Recording]
    D4 --> D5[Video + Audio Stream]
    D5 --> D6[Save to File]
    D6 --> D7[Save to Gallery]
    
    E --> E1[RecordingManager]
    E1 --> E2[Setup Video Encoder H264]
    E1 --> E3[Setup Audio Encoder AAC]
    E1 --> E4[Setup MediaMuxer]
    E1 --> E5[Setup EGL Context]
    E2 --> E6[OpenGL Filter Rendering]
    E3 --> E7[AudioRecord Processing]
    E4 --> E8[Multiplex Streams]
    E6 --> E9[Encoder Surface]
    E7 --> E10[Audio Data Processing]
    E9 --> E8
    E10 --> E8
    E8 --> E11[MP4 File Output]
    E11 --> E12[Save to Gallery]
    
    D7 --> F[Video Saved Successfully]
    E12 --> F
    
    style A fill:#e1f5fe
    style D fill:#e3f2fd
    style E fill:#f3e5f5
    style E1 fill:#fff3e0
    style F fill:#e8f5e8
```

## Technical Details

### Filter Preview Architecture

1. **CameraX ImageAnalyzer** captures camera frames
2. **GPUPixelHelper** processes ImageProxy data
3. **FilterRenderer** applies OpenGL ES filters
4. **CameraGLSurfaceView** displays filtered preview

### Photo Capture Architecture

- **Normal Photos**: Uses CameraX ImageCapture API
- **Filtered Photos**: Uses OpenGL ES rendering pipeline
- **File Management**: Unified save to gallery system

### Video Recording Architecture

- **Normal Videos**: CameraX VideoCapture with audio enabled
- **Filtered Videos**: Custom MediaCodec + MediaMuxer pipeline
- **Audio Processing**: AudioRecord + AAC encoding
- **Video Processing**: H264 encoding with OpenGL ES filters

## Key Components

### RecordingManager
- Manages MediaCodec encoders (H264 video, AAC audio)
- Handles MediaMuxer for stream multiplexing
- Provides EGL context for off-screen rendering
- Uses coroutines for audio processing

### FilterRenderer
- OpenGL ES-based image processing
- Real-time filter application
- GPU-accelerated rendering

### GPUPixelHelper
- Bridges CameraX ImageAnalyzer with OpenGL ES
- Handles ImageProxy to texture conversion
- Manages filter state and rendering

## Dependencies

- **CameraX**: Camera functionality
- **OpenGL ES**: Graphics rendering
- **MediaCodec**: Video/audio encoding
- **Filament**: 3D model rendering
- **Jetpack Compose**: Modern UI
- **Kotlin Coroutines**: Async processing

## Build Requirements

- Android API 28+
- Kotlin 1.9.0+
- Gradle 8.5.0+

## Usage

1. **Normal Photo/Video**: Use standard camera controls
2. **Filtered Content**: Select filter from bottom navigation
3. **3D Models**: Choose 3D model for AR effects
4. **Settings**: Adjust resolution, flash, timer, etc.

## Performance Notes

- Filtered recording uses more CPU/GPU resources
- Audio recording requires RECORD_AUDIO permission
- Large video files may take time to process
- 3D models impact rendering performance