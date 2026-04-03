package com.example.fieldsense.ui.map

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.MainActivity
import com.example.fieldsense.location.LocationHelper
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapScreen(modifier: Modifier = Modifier, viewModel: LocationViewModel, onNavigateToOfflineMap: () -> Unit) {
    val context = LocalContext.current
    val myLocationHelper = LocationHelper(context)
    DisplayLocation(modifier = modifier, myLocationHelper = myLocationHelper, viewModel, context = context, onNavigateToOfflineMap = onNavigateToOfflineMap)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayLocation(
    modifier: Modifier,
    myLocationHelper: LocationHelper,
    viewModel: LocationViewModel,
    context: Context,
    onNavigateToOfflineMap: () -> Unit
) {
    val location by viewModel.location
    val isFetching = viewModel.isFetchingLocation
    
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(-9.1399, 38.7169), // Lisbon default
            zoom = 5.0
        )
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Flag to track if we should animate to the next received location
    var shouldAnimateToLocation by remember { mutableStateOf(false) }

    // Effect to automatically move camera when location is updated
    LaunchedEffect(location) {
        if (shouldAnimateToLocation && location != null) {
            cameraState.animateTo(
                CameraPosition(
                    target = Position(location!!.longitude, location!!.latitude),
                    zoom = 15.0
                )
            )
            shouldAnimateToLocation = false
        }
    }

    val textFieldState: TextFieldState = rememberTextFieldState()
    val onSearch: (String) -> Unit =  { query -> viewModel.search(query) }
    val searchResults: List<String> = viewModel.searchResults.map { it.placeName }
    
    val onResultSelected: (String) -> Unit = { placeName: String ->
        val feature = viewModel.searchResults.find { it.placeName == placeName }
        feature?.let {
            val longitude = feature.center[0]
            val latitude = feature.center[1]
            coroutineScope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(longitude, latitude),
                        zoom = 16.0
                    )
                )
            }
        } ?: run {
            Toast.makeText(context, "Localização não encontrada, tente novamente", Toast.LENGTH_LONG).show()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if ( permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                shouldAnimateToLocation = true
                viewModel.startFetchingLocation()
                myLocationHelper.requestLocationUpdates(viewModel= viewModel)
            } else {
                val rationaleRequired = ActivityCompat.shouldShowRequestPermissionRationale(
                    context as MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) ||
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                if(rationaleRequired){
                    Toast.makeText(
                        context,
                        "Esta funcionalidade requer permissão de localização",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context,
                        "Por favor, ative a permissão de localização nas definições do telefone",
                        Toast.LENGTH_LONG).show()
                }
            }
        })

    Box(modifier = modifier.fillMaxSize()){
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
            cameraState = cameraState
        ) {
            location?.let { latLng ->
                val locationSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        FeatureCollection(
                            Feature(
                                geometry = Point(Position(latLng.longitude, latLng.latitude)),
                                properties = JsonObject(content = emptyMap())
                            )
                        )
                    )
                )
                CircleLayer(
                    id = "user-location-ring",
                    source = locationSource,
                    radius = const(18.dp),
                    color = const(Color(0x554285F4)),
                    strokeWidth = const(0.dp)
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Search Bar Overlay
            SimpleSearchBar(
                textFieldState,
                onSearch,
                searchResults,
                viewModel = viewModel,
                onResultSelected = onResultSelected
            )

            // Location Info Card (Center below search)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("A obter localização...", color = Color.White, fontSize = 14.sp)
                        } else if (location != null) {
                            Icon(Icons.Default.GpsFixed, null, modifier = Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "lat: ${"%.4f".format(location!!.latitude)}, long: ${"%.4f".format(location!!.longitude)}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text("Localização indisponível", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Bottom Controls
        Column( 
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom
        ) {
            // My Location FAB
            FloatingActionButton(
                onClick = {
                    if (myLocationHelper.hasLocationPermission(context)){
                        shouldAnimateToLocation = true
                        viewModel.startFetchingLocation()
                        myLocationHelper.requestLocationUpdates(viewModel= viewModel)
                    }
                    else {
                        requestPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (isFetching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização")
                }
            }

            Button(
                onClick = onNavigateToOfflineMap,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Download, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mapas Offline", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    searchResults: List<String>,
    onResultSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel,
) {
    var expanded by rememberSaveable{ mutableStateOf(false)}

    Box(
        modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                        viewModel.clearResults()
                                    },
                    onSearch = {
                        viewModel.clearResults()
                        onSearch(textFieldState.text.toString())
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Pesquisar local...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (textFieldState.text.isNotEmpty()) {
                            IconButton(onClick = { textFieldState.edit { replace(0, length, "") } }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                searchResults.forEach { result ->
                    ListItem(
                        headlineContent = { Text(result) },
                        modifier = Modifier
                            .clickable {
                                textFieldState.edit { replace(0, length, result) }
                                expanded = false
                                onResultSelected(result)
                            }
                            .fillMaxWidth()
                    )
                }
            }
        }
    }
}