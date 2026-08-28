package com.example.team_gamma.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.team_gamma.screens.GoogleMapScreen
import com.example.team_gamma.data.FloodPredictionViewModel
import com.example.team_gamma.data.HubViewModel

data class HubQuickAction(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreparednessHubScreen(
    navController: NavController,
    hubViewModel: HubViewModel = hiltViewModel(),
    floodPredictionViewModel: FloodPredictionViewModel = hiltViewModel()
) {
    val quickActions = listOf(
        HubQuickAction(Icons.Default.Contacts, "Contacts", "manage_contacts"),
        HubQuickAction(Icons.Default.VolumeUp, "Loud Alarm", "loud_alarm"),
        HubQuickAction(Icons.Default.ListAlt, "Do's & Don'ts", "dos_and_donts"),
        HubQuickAction(Icons.Default.Chat, "AI Assistant", "ai_assistant"),
        HubQuickAction(Icons.Default.Route, "Evacuation", "evacuation_routes"),
        HubQuickAction(Icons.Default.Info, "Information", "information")
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- MAP SECTION ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                GoogleMapScreen()
            }

            // --- NEUTRAL STATUS SECTION ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                PredictionStatusCard(
                    title = "Live Risk Data Unavailable",
                    message = "Stay prepared and monitor official local news for real-time updates.",
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            // --- Quick Actions Section ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // First row of actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        quickActions.take(3).forEach { action ->
                            QuickActionItem(
                                action = action,
                                onClick = { navController.navigate(action.route) }
                            )
                        }
                    }
                    // Second row of actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        quickActions.drop(3).forEach { action ->
                            QuickActionItem(
                                action = action,
                                onClick = { navController.navigate(action.route) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                
                // Guidance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Safety Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "In case of an immediate emergency, use the SOS button below to alert your saved contacts with your location.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp)) // Padding for FAB
            }
        }
    }
}

@Composable
fun PredictionStatusCard(title: String, message: String, icon: ImageVector, iconTint: Color) {
    Card(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionItem(action: HubQuickAction, onClick: () -> Unit) {
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
