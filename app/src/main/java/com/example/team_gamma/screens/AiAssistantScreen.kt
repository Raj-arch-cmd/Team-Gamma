package com.example.team_gamma.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.team_gamma.data.AiAssistantViewModel

// Data class to represent a single message in the chat
data class ChatMessage(val text: String, val isFromUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel = hiltViewModel(), // Receive the ViewModel from Hilt
    onNavigateBack: () -> Unit
) {
    // Read the messages directly from the ViewModel
    val messages = viewModel.messages
    var userInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val suggestionChips = listOf(
        "What to do in a flood?",
        "First aid for burns",
        "How to create an emergency kit?"
    )

    // Scroll to the bottom whenever a new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Assistant") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            UserInputBar(
                onSend = { text ->
                    // Send the message via the ViewModel
                    viewModel.sendMessage(text)
                    userInput = ""
                    focusManager.clearFocus()
                },
                userInput = userInput,
                onUserInputChange = { userInput = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "AI Assistant",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Emergency AI Assistant",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ask questions about disaster preparedness, emergency protocols, or first aid guidance.",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SuggestionChipsRow(
                    suggestions = suggestionChips,
                    onSuggestionClick = { suggestion ->
                        viewModel.sendMessage(suggestion)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(messages) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isFromUser) 0.dp else 16.dp
                    )
                )
                .background(
                    if (message.isFromUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isFromUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInputBar(
    onSend: (String) -> Unit,
    userInput: String,
    onUserInputChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = userInput,
            onValueChange = onUserInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Ask a question...") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (userInput.isNotBlank()) {
                    onSend(userInput)
                }
            })
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = {
                if (userInput.isNotBlank()) {
                    onSend(userInput)
                }
            },
            enabled = userInput.isNotBlank()
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}

@Composable
fun SuggestionChipsRow(suggestions: List<String>, onSuggestionClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.take(3).forEach { suggestion ->
            SuggestionChip(text = suggestion, onClick = { onSuggestionClick(suggestion) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text, maxLines = 1) }
    )
}



