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
import androidx.navigation.NavController
import com.example.team_gamma.FlaskApi.EvacuationRoute
import com.example.team_gamma.FlaskApi.RiskAssessment
import com.example.team_gamma.data.FloodPredictionViewModel
import com.example.team_gamma.data.PredictionUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvacuationRoutesScreen(
    navController: NavController,
    viewModel: FloodPredictionViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evacuation Plan & Risk Assessment") },
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
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
            is PredictionUiState.Success -> {
                val data = state.prediction
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section 1: Urgent Header
                    item { UrgentHeader(data.riskAssessment?.totalRisk) }

                    // Section 2: Risk Analysis
                    item { ReportSectionHeader(title = "1. Risk Analysis") }
                    item {
                        data.riskAssessment?.let {
                            RiskAnalysisContent(assessment = it)
                        }
                    }

                    // Section 3: Evacuation Shelters
                    item { ReportSectionHeader(title = "2. Available Evacuation Shelters") }
                    item {
                        Text(
                            "There are 3 evacuation shelters available in your area. All shelters are marked as HIGH priority.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Choose the closest shelter based on your current location and safety of the route.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val routes = data.evacuationRoutes?.sortedBy { it.distanceKm }
                    if (!routes.isNullOrEmpty()) {
                        // Show all three shelters in simple text format
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "Available Evacuation Shelters:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Shelter 1
                                Text(
                                    "1. ${routes[0].shelter} - ${routes[0].distanceKm} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                // Shelter 2
                                Text(
                                    "2. ${routes[1].shelter} - ${routes[1].distanceKm} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                // Shelter 3
                                Text(
                                    "3. ${routes[2].shelter} - ${routes[2].distanceKm} km",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "All shelters are HIGH priority",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Section 4: Immediate Actions
                    item { ReportSectionHeader(title = "3. Immediate Actions") }
                    item { ImmediateActionsContent() }

                    // Section 5: Final Warning
                    item { FinalWarning() }
                }
            }
        }
    }
}

@Composable
fun UrgentHeader(totalRisk: Double?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "STATUS: HIGH-RISK ALERT",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))

        totalRisk?.let { risk ->
            Text(
                "Overall Risk: ${(risk * 100).toInt()}%",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        Text(
            "A significant risk has been detected. Please review this report immediately and prepare for possible evacuation.",
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
fun RiskAnalysisContent(assessment: RiskAssessment) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RiskInfoItem(
            label = "Total Risk Score",
            value = "${(assessment.totalRisk * 100).toInt()}%",
            description = "This is the combined threat level, considering geographical and demographic factors.",
            icon = Icons.Default.Warning
        )
        RiskInfoItem(
            label = "Elevation Risk",
            value = "${(assessment.elevationRisk * 100).toInt()}% (Very High)",
            description = "Your location is highly susceptible to flooding. This is the most critical factor.",
            icon = Icons.Default.Landscape
        )
        RiskInfoItem(
            label = "Population Risk",
            value = "${(assessment.populationRisk * 100).toInt()}% (Moderate)",
            description = "The population density in your area contributes moderately to the overall risk.",
            icon = Icons.Default.People
        )
    }
}

@Composable
fun RiskInfoItem(label: String, value: String, description: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.padding(top = 4.dp).size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.Bold)
                Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EvacuationRouteCard(route: EvacuationRoute, index: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (index == 1) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with shelter number and priority
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Shelter $index",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Chip(label = route.priority, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Shelter details
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        route.shelter,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Distance: ${route.distanceKm} km",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Recommendation for closest shelter
                    if (index == 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⭐ Recommended - Closest shelter",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShelterItem(number: Int, name: String, distance: Double, priority: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (number == 1) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shelter Number
            Text(
                "Shelter $number:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(100.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Shelter Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Distance: ${distance} km",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Priority: $priority",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Recommendation for closest shelter
                if (number == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⭐ Recommended - Closest shelter",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Priority Chip
            Chip(label = priority, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ShelterSummary(routes: List<EvacuationRoute>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Shelter Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val closestDistance = routes.minByOrNull { it.distanceKm }?.distanceKm ?: 0.0
            val averageDistance = routes.map { it.distanceKm }.average()

            SummaryItem(
                label = "Total Shelters Available",
                value = "${routes.size} shelters"
            )
            SummaryItem(
                label = "Closest Shelter Distance",
                value = "${String.format("%.2f", closestDistance)} km"
            )
            SummaryItem(
                label = "Average Distance",
                value = "${String.format("%.2f", averageDistance)} km"
            )
            SummaryItem(
                label = "All Shelters Priority",
                value = "HIGH"
            )
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
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
        Text(
            "Recommended Actions:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        ActionItem(text = "Choose the closest shelter (Shelter 1: 0.44 km) for fastest evacuation")
        ActionItem(text = "Have alternative routes planned in case your primary route is blocked")
        ActionItem(text = "Prepare emergency kit with essentials: documents, medicines, water, food")
        ActionItem(text = "Monitor official alerts and evacuate when instructed by authorities")
        ActionItem(text = "Avoid low-lying areas and flooded roads during evacuation")
    }
}

@Composable
fun ActionItem(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun FinalWarning() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "🚨 IMPORTANT",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your safety is the number one priority. Do not wait until it is too late to evacuate.",
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "All three shelters are within 1 km distance. Choose the closest safe route.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Chip(label: String, color: Color) {
    AssistChip(
        onClick = { /* No action */ },
        label = { Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
        colors = AssistChipDefaults.assistChipColors(containerColor = color),
        border = null
    )
}