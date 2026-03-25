package com.example.fieldsense.ui.map

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun OfflineMapScreen(viewModel: BoundingBoxViewModel = viewModel()) {
    val context = LocalContext.current
    val state = viewModel.state
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(38.7169, -9.1399),
            zoom = 5.0
        )
    )
    val coroutineScope = rememberCoroutineScope()
    val offlineManager = rememberOfflineManager()

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
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            val hint = when {
                state.point1 == null -> "Tap to set first corner"
                state.point2 == null -> "Tap to set second corner"
                else -> "Box ready — tap map to restart"
            }
            Text(
                text = hint,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.Red
            )
            Spacer(Modifier.height(8.dp))
            if (state.isComplete) {
                Button(onClick = {
                /* use getBoundingBox() result */
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
                    Log.d("Offline", "Download started")

                    // Monitor progress by observing the packs from the manager
                    snapshotFlow { pack.downloadProgress }
                        .collect { progress ->
                            when (progress) {
                                is DownloadProgress.Healthy -> {
                                    val percentage = if (progress.requiredResourceCount > 0) {
                                        100 * progress.completedResourceCount / progress.requiredResourceCount
                                    } else 0
                                    Log.d("Offline", "Downloading: $percentage% — ${progress.completedResourceCount}/${progress.requiredResourceCount} resources")
                                    Log.d("Offline", "Status: ${progress.status}")

                                    if (progress.status == DownloadStatus.Complete) {
                                        Toast.makeText(context, "Download complete!", Toast.LENGTH_SHORT).show()
                                        //OutlinedButton(onClick = {onPreviewPack(pack)}
                                    }
                                }
                                is DownloadProgress.Error -> {
                                    Log.d("Offline", "Download error!")
                                    Log.d("Offline", "Error reason: ${progress.reason}")
                                }

                                else -> Log.d("Offline", "Unknown download progress")

                            }
                        }

                }
                }) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download")
                }
                Spacer(Modifier.height(4.dp))
            }
            if (state.point1 != null) {
                OutlinedButton(onClick = { viewModel.reset() }) {
                    Text("Reset")
                }
            }
        }
    }
}