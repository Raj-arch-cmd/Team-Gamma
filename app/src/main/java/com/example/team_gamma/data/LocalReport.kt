package com.example.team_gamma.data


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

// This defines the structure for a single community-submitted report.
// In the future, this will come from your Firebase Firestore database.
data class LocalReport(
    val id: String,
    val category: ReportCategory,
    val description: String,
    val locationName: String,
    val timestamp: String,
    val imageUrl: String?=null,
    val confirmations: Int=0,
    val isResolved: Boolean = false
)

// This defines the different types of reports a user can submit.
data class ReportCategory(
    val name: String,
    val icon: ImageVector
)

val reportCategories = listOf(
    ReportCategory("Road Blockage", Icons.Default.Warning),
    ReportCategory("Power Outage", Icons.Default.ElectricBolt),
    // Add more categories here in the future
)
