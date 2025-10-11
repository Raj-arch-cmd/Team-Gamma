package com.example.team_gamma.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp

import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Data class to hold the content for each onboarding page
data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

// The list of pages for our onboarding flow
val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.LocationOn,
        title = "Location Permission",
        description = "We need your location to send hyper-local alerts and guide you to safety, even when the app is in the background."
    ),
    OnboardingPage(
        icon = Icons.Default.Notifications,
        title = "Notification Permission",
        description = "This allows us to send you critical, life-saving alerts in real-time during an emergency."
    ),
    OnboardingPage(
        icon = Icons.Default.Contacts,
        title = "Contacts Permission",
        description = "To quickly notify your chosen emergency contacts for you when you activate the SOS feature."
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onPermissionsGranted: () -> Unit) {

    val permissionsToRequest = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_CONTACTS
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.values.all { it }) {
                onPermissionsGranted()
            }
            // Optional: Handle the case where permissions are denied
        }
    )

    val pagerState = rememberPagerState { onboardingPages.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }) {
                        Text("Skip")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page indicators (dots)
                Row(
                    Modifier.padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(pagerState.pageCount) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(color)
                                .size(10.dp)
                        )
                    }
                }

                // --- REFINED BUTTON LOGIC ---
                val isLastPage = pagerState.currentPage == onboardingPages.size - 1

                if (pagerState.currentPage == 0) {
                    // First Page: Show only a "Next" button
                    Button(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = tween(durationMillis = 500) // Added animation spec
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Next", fontSize = 18.sp)
                    }
                } else {
                    // Subsequent Pages: Show "Back" and "Next" or "Grant"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button - Now a styled Button
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        page = pagerState.currentPage - 1,
                                        animationSpec = tween(durationMillis = 500) // Added animation spec
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Back", fontSize = 18.sp)
                        }

                        // Next or Grant Permissions Button
                        Button(
                            onClick = {
                                scope.launch {
                                    if (isLastPage) {
                                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                                    } else {
                                        pagerState.animateScrollToPage(
                                            page = pagerState.currentPage + 1,
                                            animationSpec = tween(durationMillis = 500) // Added animation spec
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            val buttonText = if (isLastPage) "Grant Permissions" else "Next"
                            // Reduce font size for the longer text to prevent awkward wrapping
                            val fontSize = if (isLastPage) 15.sp else 18.sp
                            Text(
                                text = buttonText,
                                fontSize = fontSize,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            val page = onboardingPages[pageIndex]

            // --- ANIMATION LOGIC ---
            // Calculate the offset of the page from the center
            val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction

            // Apply transformations based on the offset
            OnboardingPageContent(
                page = page,
                modifier = Modifier.graphicsLayer {
                    // Fade effect: Fades out as it moves away from the center
                    alpha = lerp(
                        start = 0.4f,
                        stop = 1f,
                        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                    )
                    // Scale effect: Shrinks as it moves away from the center
                    val scale = lerp(
                        start = 0.85f,
                        stop = 1f,
                        fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                    )
                    scaleX = scale
                    scaleY = scale
                }
            )
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier // Apply the incoming modifier here for animations
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = page.title,
            modifier = Modifier.size(150.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
