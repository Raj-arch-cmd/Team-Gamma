package com.example.resqtech.ui.theme.screens


import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team_gamma.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoudAlarmScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    // A simple way to manage the MediaPlayer instance across recompositions.
    val mediaPlayer = remember {
        MediaPlayer.create(context, R.raw.emergency_alarm)
    }

    // This DisposableEffect will clean up the MediaPlayer when the screen is left.
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Loud Alarm") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Activate this alarm to attract attention or deter threats.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (isPlaying) {
                        mediaPlayer.pause()
                        mediaPlayer.seekTo(0) // Rewind to the beginning
                    } else {
                        mediaPlayer.isLooping = true // Make the alarm loop continuously
                        mediaPlayer.start()
                    }
                    isPlaying = !isPlaying
                },
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPlaying) Color.Gray else Color.Red
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.StopCircle else Icons.Default.Campaign,
                        contentDescription = if (isPlaying) "Stop Alarm" else "Play Alarm",
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isPlaying) "STOP" else "START",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            if (isPlaying) {
                Text(
                    "ALARM ACTIVATED",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}

