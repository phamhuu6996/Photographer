package com.phamhuu.photographer.routes

import LocalNavController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.phamhuu.photographer.presentation.camera.CameraScreen
import com.phamhuu.photographer.presentation.gallery.GalleryScreen
import com.phamhuu.photographer.presentation.gallery.LargeImageScreen

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
        }
    }
}