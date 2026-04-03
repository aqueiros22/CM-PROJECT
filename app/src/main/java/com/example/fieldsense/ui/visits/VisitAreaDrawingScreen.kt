package com.example.fieldsense.ui.visits

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.data.model.Visit
import kotlinx.serialization.json.JsonObject
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitAreaDrawingScreen(
    visit: Visit,
    onSave: (String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val points = remember { mutableStateListOf<Position>() }

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
            target = center,
            zoom = 15.0
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draw Visit Area", fontWeight = FontWeight.Bold) },
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
                                Toast.makeText(context, "Need at least 3 points", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("SAVE", fontWeight = FontWeight.Bold)
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "Tap on the map to define the perimeter. Minimum 3 points.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}