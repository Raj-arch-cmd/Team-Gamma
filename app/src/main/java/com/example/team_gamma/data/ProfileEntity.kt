package com.example.team_gamma.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String,
    val bloodType: String,
    val allergies: String,
    val medicalConditions: String,
    val email: String,
    val phone: String,
    val emergencyContact: String,
    val dateOfBirth: String,
    val gender: String,
    val weight: String,
    val height: String,
    val address: String,
    val emergencyInstructions: String,
    val profileImageUri: String?
)