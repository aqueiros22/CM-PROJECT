package com.example.fieldsense

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fieldsense.ui.theme.FieldSenseTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.lifecycle.viewmodel.compose.viewModel



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val authRepository = AuthRepository(Firebase.auth)
        val factory = AuthViewModelFactory(authRepository)

        setContent {
            FieldSenseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val authViewModel: AuthViewModel = viewModel(factory = factory)
                    val authState by authViewModel.authState.collectAsState()

                    when (authState) {
                        is AuthState.Authenticated -> {
                            MainScreen(
                                email = authViewModel.getUserEmail(),
                                modifier = Modifier.padding(innerPadding),
                                onLogout = { authViewModel.signOut() }
                            )
                        }
                        else -> {
                            AuthenticationScreen(
                                authViewModel = authViewModel,
                                authState = authState,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthenticationScreen(
    authViewModel: AuthViewModel,
    authState: AuthState,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }

    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            Toast.makeText(context, authState.message, Toast.LENGTH_SHORT).show()
        }
    }

    val token = stringResource(R.string.default_web_client_id)
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(token)
        .requestEmail()
        .build()

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    authViewModel.authenticateWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In Failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = if (isLoginMode) "Sign In" else "Create Account",
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = authState !is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            enabled = authState !is AuthState.Loading
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                authViewModel.authenticateWithEmail(email, password, isLoginMode)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.small,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (isLoginMode) "Continue" else "Register",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { isLoginMode = !isLoginMode },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = authState !is AuthState.Loading
        ) {
            Text(
                text = if (isLoginMode) "Don't have an account? Sign up" else "Already have an account? Sign in",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(" OR ", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { launcher.launch(googleSignInClient.signInIntent) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.small,
            enabled = authState !is AuthState.Loading
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continue with Google",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun MainScreen(email: String, modifier: Modifier = Modifier, onLogout: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to FieldSense!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Logged in as: $email",
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            shape = MaterialTheme.shapes.small
        ) {
            Text("Log Out")
        }
    }
}