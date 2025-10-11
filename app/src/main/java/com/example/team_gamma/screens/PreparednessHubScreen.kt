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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.resqtech.ui.theme.screens.GoogleMapScreen
import com.example.team_gamma.data.HubViewModel
import com.example.team_gamma.data.ManualAlertState
import com.example.team_gamma.data.ManualAlertViewModel

data class HubQuickAction(
    val icon: ImageVector,
    val label: String,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreparednessHubScreen(
    navController: NavController,
    hubViewModel: HubViewModel,
    manualAlertViewModel: ManualAlertViewModel
) {
    val quickActions = listOf(
        HubQuickAction(Icons.Default.Contacts, "Contacts", "manage_contacts"),
        HubQuickAction(Icons.Default.VolumeUp, "Loud Alarm", "loud_alarm"),
        HubQuickAction(Icons.Default.ListAlt, "Do's & Don'ts", "dos_and_donts"),
        HubQuickAction(Icons.Default.Chat, "AI Assistant", "ai_assistant"),
        // 👇 UPDATED THIS LINE
        HubQuickAction(Icons.Default.LocalHospital, "Nearest Hospital", "nearest_hospital"),
        HubQuickAction(Icons.Default.Info, "Information", "information"),
    )
    // Collect the state from the manual ViewModel
    val currentAlertState by manualAlertViewModel.uiState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Top Map Section ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                GoogleMapScreen()
            }

            // --- MANUAL ALERT STATUS SECTION ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .animateContentSize(),
                contentAlignment = Alignment.Center
            ) {
                ManualAlertStatusCard(
                    currentAlertState = currentAlertState,
                    manualAlertViewModel = manualAlertViewModel
                )
            }

            // --- Quick Actions Section ---
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
                    modifier = Modifier.fillMaxWidth().height(250.dp),
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
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ManualAlertStatusCard(
    currentAlertState: ManualAlertState,
    manualAlertViewModel: ManualAlertViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = currentAlertState.color)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = currentAlertState.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentAlertState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )

            // Demo controls for testing
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { manualAlertViewModel.setNoRisk() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("All Clear", color = Color.Black)
                }
                Button(
                    onClick = { manualAlertViewModel.setHighRisk() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("High Risk", color = Color.Black)
                }
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