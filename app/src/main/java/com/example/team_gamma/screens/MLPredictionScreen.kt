package com.example.team_gamma.screens


import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.team_gamma.data.MLPredictionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MLPredictionScreen(
    viewModel: MLPredictionViewModel = MLPredictionViewModel() // Direct instantiation
) {
    val predictionState by viewModel.predictionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Disaster Prediction AI") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val state = predictionState) {
                is MLPredictionViewModel.PredictionState.Idle -> {
                    Text("Ready to analyze disaster risk")
                    Button(onClick = {
                        val sensorData = listOf(6.5, 120.0, 45.0, 0.8)
                        viewModel.predictDisaster(sensorData)
                    }) {
                        Text("Run Prediction")
                    }
                }
                is MLPredictionViewModel.PredictionState.Loading -> {
                    CircularProgressIndicator()
                    Text("Analyzing data...")
                }
                is MLPredictionViewModel.PredictionState.Success -> {
                    val result = state.result
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.isDisaster) Color(0xFFFFE5E5) else Color(0xFFE5FFE5)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (result.isDisaster) "🚨 DISASTER DETECTED" else "✅ ALL CLEAR",
                                fontWeight = FontWeight.Bold,
                                color = if (result.isDisaster) Color.Red else Color.Green
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Type: ${result.type}")
                            Text("Confidence: ${(result.confidence * 100).toInt()}%")
                            Text("Severity: ${result.severity}")

                            if (result.isDisaster) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Recommended Actions:", fontWeight = FontWeight.Bold)
                                result.recommendedActions.forEach { action ->
                                    Text("• $action")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetState() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Analyze Again")
                            }
                        }
                    }
                }
                is MLPredictionViewModel.PredictionState.Error -> {
                    Text("Error: ${state.message}")
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}