package com.phamhuu.photographer.presentation.video.ui

import LocalNavController
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.phamhuu.photographer.R
import com.phamhuu.photographer.contants.ImageMode
import com.phamhuu.photographer.contants.SnackbarType
import com.phamhuu.photographer.presentation.common.DetailViewerAppBar
import com.phamhuu.photographer.presentation.common.ImageCustom
import com.phamhuu.photographer.presentation.common.SnackbarManager
import com.phamhuu.photographer.services.android.ShareService
import com.phamhuu.photographer.presentation.video.vm.VideoPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.androidx.compose.koinViewModel

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(videoUri: String, viewModel: VideoPlayerViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context as? Activity

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(videoUri) { viewModel.load(player, videoUri) }

    // Periodic position updates to VM
    LaunchedEffect(player) {
        while (isActive) {
            viewModel.updatePosition(player.currentPosition)
            delay(500)
        }
    }

    // Unified DisposableEffect for lifecycle, release, and orientation restore
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    player.playWhenReady = state.isPlaying
                }
                Lifecycle.Event.ON_STOP -> {
                    player.playWhenReady = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.updatePosition(player.currentPosition)
            player.release()
            Log.d("VideoPlayerScreen", "activity?.isFinishing: ${activity?.isChangingConfigurations}")
            if (activity?.isChangingConfigurations == false) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Listen to player callbacks
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsLoadingChanged(isLoading: Boolean) {
                viewModel.setBuffering(isLoading)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.setPlaying(isPlaying)
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                viewModel.setError(error.message)
                SnackbarManager.show(message = error.message ?: "Playback error", type = SnackbarType.FAIL)
            }
        }
        player.addListener(listener)

        onDispose { player.removeListener(listener) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setControllerAutoShow(true)
                    setControllerHideOnTouch(true)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == View.VISIBLE
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (controlsVisible) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                if (state.isBuffering) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
                }
                if (state.hasError) {
                    Text(text = state.errorMessage ?: stringResource(R.string.playback_error), color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.load(player, videoUri) }) {
                        Text(text = stringResource(R.string.retry))
                    }
                }
            }

            // AppBar with Share button
            DetailViewerAppBar(
                title = stringResource(R.string.video),
                onBackClick = { navController.popBackStack() },
                onShareClick = {
                    // Share the video
                    ShareService.multiShare(context, listOf(videoUri.toUri()))
                },
                titleColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                actions = {
                    // Full screen toggle button
                    IconButton(onClick = {
                        viewModel.toggleOrientation(activity)
                    }) {
                        ImageCustom(
                            id = R.drawable.full_screen,
                            imageMode = ImageMode.MEDIUM,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                        )
                    }
                }
            )
        }
    }
}
