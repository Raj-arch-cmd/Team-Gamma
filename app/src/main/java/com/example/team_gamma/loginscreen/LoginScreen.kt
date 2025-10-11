package com.example.team_gamma.loginscreen

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.team_gamma.R
import com.example.team_gamma.auth.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    // ✅ Observe auth state
    LaunchedEffect(authState) {
        when (authState) {
            is AuthViewModel.AuthState.Success -> {
                Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            }
            is AuthViewModel.AuthState.Error -> {
                val errorMessage = (authState as AuthViewModel.AuthState.Error).message
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                authViewModel.resetAuthState()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- Logo Section ---
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )
        Text("ResQTech", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Disaster Management App", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        Spacer(modifier = Modifier.height(48.dp))

        Text("Login to your Account", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        // --- Email Field ---
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // --- Password Field ---
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val icon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(icon, contentDescription = null)
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        val isLoading = authState is AuthViewModel.AuthState.Loading

        Button(
            onClick = {
                if (email.isNotBlank() && password.isNotBlank()) {
                    if (!isValidEmail(email)) {
                        Toast.makeText(context, "Enter valid email", Toast.LENGTH_SHORT).show()
                    } else {
                        authViewModel.loginUser(email, password)
                    }
                } else {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("SIGN IN", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            Text("Don't have an account? ")
            ClickableText(
                text = AnnotatedString("Create Account"),
                onClick = { navController.navigate("signup") },
                style = TextStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            )
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    val pattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    return email.matches(pattern.toRegex())
}
