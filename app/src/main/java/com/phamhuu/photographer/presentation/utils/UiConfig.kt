package com.phamhuu.photographer.presentation.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity

object UiConfig {
    fun hideSystemUI(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ sử dụng WindowInsetsController
            val window = (context as ComponentActivity).window
            window.insetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            // API < 30 sử dụng Immersive Mode
            val window = (context as Activity).window
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    fun showSystemUI(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ sử dụng WindowInsetsController
            val window = (context as ComponentActivity).window
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            // API < 30 sử dụng Immersive Mode
            val window = (context as Activity).window
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }
}