package com.example.team_gamma.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.team_gamma.R
import com.example.team_gamma.auth.AuthViewModel
import com.example.team_gamma.data.ProfileViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    navController: NavController
) {
    val context = LocalContext.current

    // Collect StateFlow values
    val userName by profileViewModel.userName.collectAsState()
    val bloodType by profileViewModel.bloodType.collectAsState()
    val allergies by profileViewModel.allergies.collectAsState()
    val medicalConditions by profileViewModel.medicalConditions.collectAsState()
    val email by profileViewModel.email.collectAsState()
    val phone by profileViewModel.phone.collectAsState()
    val emergencyContact by profileViewModel.emergencyContact.collectAsState()
    val dateOfBirth by profileViewModel.dateOfBirth.collectAsState()
    val gender by profileViewModel.gender.collectAsState()
    val weight by profileViewModel.weight.collectAsState()
    val height by profileViewModel.height.collectAsState()
    val address by profileViewModel.address.collectAsState()
    val emergencyInstructions by profileViewModel.emergencyInstructions.collectAsState()
    val profileImageUri by profileViewModel.profileImageUri.collectAsState()
    val profileCompletion by profileViewModel.profileCompletion.collectAsState()

    var isEditing by remember { mutableStateOf(false) }

    // Image Picker - Save image to internal storage
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                val filePath = saveImageToInternalStorage(context, selectedUri)
                filePath?.let { path ->
                    profileViewModel.updateProfileImageUri(path)
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Profile",
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
                actions = {
                    IconButton(onClick = {
                        isEditing = !isEditing
                    }) {
                        Icon(
                            if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save" else "Edit",
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Completion
            ProfileCompletionCard(completionPercentage = profileCompletion)

            Spacer(modifier = Modifier.height(16.dp))

            // Header Section
            ProfileHeaderSection(
                userName = userName,
                profileImageUri = profileImageUri,
                isEditing = isEditing,
                onEditImage = { imagePicker.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isEditing) {
                EditProfileForm(
                    profileViewModel = profileViewModel,
                    userName = userName,
                    email = email,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    weight = weight,
                    height = height,
                    phone = phone,
                    emergencyContact = emergencyContact,
                    bloodType = bloodType,
                    allergies = allergies,
                    medicalConditions = medicalConditions,
                    address = address,
                    emergencyInstructions = emergencyInstructions
                )
            } else {
                ViewProfileDetails(
                    userName = userName,
                    email = email,
                    dateOfBirth = dateOfBirth,
                    gender = gender,
                    weight = weight,
                    height = height,
                    phone = phone,
                    emergencyContact = emergencyContact,
                    bloodType = bloodType,
                    allergies = allergies,
                    medicalConditions = medicalConditions,
                    address = address,
                    emergencyInstructions = emergencyInstructions
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ✅ Logout Button (Full width and styled)
            Button(
                onClick = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Logout",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Save image to internal storage
private fun saveImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "profile_image_$timeStamp.jpg"
        val file = File(context.filesDir, imageFileName)
        val outputStream = FileOutputStream(file)
        inputStream?.use { input -> outputStream.use { output -> input.copyTo(output) } }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun ProfileCompletionCard(completionPercentage: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Profile Completion: $completionPercentage%",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = completionPercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProfileHeaderSection(
    userName: String,
    profileImageUri: String?,
    isEditing: Boolean,
    onEditImage: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(120.dp)) {
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.Center),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                if (!profileImageUri.isNullOrEmpty()) {
                    val imageModel = if (profileImageUri.startsWith("content://")) {
                        Uri.parse(profileImageUri)
                    } else File(profileImageUri)

                    Image(
                        painter = rememberAsyncImagePainter(model = imageModel),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.map_placeholder),
                        contentDescription = "Default Profile",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            if (isEditing) {
                FloatingActionButton(
                    onClick = onEditImage,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Edit Photo")
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ViewProfileDetails(
    userName: String,
    email: String,
    dateOfBirth: String,
    gender: String,
    weight: String,
    height: String,
    phone: String,
    emergencyContact: String,
    bloodType: String,
    allergies: String,
    medicalConditions: String,
    address: String,
    emergencyInstructions: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ProfileSectionCard("Private Information", listOf(
            "Email" to email, "Birthdate" to dateOfBirth,
            "Gender" to gender, "Weight" to weight, "Height" to height
        ))
        ProfileSectionCard("Contact Information", listOf(
            "Phone" to phone, "Emergency Contact" to emergencyContact, "Address" to address
        ))
        ProfileSectionCard("Medical Information", listOf(
            "Blood Type" to bloodType, "Allergies" to allergies, "Medical Conditions" to medicalConditions
        ))
        if (emergencyInstructions.isNotBlank()) {
            ProfileSectionCard("Emergency Instructions", listOf("Special Instructions" to emergencyInstructions))
        }
    }
}

@Composable
fun ProfileSectionCard(title: String, items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            items.forEachIndexed { index, (label, value) ->
                ProfileInfoRow(label, value)
                if (index != items.lastIndex) Divider(modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value)
    }
}

@Composable
fun EditProfileForm(
    profileViewModel: ProfileViewModel,
    userName: String,
    email: String,
    dateOfBirth: String,
    gender: String,
    weight: String,
    height: String,
    phone: String,
    emergencyContact: String,
    bloodType: String,
    allergies: String,
    medicalConditions: String,
    address: String,
    emergencyInstructions: String
) {
    var localUserName by remember { mutableStateOf(userName) }
    var localEmail by remember { mutableStateOf(email) }
    var localDateOfBirth by remember { mutableStateOf(dateOfBirth) }
    var localGender by remember { mutableStateOf(gender) }
    var localWeight by remember { mutableStateOf(weight) }
    var localHeight by remember { mutableStateOf(height) }
    var localPhone by remember { mutableStateOf(phone) }
    var localEmergencyContact by remember { mutableStateOf(emergencyContact) }
    var localBloodType by remember { mutableStateOf(bloodType) }
    var localAllergies by remember { mutableStateOf(allergies) }
    var localMedicalConditions by remember { mutableStateOf(medicalConditions) }
    var localAddress by remember { mutableStateOf(address) }
    var localEmergencyInstructions by remember { mutableStateOf(emergencyInstructions) }

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ProfileEditSection("Private Information", listOf(
            EditField("Name", localUserName) {
                localUserName = it; profileViewModel.updateUserName(it)
            },
            EditField("Email", localEmail) {
                localEmail = it; profileViewModel.updateEmail(it)
            },
            EditField("Birthdate", localDateOfBirth) {
                localDateOfBirth = it; profileViewModel.updateDateOfBirth(it)
            },
            EditField("Gender", localGender) {
                localGender = it; profileViewModel.updateGender(it)
            },
            EditField("Weight", localWeight) {
                localWeight = it; profileViewModel.updateWeight(it)
            },
            EditField("Height", localHeight) {
                localHeight = it; profileViewModel.updateHeight(it)
            }
        ))

        ProfileEditSection("Contact Information", listOf(
            EditField("Phone", localPhone) {
                localPhone = it; profileViewModel.updatePhone(it)
            },
            EditField("Emergency Contact", localEmergencyContact) {
                localEmergencyContact = it; profileViewModel.updateEmergencyContact(it)
            },
            EditField("Address", localAddress) {
                localAddress = it; profileViewModel.updateAddress(it)
            }
        ))

        ProfileEditSection("Medical Information", listOf(
            EditField("Blood Type", localBloodType) {
                localBloodType = it; profileViewModel.updateBloodType(it)
            },
            EditField("Allergies", localAllergies) {
                localAllergies = it; profileViewModel.updateAllergies(it)
            },
            EditField("Medical Conditions", localMedicalConditions) {
                localMedicalConditions = it; profileViewModel.updateMedicalConditions(it)
            },
            EditField("Emergency Instructions", localEmergencyInstructions) {
                localEmergencyInstructions = it; profileViewModel.updateEmergencyInstructions(it)
            }
        ))
    }
}

@Composable
fun ProfileEditSection(title: String, fields: List<EditField>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            fields.forEach { field ->
                OutlinedTextField(
                    value = field.value,
                    onValueChange = field.onValueChange,
                    label = { Text(field.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    singleLine = field.label !in listOf("Allergies", "Medical Conditions", "Emergency Instructions")
                )
            }
        }
    }
}

data class EditField(
    val label: String,
    val value: String,
    val onValueChange: (String) -> Unit
)
