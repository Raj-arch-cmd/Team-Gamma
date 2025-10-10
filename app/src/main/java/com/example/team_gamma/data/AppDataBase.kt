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
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateInitialData(database.appDao())
                }
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


            // Insert a default profile with all the new, comprehensive fields.
            val defaultProfile = ProfileEntity(
                id = 1,
                userName = "Haseeb Jameel",
                bloodType = "O+",
                allergies = "None",
                medicalConditions = "None",
                email = "haseeb.jameel9570@gmail.com",
                phone = "+91 1234567890",
                emergencyContact = "+91 9876543210",
                dateOfBirth = "04-July-2004",
                gender = "Male",
                weight = "55 kg",
                height = "5ft 6inch",
                address = "Mumbai, India",
                emergencyInstructions = "Contact emergency contact immediately",
                profileImageUri = null
            )
            appDao.insertProfile(defaultProfile)
        }
    }
}

