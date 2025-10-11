package com.example.team_gamma.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.team_gamma.R

@Composable
fun EvacuationMapView() {
    Image(
        // This line directly looks for the file in res/drawable
        painter = painterResource(id = R.drawable.evacuation_map),
        contentDescription = "Evacuation Map",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}