package com.example.team_gamma.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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

data class PermissionPage(
    val imageRes: Int,
    val title: String,
    val description: String,
    val permission: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    val pages = remember {
        listOf(
            PermissionPage(
                imageRes = R.drawable.ic_permission_location, // Corrected image
                title = "Location Permission",
                description = "To provide you with location-based alerts and share your precise location during an SOS.",
                permission = Manifest.permission.ACCESS_FINE_LOCATION
            ),
            PermissionPage(
                imageRes = R.drawable.ic_permission_notification,
                title = "Notification Permission",
                description = "To send you timely alerts and important safety notifications.",
                permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else "permission.POST_NOTIFICATIONS_DUMMY"
            ),
            PermissionPage(
                imageRes = R.drawable.ic_permission_contacts, // Corrected image
                title = "Contacts Permission",
                description = "To quickly notify your chosen emergency contacts for you when you activate the SOS feature.",
                permission = Manifest.permission.READ_CONTACTS
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    val permissionsToRequest = pages.map { it.permission }.filter { it != "permission.POST_NOTIFICATIONS_DUMMY" }.toTypedArray()

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        onPermissionsGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Skip",
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp, end = 24.dp)
                .clickable { onPermissionsGranted() },
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            PermissionPageContent(page = pages[pageIndex])
        }

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

        // --- CORRECTED BUTTON LAYOUT ---
        val isLastPage = pagerState.currentPage == pages.size - 1

        if (pagerState.currentPage == 0) {
            // ✅ FIX 1: On the first page, show a single, full-width "Next" button.
            Button(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .height(50.dp)
            ) {
                Text("Next", fontSize = 18.sp)
            }
        } else {
            // On subsequent pages, show "Back" and "Next"/"Grant" with equal size.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    // ✅ FIX 2: Both buttons now have equal weight.
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text("Back", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 18.sp)
                }

                // Next or Grant Permissions Button
                Button(
                    onClick = {
                        if (isLastPage) {
                            multiplePermissionsLauncher.launch(permissionsToRequest)
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    // ✅ FIX 2: Both buttons now have equal weight.
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (isLastPage) "Grant Permissions" else "Next",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = if (isLastPage) 12.sp else 18.sp
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

