package com.example.team_gamma

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.team_gamma.component.SosConfirmationDialog
import com.example.team_gamma.data.*
import com.example.team_gamma.onboarding.EmergencyContactScreen
import com.example.team_gamma.onboarding.WelcomeScreen
import com.example.team_gamma.screens.*
import com.example.team_gamma.ui.theme.TeamGammaTheme
import kotlin.getValue

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

class MainActivity : ComponentActivity() {
    private val contactsViewModel by viewModels<ContactsViewModel>()
    private val profileViewModel by viewModels<ProfileViewModel>()
    private val aiAssistantViewModel by viewModels<AiAssistantViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()
    private val hubViewModel by viewModels<HubViewModel>()
    private val alertsViewModel by viewModels<AlertsViewModel>()
    private val localReportsViewModel by viewModels<LocalReportsViewModel>()
    // <-- 1. ADD THIS VIEWMODEL
    private val floodPredictionViewModel by viewModels<FloodPredictionViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themePreference by settingsViewModel.themePreference.collectAsState()
            val useDarkTheme = when (themePreference) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            TeamGammaTheme (darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val bottomBarRoutes = listOf("dashboard", "alerts", "local_reports", "profile")
                val showBottomBar = currentDestination?.route in bottomBarRoutes

                var showSosDialog by remember { mutableStateOf(false) }
                val smsPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { granted ->
                        if (granted) showSosDialog = true
                        else Toast.makeText(this, "SMS permission is required for the SOS feature.", Toast.LENGTH_LONG).show()
                    }
                )

                if (showSosDialog) {
                    SosConfirmationDialog(
                        onConfirm = {
                            showSosDialog = false
                            sendSosMessage()
                        },
                        onDismiss = { showSosDialog = false }
                    )
                }

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                val items = listOf(
                                    BottomNavItem("Home", Icons.Default.Home, "dashboard"),
                                    BottomNavItem("Alerts", Icons.Default.Warning, "alerts"),
                                    BottomNavItem("Reports", Icons.Default.Campaign, "local_reports"),
                                    BottomNavItem("Profile", Icons.Default.Person, "profile")
                                )
                                items.forEach { item ->
                                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                    NavigationBarItem(
                                        icon = { Icon(item.icon, contentDescription = item.label) },
                                        label = { Text(item.label) },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (showBottomBar) {
                            FloatingActionButton(
                                onClick = {
                                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                        showSosDialog = true
                                    } else {
                                        smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
                                    }
                                },
                                containerColor = Color.Red,
                                contentColor = Color.White,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Icon(Icons.Default.Sos, contentDescription = "SOS", modifier = Modifier.size(32.dp))
                            }
                        }
                    },
                    floatingActionButtonPosition = FabPosition.Center
                ) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController,
                        contactsViewModel = contactsViewModel,
                        profileViewModel = profileViewModel,
                        aiAssistantViewModel = aiAssistantViewModel,
                        settingsViewModel = settingsViewModel,
                        hubViewModel = hubViewModel,
                        alertsViewModel = alertsViewModel,
                        localReportsViewModel = localReportsViewModel,
                        // <-- 2. PASS THE VIEWMODEL DOWN
                        floodPredictionViewModel = floodPredictionViewModel
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSosMessage() {
        // NOTE: This is a basic implementation. You'll need to add logic to get the user's location.
        val message = "Emergency! I need help. This is an automated alert."
        val smsManager = SmsManager.getDefault()
        contactsViewModel.emergencyContacts.value.forEach { contact ->
            try {
                smsManager.sendTextMessage(contact.number, null, message, null, null)
                Toast.makeText(this, "SOS message sent to ${contact.name}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to send SOS to ${contact.name}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    contactsViewModel: ContactsViewModel,
    profileViewModel: ProfileViewModel,
    aiAssistantViewModel: AiAssistantViewModel,
    settingsViewModel: SettingsViewModel,
    hubViewModel: HubViewModel,
    alertsViewModel: AlertsViewModel,
    localReportsViewModel: LocalReportsViewModel,
    // <-- 3. RECEIVE THE VIEWMODEL
    floodPredictionViewModel: FloodPredictionViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "welcome",
        modifier = modifier
    ) {
        composable("welcome") { WelcomeScreen(onPermissionsGranted = { navController.navigate("emergency_contact_setup") }) }
        composable("emergency_contact_setup") { EmergencyContactScreen(contactsViewModel = contactsViewModel, onContactsConfirmed = { navController.navigate("dashboard") }, onNavigateBack = { navController.popBackStack() }) }

        composable("dashboard") {
            // <-- 4. PASS IT TO THE SCREEN
            PreparednessHubScreen(
                navController = navController,
                hubViewModel = hubViewModel,
                floodPredictionViewModel = floodPredictionViewModel
            )
        }
        composable("manage_contacts") { ManageContactsScreen(contactsViewModel = contactsViewModel, onNavigateBack = { navController.popBackStack() }) }
        composable("dos_and_donts") { DosAndDontsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("loud_alarm") { LoudAlarmScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("ai_assistant") { AiAssistantScreen(viewModel = aiAssistantViewModel, onNavigateBack = { navController.popBackStack() }) }
        composable("map") { MapScreen() }
        composable("alerts") { AlertsScreen(viewModel = alertsViewModel) }
        composable("local_reports") { LocalReportsScreen(navController = navController, viewModel = localReportsViewModel) }
        composable("create_report") { CreateReportScreen(navController = navController, viewModel = localReportsViewModel) }
        composable("profile") { ProfileScreen(profileViewModel = profileViewModel, navController = navController) }
        composable("settings") { SettingsScreen(viewModel = settingsViewModel, onNavigateBack = { navController.popBackStack() }) }
        composable("information") { InformationScreen(navController = navController) }
    }
}