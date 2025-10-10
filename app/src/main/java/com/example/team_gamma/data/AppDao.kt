package com.example.team_gamma.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- FAQ Queries ---
    @Query("SELECT * FROM faq_items WHERE question LIKE '%' || :query || '%' LIMIT 1")
    suspend fun findAnswer(query: String): FaqItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllFaqs(faqs: List<FaqItem>)


    // --- Emergency Contact Queries ---
    @Query("SELECT * FROM emergency_contacts")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)


    // --- Profile Queries ---
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
}
