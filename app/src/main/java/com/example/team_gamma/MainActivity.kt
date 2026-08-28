package com.example.team_gamma

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.team_gamma.screens.LoudAlarmScreen
import com.example.team_gamma.auth.AuthViewModel
import com.example.team_gamma.component.SosConfirmationDialog
import com.example.team_gamma.data.*
import com.example.team_gamma.loginscreen.LoginScreen
import com.example.team_gamma.loginscreen.SignUpScreen
import com.example.team_gamma.onboarding.EmergencyContactScreen
import com.example.team_gamma.onboarding.WelcomeScreen
import com.example.team_gamma.screens.*
import com.example.team_gamma.ui.theme.TeamGammaTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// ✅ THIS IS THE FIX: The data class definition was missing.
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

                val isFirstLaunch by settingsViewModel.isFirstLaunch.collectAsState()
                val currentUser by authViewModel.currentUser.collectAsState()
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(isFirstLaunch, currentUser) {
                    if (isFirstLaunch != null) {
                        startDestination = when {
                            isFirstLaunch == true -> "welcome"
                            currentUser != null -> "dashboard"
                            else -> "login"
                        }
                    }
                }

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
                                sendSosMessageWithLocation()
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
                        val floodPredictionViewModel: FloodPredictionViewModel = viewModel()
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
                            startDestination = startDestination!!,
                            floodPredictionViewModel = floodPredictionViewModel
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendSosMessageWithLocation() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) {
            Toast.makeText(this, "Location permission is required to send location in SOS.", Toast.LENGTH_LONG).show()
            sendSmsToContacts("Location permission not granted.")
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                val finalMessage: String
                if (location != null) {
                    val lat = location.latitude
                    val lon = location.longitude
                    val mapLink = "https://maps.google.com/maps?q=loc:$lat,$lon"

                    val baseMessage = settingsViewModel.sosMessage.value
                    finalMessage = "$baseMessage\nMy current location is: $mapLink"

                    Toast.makeText(this, "Location found! Sending SOS.", Toast.LENGTH_SHORT).show()
                } else {
                    finalMessage = "${settingsViewModel.sosMessage.value}\nLocation could not be determined."
                    Toast.makeText(this, "Could not get location. Sending SOS without it.", Toast.LENGTH_LONG).show()
                }
                sendSmsToContacts(finalMessage)
            }
            .addOnFailureListener { exception ->
                val finalMessage = "${settingsViewModel.sosMessage.value}\nLocation could not be determined. (Error: ${exception.message})"
                Toast.makeText(this, "Failed to get location. Sending SOS without it.", Toast.LENGTH_LONG).show()
                sendSmsToContacts(finalMessage)
            }
    }

    private fun sendSmsToContacts(message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission is required to send SOS messages.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val smsManager = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this@MainActivity.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            } catch (e: Exception) {
                null
            }

            if (smsManager == null) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "SMS service is unavailable on this device.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            try {
                val contactList = contactsViewModel.getSavedContacts().filter { it.number.isNotBlank() }
                if (contactList.isEmpty()) {
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "No emergency contacts found.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                contactList.forEach { contact ->
                    try {
                        val parts = smsManager.divideMessage(message)
                        if (parts.size > 1) {
                            smsManager.sendMultipartTextMessage(contact.number, null, parts, null, null)
                        } else {
                            smsManager.sendTextMessage(contact.number, null, message, null, null)
                        }
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "SOS sent to ${contact.name}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Failed to send SMS to ${contact.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Could not load contacts from database.", Toast.LENGTH_LONG).show()
                }
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
    startDestination: String,
    floodPredictionViewModel: FloodPredictionViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    val isFirstLaunch = settingsViewModel.isFirstLaunch.value
                    if (isFirstLaunch == true) {
                        navController.navigate("emergency_contact_setup") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("signup") {
            SignUpScreen(
                navController = navController,
                onSignUpSuccess = {
                    navController.navigate("emergency_contact_setup") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }
        composable("welcome") {
            WelcomeScreen(onPermissionsGranted = {
                navController.navigate("login") {
                    popUpTo("welcome") { inclusive = true }
                }
                authViewModel.logout()
            })
        }
        composable("emergency_contact_setup") {
            EmergencyContactScreen(
                contactsViewModel = contactsViewModel,
                onContactsConfirmed = {
                    settingsViewModel.setFirstLaunchCompleted()
                    navController.navigate("dashboard") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("dashboard") {
            PreparednessHubScreen(
                navController = navController,
                hubViewModel = hubViewModel,
                floodPredictionViewModel = floodPredictionViewModel
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
        composable("evacuation_routes") {
            EvacuationRoutesScreen(
                navController = navController,
                viewModel = floodPredictionViewModel
            )
        }
        composable("loud_alarm") {
            LoudAlarmScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("nearest_hospital") {
            NearestHospitalScreen(navController = navController)
        }
        composable("map") {
            MapScreen()
        }
    }
}

