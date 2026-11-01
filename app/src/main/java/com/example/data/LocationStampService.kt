package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ProvenanceStamp(
    val locationStamp: String,
    val dateStamp: String,
    val timeStamp: String,
    val isGpsResolved: Boolean,
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194
) {
    fun toTimestampInfo(): TimestampInfo = TimestampInfo(
        location = locationStamp,
        date = dateStamp,
        time = timeStamp,
        capturedAt = "$dateStamp at $timeStamp"
    )
}

class LocationStampService(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)
) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun currentDateAndTimeStamps(now: Date = Date()): Pair<String, String> {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a z", Locale.getDefault())
        return dateFormat.format(now) to timeFormat.format(now)
    }

    fun currentDayKey(now: Date = Date()): String {
        val dayFormat = SimpleDateFormat("yyyy_MM_dd", Locale.US)
        return dayFormat.format(now)
    }

    suspend fun captureTimestampInfo(customLocationOverride: String? = null): TimestampInfo {
        return captureProvenanceStamp(customLocationOverride).toTimestampInfo()
    }

    @SuppressLint("MissingPermission")
    suspend fun captureProvenanceStamp(customLocationOverride: String? = null): ProvenanceStamp =
        withContext(Dispatchers.IO) {
            val (dateStr, timeStr) = currentDateAndTimeStamps()

            if (!customLocationOverride.isNullOrBlank()) {
                return@withContext ProvenanceStamp(
                    locationStamp = customLocationOverride.trim(),
                    dateStamp = dateStr,
                    timeStamp = timeStr,
                    isGpsResolved = false
                )
            }

            if (!hasLocationPermission()) {
                return@withContext ProvenanceStamp(
                    locationStamp = "Personal Reading Desk (Location Off)",
                    dateStamp = dateStr,
                    timeStamp = timeStr,
                    isGpsResolved = false
                )
            }

            try {
                val location = withTimeoutOrNull(3500L) {
                    fusedLocationClient.lastLocation.await()
                } ?: withTimeoutOrNull(4500L) {
                    val tokenSource = CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        tokenSource.token
                    ).await()
                }

                if (location != null) {
                    val placeLabel = resolveGeocoderPlace(location.latitude, location.longitude)
                    ProvenanceStamp(
                        locationStamp = placeLabel,
                        dateStamp = dateStr,
                        timeStamp = timeStr,
                        isGpsResolved = true,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    ProvenanceStamp(
                        locationStamp = "Local Study • GPS Standby",
                        dateStamp = dateStr,
                        timeStamp = timeStr,
                        isGpsResolved = false
                    )
                }
            } catch (e: Exception) {
                ProvenanceStamp(
                    locationStamp = "Personal Study Archive",
                    dateStamp = dateStr,
                    timeStamp = timeStr,
                    isGpsResolved = false
                )
            }
        }

    @Suppress("DEPRECATION")
    private fun resolveGeocoderPlace(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                val city = addr.locality ?: addr.subAdminArea ?: addr.featureName
                val region = addr.adminArea ?: addr.countryName
                when {
                    !city.isNullOrBlank() && !region.isNullOrBlank() -> "$city, $region"
                    !city.isNullOrBlank() -> city
                    !region.isNullOrBlank() -> region
                    else -> String.format(Locale.US, "%.3f° N, %.3f° E", lat, lon)
                }
            } else {
                String.format(Locale.US, "%.3f°, %.3f°", lat, lon)
            }
        } catch (e: Exception) {
            String.format(Locale.US, "%.3f°, %.3f°", lat, lon)
        }
    }
}
