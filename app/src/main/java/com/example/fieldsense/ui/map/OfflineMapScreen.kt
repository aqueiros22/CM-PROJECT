package com.example.fieldsense.ui.map

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.ui.theme.Shapes
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.offline.DownloadProgress
import org.maplibre.compose.offline.DownloadStatus
import org.maplibre.compose.offline.OfflineManager
import org.maplibre.compose.offline.OfflinePack
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.compose.offline.rememberOfflineManager
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position


@Composable
fun BoundingBoxOverlay(state: BoundingBoxState) {
    val p1 = state.point1
    val p2 = state.point2

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
}



@Composable
fun OfflineMapScreen(
    offlineManager: OfflineManager, 
    viewModel: BoundingBoxViewModel = viewModel(),
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current
    val state = viewModel.state
    val userLocation by locationViewModel.location
    
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = userLocation?.let { Position(it.longitude, it.latitude) } ?: Position(-9.1399, 38.7169),
            zoom = if (userLocation != null) 13.0 else 5.0
        )
    )
    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(viewModel.previewLocation) {
        viewModel.previewLocation?.let { position ->
            cameraState.animateTo(
                CameraPosition(
                    target = position,
                    zoom = 13.0
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier,
            baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
            cameraState = cameraState,
            onMapClick = {point: Position, screenPoint: DpOffset ->
                val latLng = LatLng(point.latitude, point.longitude)
                viewModel.onMapTap(latLng)
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

        // UI controls overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
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
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 5.dp)
                    )
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
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
