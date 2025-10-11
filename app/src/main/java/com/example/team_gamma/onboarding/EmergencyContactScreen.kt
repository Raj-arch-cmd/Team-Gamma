package com.example.team_gamma.onboarding

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team_gamma.R
import com.example.resqtech.data.Contact
import com.example.team_gamma.data.ContactEntity
import com.example.team_gamma.data.ContactsViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactScreen(
    contactsViewModel: ContactsViewModel,
    onContactsConfirmed: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val selectedContacts = contactsViewModel.emergencyContacts.collectAsState().value // FIX: Use .value to get the actual list
    val context = LocalContext.current
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
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(contactId),
                                null
                            )
                            phoneCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val numberIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    val number = pc.getString(numberIndex)
                                    contactsViewModel.addContact(Contact(name, number))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("$name added successfully")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Contacts") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (selectedContacts.isNotEmpty()) {
                FloatingActionButton(onClick = { contactPickerLauncher.launch(null) }) {
                    Icon(Icons.Default.Add, contentDescription = "Add another contact")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedContacts.isEmpty()) {
                EmptyStateContent(onAddContact = { contactPickerLauncher.launch(null) })
            } else {
                ContactListContent(
                    contacts = selectedContacts,
                    onRemoveContact = { contactToRemove ->
                        contactsViewModel.removeContact(contactToRemove)
                    },
                    onConfirm = {
                        scope.launch {
                            delay(300)
                            onContactsConfirmed()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyStateContent(onAddContact: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.weight(0.5f))
        Image(
            painter = painterResource(id = R.drawable.contact_illustration),
            contentDescription = "Illustration of adding contacts",
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Add emergency contacts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "If you trigger an SOS, your emergency contacts will be notified with your location.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onAddContact,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Add first contact", fontSize = 18.sp)
        }
    }
}

@Composable
fun ContactListContent(
    contacts: List<ContactEntity>,
    onRemoveContact: (ContactEntity) -> Unit,
    onConfirm: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "These contacts will be notified in an emergency. You can add up to 5.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts) { contact ->
                ContactItem(
                    contact = contact,
                    onRemove = { onRemoveContact(contact) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = contacts.isNotEmpty()
        ) {
            Text("Confirm Contacts", fontSize = 18.sp)
        }
    }
}

@Composable
fun ContactItem(contact: ContactEntity, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
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
                Text(text = contact.name, fontWeight = FontWeight.Bold)
                Text(text = contact.number, style = MaterialTheme.typography.bodyMedium, color = LocalContentColor.current.copy(alpha = 0.7f))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove ${contact.name}")
            }
        }
    }
}