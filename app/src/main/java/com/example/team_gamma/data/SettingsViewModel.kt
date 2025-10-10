package com.example.team_gamma.data


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Create an instance of our new DataStore to handle saving/loading
    private val settingsDataStore = SettingsDataStore(application)

    // Expose the settings as state flows that the UI can collect and react to.
    // This will automatically update the UI when the saved value changes.
    val themePreference = settingsDataStore.themePreference.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "System Default" // The initial value before anything is loaded
    )

    val sosMessage = settingsDataStore.sosMessage.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "EMERGENCY SOS! I may be in need of help. My current location is:" // Default message
    )

    // Function called by the UI to save the new theme preference
    fun updateThemePreference(theme: String) {
        viewModelScope.launch {
            settingsDataStore.saveThemePreference(theme)
        }
    }

    // Function called by the UI to save the new SOS message
    fun updateSosMessage(newMessage: String) {
        viewModelScope.launch {
            settingsDataStore.saveSosMessage(newMessage)
        }
    }
}

