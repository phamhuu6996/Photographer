package com.phamhuu.photographer.data.service

import android.content.Context
import android.graphics.Canvas
import android.net.Uri
import com.phamhuu.photographer.data.renderer.AddTextService
import com.phamhuu.photographer.models.LocationInfo

class VideoRecordingService(private val context: Context) {

    // For now, we'll implement a simple approach that adds location to video metadata
    // Real-time overlay during recording would require more complex video processing
    
    fun addAddressToVideoMetadata(
        videoUri: Uri,
        locationInfo: LocationInfo?
    ): Uri {
        if (locationInfo == null) return videoUri

        // TODO: Implement video metadata writing
        // This is a placeholder - real implementation would use MediaMetadataRetriever
        // to add location information to video file metadata
        
        return videoUri
    }

    /**
     * Renders address overlay to video frame
     * Used for real-time overlay during recording
     */
    fun renderAddressToVideoFrame(
        canvas: Canvas,
        address: String,
        frameWidth: Int,
    ) {
        AddTextService.renderAddressToVideo(
            canvas = canvas,
            address = address,
            frameWidth = frameWidth,
        )
    }

    // Future implementation for real-time overlay during recording
    fun enableAddressOverlayForRecording(enabled: Boolean) {
        // TODO: This would integrate with the camera recording pipeline
        // to draw address text on each frame during recording using renderAddressToVideoFrame()
    }
}
