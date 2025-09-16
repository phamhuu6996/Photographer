## Goal
Add a Video Viewer (trình xem video) to the app using Jetpack Compose, integrated with navigation, lifecycle-safe playback, and gallery support.

## Proposed Solution
- Use AndroidX Media3 ExoPlayer for robust playback.
- Compose integration via `AndroidView` hosting a `PlayerView` or Compose Media3.
- Add a `VideoPlayerScreen(videoUri: String)` composable.
- Add route: `video/{videoUri}` and navigate with encoded URIs.
- Update gallery to detect videos and open them in the video viewer.
- Support basic controls: play/pause, seek, mute, fullscreen orientation lock.
- Respect lifecycle: prepare on Start, pause on Stop, release on Dispose.
- Show a thumbnail/preview while buffering; display errors via existing `SnackbarManager`.

## Tasks
1) Dependencies
- Add Media3 libraries in `app/build.gradle.kts`:
  - `androidx.media3:media3-exoplayer`
  - `androidx.media3:media3-ui`
  - (Optional) `androidx.media3:media3-session`

2) Navigation
- Update `AppNavHost` to include: `composable("video/{videoUri}")`
- Extract `videoUri` from args and pass to `VideoPlayerScreen`

3) VideoPlayerScreen (Compose)
- Create `app/src/main/java/.../presentation/video/ui/VideoPlayerScreen.kt`
- Build with:
  - `remember` ExoPlayer instance
  - `AndroidView` with `PlayerView` bound to player
  - `DisposableEffect` to release player
  - Lifecycle-aware pause/resume via `LocalLifecycleOwner`
  - Overlay UI: back button, loading, error

4) ViewModel (optional)
- `VideoPlayerViewModel`: `isBuffering`, `hasError`, `playbackPosition`, `isPlaying`
- Functions: `onPlayToggle`, `seekTo`, `onError`
- Register with Koin if needed

5) Gallery integration
- Load videos in addition to images (MediaStore)
- Distinguish item type via MIME/bucket
- On click:
  - Image -> `largeImage/{uri}`
  - Video -> `video/{uri}`
- Add video badge overlay for video items

7) Permissions
- Ensure `READ_MEDIA_VIDEO` (Android 13+) or legacy permissions covered

8) Orientation & fullscreen (optional)
- Lock landscape or enable immersive mode

9) Error handling
- On player error, show Snackbar (FAIL) with Retry

10) Testing
- Test with recorded and gallery videos, long videos, rotation
- Verify back stack and resource release (no leaks)

## Acceptance Criteria
- Navigating to `video/{videoUri}` plays the video with controls
- Back returns cleanly; camera resumes if returning
- Gallery opens correct viewer per media type
- Lifecycle respected; errors surfaced via Snackbar
