package com.phamhuu.photographer

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.android.filament.utils.Utils
import com.phamhuu.photographer.di.appModule
import com.phamhuu.photographer.presentation.common.AppScaffold
import com.phamhuu.photographer.presentation.common.FullScreen
import com.phamhuu.photographer.presentation.common.SnackbarManager
import com.phamhuu.photographer.routes.AppNavHost
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class Photographer : Application() {
    override fun onCreate() {
        super.onCreate()

        // Khởi tạo Koin ở đây
        startKoin {
            androidContext(this@Photographer)
            modules(appModule)
        }
    }
}

class MainActivity : ComponentActivity() {

    companion object {
        init {
            Utils.init()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val snackbarHostState = SnackbarHostState()
        SnackbarManager.init(snackbarHostState, lifecycleScope)
        setContent {
            MaterialTheme {
                AppScaffold {
                    FullScreen {
                        val navController = rememberNavController()
                        AppNavHost(navController)
                    }
                }
            }
        }
    }
}