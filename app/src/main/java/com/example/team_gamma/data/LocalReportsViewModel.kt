package com.example.team_gamma.data

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
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
    private val storage = try {
        FirebaseStorage.getInstance("gs://teamgamma-b6f6f.firebasestorage.app")
    } catch (_: Exception) {
        FirebaseStorage.getInstance()
    }

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

    fun addReport(category: ReportCategory, description: String, imageUriString: String?, onComplete: (Boolean) -> Unit = {}) {
        val reportId = UUID.randomUUID().toString()
        val currentUserId = auth.currentUser?.uid ?: "anonymous"
        val createdAt = System.currentTimeMillis()

        _isLoading.value = true

        val saveFirestoreReport = { downloadUrl: String? ->
            val reportData = hashMapOf(
                "id" to reportId,
                "categoryName" to category.name,
                "description" to description,
                "locationName" to "Pimpri-Chinchwad",
                "timestamp" to "Just now",
                "createdAt" to createdAt,
                "imageUrl" to downloadUrl,
                "confirmations" to 0,
                "confirmedUserIds" to emptyList<String>(),
                "userId" to currentUserId,
                "isResolved" to false
            )

            firestore.collection("reports")
                .document(reportId)
                .set(reportData)
                .addOnSuccessListener {
                    _isLoading.value = false
                    Log.d("LocalReportsViewModel", "Report $reportId written to Firestore successfully")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    _isLoading.value = false
                    Log.e("LocalReportsViewModel", "Failed to write report $reportId to Firestore", e)
                    _errorMessage.value = "Failed to save report: ${e.localizedMessage}"
                    onComplete(false)
                }
        }

        if (!imageUriString.isNullOrBlank()) {
            try {
                val localUri = Uri.parse(imageUriString)
                val storageRef = storage.reference.child("reports_photos/$reportId.jpg")
                storageRef.putFile(localUri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl
                            .addOnSuccessListener { downloadUrl ->
                                saveFirestoreReport(downloadUrl.toString())
                            }
                            .addOnFailureListener { e ->
                                Log.e("LocalReportsViewModel", "Failed to get download URL, saving report without cloud image", e)
                                saveFirestoreReport(null)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("LocalReportsViewModel", "Failed to upload image to Firebase Storage, saving report without cloud image", e)
                        saveFirestoreReport(null)
                    }
            } catch (e: Exception) {
                Log.e("LocalReportsViewModel", "Error parsing image Uri, saving report without cloud image", e)
                saveFirestoreReport(null)
            }
        } else {
            saveFirestoreReport(null)
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

    fun deleteReport(report: LocalReport, onComplete: (Boolean) -> Unit = {}) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId.isNullOrBlank() || currentUserId != report.userId || report.userId == "anonymous") {
            _errorMessage.value = "You are not authorized to delete this report."
            onComplete(false)
            return
        }

        _isLoading.value = true

        firestore.collection("reports")
            .document(report.id)
            .delete()
            .addOnSuccessListener {
                _isLoading.value = false
                Log.d("LocalReportsViewModel", "Report ${report.id} deleted from Firestore successfully")
                _errorMessage.value = null

                // If report has an image, delete the corresponding image from Firebase Storage
                if (!report.imageUrl.isNullOrBlank()) {
                    try {
                        val storageRef = storage.reference.child("reports_photos/${report.id}.jpg")
                        storageRef.delete().addOnSuccessListener {
                            Log.d("LocalReportsViewModel", "Storage image for report ${report.id} deleted successfully")
                        }.addOnFailureListener { e ->
                            Log.e("LocalReportsViewModel", "Failed to delete storage image for report ${report.id}", e)
                        }
                    } catch (e: Exception) {
                        Log.e("LocalReportsViewModel", "Error deleting image from Storage", e)
                    }
                }

                onComplete(true)
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                Log.e("LocalReportsViewModel", "Failed to delete report ${report.id} from Firestore", e)
                _errorMessage.value = "Failed to delete report: ${e.localizedMessage}"
                onComplete(false)
            }
    }
}
