package com.phamhuu.photographer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.phamhuu.photographer.presentation.camera.CameraScreen
import com.phamhuu.photographer.presentation.common.FullScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FullScreen {
                    CameraScreen()
                }
            }
        }
    }
}