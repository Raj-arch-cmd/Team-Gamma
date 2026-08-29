package com.example.team_gamma.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

@HiltViewModel
class LocalReportsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    private suspend fun compressImage(imageUriString: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val uri = Uri.parse(imageUriString)

                // Read input stream bytes ONCE into memory to work with all Camera and Gallery content Uris
                val rawBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes()
                } ?: return@withContext null

                val origSizeKb = rawBytes.size / 1024
                Log.d("LocalReportsViewModel", "Compression Start -> Raw input byte size: ${origSizeKb} KB")

                // 1. Get original bounds
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)

                val origWidth = options.outWidth
                val origHeight = options.outHeight
                if (origWidth <= 0 || origHeight <= 0) {
                    Log.e("LocalReportsViewModel", "Failed to decode image bounds for $imageUriString")
                    return@withContext null
                }

                // 2. Calculate inSampleSize so max dimension <= 1280px
                val maxTargetDimension = 1280
                var sampleSize = 1
                val maxOrigDimension = Math.max(origWidth, origHeight)
                while (maxOrigDimension / sampleSize > maxTargetDimension) {
                    sampleSize *= 2
                }

                // 3. Decode scaled bitmap from raw bytes
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val sampleBitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                    ?: return@withContext null

                // 4. Exact scale if still > 1280px
                val currentMax = Math.max(sampleBitmap.width, sampleBitmap.height)
                val finalBitmap = if (currentMax > maxTargetDimension) {
                    val scaleFactor = maxTargetDimension.toFloat() / currentMax.toFloat()
                    val targetW = (sampleBitmap.width * scaleFactor).toInt()
                    val targetH = (sampleBitmap.height * scaleFactor).toInt()
                    val scaled = Bitmap.createScaledBitmap(sampleBitmap, targetW, targetH, true)
                    if (scaled != sampleBitmap) {
                        sampleBitmap.recycle()
                    }
                    scaled
                } else {
                    sampleBitmap
                }

                // 5. Compress to JPEG quality 80%
                val outputStream = ByteArrayOutputStream()
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val finalWidth = finalBitmap.width
                val finalHeight = finalBitmap.height
                finalBitmap.recycle()

                val byteArray = outputStream.toByteArray()
                val durationMs = System.currentTimeMillis() - startTime
                Log.d("LocalReportsViewModel", "Compression Complete -> Final dimensions: ${finalWidth}x${finalHeight}, Final size: ${byteArray.size / 1024} KB, Took: ${durationMs} ms")

                byteArray
            } catch (e: Exception) {
                Log.e("LocalReportsViewModel", "Error compressing image $imageUriString", e)
                null
            }
        }
    }

    fun addReport(category: ReportCategory, description: String, imageUriString: String?, onComplete: (Boolean) -> Unit = {}) {
        val reportId = UUID.randomUUID().toString()
        val currentUserId = auth.currentUser?.uid ?: "anonymous"
        val createdAt = System.currentTimeMillis()

        _isLoading.value = true

        val saveFirestoreReport = { downloadUrl: String? ->
            val firestoreStartTime = System.currentTimeMillis()
            Log.d("LocalReportsViewModel", "Firestore Write Start -> reportId: $reportId")

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
                    val firestoreDurationMs = System.currentTimeMillis() - firestoreStartTime
                    Log.d("LocalReportsViewModel", "Firestore Write Complete -> Took: ${firestoreDurationMs} ms")
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
            viewModelScope.launch {
                val compressedBytes = compressImage(imageUriString)
                if (compressedBytes != null) {
                    val uploadStartTime = System.currentTimeMillis()
                    Log.d("LocalReportsViewModel", "Storage Upload Start -> reportId: $reportId, payload size: ${compressedBytes.size / 1024} KB")

                    val storageRef = storage.reference.child("reports_photos/$reportId.jpg")
                    storageRef.putBytes(compressedBytes)
                        .addOnSuccessListener {
                            val uploadDurationMs = System.currentTimeMillis() - uploadStartTime
                            Log.d("LocalReportsViewModel", "Storage Upload Complete -> Took: ${uploadDurationMs} ms")

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
                            Log.e("LocalReportsViewModel", "Failed to upload image bytes to Firebase Storage, saving report without cloud image", e)
                            saveFirestoreReport(null)
                        }
                } else {
                    Log.w("LocalReportsViewModel", "Compression returned null, saving report without cloud image")
                    saveFirestoreReport(null)
                }
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
