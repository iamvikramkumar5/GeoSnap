package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import com.example.model.GPSData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LocationService(private val context: Context) {
    private val TAG = "LocationService"
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null
    
    private val _gpsState = MutableStateFlow(GPSData())
    val gpsState: StateFlow<GPSData> = _gpsState.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timeUpdaterJob: Job? = null

    init {
        startTimeUpdates()
    }

    private fun startTimeUpdates() {
        timeUpdaterJob?.cancel()
        timeUpdaterJob = serviceScope.launch {
            while (true) {
                updateTimeAndDate()
                delay(1000)
            }
        }
    }

    private fun updateTimeAndDate() {
        val calendar = Calendar.getInstance()
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.US)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        val dayOfWeekStr = dayOfWeekFormat.format(calendar.time)
        val dateStr = "$dayOfWeekStr, ${dateFormat.format(calendar.time)}"
        val timeStr = timeFormat.format(calendar.time)

        val tz = calendar.timeZone
        val offsetMillis = tz.getOffset(calendar.timeInMillis)
        val offsetHours = Math.abs(offsetMillis / 3600000)
        val offsetMinutes = Math.abs((offsetMillis % 3600000) / 60000)
        val sign = if (offsetMillis >= 0) "+" else "-"
        val tzStr = String.format("GMT %s%02d:%02d", sign, offsetHours, offsetMinutes)

        _gpsState.value = _gpsState.value.copy(
            formattedDate = dateStr,
            formattedTime = timeStr,
            timezone = tzStr
        )
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        Log.d(TAG, "Starting location updates...")
        
        // Stop any previous updates
        stopLocationUpdates()

        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission missing. Falling back to simulator mode for instant preview.")
            startSimulatorMode()
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L // 3 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000L)
            setWaitForAccurateLocation(false)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                processNewLocation(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                Log.e(TAG, "Failed to start location updates, falling back to simulator", e)
                startSimulatorMode()
            }
            
            // Immediately request last known location to load faster
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    processNewLocation(location)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates, using simulator", e)
            startSimulatorMode()
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private var mockJob: Job? = null
    fun startSimulatorMode() {
        Log.d(TAG, "Entering GPS Simulator Mode...")
        mockJob?.cancel()
        
        // Let's cycle through a few beautiful premium scenic coordinates
        val demoLocations = listOf(
            Triple(37.7749, -122.4194, "San Francisco, CA"),
            Triple(48.8566, 2.3522, "Paris, France"),
            Triple(35.6762, 139.6503, "Tokyo, Japan"),
            Triple(-33.8688, 151.2093, "Sydney, Australia"),
            Triple(12.9716, 77.5946, "Bengaluru, India")
        )
        
        var locationIdx = 0

        mockJob = serviceScope.launch {
            while (true) {
                val demo = demoLocations[locationIdx % demoLocations.size]
                val loc = Location("simulator").apply {
                    latitude = demo.first
                    longitude = demo.second
                    altitude = 120.0 + (10 * (locationIdx % 5))
                    speed = 2.5f * (locationIdx % 3)
                    time = System.currentTimeMillis()
                }
                processNewLocation(loc, isMocked = true)
                // Cycle location every 30 seconds
                delay(15000)
                locationIdx++
            }
        }
    }

    private fun processNewLocation(location: Location, isMocked: Boolean = false) {
        val lat = location.latitude
        val lng = location.longitude
        val alt = location.altitude
        val spd = location.speed

        serviceScope.launch {
            val details = getAddressFromCoords(lat, lng)
            val flag = getCountryFlagEmoji(details.countryCode)

            _gpsState.value = _gpsState.value.copy(
                latitude = lat,
                longitude = lng,
                altitude = alt,
                speed = spd,
                address = details.fullAddress,
                city = details.city,
                state = details.state,
                country = if (details.country.isNotEmpty()) details.country else "Planet Earth",
                countryCode = details.countryCode,
                flagEmoji = flag,
                isMocked = isMocked,
                shortAddress = details.shortAddress
            )
        }
    }

    private suspend fun getAddressFromCoords(lat: Double, lng: Double): AddressDetails =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val fullAddress = addr.getAddressLine(0) ?: "Latitude: ${String.format("%.4f", lat)}, Longitude: ${String.format("%.4f", lng)}"
                    val city = addr.locality ?: addr.subLocality ?: addr.subAdminArea ?: ""
                    val state = addr.adminArea ?: ""
                    val country = addr.countryName ?: ""
                    val countryCode = addr.countryCode ?: ""
                    
                    val parts = listOfNotNull(
                        city.ifEmpty { null },
                        state.ifEmpty { null },
                        country.ifEmpty { null }
                    )
                    val shortAddress = parts.joinToString(", ")
                    
                    AddressDetails(fullAddress, city, state, country, countryCode, shortAddress)
                } else {
                    val fallbackShort = "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
                    AddressDetails(
                        "Unknown Address near Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}",
                        "", "", "", "", fallbackShort
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoder exception", e)
                val fallbackShort = "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
                AddressDetails(
                    "Coordinates lookup unavailable. Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}",
                    "", "", "", "", fallbackShort
                )
            }
        }

    private fun getCountryFlagEmoji(countryCode: String?): String {
        if (countryCode == null || countryCode.length != 2) return "📍"
        try {
            val firstChar = Character.codePointAt(countryCode.uppercase(), 0) - 0x41 + 0x1F1E6
            val secondChar = Character.codePointAt(countryCode.uppercase(), 1) - 0x41 + 0x1F1E6
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        } catch (e: Exception) {
            return "📍"
        }
    }
}

data class AddressDetails(
    val fullAddress: String,
    val city: String,
    val state: String,
    val country: String,
    val countryCode: String,
    val shortAddress: String
)
