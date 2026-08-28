package com.example.team_gamma.data

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@HiltViewModel
class LocalReportsViewModel @Inject constructor() : ViewModel() {

    private val _reports = MutableStateFlow<List<LocalReport>>(emptyList())
    val reports: StateFlow<List<LocalReport>> = _reports.asStateFlow()

    init {
        // Pre-populate with fake data for demonstration purposes.
        // In the future, this list will be populated from Firebase Firestore.
        _reports.value = listOf(
            LocalReport(
                id = UUID.randomUUID().toString(),
                category = reportCategories[0],
                description = "A large banyan tree has fallen and is completely blocking the main road near the market.",
                locationName = "Pimpri-Chinchwad",
                timestamp = "15 mins ago",
                imageUrl = null, // Placeholder for an image
                confirmations = 5
            ),
            LocalReport(
                id = UUID.randomUUID().toString(),
                category = reportCategories[1],
                description = "Complete power outage in Sector 21. No electricity for the last hour.",
                locationName = "Nigdi",
                timestamp = "1 hour ago",
                imageUrl = null,
                confirmations = 12
            )
        )
    }

    // This function will be called from the CreateReportScreen to add a new report.
    fun addReport(category: ReportCategory, description: String, imageUri: String?) {
        val newReport = LocalReport(
            id = UUID.randomUUID().toString(),
            category = category,
            description = description,
            locationName = "Pimpri-Chinchwad", // Placeholder location
            timestamp = "Just now",
            imageUrl = imageUri,
            confirmations = 0
        )
        // Add the new report to the top of the list.
        _reports.value = listOf(newReport) + _reports.value
    }
}
