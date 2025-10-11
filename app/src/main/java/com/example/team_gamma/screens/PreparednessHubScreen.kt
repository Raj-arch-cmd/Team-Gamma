package com.example.team_gamma.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.resqtech.ui.theme.screens.GoogleMapScreen
import com.example.team_gamma.data.FloodPredictionViewModel
import com.example.team_gamma.data.HubViewModel
import com.example.team_gamma.data.PredictionUiState

data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreparednessHubScreen(
    navController: NavController,
    hubViewModel: HubViewModel,
    // The new ViewModel is now a required parameter
    floodPredictionViewModel: FloodPredictionViewModel
) {
    val quickActions = listOf(
        QuickAction(Icons.Default.Contacts, "Contacts", "manage_contacts"),
        QuickAction(Icons.Default.VolumeUp, "Loud Alarm", "loud_alarm"),
        QuickAction(Icons.Default.ListAlt, "Do's & Don'ts", "dos_and_donts"),
        QuickAction(Icons.Default.Chat, "AI Assistant", "ai_assistant"),
        QuickAction(Icons.Default.Route, "Safe Route", "map"),
        QuickAction(Icons.Default.Info, "Information", "information"),
    )

    // --- API DATA FETCHING ---
    // This effect runs once when the screen is first shown
    LaunchedEffect(key1 = true) {
        val userLatitude = 26.2183
        val userLongitude = 78.1828
        floodPredictionViewModel.fetchPrediction()
    }
    // Collect the state from the ViewModel
    val uiState by floodPredictionViewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()) // Allow the whole screen to scroll
        ) {
            // --- Top Map Section (Your original code) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                GoogleMapScreen()
            }

            // --- DYNAMIC STATUS/ALERT SECTION (This part is new) ---
            // It replaces your old StatusCard/EmergencyAlertCard logic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(), // Smoothly animates size changes
                contentAlignment = Alignment.Center
            ) {
                // Inside PreparednessHubScreen.kt, find the when(state) block and replace it.

                when (val state = uiState) {
                    is PredictionUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is PredictionUiState.Error -> {
                        PredictionAlertCard(
                            title = "Connection Error",
                            message = state.message,
                            cardColor = MaterialTheme.colorScheme.errorContainer
                        )
                    }
                    is PredictionUiState.Success -> {
                        val prediction = state.prediction
                        // We now check the "status" field from our new data class
                        when (prediction.status) {
                            "CLEAR" -> PredictionStatusCard(
                                title = "All Clear",
                                // We create our own message
                                message = "No immediate risk detected in your area. Stay prepared.",
                                icon = Icons.Default.CheckCircle,
                                iconTint = Color(0xFF00C853)
                            )
                            "RAINFALL_STARTED" -> PredictionStatusCard(
                                title = "Rainfall Started",
                                // We format our own message using the riskPercentage
                                message = "Risk: ${prediction.riskPercentage?.times(100)?.toInt() ?: "N/A"}%",
                                icon = Icons.Default.Cloud,
                                iconTint = Color.Gray
                            )
                            "HIGH_RISK" -> PredictionAlertCard(
                                title = "HIGH RISK ALERT",
                                // We create our own message
                                message = "Water levels may be rising. Evacuation could be necessary.",
                                cardColor = MaterialTheme.colorScheme.error,
                                // The evacuationMapUrl field now matches the server's response
                                evacuationMapUrl = prediction.evacuationMapUrl
                            )
                            else -> {
                                // A fallback for any unexpected status from the server
                                PredictionStatusCard(
                                    title = "Unknown Status",
                                    message = "Received an unrecognized status: ${prediction.status}",
                                    icon = Icons.Default.Help,
                                    iconTint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }


            // --- Quick Actions Section (Your original code) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().height(250.dp), // Adjust height as needed
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    userScrollEnabled = false
                ) {
                    items(quickActions) { action ->
                        QuickActionItem(
                            action = action,
                            onClick = { navController.navigate(action.route) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB
            }
        }
    }
}


// --- REUSABLE PREDICTION CARDS (NEW) ---

@Composable
fun PredictionStatusCard(
    title: String,
    message: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PredictionAlertCard(
    title: String,
    message: String,
    cardColor: Color,
    modifier: Modifier = Modifier,
    evacuationMapUrl: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
            }
            evacuationMapUrl?.let { url ->
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = url,
                    contentDescription = "Evacuation Map",
                    modifier = Modifier.fillMaxWidth().height(150.dp).padding(top = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}


// --- YOUR ORIGINAL QUICK ACTION ITEM (UNCHANGED) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionItem(action: QuickAction, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(action.icon, contentDescription = action.label, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = action.label, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
    }
}