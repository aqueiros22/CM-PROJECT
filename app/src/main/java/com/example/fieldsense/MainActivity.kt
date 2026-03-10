package com.example.fieldsense

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.fieldsense.ui.theme.FieldSenseTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    // Declare the Firebase Auth variable
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase Auth
        auth = Firebase.auth

        setContent {
            FieldSenseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // State to track if the user is already logged in
                    var isUserLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

                    if (isUserLoggedIn) {
                        // USER IS LOGGED IN (Works Offline!)
                        MainScreen(
                            email = auth.currentUser?.email ?: "Unknown",
                            modifier = Modifier.padding(innerPadding),
                            onLogout = {
                                auth.signOut()
                                isUserLoggedIn = false // Go back to login screen
                            }
                        )
                    } else {
                        // NO SESSION - Show Login/Register screen
                        AuthenticationScreen(
                            auth = auth,
                            modifier = Modifier.padding(innerPadding),
                            onAuthenticationSuccess = {
                                isUserLoggedIn = true // Switch to main screen
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuthenticationScreen(
    auth: FirebaseAuth,
    modifier: Modifier = Modifier,
    onAuthenticationSuccess: () -> Unit
) {
    // State variables to hold user input
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Variable to toggle between "Login" and "Register" modes
    var isLoginMode by remember { mutableStateOf(true) }

    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Sign In" else "Create New Account",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (email.isNotEmpty() && password.isNotEmpty()) {
                    if (isLoginMode) {
                        // SIGN IN
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onAuthenticationSuccess()
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        // REGISTER NEW ACCOUNT
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "Account created!", Toast.LENGTH_SHORT).show()
                                    onAuthenticationSuccess()
                                } else {
                                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(context, "Please fill in all fields!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoginMode) "Login" else "Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to toggle mode (Login <-> Register)
        TextButton(onClick = { isLoginMode = !isLoginMode }) {
            Text(if (isLoginMode) "Don't have an account? Register here." else "Already have an account? Sign in.")
        }
    }
}

@Composable
fun MainScreen(email: String, modifier: Modifier = Modifier, onLogout: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to FieldSense!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Logged in as: $email")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onLogout) {
            Text("Log Out")
        }
    }
}