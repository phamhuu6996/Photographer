package com.phamhuu.photographer.presentation.common

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.phamhuu.photographer.R
import com.phamhuu.photographer.services.android.AppPermissionManager
import com.phamhuu.photographer.services.android.PermissionService

@Composable
fun InitCameraPermission(callback: () -> Unit, context: Context) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            callback()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.permissions_not_granted),
                Toast.LENGTH_SHORT
            ).show()
            PermissionService.openSettings(context)
        }
    }

    val permissions = AppPermissionManager.getInitialPermissions()

    LaunchedEffect(Unit) {
        if (!AppPermissionManager.hasPermissions(context, permissions)) {
            permissionLauncher.launch(permissions)
        } else {
            callback()
        }
    }
}