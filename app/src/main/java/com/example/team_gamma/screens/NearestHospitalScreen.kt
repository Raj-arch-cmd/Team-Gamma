package com.example.team_gamma.screens


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@SuppressLint("MissingPermission")
@Composable
fun NearestHospitalScreen(navController: NavController) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                // Permission granted, now find location
                findAndShowHospitals(context, navController)
            } else {
                // Permission denied, inform user and go back
                Toast.makeText(context, "Location permission is required to find hospitals.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
    )

    // This effect runs when the screen is first composed
    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Permission already granted
                findAndShowHospitals(context, navController)
            }
            else -> {
                // Request permission
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Show a loading indicator while we work
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Finding nearest hospitals...")
    }
}

/**
 * Gets the current location and launches a Google Maps intent.
 */
@SuppressLint("MissingPermission")
private fun findAndShowHospitals(context: Context, navController: NavController) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Get the current location with high accuracy
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { location ->
            if (location != null) {
                val userLat = location.latitude
                val userLng = location.longitude

                // Create a URI for a Google Maps search query.
                // "geo:lat,lng?q=query" searches for 'query' near 'lat,lng'.
                val gmmIntentUri = Uri.parse("geo:$userLat,$userLng?q=hospital")

                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps")

                // Attempt to start an activity that can handle the Intent
                context.startActivity(mapIntent)

                // Go back to the previous screen
                navController.popBackStack()

            } else {
                Toast.makeText(context, "Could not get current location. Please enable GPS.", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        }
        .addOnFailureListener {
            Toast.makeText(context, "Failed to get location: ${it.message}", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
}