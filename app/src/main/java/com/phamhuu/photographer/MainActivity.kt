package com.phamhuu.photographer

import LocalNavController
import RenderableModel
import android.os.Bundle
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.filament.utils.Utils
import com.phamhuu.photographer.presentation.camera.CameraScreen
import com.phamhuu.photographer.presentation.common.FullScreen
import com.phamhuu.photographer.presentation.gallery.GalleryScreen
import com.phamhuu.photographer.presentation.gallery.LargeImageScreen

class MainActivity : ComponentActivity() {

    companion object {
        init {
            Utils.init()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FullScreen {
                    val navController = rememberNavController()
                    AppNavHost(navController)
                }
            }
        }
    }
}

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