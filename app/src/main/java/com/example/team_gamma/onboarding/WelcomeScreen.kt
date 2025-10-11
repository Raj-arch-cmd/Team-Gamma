package com.example.team_gamma.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.team_gamma.R
import kotlinx.coroutines.launch

// ... (PermissionPage data class remains unchanged) ...
data class PermissionPage(
    val imageRes: Int,
    val title: String,
    val description: String,
    val permission: String // Added for easier permission management
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val pages = remember {
        listOf(
            PermissionPage(
                imageRes = R.drawable.ic_permission_contacts, // Make sure you have these drawables
                title = "Location Permission",
                description = "To provide you with location-based alerts and share your precise location during an SOS.",
                permission = Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PermissionPage(
                imageRes = R.drawable.ic_permission_notification, // Make sure you have these drawables
                title = "Notification Permission",
                description = "To send you timely alerts and important safety notifications.",
                permission = Manifest.permission.POST_NOTIFICATIONS // Android 13+
            ),
            PermissionPage(
                imageRes = R.drawable.ic_permission_location, // Make sure you have these drawables
                title = "Contacts Permission",
                description = "To quickly notify your chosen emergency contacts for you when you activate the SOS feature.",
                permission = Manifest.permission.READ_CONTACTS
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    // Combined permission launcher for all needed permissions
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.all { it.value }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            // Optional: Show a toast or dialog indicating not all permissions were granted
            // For simplicity, we'll just allow the user to retry or manually enable
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip Button - always visible
        Text(
            text = "Skip",
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp, end = 24.dp)
                // You might want to handle what 'skip' does in the first launch.
                // For now, it will simply go to the next screen without granting permissions.
                .clickable { onPermissionsGranted() }, // TODO: Revisit skip behavior if mandatory
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes available space
        ) { pageIndex ->
            val page = pages[pageIndex]
            PermissionPageContent(page = page)
        }

        // Pager indicator
        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            if (pagerState.currentPage > 0) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    // ✅ Fixed alignment for "Back" button text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Back", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f)) // Maintain spacing if no back button
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Grant Permissions / Next Button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        // This is the last page, request permissions
                        val permissionsToRequest = pages.map { it.permission }.toTypedArray()
                        multiplePermissionsLauncher.launch(permissionsToRequest)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.5f) // Make Grant Permissions button slightly wider
            ) {
                // ✅ Fixed alignment for "Grant Permissions" / "Next" button text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.size - 1) "Grant Permissions" else "Next",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}


@Composable
fun PermissionPageContent(page: PermissionPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = page.title,
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 32.dp)
        )
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

// Helper function to open app settings if permissions are denied permanently
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addCategory(Intent.CATEGORY_DEFAULT)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

// Helper function to check if a permission is granted
fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}