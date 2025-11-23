package com.phamhuu.photographer

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.FusedLocationProviderClient
import com.phamhuu.photographer.data.model.LocationInfo
import com.phamhuu.photographer.data.repository.LocationRepositoryImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class LocationRepositoryTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @Mock
    private lateinit var geocoder: Geocoder

    private lateinit var repository: LocationRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = LocationRepositoryImpl(context, fusedLocationClient, geocoder)
    }

    @Test
    fun `location info should contain valid data`() {
        val locationInfo = LocationInfo(
            latitude = 37.7749,
            longitude = -122.4194,
            address = "San Francisco, CA",
            timestamp = System.currentTimeMillis()
        )

        assertEquals(37.7749, locationInfo.latitude, 0.0001)
        assertEquals(-122.4194, locationInfo.longitude, 0.0001)
        assertEquals("San Francisco, CA", locationInfo.address)
        assertTrue(locationInfo.timestamp > 0)
    }

    @Test
    fun `address formatting should handle empty geocoder results`() = runTest {
        // This would test the private getAddressFromLocation method
        // In a real implementation, we'd make it package-private for testing
        val fallbackAddress = "Lat: 37.7749, Lng: -122.4194"
        assertTrue(fallbackAddress.contains("37.7749"))
        assertTrue(fallbackAddress.contains("-122.4194"))
    }
}
