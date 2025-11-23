package com.phamhuu.photographer.presentation.video.vm

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.lifecycle.ViewModel
import androidx.media3.exoplayer.ExoPlayer
import com.phamhuu.photographer.contants.SnackbarType
import com.phamhuu.photographer.presentation.common.SnackbarManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val playbackPositionMs: Long = 0L,
    val orientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
)

class VideoPlayerViewModel : ViewModel() {

    private val _state = MutableStateFlow(VideoPlayerState())
    val state: StateFlow<VideoPlayerState> = _state.asStateFlow()

    fun setBuffering(buffering: Boolean) {
        _state.value = _state.value.copy(isBuffering = buffering)
    }

    fun setPlaying(playing: Boolean) {
        _state.value = _state.value.copy(isPlaying = playing)
    }

    fun setError(message: String?) {
        _state.value = _state.value.copy(hasError = message != null, errorMessage = message)
    }

    fun clearError() {
        _state.value = _state.value.copy(hasError = false, errorMessage = null)
    }

    fun updatePosition(positionMs: Long) {
        _state.value = _state.value.copy(playbackPositionMs = positionMs)
    }

    fun setOrientation(orientation: Int, activity: Activity?) {
        _state.value = _state.value.copy(orientation = orientation)
        applyOrientation(activity)
    }

    fun toggleOrientation(activity: Activity?) {
        val newOrientation =
            if (_state.value.orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        setOrientation(newOrientation, activity)
    }

    fun applyOrientation(activity: Activity?) {
        activity?.requestedOrientation = _state.value.orientation
    }

    fun load(player: ExoPlayer, videoUri: String) {
        try {
            clearError()
            setBuffering(true)
            player.setMediaItem(androidx.media3.common.MediaItem.fromUri(videoUri))
            player.prepare()
            val pos = state.value.playbackPositionMs
            if (pos > 0) {
                player.seekTo(pos)
            }
            player.playWhenReady = true
            setPlaying(true)
        } catch (e: Exception) {
            val msg = e.message ?: "Playback error"
            setError(msg)
            SnackbarManager.show(message = msg, type = SnackbarType.FAIL)
        } finally {
            setBuffering(false)
        }
    }
}
