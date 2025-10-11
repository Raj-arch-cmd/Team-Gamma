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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.resqtech.ui.theme.screens.LoudAlarmScreen
import com.example.team_gamma.auth.AuthViewModel
import com.example.team_gamma.component.SosConfirmationDialog
import com.example.team_gamma.data.*
import com.example.team_gamma.loginscreen.LoginScreen
import com.example.team_gamma.loginscreen.SignUpScreen
import com.example.team_gamma.onboarding.EmergencyContactScreen
import com.example.team_gamma.onboarding.WelcomeScreen
import com.example.team_gamma.screens.*
import com.example.team_gamma.ui.theme.TeamGammaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

class MainActivity : ComponentActivity() {
    private val contactsViewModel by viewModels<ContactsViewModel>()
    private val profileViewModel by viewModels<ProfileViewModel>()
    private val aiAssistantViewModel by viewModels<AiAssistantViewModel>()
    private val settingsViewModel by viewModels<SettingsViewModel>()
    private val hubViewModel by viewModels<HubViewModel>()
    private val alertsViewModel by viewModels<AlertsViewModel>()
    private val localReportsViewModel by viewModels<LocalReportsViewModel>()
    private val manualAlertViewModel by viewModels<ManualAlertViewModel>()
    private val authViewModel by viewModels<AuthViewModel>()

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

            TeamGammaTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // ✅ UPDATED LOGIC: Determine the correct start destination
                val isFirstLaunch by settingsViewModel.isFirstLaunch.collectAsState()
                val currentUser by authViewModel.currentUser.collectAsState()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(isFirstLaunch, currentUser) {
                    if (isFirstLaunch != null) { // Wait for DataStore to load
                        startDestination = when {
                            isFirstLaunch == true -> "welcome" // First priority: Permissions onboarding
                            currentUser != null -> "dashboard"  // If not first launch & logged in
                            else -> "login"             // If not first launch & not logged in
                        }
                    }
                }

                // Only show UI when the start destination is determined
                if (startDestination != null) {
                    val bottomBarRoutes = listOf("dashboard", "alerts", "local_reports", "profile")
                    val showBottomBar = currentDestination?.route in bottomBarRoutes

                    var showSosDialog by remember { mutableStateOf(false) }
                    val smsPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { granted ->
                            if (granted) showSosDialog = true
                            else Toast.makeText(this, "SMS permission is required.", Toast.LENGTH_LONG).show()
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
                            manualAlertViewModel = manualAlertViewModel,
                            authViewModel = authViewModel,
                            startDestination = startDestination!!
                        )
                    }
                } else {
                    // Show a loading screen while we determine the route
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun sendSosMessage() {
        CoroutineScope(Dispatchers.Main).launch {
            val message = "🚨 Emergency! I need help. This is an automated alert from ResQTech."
            val smsManager = SmsManager.getDefault()

            try {
                val contactList = contactsViewModel.getSavedContacts()
                if (contactList.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No emergency contacts found.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                contactList.forEach { contact ->
                    try {
                        smsManager.sendTextMessage(contact.number, null, message, null, null)
                        Toast.makeText(this@MainActivity, "SOS sent to ${contact.name}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Failed to send to ${contact.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Could not load contacts.", Toast.LENGTH_LONG).show()
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
    manualAlertViewModel: ManualAlertViewModel,
    authViewModel: AuthViewModel,
    startDestination: String
) {

    val floodPredictionViewModel: FloodPredictionViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth routes
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    // ✅ Navigate to emergency_contact_setup AFTER successful login
                    navController.navigate("emergency_contact_setup") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = {
                    // ✅ Navigate to emergency_contact_setup AFTER successful signup
                    navController.navigate("emergency_contact_setup") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Flow
        composable("welcome") {
            WelcomeScreen(onPermissionsGranted = {
                // ✅ After permissions are granted, navigate to Login/Signup
                navController.navigate("login") {
                    popUpTo("welcome") { inclusive = true } // Clear WelcomeScreen
                }
                // Also, ensure any existing user is logged out so they face the login screen
                authViewModel.logout()
            })
        }

        // ✅ Emergency Contact screen now comes AFTER Login/Signup for first-timers
        composable("emergency_contact_setup") {
            EmergencyContactScreen(
                contactsViewModel = contactsViewModel,
                onContactsConfirmed = {
                    // Mark onboarding as complete AFTER contacts are set up
                    settingsViewModel.setFirstLaunchCompleted()
                    // Finally navigate to dashboard
                    navController.navigate("dashboard") {
                        popUpTo("emergency_contact_setup") { inclusive = true } // Clear contacts screen
                    }
                },
                onNavigateBack = { navController.popBackStack() } // Keep back navigation if needed
            )
        }


        // Main App Screens (remain unchanged)
        composable("dashboard") {
            PreparednessHubScreen(
                navController = navController,
                hubViewModel = hubViewModel,
                floodPredictionViewModel = floodPredictionViewModel
                // No manualAlertViewModel is passed here
            )
        }
        composable("evacuation_routes") {
            EvacuationRoutesScreen(
                navController = navController,
                // ✅ Pass the SAME shared ViewModel to the new screen
                viewModel = floodPredictionViewModel
            )
        }

        composable("alerts") { AlertsScreen(viewModel = alertsViewModel) }
        composable("local_reports") {
            LocalReportsScreen(navController = navController, viewModel = localReportsViewModel)
        }
        composable("create_report") {
            CreateReportScreen(navController = navController, viewModel = localReportsViewModel)
        }
        composable("profile") {
            ProfileScreen(
                profileViewModel = profileViewModel,
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable("loud_alarm") {
            LoudAlarmScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable("nearest_hospital") {
            NearestHospitalScreen(navController = navController)
        }

        composable("settings") {
            SettingsScreen(viewModel = settingsViewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable("information") { InformationScreen(navController = navController) }
        composable("dos_and_donts") { DosAndDontsScreen(onNavigateBack = { navController.popBackStack() }) }
        composable("manage_contacts") {
            ManageContactsScreen(contactsViewModel = contactsViewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable("ai_assistant") {
            AiAssistantScreen(viewModel = aiAssistantViewModel, onNavigateBack = { navController.popBackStack() })
        }
    }
}