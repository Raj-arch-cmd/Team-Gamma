package com.example.team_gamma.screens

// Core Android / Compose
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable

// Google Maps Compose
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState

// Google Maps / Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.tasks.CancellationTokenSource


@SuppressLint("MissingPermission")
@Composable
fun GoogleMapScreen() {
    val context = LocalContext.current

    // Permission state
    var hasLocationPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    // Request permission once on screen launch if not already granted
    LaunchedEffect(key1 = Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Default location (New Delhi) - used until current location is found
    val defaultLatLng = LatLng(28.6139, 77.2090)

    // Camera state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 12f)
    }

    // Fused location client and coroutine scope
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val coroutineScope = rememberCoroutineScope()

    // When permission is granted, fetch the CURRENT location
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            // This is a more robust way to get the current location
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)
                        coroutineScope.launch {
                            // Animate camera to the user's current location
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f)
                            )
                        }
                    }
                }
        }
    }

    // UI: Map + overlay prompt if permission is not granted
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp) // As defined in your PreparednessHubScreen
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
        )
        // No need for a marker here unless you want to add specific points of interest

        if (!hasLocationPermission) {
            // Show a translucent overlay to prompt the user for permission
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)), // Darker, semi-transparent overlay
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Please grant location permission to see your current location on the map.",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
