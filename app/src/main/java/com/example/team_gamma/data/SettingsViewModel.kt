package com.example.team_gamma.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)

    val themePreference = settingsDataStore.themePreference.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "System Default"
    )

    val sosMessage = settingsDataStore.sosMessage.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "EMERGENCY SOS! I may be in need of help. My current location is:"
    )

    // ✅ ADD THIS STATE FLOW
    // This will let our UI know if it's the first time the app is running.
    val isFirstLaunch = settingsDataStore.isFirstLaunch.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null // Start as null until DataStore loads the value
    )

    fun updateThemePreference(theme: String) {
        viewModelScope.launch {
            settingsDataStore.saveThemePreference(theme)
        }
    }

    fun updateSosMessage(newMessage: String) {
        viewModelScope.launch {
            settingsDataStore.saveSosMessage(newMessage)
        }
    }

    // ✅ ADD THIS FUNCTION
    // We will call this after the user sets up their emergency contacts.
    fun setFirstLaunchCompleted() {
        viewModelScope.launch {
            settingsDataStore.setFirstLaunchCompleted()
        }
    }
}