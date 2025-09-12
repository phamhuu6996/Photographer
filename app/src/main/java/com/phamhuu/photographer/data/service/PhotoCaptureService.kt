package com.phamhuu.photographer.data.service

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import com.phamhuu.photographer.models.LocationInfo
import com.phamhuu.photographer.data.renderer.AddTextService
import java.io.IOException

class PhotoCaptureService(private val context: Context) {

    fun addAddressToBitmap(
        bitmap: Bitmap,
        locationInfo: LocationInfo
    ): Bitmap {
        return addTextOverlay(bitmap, locationInfo.address)
    }

    private fun addTextOverlay(
        bitmap: Bitmap,
        address: String
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Use AddTextService for photo capture
        AddTextService.renderAddressToPhoto(
            canvas = canvas,
            address = address,
            bitmapWidth = bitmap.width,
        )

        return mutableBitmap
    }



}
