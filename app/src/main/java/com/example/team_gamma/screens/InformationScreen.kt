package com.example.team_gamma.screens


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.team_gamma.R // Correct R class import
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Data classes for different types of content
data class EmergencyContact(val label: String, val number: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
data class SafetyTip(val title: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
data class UtilityAction(val title: String, val description: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)
data class FirstAidTip(val title: String, val immediateAction: String, val detailedSteps: String)
data class ChecklistItem(val text: String, var isChecked: Boolean)

@SuppressLint("MissingPermission") // Suppress lint check as permissions should be requested at app start
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InformationScreen(navController: NavController) {
    val context = LocalContext.current
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isStrobeOn by remember { mutableStateOf(false) }
    var expandedTipIndex by remember { mutableStateOf(-1) }
    var expandedFirstAidIndex by remember { mutableStateOf(-1) }
    var showFirstAidGuide by remember { mutableStateOf(false) }
    var showEmergencyKit by remember { mutableStateOf(false) }

    // Emergency Numbers
    val emergencyNumbers = listOf(
        EmergencyContact("Single Emergency", "112", Icons.Default.Warning),
        EmergencyContact("Police", "100", Icons.Default.Security),
        EmergencyContact("Ambulance", "108", Icons.Default.LocalHospital),
        EmergencyContact("Fire Brigade", "101", Icons.Default.Fireplace),
        EmergencyContact("Women Helpline", "1091", Icons.Default.Female),
        EmergencyContact("Child Helpline", "1098", Icons.Default.ChildFriendly),
        EmergencyContact("Disaster Management", "1078", Icons.Default.Assistant),
        EmergencyContact("Road Accident", "1073", Icons.Default.CarRepair),
        EmergencyContact("Senior Citizen", "14567", Icons.Default.Elderly)
    )

    // Safety Tips for India
    val safetyTips = listOf(
        SafetyTip(
            "Earthquake Safety",
            "• Drop, Cover, Hold under sturdy furniture\n• Stay away from windows and heavy objects\n• If outdoors, move to open area away from buildings\n• After shaking stops, check for injuries and hazards",
            Icons.Default.Warning
        ),
        SafetyTip(
            "Flood Preparedness",
            "• Move to higher ground immediately\n• Avoid walking or driving through floodwaters\n• Turn off electricity and gas mains\n• Keep emergency kit and documents ready",
            Icons.Default.Water
        ),
        SafetyTip(
            "Heat Wave Protection",
            "• Stay hydrated - drink water frequently\n• Avoid direct sun between 12 PM - 4 PM\n• Wear light-colored, loose cotton clothes\n• Recognize heat stroke symptoms: dizziness, nausea, high body temperature",
            Icons.Default.WbSunny
        ),
        SafetyTip(
            "Cyclone Safety",
            "• Stay indoors away from windows\n• Keep emergency kit with torch, radio, food\n• Listen to weather updates on radio/TV\n• Evacuate if advised by authorities",
            Icons.Default.Cloud
        )
    )

    // First Aid Tips
    val firstAidTips = listOf(
        FirstAidTip(
            "Burns",
            "Cool under running water for 10-15 minutes",
            "• Do not use ice, butter, or ointments\n• Cover with sterile non-stick dressing\n• Seek medical help for large or deep burns\n• Don't break blisters"
        ),
        FirstAidTip(
            "Bleeding",
            "Apply direct pressure with clean cloth",
            "• Elevate the injured area above heart level\n• Use pressure points if bleeding continues\n• Don't remove soaked dressings - add more layers\n• Seek immediate medical help for heavy bleeding"
        ),
        FirstAidTip(
            "Choking",
            "Perform 5 back blows then 5 abdominal thrusts",
            "• For adults: Heimlich maneuver\n• For infants: back blows and chest thrusts\n• Call emergency if person becomes unconscious\n• Learn CPR basics"
        ),
        FirstAidTip(
            "Fractures",
            "Immobilize the injured area",
            "• Don't try to straighten broken bones\n• Use splints or sturdy materials for support\n• Apply ice pack to reduce swelling\n• Keep person still and calm"
        )
    )

    // Emergency Kit Checklist
    val emergencyKitItems = remember { mutableStateListOf(
        ChecklistItem("Water (3 liters per person per day)", false),
        ChecklistItem("Non-perishable food (3-day supply)", false),
        ChecklistItem("First aid kit with medicines", false),
        ChecklistItem("Flashlight with extra batteries", false),
        ChecklistItem("Battery-powered radio", false),
        ChecklistItem("Important documents copies", false),
        ChecklistItem("Whistle to signal for help", false),
        ChecklistItem("Dust masks and plastic sheeting", false),
        ChecklistItem("Moist towelettes and sanitation items", false),
        ChecklistItem("Local maps and emergency contact numbers", false)
    )}

    // Quick Actions
    val quickActions = listOf(
        UtilityAction(
            "Emergency Contacts",
            "Save and manage emergency contacts",
            Icons.Default.Contacts,
            { navController.navigate("manage_contacts") }
        ),
        UtilityAction(
            "Weather Updates",
            "Check IMD weather alerts and forecasts",
            Icons.Default.Cloud,
            {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mausam.imd.gov.in"))
                context.startActivity(intent)
            }
        ),
        UtilityAction(
            "Disaster Alerts",
            "Government disaster management updates",
            Icons.Default.Notifications,
            {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ndma.gov.in"))
                context.startActivity(intent)
            }
        )
    )

    // Handle flashlight strobe effect
    LaunchedEffect(isStrobeOn) {
        if (isStrobeOn) {
            while (isActive && isStrobeOn) {
                val success = toggleFlashlight(context, true)
                if (!success) {
                    isStrobeOn = false
                    Toast.makeText(context, "Flashlight hardware is unavailable on this device.", Toast.LENGTH_SHORT).show()
                    break
                }
                delay(150)
                if (!isStrobeOn) break
                toggleFlashlight(context, false)
                delay(100)
                if (!isStrobeOn) break
                toggleFlashlight(context, true)
                delay(150)
                if (!isStrobeOn) break
                toggleFlashlight(context, false)
                delay(100)
                if (!isStrobeOn) break
                toggleFlashlight(context, true)
                delay(150)
                if (!isStrobeOn) break
                toggleFlashlight(context, false)
                delay(400)
            }
        } else {
            toggleFlashlight(context, false)
        }
    }

    // Stop strobe when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            isStrobeOn = false
            toggleFlashlight(context, false)
        }
    }

    // Haptic feedback function
    fun vibrate() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Safety Toolkit & Information",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emergency Tools Section
            SectionHeader("Emergency Tools")

            // Flashlight Card
            EmergencyToolCard(
                icon = Icons.Default.FlashlightOn,
                title = "Flashlight & SOS Strobe",
                content = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Torch Light",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isFlashlightOn,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    val success = toggleFlashlight(context, true)
                                    if (success) {
                                        isFlashlightOn = true
                                        if (isStrobeOn) isStrobeOn = false
                                    } else {
                                        isFlashlightOn = false
                                        Toast.makeText(context, "Flashlight hardware is unavailable on this device.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    isFlashlightOn = false
                                    toggleFlashlight(context, false)
                                }
                                vibrate()
                            }
                        )
                    }
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )

                    Button(
                        onClick = {
                            val nextStrobeState = !isStrobeOn
                            if (nextStrobeState) {
                                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                                val cameraId = cameraManager?.let { getFlashCameraId(it) }
                                if (cameraId == null) {
                                    Toast.makeText(context, "Flashlight hardware is unavailable on this device.", Toast.LENGTH_SHORT).show()
                                    isStrobeOn = false
                                } else {
                                    isStrobeOn = true
                                    if (isFlashlightOn) isFlashlightOn = false
                                }
                            } else {
                                isStrobeOn = false
                            }
                            vibrate()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStrobeOn) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = if (isStrobeOn) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    ) {
                        Text(
                            if (isStrobeOn) "🆘 Stop SOS Strobe" else "🆘 Start SOS Strobe",
                            color = if (isStrobeOn) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }
            )

            // Emergency Whistle Card
            EmergencyToolCard(
                icon = Icons.Default.MusicNote,
                title = "Emergency Whistle & Alarm",
                content = {
                    var isPlaying by remember { mutableStateOf(false) }
                    // Correctly uses R.raw.emergency_alarm from your project
                    val mediaPlayer = remember { MediaPlayer.create(context, R.raw.emergency_alarm) }

                    DisposableEffect(Unit) {
                        onDispose {
                            mediaPlayer.release()
                        }
                    }

                    Button(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer.pause()
                                mediaPlayer.seekTo(0)
                                isPlaying = false
                            } else {
                                mediaPlayer.start()
                                mediaPlayer.isLooping = true
                                isPlaying = true
                            }
                            vibrate()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPlaying) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = if (isPlaying) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    ) {
                        Text(
                            if (isPlaying) "🔇 Stop Alarm" else "🔊 Play Emergency Alarm",
                            color = if (isPlaying) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                }
            )

            // Share Location Card
            EmergencyToolCard(
                icon = Icons.Default.Share,
                title = "Share My Location",
                content = {
                    Button(
                        onClick = {
                            // **FIXED CODE**: This now gets the real location.
                            // Ensure you have location permissions in AndroidManifest.xml
                            // (ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION)
                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                            try {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    val mapLink = if (location != null) {
                                        "http://maps.google.com/maps?q=loc:$${location.latitude},${location.longitude}"
                                    } else {
                                        // Fallback if location is not immediately available
                                        "Location not available. Enable GPS."
                                    }
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT,
                                            "🚨 EMERGENCY: I need help! My current location is: $mapLink\n" +
                                                    "Sent via ResQTech Safety App"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Location via"))
                                }
                                vibrate()
                            } catch (e: SecurityException) {
                                e.printStackTrace()
                                // Optionally show a toast message to the user to enable permissions
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text(
                            "📍 Share Location with Emergency Contacts",
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            )

            // Quick Actions Section
            SectionHeader("Quick Actions")

            quickActions.forEach { action ->
                QuickActionCard(action = action, onVibrate = { vibrate() })
            }

            // First Aid Guide
            EmergencyToolCard(
                icon = Icons.Default.MedicalServices,
                title = "First Aid Guide",
                content = {
                    Button(
                        onClick = {
                            showFirstAidGuide = !showFirstAidGuide
                            showEmergencyKit = false
                            vibrate()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showFirstAidGuide) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (showFirstAidGuide) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Text(
                            if (showFirstAidGuide) "📖 Hide First Aid Guide" else "📖 Show First Aid Guide",
                            color = if (showFirstAidGuide) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )

            // Show First Aid Guide Content when toggled
            if (showFirstAidGuide) {
                EmergencyToolCard(
                    icon = Icons.Default.MedicalServices,
                    title = "First Aid Instructions",
                    content = {
                        Column {
                            firstAidTips.forEachIndexed { index, tip ->
                                FirstAidTipCard(
                                    tip = tip,
                                    isExpanded = expandedFirstAidIndex == index,
                                    onClick = {
                                        expandedFirstAidIndex = if (expandedFirstAidIndex == index) -1 else index
                                        vibrate()
                                    }
                                )
                                if (index < firstAidTips.size - 1) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Emergency Preparedness Kit
            EmergencyToolCard(
                icon = Icons.Default.Checklist,
                title = "Emergency Preparedness Kit",
                content = {
                    Button(
                        onClick = {
                            showEmergencyKit = !showEmergencyKit
                            showFirstAidGuide = false
                            vibrate()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (showEmergencyKit) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (showEmergencyKit) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    ) {
                        Text(
                            if (showEmergencyKit) "🎒 Hide Emergency Kit" else "🎒 Show Emergency Kit",
                            color = if (showEmergencyKit) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            )

            // Show Emergency Kit Checklist when toggled
            if (showEmergencyKit) {
                EmergencyToolCard(
                    icon = Icons.Default.Checklist,
                    title = "Emergency Kit Checklist",
                    content = {
                        Column {
                            emergencyKitItems.forEachIndexed { index, item ->
                                EmergencyKitItem(
                                    item = item,
                                    onCheckedChange = { checked ->
                                        emergencyKitItems[index] = item.copy(isChecked = checked)
                                        vibrate()
                                    }
                                )
                                if (index < emergencyKitItems.size - 1) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        emergencyKitItems.forEachIndexed { i, _ ->
                                            emergencyKitItems[i] = emergencyKitItems[i].copy(isChecked = true)
                                        }
                                        vibrate()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Text("Check All")
                                }

                                Button(
                                    onClick = {
                                        emergencyKitItems.forEachIndexed { i, _ ->
                                            emergencyKitItems[i] = emergencyKitItems[i].copy(isChecked = false)
                                        }
                                        vibrate()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text("Uncheck All")
                                }
                            }
                        }
                    }
                )
            }

            // Safety Tips Section
            SectionHeader("Safety Tips")

            safetyTips.forEachIndexed { index, tip ->
                SafetyTipCard(
                    tip = tip,
                    isExpanded = expandedTipIndex == index,
                    onClick = {
                        expandedTipIndex = if (expandedTipIndex == index) -1 else index
                        vibrate()
                    }
                )
            }

            // Emergency Numbers Section
            SectionHeader("Emergency Numbers")

            emergencyNumbers.forEach { emergencyContact ->
                EmergencyNumberCard(
                    emergencyContact = emergencyContact,
                    context = context,
                    onVibrate = { vibrate() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
fun EmergencyToolCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun QuickActionCard(action: UtilityAction, onVibrate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable {
                action.action()
                onVibrate()
            },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                action.icon,
                contentDescription = action.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    action.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    action.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Go",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SafetyTipCard(tip: SafetyTip, isExpanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    tip.icon,
                    contentDescription = tip.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    tip.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    tip.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FirstAidTipCard(tip: FirstAidTip, isExpanded: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = tip.title,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    tip.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Immediate Action: ${tip.immediateAction}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Detailed Steps:\n${tip.detailedSteps}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmergencyKitItem(item: ChecklistItem, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EmergencyNumberCard(emergencyContact: EmergencyContact, context: Context, onVibrate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${emergencyContact.number}"))
                    context.startActivity(intent)
                    onVibrate()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                emergencyContact.icon,
                contentDescription = emergencyContact.label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                emergencyContact.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                emergencyContact.number,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Default.Call,
                contentDescription = "Call",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Helper function to find a camera ID that supports a flash unit
private fun getFlashCameraId(cameraManager: CameraManager): String? {
    return try {
        val idList = cameraManager.cameraIdList
        if (idList.isEmpty()) return null

        // Look for rear camera with flash first
        for (id in idList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val isBack = characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            if (hasFlash && isBack) return id
        }

        // Fallback to any camera with flash
        idList.firstOrNull { id ->
            try {
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } catch (_: Exception) {
                false
            }
        }
    } catch (e: Exception) {
        null
    }
}

// Helper function to control the flashlight
private fun toggleFlashlight(context: Context, turnOn: Boolean): Boolean {
    return try {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        val cameraId = getFlashCameraId(cameraManager) ?: return false
        cameraManager.setTorchMode(cameraId, turnOn)
        true
    } catch (e: Exception) {
        false
    }
}