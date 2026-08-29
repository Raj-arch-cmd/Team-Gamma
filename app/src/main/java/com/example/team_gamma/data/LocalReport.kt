package com.example.team_gamma.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.ui.graphics.vector.ImageVector

// Defines the structure for a single community-submitted report stored in Firestore.
data class LocalReport(
    val id: String = "",
    val category: ReportCategory = reportCategories[0],
    val description: String = "",
    val locationName: String = "Pimpri-Chinchwad",
    val timestamp: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val confirmations: Int = 0,
    val confirmedUserIds: List<String> = emptyList(),
    val userId: String = "",
    val isResolved: Boolean = false
)

// Defines the category types for reports.
data class ReportCategory(
    val name: String,
    val icon: ImageVector
)

val reportCategories = listOf(
    ReportCategory("Road Blockage", Icons.Default.Warning),
    ReportCategory("Power Outage", Icons.Default.ElectricBolt),
    ReportCategory("Flooding", Icons.Default.Water),
    ReportCategory("Medical Emergency", Icons.Default.LocalHospital),
    ReportCategory("Fire Hazard", Icons.Default.LocalFireDepartment),
    ReportCategory("Infrastructure Damage", Icons.Default.Build),
    ReportCategory("Shelter Needed", Icons.Default.Home)
)

fun getCategoryByName(name: String): ReportCategory {
    return reportCategories.find { it.name.equals(name, ignoreCase = true) } ?: reportCategories[0]
}
