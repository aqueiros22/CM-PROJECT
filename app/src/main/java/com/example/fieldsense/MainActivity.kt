package com.example.fieldsense

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fieldsense.ui.theme.FieldSenseTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fieldsense.data.local.AppDatabase
import com.example.fieldsense.data.remote.FirestoreService
import com.example.fieldsense.data.repository.VisitRepository
import com.example.fieldsense.ui.visits.VisitViewModel
import com.example.fieldsense.ui.visits.VisitViewModelFactory
import com.example.fieldsense.data.repository.NoteRepository
import com.example.fieldsense.ui.notes.NoteViewModel
import com.example.fieldsense.ui.notes.NoteViewModelFactory
import com.example.fieldsense.ui.attachments.AttachmentViewModel
import com.example.fieldsense.ui.attachments.AttachmentViewModelFactory
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
import com.cloudinary.android.MediaManager
import com.example.fieldsense.data.remote.CloudinaryService
import com.example.fieldsense.data.repository.AttachmentRepository
import com.example.fieldsense.data.repository.AuthRepository
import com.example.fieldsense.ui.map.LocationViewModel
import com.example.fieldsense.ui.visits.VisitDetailScreen
import com.example.fieldsense.location.getAddressFromLocation
import com.example.fieldsense.ui.auth.AuthState
import com.example.fieldsense.ui.auth.AuthViewModel
import com.example.fieldsense.ui.auth.AuthViewModelFactory
import com.example.fieldsense.ui.auth.AuthenticationScreen
import com.example.fieldsense.ui.utils.AddVisitDialog
import com.example.fieldsense.ui.utils.NavigationBar
import com.example.fieldsense.ui.utils.VisitCard



class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val cloudinaryConfig = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY,
            "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
        )
        MediaManager.init(this, cloudinaryConfig)

        val authRepository = AuthRepository(Firebase.auth)
        val authFactory = AuthViewModelFactory(authRepository)

        val database = AppDatabase.getDatabase(applicationContext)
        val firestoreService = FirestoreService()

        val visitRepository = VisitRepository(database.visitDao(), database.noteDao(), firestoreService, applicationContext)
        val visitFactory = VisitViewModelFactory(visitRepository)

        val noteRepository = NoteRepository(database.noteDao(), firestoreService)
        val noteFactory = NoteViewModelFactory(noteRepository)

        val cloudinaryService = CloudinaryService(applicationContext)
        val attachmentRepository =
            AttachmentRepository(database.attachmentDao(), firestoreService, cloudinaryService)
        val attachmentFactory = AttachmentViewModelFactory(attachmentRepository)

        setContent {
            FieldSenseTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    val authViewModel: AuthViewModel = viewModel(factory = authFactory)
                    val visitViewModel: VisitViewModel = viewModel(factory = visitFactory)
                    val locationViewModel : LocationViewModel = viewModel()
                    val authState by authViewModel.authState.collectAsState()

                    LaunchedEffect(authState) {
                        if (authState is AuthState.Authenticated) {
                            visitViewModel.setUserId(authRepository.getCurrentUserId())
                        }
                    }

                    when (authState) {
                        is AuthState.Authenticated -> {
                            NavigationBar (
                                userId = authRepository.getCurrentUserId(),
                                email = authViewModel.getUserEmail(),
                                visitViewModel = visitViewModel,
                                attachmentFactory = attachmentFactory,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    userId: String,
    email: String,
    visitViewModel: VisitViewModel,
    noteFactory: NoteViewModelFactory,
    attachmentFactory: AttachmentViewModelFactory,
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
    val attachmentViewModel: AttachmentViewModel = viewModel(factory = attachmentFactory)
    LaunchedEffect(userId) {
        noteViewModel.setUserId(userId)
    }

    if (selectedVisit != null) {
        VisitDetailScreen(
            visit = selectedVisit!!,
            visitViewModel = visitViewModel,
            noteViewModel = noteViewModel,
            attachmentViewModel = attachmentViewModel,
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
                attachmentViewModel.onNetworkRestored()
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
                    visitViewModel.insertVisit(userId, code, name, currentDate, loc)
                    showAddDialog = false
                }
            )
        }
    }
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