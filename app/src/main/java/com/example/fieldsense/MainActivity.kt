package com.example.fieldsense

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fieldsense.data.local.AppDatabase
import com.example.fieldsense.data.remote.FirestoreService
import com.example.fieldsense.data.repository.NoteRepository
import com.example.fieldsense.data.repository.VisitRepository
import com.example.fieldsense.location.getAddressFromLocation
import com.example.fieldsense.ui.attachments.AttachmentViewModel
import com.example.fieldsense.ui.attachments.AttachmentViewModelFactory
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import com.cloudinary.android.MediaManager
import com.example.fieldsense.data.remote.CloudinaryService
import com.example.fieldsense.data.repository.AttachmentRepository
import com.example.fieldsense.data.repository.AuthRepository
import com.example.fieldsense.ui.visits.VisitDetailScreen
import com.example.fieldsense.ui.auth.AuthState
import com.example.fieldsense.ui.auth.AuthViewModel
import com.example.fieldsense.ui.auth.AuthViewModelFactory
import com.example.fieldsense.ui.auth.AuthenticationScreen
import com.example.fieldsense.ui.map.LocationViewModel
import com.example.fieldsense.ui.notes.NoteViewModel
import com.example.fieldsense.ui.notes.NoteViewModelFactory
import com.example.fieldsense.ui.theme.FieldSenseTheme
import com.example.fieldsense.ui.utils.AddVisitDialog
import com.example.fieldsense.ui.utils.NavigationBar
import com.example.fieldsense.ui.utils.VisitCard
import com.example.fieldsense.ui.visits.VisitViewModel
import com.example.fieldsense.ui.visits.VisitViewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val cloudinaryConfig = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            MediaManager.init(this, cloudinaryConfig)
        } catch (e: Exception) {
            // Already initialized
        }

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
                val authViewModel: AuthViewModel = viewModel(factory = authFactory)
                val visitViewModel: VisitViewModel = viewModel(factory = visitFactory)
                val locationViewModel : LocationViewModel = viewModel()
                val authState by authViewModel.authState.collectAsState()

                LaunchedEffect(authState) {
                    if (authState is AuthState.Authenticated) {
                        visitViewModel.setUserId(authRepository.getCurrentUserId())
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
    var selectedVisitId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedVisit = visits.find { it.id == selectedVisitId }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredVisits = remember(visits, searchQuery) {
        if (searchQuery.isEmpty()) visits
        else visits.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()
    val isFabExtended by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

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
                                fetchedLocation = "Finding address..."
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
            visit = selectedVisit,
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
    val collapsedFraction = scrollBehavior.state.collapsedFraction

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    // Usamos uma Column com alinhamento dinâmico
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        // Se quiser que ele centralize TOTALMENTE ao subir, usamos a fração
                        horizontalAlignment = if (collapsedFraction > 0.5f) Alignment.CenterHorizontally else Alignment.Start
                    ) {
                        Text(
                            "FieldSense",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.graphicsLayer {
                                val scale = lerp(1f, 0.8f, collapsedFraction)
                                scaleX = scale
                                scaleY = scale
                            }
                        )

                        // O subtítulo desaparece conforme você faz scroll
                        if (collapsedFraction < 0.2f) { // Sumir logo no início do scroll
                            Text(
                                "My Field Visits",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.graphicsLayer {
                                    // Faz o fade out suave
                                    alpha = 1f - (collapsedFraction * 5).coerceIn(0f, 1f)
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color(0xF0E8F5E9)
                )
            )
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
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
                expanded = isFabExtended,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Visit") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or code...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = CircleShape,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            if (filteredVisits.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (searchQuery.isEmpty()) Icons.Default.LocationOn else Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isEmpty()) "No visits recorded yet." else "No matches found.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (searchQuery.isEmpty()) "Tap 'New Visit' to start tracking." else "Try a different search term.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 12.dp, end = 12.dp)
                ) {
                    items(filteredVisits) { visit ->
                        VisitCard(
                            visit = visit,
                            onDelete = { visitViewModel.deleteVisit(visit.id) },
                            onClick = { selectedVisitId = visit.id }
                        )
                    }
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
    DOWNLOADED_MAPS("downloaded_map", "", Icons.Default.Download, ""),
}

