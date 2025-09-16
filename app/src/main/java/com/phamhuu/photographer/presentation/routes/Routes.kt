package com.phamhuu.photographer.presentation.routes

import LocalNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.phamhuu.photographer.presentation.camera.ui.CameraScreen
import com.phamhuu.photographer.presentation.gallery.LargeImageScreen
import com.phamhuu.photographer.presentation.gallery.ui.GalleryScreen
import com.phamhuu.photographer.presentation.video.ui.VideoPlayerScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    CompositionLocalProvider(LocalNavController provides navController) {
        NavHost(navController = navController, startDestination = "camera") {
            composable("camera") { CameraScreen() }
            composable("gallery") { GalleryScreen() }
            composable("largeImage/{imageUri}") { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
                LargeImageScreen(imageUri = imageUri)
            }
            composable("video/{videoUri}") { backStackEntry ->
                val videoUri = backStackEntry.arguments?.getString("videoUri") ?: return@composable
                VideoPlayerScreen(videoUri = videoUri)
            }
        }
    }
}