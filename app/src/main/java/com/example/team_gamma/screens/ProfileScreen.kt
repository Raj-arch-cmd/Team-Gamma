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
import com.example.resqtech.R
import com.example.resqtech.data.ProfileViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
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

    // Image Picker - FIXED: Save image to app's internal storage
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                // Convert the content URI to a file path in app's internal storage
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
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Completion Card
            ProfileCompletionCard(completionPercentage = profileCompletion)

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Header with Image in Center
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// NEW FUNCTION: Save image to app's internal storage
private fun saveImageToInternalStorage(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "profile_image_$timeStamp.jpg"

        // Save to app's internal storage
        val file = File(context.filesDir, imageFileName)
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        // Return the file path that we can use later
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
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = completionPercentage / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (completionPercentage < 70) "Complete your profile for better emergency assistance"
                else "Great! Your profile is well maintained",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// FIXED: Update ProfileHeaderSection to handle file paths
@Composable
fun ProfileHeaderSection(
    userName: String,
    profileImageUri: String?,
    isEditing: Boolean,
    onEditImage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Image with Edit Button
        Box(
            modifier = Modifier.size(120.dp)
        ) {
            Card(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.Center),
                shape = CircleShape,
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                // FIXED: Handle both file paths and URIs
                if (!profileImageUri.isNullOrEmpty()) {
                    val imageModel = if (profileImageUri.startsWith("content://")) {
                        // It's a content URI (temporary)
                        Uri.parse(profileImageUri)
                    } else {
                        // It's a file path from internal storage (persistent)
                        File(profileImageUri)
                    }

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

            // Edit Image Button
            if (isEditing) {
                FloatingActionButton(
                    onClick = onEditImage,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.BottomEnd),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Edit Photo",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name and Blood Type
        Text(
            userName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
        // Private Information Section
        ProfileSectionCard(
            title = "Private Information",
            items = listOf(
                "Email" to email,
                "Birthdate" to dateOfBirth,
                "Gender" to gender,
                "Weight" to weight,
                "Height" to height
            )
        )

        // Contact Information Section
        ProfileSectionCard(
            title = "Contact Information",
            items = listOf(
                "Phone" to phone,
                "Emergency Contact" to emergencyContact,
                "Address" to address
            )
        )

        // Medical Information Section
        ProfileSectionCard(
            title = "Medical Information",
            items = listOf(
                "Blood Type" to bloodType,
                "Allergies" to allergies,
                "Medical Conditions" to medicalConditions
            )
        )

        // Emergency Instructions
        if (emergencyInstructions.isNotBlank()) {
            ProfileSectionCard(
                title = "Emergency Instructions",
                items = listOf("Special Instructions" to emergencyInstructions)
            )
        }
    }
}

@Composable
fun ProfileSectionCard(title: String, items: List<Pair<String, String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            items.forEach { (label, value) ->
                ProfileInfoRow(label = label, value = value)
                if (label != items.last().first) {
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
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
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
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
    // Create local state variables for each field to prevent cursor jumping
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

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Private Information Section
        ProfileEditSection(
            title = "Private Information",
            fields = listOf(
                EditField("Name", localUserName,
                    onValueChange = { newValue ->
                        localUserName = newValue
                        profileViewModel.updateUserName(newValue)
                    }
                ),
                EditField("Email", localEmail,
                    onValueChange = { newValue ->
                        localEmail = newValue
                        profileViewModel.updateEmail(newValue)
                    }
                ),
                EditField("Birthdate", localDateOfBirth,
                    onValueChange = { newValue ->
                        localDateOfBirth = newValue
                        profileViewModel.updateDateOfBirth(newValue)
                    }
                ),
                EditField("Gender", localGender,
                    onValueChange = { newValue ->
                        localGender = newValue
                        profileViewModel.updateGender(newValue)
                    }
                ),
                EditField("Weight", localWeight,
                    onValueChange = { newValue ->
                        localWeight = newValue
                        profileViewModel.updateWeight(newValue)
                    }
                ),
                EditField("Height", localHeight,
                    onValueChange = { newValue ->
                        localHeight = newValue
                        profileViewModel.updateHeight(newValue)
                    }
                )
            )
        )

        // Contact Information Section
        ProfileEditSection(
            title = "Contact Information",
            fields = listOf(
                EditField("Phone", localPhone,
                    onValueChange = { newValue ->
                        localPhone = newValue
                        profileViewModel.updatePhone(newValue)
                    }
                ),
                EditField("Emergency Contact", localEmergencyContact,
                    onValueChange = { newValue ->
                        localEmergencyContact = newValue
                        profileViewModel.updateEmergencyContact(newValue)
                    }
                ),
                EditField("Address", localAddress,
                    onValueChange = { newValue ->
                        localAddress = newValue
                        profileViewModel.updateAddress(newValue)
                    }
                )
            )
        )

        // Medical Information Section
        ProfileEditSection(
            title = "Medical Information",
            fields = listOf(
                EditField("Blood Type", localBloodType,
                    onValueChange = { newValue ->
                        localBloodType = newValue
                        profileViewModel.updateBloodType(newValue)
                    }
                ),
                EditField("Allergies", localAllergies,
                    onValueChange = { newValue ->
                        localAllergies = newValue
                        profileViewModel.updateAllergies(newValue)
                    }
                ),
                EditField("Medical Conditions", localMedicalConditions,
                    onValueChange = { newValue ->
                        localMedicalConditions = newValue
                        profileViewModel.updateMedicalConditions(newValue)
                    }
                ),
                EditField("Emergency Instructions", localEmergencyInstructions,
                    onValueChange = { newValue ->
                        localEmergencyInstructions = newValue
                        profileViewModel.updateEmergencyInstructions(newValue)
                    }
                )
            )
        )
    }
}

@Composable
fun ProfileEditSection(title: String, fields: List<EditField>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            fields.forEach { field ->
                OutlinedTextField(
                    value = field.value,
                    onValueChange = field.onValueChange,
                    label = { Text(field.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    singleLine = field.label != "Allergies" && field.label != "Medical Conditions" && field.label != "Emergency Instructions"
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