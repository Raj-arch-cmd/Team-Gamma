package com.example.team_gamma.data

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@HiltViewModel
class LocalReportsViewModel @Inject constructor() : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _reports = MutableStateFlow<List<LocalReport>>(emptyList())
    val reports: StateFlow<List<LocalReport>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        listenToReports()
    }

    private fun listenToReports() {
        _isLoading.value = true
        firestore.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.e("LocalReportsViewModel", "Error listening to Firestore reports", error)
                    _errorMessage.value = "Failed to load live reports: ${error.localizedMessage}"
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val reportList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.id
                            val categoryName = doc.getString("categoryName") ?: reportCategories[0].name
                            val description = doc.getString("description") ?: ""
                            val locationName = doc.getString("locationName") ?: "Pimpri-Chinchwad"
                            val timestamp = doc.getString("timestamp") ?: "Recently"
                            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            val imageUrl = doc.getString("imageUrl")
                            val confirmations = doc.getLong("confirmations")?.toInt() ?: 0
                            @Suppress("UNCHECKED_CAST")
                            val confirmedUserIds = (doc.get("confirmedUserIds") as? List<String>) ?: emptyList()
                            val userId = doc.getString("userId") ?: ""
                            val isResolved = doc.getBoolean("isResolved") ?: false

                            LocalReport(
                                id = id,
                                category = getCategoryByName(categoryName),
                                description = description,
                                locationName = locationName,
                                timestamp = timestamp,
                                createdAt = createdAt,
                                imageUrl = imageUrl,
                                confirmations = confirmations,
                                confirmedUserIds = confirmedUserIds,
                                userId = userId,
                                isResolved = isResolved
                            )
                        } catch (e: Exception) {
                            Log.e("LocalReportsViewModel", "Error parsing report doc ${doc.id}", e)
                            null
                        }
                    }
                    _reports.value = reportList
                    _errorMessage.value = null
                }
            }
    }

    fun addReport(category: ReportCategory, description: String, imageUri: String?) {
        val reportId = UUID.randomUUID().toString()
        val currentUserId = auth.currentUser?.uid ?: "anonymous"
        val createdAt = System.currentTimeMillis()

        val reportData = hashMapOf(
            "id" to reportId,
            "categoryName" to category.name,
            "description" to description,
            "locationName" to "Pimpri-Chinchwad",
            "timestamp" to "Just now",
            "createdAt" to createdAt,
            "imageUrl" to imageUri,
            "confirmations" to 0,
            "confirmedUserIds" to emptyList<String>(),
            "userId" to currentUserId,
            "isResolved" to false
        )

        firestore.collection("reports")
            .document(reportId)
            .set(reportData)
            .addOnSuccessListener {
                Log.d("LocalReportsViewModel", "Report $reportId written to Firestore successfully")
            }
            .addOnFailureListener { e ->
                Log.e("LocalReportsViewModel", "Failed to write report $reportId to Firestore", e)
                _errorMessage.value = "Failed to save report: ${e.localizedMessage}"
            }
    }

    fun toggleConfirmation(report: LocalReport) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrBlank()) {
            _errorMessage.value = "Please sign in to confirm reports."
            return
        }

        val docRef = firestore.collection("reports").document(report.id)
        val hasConfirmed = report.confirmedUserIds.contains(currentUserId)

        if (hasConfirmed) {
            docRef.update(
                "confirmations", FieldValue.increment(-1),
                "confirmedUserIds", FieldValue.arrayRemove(currentUserId)
            ).addOnFailureListener { e ->
                Log.e("LocalReportsViewModel", "Failed to remove confirmation for report ${report.id}", e)
            }
        } else {
            docRef.update(
                "confirmations", FieldValue.increment(1),
                "confirmedUserIds", FieldValue.arrayUnion(currentUserId)
            ).addOnFailureListener { e ->
                Log.e("LocalReportsViewModel", "Failed to add confirmation for report ${report.id}", e)
            }
        }
    }
}
