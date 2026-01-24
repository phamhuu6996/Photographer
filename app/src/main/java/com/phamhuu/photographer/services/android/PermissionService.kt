package com.phamhuu.photographer.services.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object PermissionService {
    /**
     * Check if all specified permissions are granted.
     * Delegates to AppPermissionManager for consistency.
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return AppPermissionManager.hasPermissions(context, permissions)
    }

    /**
     * Open app settings screen for user to manually grant permissions
     */
    fun openSettings(context: Context) {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(this)
        }
    }
}
