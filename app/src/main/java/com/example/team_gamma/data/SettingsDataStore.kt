package com.example.team_gamma.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey // <-- ADD THIS IMPORT
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
        val SOS_MESSAGE_KEY = stringPreferencesKey("sos_message")
        // ✅ ADD THIS KEY
        val IS_FIRST_LAUNCH_KEY = booleanPreferencesKey("is_first_launch")
    }

    // ... (themePreference and sosMessage flows remain the same) ...
    val themePreference = appContext.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System Default"
    }
    val sosMessage = appContext.dataStore.data.map { preferences ->
        preferences[SOS_MESSAGE_KEY] ?: "EMERGENCY SOS! I may be in need of help. My current location is:"
    }

    // ✅ ADD THIS FLOW TO READ THE FLAG
    val isFirstLaunch = appContext.dataStore.data.map { preferences ->
        // If the key doesn't exist, it's the first launch, so default to 'true'
        preferences[IS_FIRST_LAUNCH_KEY] ?: true
    }

    // ... (saveThemePreference and saveSosMessage functions remain the same) ...
    suspend fun saveThemePreference(theme: String) {
        appContext.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }
    suspend fun saveSosMessage(message: String) {
        appContext.dataStore.edit { preferences ->
            preferences[SOS_MESSAGE_KEY] = message
        }
    }

    // ✅ ADD THIS FUNCTION TO UPDATE THE FLAG
    suspend fun setFirstLaunchCompleted() {
        appContext.dataStore.edit { preferences ->
            preferences[IS_FIRST_LAUNCH_KEY] = false
        }
    }
}