package com.example.fieldsense.ui.map

import android.location.Geocoder
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.ui.theme.Shapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.offline.OfflineManager
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import java.util.*

@Composable
fun BoundingBoxOverlay(state: BoundingBoxState) {
    val p1 = state.point1
    val p2 = state.point2

    val pointSource = rememberGeoJsonSource(data = state.getPointsGeoJson())

    if (p1 != null && p2 != null) {
        val minLat = minOf(p1.latitude, p2.latitude)
        val maxLat = maxOf(p1.latitude, p2.latitude)
        val minLng = minOf(p1.longitude, p2.longitude)
        val maxLng = maxOf(p1.longitude, p2.longitude)

        val geoJson = """
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [[
                  [$minLng, $minLat],
                  [$maxLng, $minLat],
                  [$maxLng, $maxLat],
                  [$minLng, $maxLat],
                  [$minLng, $minLat]
                ]]
              }
            }
        """.trimIndent()

        val polygonSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson))

        FillLayer(
            id = "polygon-fill",
            source = polygonSource,
            color = const(Color(0xFF4A90D9)),
            opacity = const(0.25f)
        )
        LineLayer(
            id = "polygon-outlier",
            source = polygonSource,
            color = const(Color(0xFF4A90D9))

        )

    }
    CircleLayer(
        id = "points-ring",
        source = pointSource,
        radius = const(18.dp),
        color = const(Color(0x554285F4)),  // semi-transparent blue
        strokeWidth = const(0.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapScreen(
    onBack: () -> Unit,
    offlineManager: OfflineManager,
    viewModel: BoundingBoxViewModel = viewModel(),
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current
    val state = viewModel.state
    val userLocation by locationViewModel.location
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = userLocation?.let { Position(it.longitude, it.latitude) } ?: Position(-9.1399, 38.7169),
            zoom = if (userLocation != null) 13.0 else 5.0
        )
    )

    // Flag to ensure we only auto-center once when the screen opens
    var hasCenteredInitially by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation) {
        if (!hasCenteredInitially && userLocation != null) {
            cameraState.animateTo(
                CameraPosition(
                    target = Position(userLocation!!.longitude, userLocation!!.latitude),
                    zoom = 13.0
                )
            )
            hasCenteredInitially = true
        }
    }

    fun performSearch() {
        if (searchQuery.isBlank()) return
        isSearching = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(searchQuery, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    withContext(Dispatchers.Main) {
                        cameraState.animateTo(
                            CameraPosition(
                                target = Position(address.longitude, address.latitude),
                                zoom = 15.0
                            )
                        )
                        isSearching = false
                        focusManager.clearFocus()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Morada não encontrada", Toast.LENGTH_SHORT).show()
                        isSearching = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Erro na pesquisa", Toast.LENGTH_SHORT).show()
                    isSearching = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Mapas offline")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    )
    { paddingValues ->

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
                cameraState = cameraState,
                onMapClick = { point: Position, screenPoint: DpOffset ->
                    val latLng = LatLng(point.latitude, point.longitude)
                    viewModel.onMapTap(latLng)
                    focusManager.clearFocus()
                    ClickResult.Pass
                }
            ) {
                BoundingBoxOverlay(state)

                // Show user location marker if available
                userLocation?.let { latLng ->
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
                        id = "offline-user-location",
                        source = locationSource,
                        radius = const(10.dp),
                        color = const(Color(0xFF4285F4)),
                        strokeWidth = const(2.dp),
                        strokeColor = const(Color.White)
                    )
                }
            }

            // Search bar overlay (now relative to Box top, which is below TopAppBar)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pesquisar morada...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() })
                )
            }

            // UI controls overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val hint = when {
                    state.point1 == null -> "Toque para definir o primeiro canto"
                    state.point2 == null -> "Toque para definir o segundo canto"
                    else -> "Caixa de mapa pronta - Toque no mapa para reiniciar"
                }

                Surface(
                    shape = Shapes.medium,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 80.dp) // Offset hint from search bar
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        @Suppress("DEPRECATION")
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Center on Me Button
                    SmallFloatingActionButton(
                        onClick = {
                            userLocation?.let {
                                coroutineScope.launch {
                                    cameraState.animateTo(
                                        CameraPosition(
                                            target = Position(it.longitude, it.latitude),
                                            zoom = 13.0
                                        )
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Minha Localização")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isComplete) {
                            Button(
                                shape = Shapes.medium,
                                onClick = {
                                    coroutineScope.launch {
                                        val pack = offlineManager.create(
                                            definition = OfflinePackDefinition.TilePyramid(
                                                styleUrl = "https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}",
                                                bounds = state.toBoundingBox() ?: return@launch,
                                                minZoom = 10,
                                                maxZoom = 16
                                            )
                                        )
                                        val uniqueId = java.util.UUID.randomUUID().toString()
                                        val metadataJson = org.json.JSONObject().apply {
                                            put("id", uniqueId)
                                            put("name", searchQuery.ifBlank { "Mapa ${uniqueId.take(5)}" }) 
                                        }

                                        pack.setMetadata(metadataJson.toString().toByteArray(Charsets.UTF_8))
                                        offlineManager.resume(pack)
                                        Toast.makeText(context, "A iniciar transferência...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Transferir")
                            }

                            Button(
                                shape = Shapes.medium,
                                onClick = { viewModel.reset() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Repor")
                            }
                        }
                    }
                }
            }
        }
    }
}