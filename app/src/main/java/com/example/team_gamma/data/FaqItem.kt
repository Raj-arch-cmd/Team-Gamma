package com.example.team_gamma.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// This defines the table structure for our offline FAQs in the Room database.
@Entity(tableName = "faq_items")
data class FaqItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val answer: String
)
