package com.phamhuu.photographer.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Looper
import com.google.android.gms.location.*
import com.phamhuu.photographer.data.model.LocationInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class LocationRepositoryImpl(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val geocoder: Geocoder
) : LocationRepository {

    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(): Flow<LocationInfo?> = callbackFlow {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val address = getAddressFromLocation(location.latitude, location.longitude)
                    val locationInfo = LocationInfo(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address,
                        timestamp = System.currentTimeMillis()
                    )
                    trySend(locationInfo)
                }
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        awaitClose {
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): LocationInfo? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val address = getAddressFromLocation(location.latitude, location.longitude)
                    val locationInfo = LocationInfo(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address,
                        timestamp = System.currentTimeMillis()
                    )
                    continuation.resume(locationInfo)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    override fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildString {
                    address.getAddressLine(0)?.let { append(it) }
                }
            } else {
                "Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}"
            }
        } catch (e: Exception) {
            "Lat: ${String.format("%.4f", latitude)}, Lng: ${String.format("%.4f", longitude)}"
        }
    }
}
