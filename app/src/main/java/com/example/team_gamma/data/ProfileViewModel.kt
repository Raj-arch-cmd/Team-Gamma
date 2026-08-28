package com.example.team_gamma.data


import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val appDao = AppDatabase.getDatabase(application).appDao()

    private val profileFlow: Flow<ProfileEntity?> = appDao.getProfile()

    // Expose all the fields from the database as StateFlows
    val userName: StateFlow<String> = profileFlow.map { it?.userName ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val bloodType: StateFlow<String> = profileFlow.map { it?.bloodType ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val allergies: StateFlow<String> = profileFlow.map { it?.allergies ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val medicalConditions: StateFlow<String> = profileFlow.map { it?.medicalConditions ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val email: StateFlow<String> = profileFlow.map { it?.email ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val phone: StateFlow<String> = profileFlow.map { it?.phone ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val emergencyContact: StateFlow<String> = profileFlow.map { it?.emergencyContact ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val dateOfBirth: StateFlow<String> = profileFlow.map { it?.dateOfBirth ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val gender: StateFlow<String> = profileFlow.map { it?.gender ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val weight: StateFlow<String> = profileFlow.map { it?.weight ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val height: StateFlow<String> = profileFlow.map { it?.height ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val address: StateFlow<String> = profileFlow.map { it?.address ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val emergencyInstructions: StateFlow<String> = profileFlow.map { it?.emergencyInstructions ?: "" }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val profileImageUri: StateFlow<String?> = profileFlow.map { it?.profileImageUri }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun updateProfile(updateAction: (ProfileEntity) -> ProfileEntity) {
        viewModelScope.launch {
            profileFlow.firstOrNull()?.let { currentProfile ->
                val updatedProfile = updateAction(currentProfile)
                appDao.insertProfile(updatedProfile)
            }
        }
    }

    // Update functions for all fields
    fun updateUserName(name: String) = updateProfile { it.copy(userName = name) }
    fun updateBloodType(type: String) = updateProfile { it.copy(bloodType = type) }
    fun updateAllergies(text: String) = updateProfile { it.copy(allergies = text) }
    fun updateMedicalConditions(text: String) = updateProfile { it.copy(medicalConditions = text) }
    fun updateEmail(newEmail: String) = updateProfile { it.copy(email = newEmail) }
    fun updatePhone(newPhone: String) = updateProfile { it.copy(phone = newPhone) }
    fun updateEmergencyContact(contact: String) = updateProfile { it.copy(emergencyContact = contact) }
    fun updateDateOfBirth(dob: String) = updateProfile { it.copy(dateOfBirth = dob) }
    fun updateGender(newGender: String) = updateProfile { it.copy(gender = newGender) }
    fun updateWeight(newWeight: String) = updateProfile { it.copy(weight = newWeight) }
    fun updateHeight(newHeight: String) = updateProfile { it.copy(height = newHeight) }
    fun updateAddress(newAddress: String) = updateProfile { it.copy(address = newAddress) }
    fun updateEmergencyInstructions(instructions: String) = updateProfile { it.copy(emergencyInstructions = instructions) }

    // Save full profile in a single database transaction (prevents write-on-keystroke)
    fun saveProfile(
        userName: String,
        email: String,
        dateOfBirth: String,
        gender: String,
        weight: String,
        height: String,
        phone: String,
        emergencyContact: String,
        bloodType: String,
        allergies: String,
        medicalConditions: String,
        address: String,
        emergencyInstructions: String
    ) {
        viewModelScope.launch {
            val currentProfile = profileFlow.firstOrNull() ?: ProfileEntity(
                id = 1,
                userName = userName,
                bloodType = bloodType,
                allergies = allergies,
                medicalConditions = medicalConditions,
                email = email,
                phone = phone,
                emergencyContact = emergencyContact,
                dateOfBirth = dateOfBirth,
                gender = gender,
                weight = weight,
                height = height,
                address = address,
                emergencyInstructions = emergencyInstructions,
                profileImageUri = null
            )
            val updatedProfile = currentProfile.copy(
                userName = userName,
                email = email,
                dateOfBirth = dateOfBirth,
                gender = gender,
                weight = weight,
                height = height,
                phone = phone,
                emergencyContact = emergencyContact,
                bloodType = bloodType,
                allergies = allergies,
                medicalConditions = medicalConditions,
                address = address,
                emergencyInstructions = emergencyInstructions
            )
            appDao.insertProfile(updatedProfile)
        }
    }

    // FIX: Properly save profile image URI to database
    fun updateProfileImageUri(uri: String?) = updateProfile { it.copy(profileImageUri = uri) }

    // Profile completion calculation
    val profileCompletion: StateFlow<Int> = profileFlow.map { profile ->
        if (profile == null) return@map 0
        var completedFields = 0
        val totalFields = 13 // Total number of profile fields

        if (profile.userName.isNotBlank()) completedFields++
        if (profile.bloodType.isNotBlank()) completedFields++
        if (profile.phone.isNotBlank()) completedFields++
        if (profile.emergencyContact.isNotBlank()) completedFields++
        if (profile.email.isNotBlank()) completedFields++
        if (profile.dateOfBirth.isNotBlank()) completedFields++
        if (profile.gender.isNotBlank()) completedFields++
        if (profile.weight.isNotBlank()) completedFields++
        if (profile.height.isNotBlank()) completedFields++
        if (profile.address.isNotBlank()) completedFields++
        if (profile.allergies.isNotBlank()) completedFields++
        if (profile.medicalConditions.isNotBlank()) completedFields++
        if (profile.emergencyInstructions.isNotBlank()) completedFields++

        (completedFields * 100) / totalFields
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}