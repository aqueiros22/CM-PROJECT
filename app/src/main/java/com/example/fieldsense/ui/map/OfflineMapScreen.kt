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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
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
import org.maplibre.android.geometry.LatLng
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
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
fun OfflineMapScreen(offlineManager: OfflineManager, viewModel: BoundingBoxViewModel = viewModel()) {
    val context = LocalContext.current
    val state = viewModel.state
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(38.7169, -9.1399),
            zoom = 5.0
        )
    )
    val coroutineScope = rememberCoroutineScope()

    val onPreviewPack = { pack: OfflinePack ->
        val definition = pack.definition
        if (definition is OfflinePackDefinition.TilePyramid) {
            val bounds = definition.bounds
            val centerLat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2
            val centerLng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2

            // store in viewModel so MapScreen can read it
            viewModel.previewLocation = Position(centerLng, centerLat)
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
            //viewModel.previewLocation = null // reset after flying
        }
    }
        Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier,
            baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
            cameraState = cameraState,
            onMapClick = {point: Position, screenPoint: DpOffset ->
                println("Map clicked at: $point")
                Toast.makeText(context, "Map clicked at: $point", Toast.LENGTH_SHORT).show()
                val latLng = LatLng(point.latitude, point.longitude)
                viewModel.onMapTap(latLng)
                ClickResult.Pass
            }
        ) {
            BoundingBoxOverlay(state)
        }

        // UI controls overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // Pushes hint to top, buttons to bottom
        ) {
            val hint = when {
                state.point1 == null -> "Toque para definir o primeiro canto"
                state.point2 == null -> "Toque para definir o segundo canto"
                else -> "Caixa de mapa pronta - Toque no mapa para reiniciar"
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(top = 10.dp)

            ) {
                Surface(
                    shape = Shapes.medium,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
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
            }


            //Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.padding(top = 30.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isComplete) {
                    Log.d("BoundingBoxState", "isComplete: ${state.isComplete}")
                    Button(shape = Shapes.medium, onClick = {
                        coroutineScope.launch {
                            // 1. Create the pack (starts paused)
                            val pack = offlineManager.create(
                                definition = OfflinePackDefinition.TilePyramid(
                                    styleUrl = "https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}",
                                    bounds = state.toBoundingBox() ?: return@launch,
                                    minZoom = 10,
                                    maxZoom = 16
                                )
                            )
                            // 2. Start the download
                            offlineManager.resume(pack)

                        }
                    }) {
                        Text("Download")
                    }
                    if (state.point1 != null) {
                        Button(shape = Shapes.medium, onClick = { viewModel.reset() }) {
                            Text("Reset")
                        }
                    }

                }
            }


        }

        }
}
