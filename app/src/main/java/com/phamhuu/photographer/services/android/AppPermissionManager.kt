package com.phamhuu.photographer.services.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Centralized permission management for the app.
 * Single source of truth for all permission definitions and checks.
 */
object AppPermissionManager {
    
    // Permission groups
    val CAMERA_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.CAMERA
    )
    
    val LOCATION_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    val AUDIO_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )
    
    /**
     * Permissions required for app initialization (camera + audio only).
     * Location permission is requested on-demand when user enables location feature.
     */
    val INITIAL_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return hasPermissions(context, CAMERA_PERMISSIONS)
    }
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return hasPermissions(context, LOCATION_PERMISSIONS)
    }
    
    /**
     * Check if audio permission is granted
     */
    fun hasAudioPermission(context: Context): Boolean {
        return hasPermissions(context, AUDIO_PERMISSIONS)
    }
    
    /**
     * Check if all specified permissions are granted
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get camera permissions array
     */
    fun getCameraPermissions(): Array<String> = CAMERA_PERMISSIONS
    
    /**
     * Get location permissions array
     */
    fun getLocationPermissions(): Array<String> = LOCATION_PERMISSIONS
    
    /**
     * Get audio permissions array
     */
    fun getAudioPermissions(): Array<String> = AUDIO_PERMISSIONS
    
    /**
     * Get initial permissions array (camera + audio, no location)
     */
    fun getInitialPermissions(): Array<String> = INITIAL_PERMISSIONS
}
