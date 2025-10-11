package com.example.team_gamma.screens

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.example.team_gamma.data.ContactEntity
import com.example.team_gamma.data.ContactsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageContactsScreen(
    contactsViewModel: ContactsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val emergencyContactsState = contactsViewModel.contacts.observeAsState(initial = emptyList())
    val emergencyContacts = emergencyContactsState.value
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { contactUri: Uri? ->
            contactUri?.let { uri ->
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                        val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        val contactId = c.getString(idIndex)
                        val name = c.getString(nameIndex)

                        if (c.getInt(hasPhoneIndex) > 0) {
                            val phoneCursor = context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", arrayOf(contactId), null
                            )
                            phoneCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numberIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    val number = pc.getString(numberIndex)
                                    val newContact = ContactEntity(
                                        name = name ?: "Unknown Contact",
                                        number = number ?: "No phone number"
                                    )
                                    contactsViewModel.addContact(newContact)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("${name ?: "Contact"} added successfully")
                                    }
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Selected contact has no phone number")
                            }
                        }
                    }
                } ?: run {
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to read contact information")
                    }
                }
            } ?: run {
                scope.launch {
                    snackbarHostState.showSnackbar("Contact selection cancelled")
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Contacts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { contactPickerLauncher.launch(null) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Emergency Contact")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL)
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "Call Icon",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Call Another Contact")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Your Emergency Contacts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (emergencyContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No emergency contacts added yet.",
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(emergencyContacts, key = { it.id }) { contact ->
                        ContactItem(
                            contact = contact,
                            onRemove = { contactsViewModel.removeContact(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: ContactEntity, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "#",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = contact.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove ${contact.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}