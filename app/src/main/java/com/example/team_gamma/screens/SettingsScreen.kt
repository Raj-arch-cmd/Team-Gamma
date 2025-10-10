package com.example.team_gamma.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resqtech.data.SettingsViewModel
import com.example.resqtech.ui.theme.ResQTechTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit // Callback to go back to the Profile screen
) {
    val sosMessage by viewModel.sosMessage.collectAsState()
    val themePreference by viewModel.themePreference.collectAsState()
    var isEditingSosMessage by remember { mutableStateOf(false) }
    var tempSosMessage by remember(sosMessage) { mutableStateOf(sosMessage) }
    val isDarkTheme = themePreference == "Dark"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                // Add a back button to return to the Profile screen
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- SOS Customization Section ---
            SettingsSection(title = "SOS Configuration") {
                if (isEditingSosMessage) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = tempSosMessage,
                            onValueChange = { tempSosMessage = it },
                            label = { Text("Custom SOS Message") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isEditingSosMessage = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                viewModel.updateSosMessage(tempSosMessage)
                                isEditingSosMessage = false
                            }) {
                                Text("Save")
                            }
                        }
                    }
                } else {
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        title = "Custom SOS Message",
                        subtitle = sosMessage,
                        onClick = { isEditingSosMessage = true }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Appearance Section ---
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.Brightness4,
                    title = "Dark Mode",
                    subtitle = "Enable dark theme for the app",
                    control = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { checked ->
                                val newTheme = if (checked) "Dark" else "Light"
                                viewModel.updateThemePreference(newTheme)
                            }
                        )
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // --- About Section ---
            SettingsSection(title = "About") {
                SettingsItem(icon = Icons.Default.Policy, title = "Privacy Policy", onClick = { /* TODO */ })
                SettingsItem(icon = Icons.Default.Info, title = "App Version", subtitle = "1.0.0")
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    control: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (control != null) {
            control()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    ResQTechTheme {
        SettingsScreen(
            viewModel = SettingsViewModel(context.applicationContext as Application),
            onNavigateBack = {}
        )
    }
}

