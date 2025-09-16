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

## Detailed Tasks
1) Dependencies
- Add Media3 libraries in `app/build.gradle.kts`:
  - `androidx.media3:media3-exoplayer`
  - `androidx.media3:media3-ui`
  - (Optional) `androidx.media3:media3-session` for casting/media controls

2) Navigation
- Update `AppNavHost` to include: `composable("video/{videoUri}")`
- Extract `videoUri` from args and pass to `VideoPlayerScreen`

3) VideoPlayerScreen (Compose)
- Create `app/src/main/java/.../presentation/video/ui/VideoPlayerScreen.kt`
- Build with:
  - `remember` ExoPlayer instance
  - `AndroidView` with `PlayerView` bound to player
  - `DisposableEffect` to release player
  - Handle play/pause based on lifecycle (use `LocalLifecycleOwner` + observer)
  - UI overlays: back button (reuse `ImageCustom`), loading indicator, error state

4) ViewModel (optional but recommended)
- Create `VideoPlayerViewModel` for simple state: isBuffering, hasError, playbackPosition, isPlaying
- Expose functions: `onPlayToggle`, `seekTo`, `onError`
- Register with Koin if needed

5) Gallery integration
- Extend `GalleryViewModel`/data source to load videos in addition to images
- Distinguish items by MIME type or `MediaStore` table (Images vs Video)
- In `GalleryScreen`, on click:
  - If image -> navigate to `largeImage`
  - If video -> navigate to `video/{encodedUri}`
- Add a small video badge icon overlay for video items

7) Permissions
- Ensure `READ_MEDIA_VIDEO` (Android 13+) or legacy `READ_EXTERNAL_STORAGE` handled by existing permission flow

8) Orientation & fullscreen
- Lock orientation to landscape while in video viewer (optional), or allow system UI to hide when the player controls are visible

9) Error handling
- On player error, surface message via `SnackbarManager.show(..., type = FAIL)` and show a retry button

10) Testing
- Test with: local recorded video URI, gallery-picked video URI, long videos, and rotation
- Verify back stack and resource release (no leaks)

## Acceptance Criteria
- Navigating to `video/{videoUri}` plays the target video with working controls
- Back returns to previous screen without leaks; camera resumes if returning to camera
- Gallery items open the correct viewer based on type (image vs video)
- Playback state respects lifecycle and errors are surfaced via Snackbar
