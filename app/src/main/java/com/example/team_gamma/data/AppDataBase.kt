package com.example.team_gamma.data


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [FaqItem::class, ContactEntity::class, ProfileEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "resqtech_database"
                )
                    .addCallback(DatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateInitialData(getDatabase(context).appDao())
            }
        }

        suspend fun populateInitialData(appDao: AppDao) {
            // ... (populate FAQs remains the same)
            val initialFaqs = listOf(
                FaqItem(question = "flood", answer = "During a flood, get to higher ground immediately. Avoid walking or driving through floodwaters."),
                FaqItem(question = "burns", answer = "For minor burns, run cool water over the area for 10-15 minutes. Cover with a sterile bandage. Do not use ice."),
                FaqItem(question = "emergency kit", answer = "An emergency kit should include water, non-perishable food, a flashlight, a first-aid kit, and a battery-powered radio.")
            )
            appDao.insertAllFaqs(initialFaqs)


            // Insert a default empty profile for a new installation.
            val defaultProfile = ProfileEntity(
                id = 1,
                userName = "",
                bloodType = "",
                allergies = "",
                medicalConditions = "",
                email = "",
                phone = "",
                emergencyContact = "",
                dateOfBirth = "",
                gender = "",
                weight = "",
                height = "",
                address = "",
                emergencyInstructions = "",
                profileImageUri = null
            )
            appDao.insertProfile(defaultProfile)
        }
    }
}

