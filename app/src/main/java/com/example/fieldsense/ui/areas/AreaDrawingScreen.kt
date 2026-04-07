package com.example.fieldsense.ui.areas

import android.R
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.data.model.Area
import com.example.fieldsense.data.model.Visit
import com.example.fieldsense.ui.map.MapsChoiceScreen
import com.example.fieldsense.ui.theme.FieldSenseGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
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
import java.util.Locale
import kotlin.collections.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaDrawingScreen(
    visit: Visit,
    onBack: () -> Unit,
    offlineManager: OfflineManager,
    onMapAttach: (String?) -> Unit,
    viewModel: AreaViewModel
) {
    val context = LocalContext.current
    val currentPoints = remember { mutableStateListOf<Position>() }
    val coroutineScope = rememberCoroutineScope()

    var menuExpanded by remember { mutableStateOf(false) }
    var areaToDelete by remember { mutableStateOf<Area?>(null) } // Index of area
    var highlightedAreaId by remember { mutableStateOf<Int?>(null) }

    val mapPackId = visit.map

    val pack = remember(offlineManager.packs, mapPackId) {
        offlineManager.packs.find { p ->
            try {
                val json = p.metadata?.decodeToString() ?: ""
                org.json.JSONObject(json).optString("id", "") == mapPackId
            } catch (e: Exception) { false }
        }

    }

    when {
        //  No map is attached to the visit
        mapPackId == null -> {
            MapsChoiceScreen(
                packNotFound = false,
                onBack = onBack,
                onMapAttach = onMapAttach,
                offlineManager = offlineManager
            )
            return
        }

        // A map was attached, but the pack is not found locally
        pack == null -> {
            MapsChoiceScreen(
                packNotFound = true,
                onBack = onBack,
                onMapAttach = onMapAttach,
                offlineManager = offlineManager
            )
            return
        }
    }

    val existingAreas by viewModel.getAreasForVisit(visit.id).collectAsState(initial = emptyList())

    // Parse visit location (Address to Coordinates)
    var visitPosition by remember { mutableStateOf<Position?>(null) }

    LaunchedEffect(visit.location) {
        withContext(Dispatchers.IO) {
            try {
                // Try parsing as "lat, lng" first
                val parts = visit.location.split(",")
                if (parts.size == 2 && parts[0].toDoubleOrNull() != null && parts[1].toDoubleOrNull() != null) {
                    val lat = parts[0].trim().toDouble()
                    val lng = parts[1].trim().toDouble()
                    visitPosition = Position(lng, lat)
                } else {
                    // Try geocoding the address string
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(visit.location, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        visitPosition = Position(address.longitude, address.latitude)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var centerLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var centerLng by rememberSaveable { mutableStateOf<Double?>(null) }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(-9.1399, 38.7169),
            zoom = 15.0
        )
    )

    // Trigger animation when visitPosition is resolved or pack is loaded
    LaunchedEffect(visitPosition, pack) {
        if (visitPosition != null) {
            cameraState.animateTo(
                CameraPosition(
                    target = visitPosition!!,
                    zoom = 15.0
                )
            )
        } else {
            val definition = pack.definition
            if (definition is OfflinePackDefinition.TilePyramid) {
                val bounds = definition.bounds
                centerLat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2
                centerLng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2

                if (centerLat != null && centerLng != null) {
                    cameraState.animateTo(
                        CameraPosition(
                            target = Position(centerLng!!, centerLat!!),
                            zoom = 14.0
                        )
                    )
                }
            }
        }
    }



    areaToDelete?.let { area ->
        AlertDialog(
            onDismissRequest = { areaToDelete = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteArea(area)
                        highlightedAreaId = null
                        areaToDelete = null
                        Toast.makeText(context, "Área removida", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("Confirmar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    highlightedAreaId = null
                    areaToDelete = null
                }) {
                    Text("Cancelar", color = Color.Gray)
                }
            },
            title = { Text("Apagar Área") },
            text = { Text("Tem a certeza que deseja apagar esta área do mapa?") }
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Áreas desenhadas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {

                    // Options Menu
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Opções")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.width(280.dp) // Wider to fit coordinates
                        ) {
                            @Suppress("DEPRECATION")
                            Text(
                                "Áreas Desenhadas",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.primary
                            )

                            HorizontalDivider()

                            if (existingAreas.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Nenhuma área desenhada", color = Color.Gray) },
                                    onClick = { menuExpanded = false },
                                    enabled = false
                                )
                            } else {
                                for (area in existingAreas){
                                    val firstPoint = area.getPositions().first()
                                    DropdownMenuItem(
                                        text = {
                                            @Suppress("DEPRECATION")
                                            Text(
                                            " ${"%.4f".format(firstPoint.latitude)}, ${"%.4f".format(firstPoint.longitude)}",
                                            style = MaterialTheme.typography.bodySmall
                                            ) },
                                        leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
                                        trailingIcon = {
                                            // DELETE ACTION
                                            IconButton(onClick = {
                                                menuExpanded = false
                                                areaToDelete = area
                                                highlightedAreaId = area.id

                                                val firstPoint = area.getPositions().first()
                                                coroutineScope.launch {
                                                    cameraState.animateTo(CameraPosition(target = firstPoint, zoom = 18.0))
                                                }

                                            }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Apagar", tint = Color.Red)
                                            }

                                        },
                                        // ZOOM IN
                                        onClick = {
                                            menuExpanded = false
                                            highlightedAreaId = area.id
                                            coroutineScope.launch {
                                                cameraState.animateTo(
                                                    CameraPosition(target = firstPoint, zoom = 18.0)
                                                )
                                            }
                                            menuExpanded = false
                                        }
                                    )

                                }
                            }
                        }
                    }
                }
            )
        },
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
        ) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
                cameraState = cameraState,
                onMapClick = { position, _ ->
                    highlightedAreaId = null // when user taps map take away higlight
                    currentPoints.add(position)
                    ClickResult.Consume
                }

            ) {
                // Layer 0: Visit Location Circle
                visitPosition?.let { pos ->
                    val visitSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(
                                listOf(Feature(geometry = Point(pos), properties = JsonObject(emptyMap())))
                            )
                        )
                    )
                    CircleLayer(
                        id = "visit-location-layer",
                        source = visitSource,
                        radius = const(10.dp),
                        color = const(Color.Red),
                        strokeColor = const(Color.White),
                        strokeWidth = const(2.dp)
                    )
                }

                // Layer 1: Draw EXISTING areas
                existingAreas.forEach { area ->

                    key(area.id){
                        val posList = area.getPositions()
                        val isHighlighted = highlightedAreaId == area.id

                        if (posList.size >= 3) {
                            val existingSource = rememberGeoJsonSource(
                                data = GeoJsonData.JsonString(generatePolygonJson(posList))
                            )

                            FillLayer(
                                id = "existing-fill-${area.id}",
                                source = existingSource,
                                color = const(if (isHighlighted) Color.Yellow else FieldSenseGreen),
                                opacity = const(if (isHighlighted) 0.6f else 0.2f)
                            )
                            LineLayer(
                                id = "existing-line-${area.id}",
                                source = existingSource,
                                color = const(if (isHighlighted) Color.Yellow else FieldSenseGreen),
                                width = const(if (isHighlighted) 4.dp else 1.dp)
                            )
                        }
                    }

                }

                // Layer 2: Draw CURRENT points
                if (currentPoints.isNotEmpty()) {
                    val pointsSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(
                                currentPoints.map { Feature(geometry = Point(it), properties = JsonObject(emptyMap())) }
                            )
                        )
                    )
                    CircleLayer(
                        id = "current-points-layer",
                        source = pointsSource,
                        radius = const(6.dp),
                        color = const(Color.White),
                        strokeColor = const(MaterialTheme.colorScheme.primary),
                        strokeWidth = const(2.dp)
                    )
                }

                // Layer 3: Draw CURRENT polygon
                if (currentPoints.size >= 2) {
                    val currentPolySource = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(generatePolygonJson(currentPoints))
                    )
                    FillLayer(id = "current-fill", source = currentPolySource, color = const(Color(0xFF4CAF50)), opacity = const(0.4f))
                    LineLayer(id = "current-line", source = currentPolySource, color = const(Color(0xFF4CAF50)), width = const(3.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Toolbar (Visible when drawing )
                AnimatedVisibility(
                    visible = currentPoints.size < 3,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        @Suppress("DEPRECATION")
                        Text(
                            "Toque no mapa para desenhar (mín. 3 pontos)",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    // Floating Action Bar
                    AnimatedVisibility(
                        visible = currentPoints.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(

                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 6.dp,
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // CLEAR BUTTON
                                IconButton(onClick = { currentPoints.clear() }) {
                                    Icon(
                                        Icons.Default.DeleteSweep,
                                        contentDescription = "Limpar",
                                        tint = Color.Red
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                // UNDO BUTTON
                                IconButton(onClick = {
                                    if (currentPoints.isNotEmpty()) currentPoints.removeAt(
                                        currentPoints.size - 1
                                    )
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Desfazer"
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                // Divider between tools and primary action
                                VerticalDivider(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .width(1.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // SAVE BUTTON (Only enabled when valid)
                                Button(
                                    enabled = currentPoints.size >= 3,
                                    onClick = {
                                        val pointsStr =
                                            currentPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
                                        viewModel.insertArea(visit.id, pointsStr)
                                        currentPoints.clear()
                                        Toast.makeText(context, "Área salva!", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FieldSenseGreen
                                    ),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Guardar")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier,
                        verticalArrangement = Arrangement.spacedBy(8.dp)

                    ) {
                        if(highlightedAreaId != null){
                            FloatingActionButton(
                                onClick = { highlightedAreaId = null},
                                containerColor = Color.White
                                ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = "Limpar")
                            }
                        }

                        FloatingActionButton(
                            modifier = Modifier,
                            containerColor = FieldSenseGreen,
                            onClick = {
                                coroutineScope.launch {
                                    val target = visitPosition ?: if (centerLat != null && centerLng != null) {
                                        Position(centerLng!!, centerLat!!)
                                    } else null

                                    target?.let {
                                        cameraState.animateTo(
                                            CameraPosition(
                                                target = it,
                                                zoom = 16.0 // Zoom in more to see the visit location clearly
                                            )
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "Centrar")
                        }
                    }

                }
            }
        }
    }
}

/**
 * Helper to generate GeoJSON Polygon string
 */
private fun generatePolygonJson(points: List<Position>): String {
    return buildString {
        append("""{"type": "Feature", "geometry": {"type": "Polygon", "coordinates": [[""")
        points.forEachIndexed { index, pos ->
            if (index > 0) append(",")
            append("[${pos.longitude}, ${pos.latitude}]")
        }
        // Close polygon
        append(",[${points.first().longitude}, ${points.first().latitude}]")
        append("""]]}}""")
    }
}