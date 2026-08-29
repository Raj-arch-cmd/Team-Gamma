package com.example.team_gamma.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.team_gamma.data.EvacuationRoute
import com.example.team_gamma.data.FloodPredictionViewModel
import com.example.team_gamma.data.PredictionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvacuationRoutesScreen(
    navController: NavController,
    viewModel: FloodPredictionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchPrediction()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evacuation Plan & Shelters") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is PredictionUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PredictionUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Information Unavailable", style = MaterialTheme.typography.titleLarge)
                }
            }
            is PredictionUiState.Success, is PredictionUiState.Neutral -> {
                val data = (state as? PredictionUiState.Success)?.prediction
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section 1: General Guidance Header
                    item { GuidanceHeader() }

                    // Section 2: Evacuation Shelters
                    item { ReportSectionHeader(title = "1. Recommended Evacuation Shelters") }
                    item {
                        Text(
                            "The following shelters are established as primary evacuation points in your region. Please identify the one closest to your residence.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    val routes = data?.evacuationRoutes ?: emptyList()
                    if (routes.isNotEmpty()) {
                        items(routes) { route ->
                            ShelterCard(route = route)
                        }
                    } else {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "No specific evacuation shelters listed for this area yet. Stay tuned to local official broadcasts for designated safe zones.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Emergency Preparedness
                    item { ReportSectionHeader(title = "2. Immediate Safety Actions") }
                    item { ImmediateActionsContent() }

                    // Section 4: Final Note
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Note: All locations listed are public shelters. In case of localized flooding, follow the highest elevation routes available.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuidanceHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "GENERAL EVACUATION GUIDE",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Be prepared to evacuate if instructed by local authorities. Keep your emergency kit ready.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ReportSectionHeader(title: String) {
    Column {
        Divider(modifier = Modifier.padding(bottom = 12.dp, top = 8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ShelterCard(route: EvacuationRoute) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(route.shelter, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("Priority: ${route.priority}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ImmediateActionsContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionItem(text = "Identify at least two evacuation routes from your home.")
        ActionItem(text = "Pack an emergency bag with water, food, and essential documents.")
        ActionItem(text = "Ensure all family members know the meeting point at the closest shelter.")
        ActionItem(text = "Keep your mobile devices charged and have a backup power source.")
    }
}

@Composable
fun ActionItem(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
