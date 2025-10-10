package com.example.team_gamma.screens

import com.example.resqtech.ui.theme.screens.GoogleMapScreen


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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.resqtech.data.EmergencyAlert
import com.example.resqtech.data.HubViewModel
import com.example.resqtech.ui.theme.ResQTechTheme
import kotlinx.coroutines.launch

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
    showSnackbarOnEntry: Boolean = false
) {
    val quickActions = listOf(
        QuickAction(Icons.Default.Contacts, "Contacts", "manage_contacts"),
        QuickAction(Icons.Default.VolumeUp, "Loud Alarm", "loud_alarm"),
        QuickAction(Icons.Default.ListAlt, "Do's & Don'ts", "dos_and_donts"),
        QuickAction(Icons.Default.Chat, "AI Assistant", "ai_assistant"),
        QuickAction(Icons.Default.Route, "Safe Route", "map"),
        QuickAction(Icons.Default.Info, "Information", "information"),
        QuickAction(Icons.Default.Warning, "AI Prediction", "ml_prediction")
    )

    val snackbarHostState = remember { SnackbarHostState() }
    var hasShownSnackbar by rememberSaveable { mutableStateOf(false) }

    val activeAlert by hubViewModel.activeAlert.collectAsState()

    LaunchedEffect(key1 = showSnackbarOnEntry, key2 = hasShownSnackbar) {
        if (showSnackbarOnEntry && !hasShownSnackbar) {
            snackbarHostState.showSnackbar("Contacts saved successfully!")
            hasShownSnackbar = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Top Map Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), // You can adjust this height as needed
                contentAlignment = Alignment.Center
            ) {
                GoogleMapScreen()
            }

            // --- DYNAMIC STATUS/ALERT SECTION ---
            // The card is now placed BELOW the map's Box container.
            if (activeAlert == null) {
                StatusCard(modifier = Modifier.padding(16.dp))
            } else {
                EmergencyAlertCard(alert = activeAlert!!, modifier = Modifier.padding(16.dp))
            }

            // --- Quick Actions Section ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp)) // Reduced space as card is now here
                Text(
                    "Quick Actions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                val rowCount = (quickActions.size + 2) / 3
                val gridHeight = (72.dp + 48.dp) * rowCount
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
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

                // Temporary button for testing the crisis mode
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    if (activeAlert == null) {
                        hubViewModel.triggerTestAlert()
                    } else {
                        hubViewModel.clearAlert()
                    }
                }) {
                    Text(if (activeAlert == null) "Simulate Emergency Alert" else "Clear Alert")
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun EmergencyAlertCard(alert: EmergencyAlert, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = alert.severity.color)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Emergency Alert",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${alert.locationName}: ${alert.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun StatusCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "All Clear",
                tint = Color(0xFF00C853),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "All Clear",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "No alerts in your area. Stay prepared!",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionItem(action: QuickAction, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(action.icon, contentDescription = action.label, modifier = Modifier.size(32.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = action.label,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

