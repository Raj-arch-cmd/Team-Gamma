package com.example.team_gamma.data


data class Profile(
    val id: Int = 1, // Single profile for the app
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val emergencyContact: String = "",
    val bloodGroup: String = "",
    val dateOfBirth: String = "",
    val address: String = "",
    val medicalConditions: String = "",
    val allergies: String = "",
    val medications: String = "",
    val emergencyInstructions: String = "",
    val profileImageUri: String? = null
) {
    fun isProfileComplete(): Boolean {
        return fullName.isNotBlank() && phone.isNotBlank() && emergencyContact.isNotBlank()
    }
}