package com.phamhuu.photographer.data.repository

import com.phamhuu.photographer.models.LocationInfo
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getCurrentLocation(): Flow<LocationInfo?>
    suspend fun getLastKnownLocation(): LocationInfo?
    fun stopLocationUpdates()
}
