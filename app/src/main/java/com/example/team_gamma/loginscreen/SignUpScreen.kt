package com.example.team_gamma.loginscreen


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun SignUpScreen(navController: NavController, onSignUpSuccess: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val auth: FirebaseAuth = remember { Firebase.auth }
    val db = remember { Firebase.firestore }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
                    if (!isValidEmail(email)) {
                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (password.length < 6) {
                        Toast.makeText(context, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val uid = user?.uid
                                if (uid != null) {
                                    val userProfile = hashMapOf(
                                        "fullName" to fullName,
                                        "email" to email,
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    db.collection("users").document(uid)
                                        .set(userProfile)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onSignUpSuccess()
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            Toast.makeText(context, "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "User creation failed", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
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
                Text("CREATE ACCOUNT", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row {
            Text("Already have an account? ")
            ClickableText(
                text = AnnotatedString("Login"),
                onClick = {
                    navController.navigate("login") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                style = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun isValidEmail(email: String): Boolean {
    val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    return email.matches(emailPattern.toRegex())
}