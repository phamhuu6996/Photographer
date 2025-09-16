package com.phamhuu.photographer.data.model

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val timestamp: Long
)

data class LocationState(
    val locationInfo: LocationInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPermission: Boolean = false
)
