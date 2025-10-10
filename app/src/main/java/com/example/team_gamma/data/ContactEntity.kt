package com.example.team_gamma.data


import androidx.room.Entity
import androidx.room.PrimaryKey

// This represents a single emergency contact in the Room database.
@Entity(tableName = "emergency_contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val number: String
)
