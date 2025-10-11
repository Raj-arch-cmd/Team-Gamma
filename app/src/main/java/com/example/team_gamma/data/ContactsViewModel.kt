package com.example.team_gamma.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.resqtech.data.Contact
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val appDao = AppDatabase.getDatabase(application).appDao()

    val emergencyContacts = appDao.getAllContacts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addContact(contact: Contact) {
        viewModelScope.launch {
            // Convert the UI-layer 'Contact' object to a database 'ContactEntity'
            val contactEntity = ContactEntity(name = contact.name, number = contact.number)
            appDao.insertContact(contactEntity)
        }
    }

    fun removeContact(contactEntity: ContactEntity) {
        viewModelScope.launch {
            appDao.deleteContact(contactEntity)
        }
    }
}