package com.example.team_gamma.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// This sealed interface represents the different states our UI can be in.
sealed interface AlertsUiState {
    object Loading : AlertsUiState
    data class Success(val alerts: List<HistoricalAlert>) : AlertsUiState
    data class Error(val message: String) : AlertsUiState
}

@HiltViewModel
class AlertsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val alertsRepository = AlertsRepository()

    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    init {
        fetchAlertsForCurrentLocation()
    }

    fun fetchAlertsForCurrentLocation() {
        _uiState.value = AlertsUiState.Loading

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            Log.w("AlertsViewModel", "Location permission not granted. Falling back to regional default coordinates.")
            fetchAlerts(18.5204, 73.8567)
            return
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d("AlertsViewModel", "Device location obtained: lat=${location.latitude}, lon=${location.longitude}")
                        fetchAlerts(location.latitude, location.longitude)
                    } else {
                        Log.w("AlertsViewModel", "Current location returned null. Attempting lastLocation fallback.")
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                Log.d("AlertsViewModel", "Last location obtained: lat=${lastLocation.latitude}, lon=${lastLocation.longitude}")
                                fetchAlerts(lastLocation.latitude, lastLocation.longitude)
                            } else {
                                Log.w("AlertsViewModel", "Last location also null. Using default regional coordinates.")
                                fetchAlerts(18.5204, 73.8567)
                            }
                        }.addOnFailureListener { e ->
                            Log.e("AlertsViewModel", "Error fetching last location", e)
                            fetchAlerts(18.5204, 73.8567)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AlertsViewModel", "Error fetching current location", e)
                    fetchAlerts(18.5204, 73.8567)
                }
        } catch (e: Exception) {
            Log.e("AlertsViewModel", "Exception retrieving location", e)
            fetchAlerts(18.5204, 73.8567)
        }
    }

    fun fetchAlerts(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _uiState.value = AlertsUiState.Loading
            alertsRepository.getHistoricalAlerts(latitude, longitude)
                .onSuccess { alerts ->
                    _uiState.value = AlertsUiState.Success(alerts)
                }
                .onFailure { error ->
                    _uiState.value = AlertsUiState.Error(error.message ?: "An unknown error occurred")
                }
        }
    }
}
