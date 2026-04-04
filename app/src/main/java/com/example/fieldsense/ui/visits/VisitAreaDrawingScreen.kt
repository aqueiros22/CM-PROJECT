package com.example.fieldsense.ui.visits

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.data.model.Visit
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.offline.OfflineManager
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.fieldsense.ui.map.MapsChoiceScreen
import org.maplibre.compose.offline.OfflinePackDefinition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitAreaDrawingScreen(
    visit: Visit,
    onSave: (String?) -> Unit,
    onMapAttach: (String?) -> Unit,
    onBack: () -> Unit,
    offlineManager: OfflineManager
) {
    val context = LocalContext.current
    val points = remember { mutableStateListOf<Position>() }
    val hasMap: Boolean = visit.map != null



    if (!hasMap ) {
        MapsChoiceScreen(
            packNotFound = false,
            onBack = onBack,
            onMapAttach = onMapAttach,
            offlineManager = offlineManager
        )
        return@VisitAreaDrawingScreen
    }

    val mapPackName = visit.map

    val pack = offlineManager.packs.find { pack ->
        try {
            val json = pack.metadata?.decodeToString() ?: ""
            org.json.JSONObject(json).optString("name", "") == mapPackName
        } catch (e: Exception) {
            false
        }
    }

    if (pack == null ) {
        MapsChoiceScreen(
            packNotFound = true,
            onBack = onBack,
            onMapAttach = onMapAttach,
            offlineManager = offlineManager
        )
        return@VisitAreaDrawingScreen
    }

    var centerLat by rememberSaveable { mutableStateOf<Double?>(-9.1399) }
    var centerLng by rememberSaveable { mutableStateOf<Double?>(38.7169) }

    val getMapCenter = { mapName: String ->

        pack?.let { pack ->
            val definition = pack.definition
            if (definition is OfflinePackDefinition.TilePyramid) {
                val bounds = definition.bounds
                centerLat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2
                centerLng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2

            }}
    }

    LaunchedEffect( mapPackName) {
        if (mapPackName != null) {
            getMapCenter(mapPackName)
        }
    }

    // Initialize points from visit.area if present
    LaunchedEffect(visit.area) {
        if (points.isEmpty()) {
            visit.area?.let { areaStr ->
                areaStr.split(";").filter { it.isNotBlank() }.forEach { pair ->
                    val coords = pair.split(",")
                    if (coords.size == 2) {
                        try {
                            points.add(Position(coords[1].toDouble(), coords[0].toDouble()))
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                }
            }
        }
    }

    // Extract center from visit.location
    val center = remember(visit.location) {
        try {
            val cleanText = visit.location.replace(" (Last known)", "").trim()
            val parts = cleanText.split(",")
            if (parts.size == 2) {
                Position(parts[1].trim().toDouble(), parts[0].trim().toDouble())
            } else {
                Position(-9.1399, 38.7169)
            }
        } catch (e: Exception) {
            Position(-9.1399, 38.7169)
        }
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = if (centerLat != null && centerLng != null) Position(centerLng!!, centerLat!!) else Position(-8.6291, 41.1579),
            zoom = 15.0
        )
    )

    LaunchedEffect(centerLat, centerLng) {
        if (centerLat != null && centerLng != null) {cameraState.animateTo(
            CameraPosition(
                target = Position(centerLng!!, centerLat!!),
                zoom = 14.0 // Slightly zoomed out to see the region
            )
        )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desenhar Área da Visita", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (points.size >= 3) {
                                val areaStr = points.joinToString(";") { "${it.latitude},${it.longitude}" }
                                onSave(areaStr)
                            } else if (points.isEmpty()) {
                                onSave(null)
                            } else {
                                Toast.makeText(context, "Marca pelo menos 3 pontos", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = { if (points.isNotEmpty()) points.removeAt(points.size - 1) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                }
                FloatingActionButton(
                    onClick = { points.clear() },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(Icons.Filled.DeleteSweep, "Clear All")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
                cameraState = cameraState,
                onMapClick = { position, _ ->
                    points.add(position)
                    ClickResult.Consume
                }
            ) {
                // Visit location marker
                val centerSource = rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        FeatureCollection(
                            Feature(
                                geometry = Point(center),
                                properties = JsonObject(content = emptyMap())
                            )
                        )
                    )
                )
                CircleLayer(
                    id = "visit-location-marker",
                    source = centerSource,
                    radius = const(8.dp),
                    color = const(MaterialTheme.colorScheme.primary)
                )

                // Point markers
                if (points.isNotEmpty()) {
                    val pointsSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(
                                points.map { Feature(geometry = Point(it), properties = JsonObject(emptyMap())) }
                            )
                        )
                    )
                    CircleLayer(
                        id = "draw-points",
                        source = pointsSource,
                        radius = const(6.dp),
                        color = const(Color.White),
                        strokeColor = const(MaterialTheme.colorScheme.primary),
                        strokeWidth = const(2.dp)
                    )
                }

                // Polygon
                if (points.size >= 2) {
                    val geoJson = buildString {
                        append("""{"type": "Feature", "geometry": {"type": "Polygon", "coordinates": [[""")
                        points.forEachIndexed { index, pos ->
                            if (index > 0) append(",")
                            append("[${pos.longitude}, ${pos.latitude}]")
                        }
                        append(",[${points.first().longitude}, ${points.first().latitude}]")
                        append("""]]}}""")
                    }

                    val polygonSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson))

                    FillLayer(
                        id = "area-fill",
                        source = polygonSource,
                        color = const(Color(0xFF4CAF50)),
                        opacity = const(0.3f)
                    )
                    LineLayer(
                        id = "area-outline",
                        source = polygonSource,
                        color = const(Color(0xFF4CAF50)),
                        width = const(3.dp)
                    )
                }
            }
        }
    }
}