package com.example.fieldsense.ui.map

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position


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
fun OfflineMapScreen(onBack: () -> Unit, offlineManager: OfflineManager, viewModel: BoundingBoxViewModel = viewModel()) {
    val context = LocalContext.current
    val state = viewModel.state
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position( -8.6291, 41.1579),
            zoom = 5.0
        )
    )
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {

            TopAppBar(
                title = { Text("Mapas offline") },
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
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            // Map (background)
            MaplibreMap(
                modifier = Modifier.fillMaxSize(),
                baseStyle = BaseStyle.Uri(
                    "https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"
                ),
                cameraState = cameraState,
                onMapClick = { point: Position, _ ->
                    val latLng = LatLng(point.latitude, point.longitude)
                    viewModel.onMapTap(latLng)
                    ClickResult.Pass
                }
            ) {
                BoundingBoxOverlay(state)
            }

            // Top hint card
            val hint = when {
                state.point1 == null -> "Toque para definir o primeiro canto"
                state.point2 == null -> "Toque para definir o segundo canto"
                else -> "Área pronta — toque no mapa para reiniciar"
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(paddingValues).fillMaxHeight()
            ) {
                Surface(
                    modifier = Modifier
                        .padding(16.dp),
                    shape = Shapes.medium,
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Bottom actions
                if (state.isComplete) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        //horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

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
                                }
                            }
                        ) {
                            Text("Download")
                        }

                        Button(
                            shape = Shapes.medium,
                            onClick = { viewModel.reset() }
                        ) {
                            Text("Reset")
                        }
                    }
                }
            }

        }
    }

/*    Box(modifier = Modifier.fillMaxSize()) {
    MaplibreMap(
        modifier = Modifier,
        baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
        cameraState = cameraState,
        onMapClick = {point: Position, screenPoint: DpOffset ->
            Toast.makeText(context, "Mapa clicado nas coordenadas: $point", Toast.LENGTH_SHORT).show()
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
            state.point1 == null -> "Toque para definir o primeiro canto do mapa a transferir"
            state.point2 == null -> "Toque para definir o segundo canto do mapa a transferir"
            else -> "Caixa de mapa pronta - Toque no mapa para reiniciar"
        }
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)

        ) {
            Button(onClick = onBack, shape = Shapes.medium) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
            }
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

    }*/
}
