package com.phamhuu.photographer.domain.usecase

import android.graphics.Bitmap
import android.net.LocalSocketAddress
import android.net.Uri
import com.phamhuu.photographer.data.repository.CameraRepository
import com.phamhuu.photographer.data.repository.GalleryRepository
import java.io.File

class TakePhotoUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): Result<File> {
        return try {
            val photoFile = cameraRepository.createImageFile()
            Result.success(photoFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SavePhotoUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(photoFile: File): Result<Uri?> {
        return try {
            val uri = cameraRepository.saveImageToGallery(photoFile)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class RecordVideoUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(): Result<File> {
        return try {
            val videoFile = cameraRepository.createVideoFile()
            Result.success(videoFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SaveVideoUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(videoFile: File): Result<Uri?> {
        return try {
            val uri = cameraRepository.saveVideoToGallery(videoFile)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class GetFirstGalleryItemUseCase(
    private val galleryRepository: GalleryRepository
) {
    suspend operator fun invoke(): Result<Uri?> {
        return try {
            val uri = galleryRepository.getFirstImageOrVideo()
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AddTextCaptureUseCase(
    private val cameraRepository: CameraRepository
) {
    suspend operator fun invoke(bitmap: Bitmap, address: String): Result<Bitmap> {
        return try {
            Result.success(cameraRepository.addAddressCapture(bitmap, address))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}