package com.phamhuu.photographer.presentation.common

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.phamhuu.photographer.presentation.utils.Permission

@Composable
fun InitCameraPermission(callback: () -> Unit, context: Context) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            callback()
        } else {
            Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show()
            Permission.openSettings(context)
        }
    }

    val permissions: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    LaunchedEffect(Unit) {
        if (!Permission.hasPermissions(context, permissions)) {
            permissionLauncher.launch(permissions)
        } else {
            callback()
        }
    }
}