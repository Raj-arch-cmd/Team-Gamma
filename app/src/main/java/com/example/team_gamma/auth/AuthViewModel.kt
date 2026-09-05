package com.example.team_gamma.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_gamma.data.isNetworkAvailable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Firebase Authentication using StateFlow
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Firebase authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Holds currently logged-in Firebase user
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    // Authentication state (Idle, Loading, Success, Error)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    init {
        // ✅ Listen for login/logout automatically
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user

            Log.d("AuthViewModel", "Auth state changed: ${user?.email ?: "No user"}")

            _authState.value = if (user != null) {
                AuthState.Success(user.uid)
            } else {
                AuthState.Idle
            }
        }
    }

    /**
     * Sign up new user with email and password
     */
    fun signUp(email: String, password: String) {
        if (!isNetworkAvailable(context)) {
            _authState.value = AuthState.Error("Offline Mode: Unable to connect to network. Please check your internet connection.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        _currentUser.value = user
                        _authState.value = AuthState.Success(user?.uid ?: "")
                        Log.d("AuthViewModel", "SignUp successful: ${user?.email}")
                    } else {
                        _authState.value = AuthState.Error(task.exception?.message ?: "Signup failed")
                        Log.e("AuthViewModel", "SignUp error: ${task.exception?.message}")
                    }
                }
        }
    }

    /**
     * Login existing user
     */
    fun login(email: String, password: String) {
        if (!isNetworkAvailable(context)) {
            _authState.value = AuthState.Error("Offline Mode: Unable to connect to network. Please check your internet connection.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        _currentUser.value = user
                        _authState.value = AuthState.Success(user?.uid ?: "")
                        Log.d("AuthViewModel", "Login successful: ${user?.email}")
                    } else {
                        _authState.value = AuthState.Error(task.exception?.message ?: "Login failed")
                        Log.e("AuthViewModel", "Login error: ${task.exception?.message}")
                    }
                }
        }
    }

    /**
     * Logout user
     */
    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _authState.value = AuthState.Idle
        Log.d("AuthViewModel", "User logged out")
    }

    // 👇 These functions match your UI’s function names
    fun loginUser(email: String, password: String) = login(email, password)
    fun registerUser(fullName: String, email: String, password: String) = signUp(email, password)
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    /**
     * Represents different authentication states
     */
    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val userId: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }
}
