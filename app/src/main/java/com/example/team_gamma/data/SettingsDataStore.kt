package com.example.team_gamma.data


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

// This file manages saving and loading settings to the device's local storage.

// Create a DataStore instance, tied to the application's context.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        // Define unique keys for each setting we want to save.
        val THEME_KEY = stringPreferencesKey("theme_preference")
        val SOS_MESSAGE_KEY = stringPreferencesKey("sos_message")
    }

    // This creates a 'Flow' that automatically emits the current theme preference
    // whenever it changes in the saved file.
    val themePreference = appContext.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: "System Default" // Provide a default value
    }

    // This Flow does the same for the custom SOS message.
    val sosMessage = appContext.dataStore.data.map { preferences ->
        preferences[SOS_MESSAGE_KEY] ?: "EMERGENCY SOS! I may be in need of help. My current location is:"
    }

    // This function saves the new theme preference to the device.
    suspend fun saveThemePreference(theme: String) {
        appContext.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    // This function saves the new SOS message to the device.
    suspend fun saveSosMessage(message: String) {
        appContext.dataStore.edit { preferences ->
            preferences[SOS_MESSAGE_KEY] = message
        }
    }
}

