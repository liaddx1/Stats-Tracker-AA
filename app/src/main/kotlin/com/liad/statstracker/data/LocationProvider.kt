package com.liad.statstracker.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class LocationProvider private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _available = MutableStateFlow(isSystemLocationEnabled())
    val available: StateFlow<Boolean> = _available.asStateFlow()

    private var unavailableJob: Job? = null

    init {
        val filter = IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        appContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (!isSystemLocationEnabled()) {
                    unavailableJob?.cancel()
                    _available.value = false
                } else {
                    _available.value = true
                }
            }
        }, filter)
    }

    private fun isSystemLocationEnabled(): Boolean = try {
        locationManager.isLocationEnabled
    } catch (_: Exception) {
        true
    }

    @SuppressLint("MissingPermission")
    val location: SharedFlow<Location> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    unavailableJob?.cancel()
                    if (isSystemLocationEnabled()) _available.value = true
                    trySend(it)
                }
            }

            override fun onLocationAvailability(la: LocationAvailability) {
                if (la.isLocationAvailable) {
                    unavailableJob?.cancel()
                    if (isSystemLocationEnabled()) _available.value = true
                } else if (!isSystemLocationEnabled()) {
                    unavailableJob?.cancel()
                    _available.value = false
                } else if (unavailableJob?.isActive != true) {
                    unavailableJob = scope.launch {
                        delay(UNAVAILABLE_GRACE_MS)
                        if (!isSystemLocationEnabled()) _available.value = false
                    }
                }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setMinUpdateIntervalMillis(200L)
            .setWaitForAccurateLocation(false)
            .build()
        val started = try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            true
        } catch (_: SecurityException) {
            _available.value = false
            false
        }
        awaitClose { if (started) client.removeLocationUpdates(callback) }
    }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    companion object {
        private const val UNAVAILABLE_GRACE_MS = 8_000L

        @Volatile
        private var instance: LocationProvider? = null

        fun get(context: Context): LocationProvider =
            instance ?: synchronized(this) {
                instance ?: LocationProvider(context).also { instance = it }
            }
    }
}
