package com.example.fieldsense

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import com.example.fieldsense.data.AppDatabase
import com.example.fieldsense.data.FirestoreService
import com.example.fieldsense.data.Visit
import com.example.fieldsense.data.VisitRepository
import com.example.fieldsense.data.VisitViewModel
import com.example.fieldsense.data.VisitViewModelFactory
import com.example.fieldsense.data.NoteRepository
import com.example.fieldsense.data.NoteViewModel
import com.example.fieldsense.data.NoteViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val authRepository = AuthRepository(Firebase.auth)
        val authFactory = AuthViewModelFactory(authRepository)

        val database = AppDatabase.getDatabase(applicationContext)
        val firestoreService = FirestoreService()
        val visitRepository = VisitRepository(database.visitDao(), database.noteDao(), firestoreService, applicationContext)
        val visitFactory = VisitViewModelFactory(visitRepository)

        val noteRepository = NoteRepository(database.noteDao(), firestoreService)
        val noteFactory = NoteViewModelFactory(noteRepository)

        setContent {
            FieldSenseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val authViewModel: AuthViewModel = viewModel(factory = authFactory)
                    val visitViewModel: VisitViewModel = viewModel(factory = visitFactory)
                    val locationViewModel : LocationViewModel = viewModel()
                    val authState by authViewModel.authState.collectAsState()

                    when (authState) {
                        is AuthState.Authenticated -> {
                            NavigationBar (
                                email = authViewModel.getUserEmail(),
                                visitViewModel = visitViewModel,
                                onLogout = { authViewModel.signOut() },
                                locationViewModel = locationViewModel,
                                noteFactory = noteFactory
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
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isLoginMode by rememberSaveable { mutableStateOf(true) }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    email: String,
    visitViewModel: VisitViewModel,
    noteFactory: NoteViewModelFactory,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val visits by visitViewModel.visits.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var selectedVisitId by rememberSaveable() { mutableStateOf<Int?>(null) }
    var selectedVisit = visits.find { it.id == selectedVisitId }

    //loc
    var fetchedLocation by rememberSaveable { mutableStateOf("") }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val coroutineScope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            fetchedLocation = "${location.latitude}, ${location.longitude}"
                            coroutineScope.launch {
                                fetchedLocation = "Finding address..."
                                showAddDialog = true

                                fetchedLocation = getAddressFromLocation(
                                    context,
                                    location.latitude,
                                    location.longitude
                                )
                            }
                        } else {
                            fetchedLocation = "Location unavailable"
                            Toast.makeText(context, "Turn on GPS to get location", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        fetchedLocation = "Error fetching location"
                    }
            }
        } else {
            fetchedLocation = "Permission denied"
        }
    }

    // Se tiver visita selecionada, mostra o ecrã de detalhe
    val noteViewModel: NoteViewModel = viewModel(factory = noteFactory)
    if (selectedVisit != null) {
        VisitDetailScreen(
            visit = selectedVisit!!,
            visitViewModel = visitViewModel,
            noteViewModel = noteViewModel,
            onBack = { selectedVisitId = null }
        )
        return
    }
    LaunchedEffect(Unit) {
        visitViewModel.syncPendingVisits()
    }

    DisposableEffect(Unit) {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                visitViewModel.onNetworkRestored()
                noteViewModel.onNetworkRestored()
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FieldSense",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Log Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    fetchedLocation = "Fetching location..."
                    showAddDialog = true
                    // pedir permissão de loc
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New Visit")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        if (visits.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No visits recorded. Click '+' to add.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(visits) { visit ->
                    VisitCard(
                        visit = visit,
                        onDelete = { visitViewModel.deleteVisit(visit.id) },
                        onClick = { selectedVisitId = visit.id }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddVisitDialog(
                initialLocation = fetchedLocation, // Passamos a localização para o Diálogo!
                onDismiss = { showAddDialog = false },
                onConfirm = { code, name, loc ->
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    visitViewModel.insertVisit(code, name, currentDate, loc)
                    showAddDialog = false
                }
            )
        }
    }
}
@Composable
fun VisitCard(visit: Visit, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = visit.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (visit.isSynced) Icons.Filled.Check else Icons.Filled.Build,
                        contentDescription = if (visit.isSynced) "Synced" else "Pending Sync",
                        modifier = Modifier.size(16.dp),
                        tint = if (visit.isSynced) Color(0xFF4CAF50) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Code: ${visit.code}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Location: ${visit.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = visit.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (visit.isSynced) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Visit")
            }
        }
    }
}
@Composable
fun AddVisitDialog(
    initialLocation: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
){
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf(initialLocation) }

    LaunchedEffect(initialLocation) {
        location = initialLocation
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "New Visit",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Site Name") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (location == "Fetching location..." || location == "Finding address...") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code, name, location) },
                enabled = code.isNotBlank() &&
                          name.isNotBlank() &&
                          location.isNotBlank() &&
                          location != "Fetching location..." &&
                          location != "Finding address...",
                shape = MaterialTheme.shapes.small
            ) {
                Text("Save Visit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}



enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    MAIN("home", "", Icons.Default.Home, ""),
    MAP("map", "", Icons.Default.LocationOn, ""),
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    email: String,
    visitViewModel: VisitViewModel,
    locationViewModel: LocationViewModel,
    onLogout: () -> Unit,
    noteFactory: NoteViewModelFactory
) {
    NavHost(
        navController,
        startDestination = startDestination.route
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.MAIN -> MainScreen(
                        email = email,
                        visitViewModel = visitViewModel,
                        onLogout = onLogout,
                        noteFactory =  noteFactory)
                    Destination.MAP -> MapScreen(Modifier, locationViewModel)
                }
            }
        }
    }
}

@Composable
fun NavigationBar(
        modifier: Modifier = Modifier,
        email: String,
        visitViewModel: VisitViewModel,
        locationViewModel: LocationViewModel,
        onLogout: () -> Unit,
        noteFactory: NoteViewModelFactory) {
    val navController = rememberNavController()
    val startDestination = Destination.MAIN
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.contentDescription
                            )
                        }

                    )
                }
            }
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier.padding(contentPadding)
        ) {
            AppNavHost(
                navController,
                startDestination,
                email,
                visitViewModel,
                locationViewModel,
                onLogout,
                noteFactory)
        }

    }
}
