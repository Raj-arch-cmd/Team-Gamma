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

                    // Section 3: Risk Interpretation
                    item { ReportSectionHeader(title = "2. Risk Interpretation") }
                    item {
                        data.riskAssessment?.let {
                            RiskInterpretationContent(assessment = it)
                        }
                    }

                    // Section 4: Recommended Shelters
                    item { ReportSectionHeader(title = "3. Recommended Evacuation Shelters") }
                    item {
                        Text(
                            "The following high-priority shelters have been identified. Proceed to the nearest available shelter as soon as it is safe to do so.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    val routes = data.evacuationRoutes?.sortedBy { it.distanceKm }
                    if (!routes.isNullOrEmpty()) {
                        val closestShelter = routes.first()
                        item { PrimaryShelterCard(route = closestShelter) }

                        val otherShelters = routes.drop(1)
                        if (otherShelters.isNotEmpty()) {
                            item {
                                Text(
                                    "Secondary Recommendations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(otherShelters) { route ->
                                EvacuationRouteCard(route = route)
                            }
                        }

                        // NEW: Shelter Statistics
                        item { ShelterStatistics(routes = routes) }
                    } else {
                        item { Text("No evacuation routes available at the moment.") }
                    }

                    // Section 5: Immediate Actions
                    item { ReportSectionHeader(title = "4. Immediate Actions") }
                    item { ImmediateActionsContent(data.riskAssessment) }

                    // Section 6: Final Warning
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

        // Display actual risk percentage from JSON
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
        // Enhanced with exact percentages from JSON
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

        // NEW: Detailed risk breakdown
        DetailedRiskBreakdown(assessment)
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

// NEW: Detailed risk breakdown
@Composable
fun DetailedRiskBreakdown(assessment: RiskAssessment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Detailed Risk Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Elevation Risk Detail
            RiskDetailItem(
                riskName = "Elevation Risk",
                percentage = (assessment.elevationRisk * 100).toInt(),
                explanation = "Based on your location's altitude and proximity to water bodies"
            )

            // Population Risk Detail
            RiskDetailItem(
                riskName = "Population Risk",
                percentage = (assessment.populationRisk * 100).toInt(),
                explanation = "Based on local population density and infrastructure"
            )

            // Combined Risk Detail
            RiskDetailItem(
                riskName = "Combined Risk Score",
                percentage = (assessment.totalRisk * 100).toInt(),
                explanation = "Overall assessment combining all risk factors"
            )
        }
    }
}

@Composable
fun RiskDetailItem(riskName: String, percentage: Int, explanation: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(riskName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("$percentage%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// NEW: Risk Interpretation
@Composable
fun RiskInterpretationContent(assessment: RiskAssessment) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "What Your Risk Levels Mean:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Elevation risk interpretation
        InterpretationItem(
            title = "Elevation Risk: ${(assessment.elevationRisk * 100).toInt()}%",
            description = "Your location has very high flood susceptibility. Immediate preparation is advised."
        )

        // Population risk interpretation
        InterpretationItem(
            title = "Population Risk: ${(assessment.populationRisk * 100).toInt()}%",
            description = "Moderate population density may affect evacuation timing and route availability."
        )

        // Total risk interpretation
        InterpretationItem(
            title = "Overall Risk: ${(assessment.totalRisk * 100).toInt()}%",
            description = "High combined risk level requires immediate attention and preparedness."
        )
    }
}

@Composable
fun InterpretationItem(title: String, description: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// NEW: Shelter Statistics
@Composable
fun ShelterStatistics(routes: List<EvacuationRoute>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Shelter Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val closestDistance = routes.minByOrNull { it.distanceKm }?.distanceKm ?: 0.0
            val averageDistance = routes.map { it.distanceKm }.average()
            val highPriorityCount = routes.count { it.priority == "HIGH" }

            StatItem(
                label = "Closest Shelter Distance",
                value = "${String.format("%.2f", closestDistance)} km"
            )
            StatItem(
                label = "Average Shelter Distance",
                value = "${String.format("%.2f", averageDistance)} km"
            )
            StatItem(
                label = "Available Shelters",
                value = "${routes.size} locations"
            )
            StatItem(
                label = "High Priority Shelters",
                value = "$highPriorityCount shelters"
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrimaryShelterCard(route: EvacuationRoute) {
    Column {
        Text(
            "Primary Recommendation (Closest)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, "Primary Shelter", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(route.shelter, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("Distance: ${route.distanceKm} km", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Priority: ${route.priority}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Chip(label = route.priority, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EvacuationRouteCard(route: EvacuationRoute) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shield, "Shelter", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(route.shelter, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("Distance: ${route.distanceKm} km", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Priority: ${route.priority}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            Chip(label = route.priority, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ImmediateActionsContent(riskAssessment: RiskAssessment?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionItem(text = "Prepare to Evacuate: Gather your emergency kit and important documents.")
        ActionItem(text = "Stay Informed: Monitor local news and official alerts for evacuation orders.")
        ActionItem(text = "Plan Your Route: Identify the safest path to your chosen shelter, avoiding low-lying roads or areas already affected by water.")

        // Enhanced actions based on actual risk data
        riskAssessment?.let {
            if (it.elevationRisk > 0.8) {
                ActionItem(text = "⚠️ Critical Elevation Risk: Due to very high elevation risk (${(it.elevationRisk * 100).toInt()}%), consider immediate evacuation preparation.")
            }
            if (it.totalRisk > 0.4) {
                ActionItem(text = "🚨 High Overall Risk: With ${(it.totalRisk * 100).toInt()}% overall risk, be prepared to evacuate as soon as official orders are issued.")
            }
        }
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
    Text(
        "Your safety is the number one priority. Do not wait until it is too late.",
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    )
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