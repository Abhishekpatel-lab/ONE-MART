package com.example.nearbystoreapp.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    fun register(name: String, email: String, password: String, userType: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: run {
                    _authState.value = AuthState.Error("Registration failed. Please try again.")
                    return@launch
                }
                val userMap = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "userType" to userType,
                    "profilePic" to "",
                    "isBanned" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                database.child("users").child(uid).setValue(userMap).await()
                _authState.value = AuthState.Success(userType)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val uid = auth.currentUser?.uid

                // ✅ Fix: UID null = Error, Success nahi
                if (uid == null) {
                    _authState.value = AuthState.Error("Authentication failed. Please try again.")
                    return@launch
                }

                try {
                    val snapshot = database.child("users").child(uid).get().await()
                    val userType = snapshot.child("userType").value?.toString() ?: "user"
                    val isBanned = snapshot.child("isBanned").getValue(Boolean::class.java) ?: false
                    if (isBanned) {
                        auth.signOut()
                        _authState.value = AuthState.Error("You are banned for 30 days")
                    } else {
                        _authState.value = AuthState.Success(userType)
                    }
                } catch (e: Exception) {
                    // ✅ Fix: Network fail = Error dikhao, "user" assume mat karo
                    _authState.value = AuthState.Error("Network error. Please check your connection.")
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun googleSignIn(idToken: String, userType: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val uid = result.user?.uid ?: run {
                    _authState.value = AuthState.Error("Google Sign In failed. Please try again.")
                    return@launch
                }
                val email = result.user?.email ?: ""
                val name = result.user?.displayName ?: ""
                val photo = result.user?.photoUrl?.toString() ?: ""

                try {
                    val snapshot = database.child("users").child(uid).get().await()
                    if (!snapshot.exists()) {
                        val userMap = mapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "userType" to userType,
                            "profilePic" to photo,
                            "isBanned" to false,
                            "createdAt" to System.currentTimeMillis()
                        )
                        database.child("users").child(uid).setValue(userMap).await()
                        _authState.value = AuthState.Success(userType)
                    } else {
                        val isBanned = snapshot.child("isBanned").getValue(Boolean::class.java) ?: false
                        if (isBanned) {
                            auth.signOut()
                            _authState.value = AuthState.Error("You are banned for 30 days")
                        } else {
                            val existingType = snapshot.child("userType").value?.toString() ?: "user"
                            _authState.value = AuthState.Success(existingType)
                        }
                    }
                } catch (e: Exception) {
                    // ✅ Fix: Database fail = Error, silent success nahi
                    _authState.value = AuthState.Error("Network error. Please check your connection.")
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Google Sign In failed")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _authState.value = AuthState.Idle
    }

    fun checkCurrentUser() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val snapshot = database.child("users").child(uid).get().await()
                    val userType = snapshot.child("userType").value?.toString() ?: "user"
                    val isBanned = snapshot.child("isBanned").getValue(Boolean::class.java) ?: false
                    if (isBanned) {
                        auth.signOut()
                        _authState.value = AuthState.Idle
                    } else {
                        _authState.value = AuthState.Success(userType)
                    }
                } catch (e: Exception) {
                    // ✅ Fix: Network fail on startup = Idle rakho, crash mat karo
                    _authState.value = AuthState.Idle
                }
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userType: String) : AuthState()
    data class Error(val message: String) : AuthState()
}