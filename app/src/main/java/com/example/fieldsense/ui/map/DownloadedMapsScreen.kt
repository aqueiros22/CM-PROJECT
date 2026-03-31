package com.example.fieldsense.ui.map

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fieldsense.ui.theme.Shapes
import kotlinx.coroutines.launch
import org.maplibre.compose.offline.OfflineManager
import org.maplibre.compose.offline.DownloadProgress
import org.maplibre.compose.offline.DownloadStatus
import org.maplibre.compose.offline.OfflinePack
import org.maplibre.compose.offline.OfflinePackDefinition
import org.maplibre.compose.offline.rememberOfflineManager
import org.maplibre.spatialk.geojson.Position

@Composable
fun DownloadedMapsScreen(
    offlineManager: OfflineManager


) {
    val coroutineScope = rememberCoroutineScope()
    var packToDelete by remember { mutableStateOf<OfflinePack?>(null) }
    var centerLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var centerLng by rememberSaveable { mutableStateOf<Double?>(null) }
    val onPreviewPack: (OfflinePack) -> Unit = { pack: OfflinePack ->
        val definition = pack.definition
        if (definition is OfflinePackDefinition.TilePyramid) {
            val bounds = definition.bounds
            centerLat = (bounds.southwest.latitude + bounds.northeast.latitude) / 2
            centerLng = (bounds.southwest.longitude + bounds.northeast.longitude) / 2

        }
    }

    //val currentCenter = center
    if (centerLat != null && centerLng != null) {
        val currentLat = centerLat ?: 0.0
        val currentLng = centerLng ?: 0.0
        // Show the preview screen if a center is set
        PreviewMapScreen(
            onBack = { centerLat = null; centerLng = null },
            previewLocation = Position(currentLng, currentLat) ?: Position(0.0, 0.0)
        )
        return

    }
    // Delete confirmation dialog
    packToDelete?.let { pack ->
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            title = { Text("Delete map") },
            text = { Text("Are you sure you want to delete \"${pack.metadata?.decodeToString()}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            offlineManager.delete(pack)
                        }
                        packToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { packToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {

                Text(
                    text = "Downloaded Maps",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            if (offlineManager.packs.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OfflinePin,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Text(
                            text = "Nenhum mapa transferido",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Text(
                            text = "Transfira um mapa para usar offline",
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(offlineManager.packs.toList()) { pack ->
                        DownloadedMapCard(
                            pack = pack,
                            onPreview = { onPreviewPack(pack) },
                            onDelete = { packToDelete = pack },
                            onPause = { offlineManager.pause(pack) },
                            onResume = { coroutineScope.launch { offlineManager.resume(pack) } }
                        )
                    }
                }
            }
        }



}

@Composable
fun DownloadedMapCard(
    pack: OfflinePack,
    onPreview: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    val packName = remember(pack) {
        pack.metadata?.decodeToString()?.ifEmpty { "Unknown" } ?: "Unknown"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        //shape = RoundedCornerShape(12.dp)
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Icon based on status
                    val iconTint = when (pack.downloadProgress) {
                        is DownloadProgress.Healthy -> Color(0xFF4CAF50)
                        else -> Color(0xFF4285F4)
                    }
                    Icon(
                        imageVector = when (pack.downloadProgress) {
                            is DownloadProgress.Healthy -> Icons.Default.OfflinePin
                            else -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(packName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        // Zoom range
                        val definition = pack.definition
                        if (definition is OfflinePackDefinition.TilePyramid) {
                            Text(
                                text = "Zoom ${definition.minZoom} – ${definition.maxZoom ?: 16}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress / status
            when (val progress = pack.downloadProgress) {
                is DownloadProgress.Healthy -> {
                    if (progress.status == DownloadStatus.Complete) {
                        // Size info
                        val sizeInMb = progress.completedResourceBytes / (1024.0 * 1024.0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Pronto  •  ${"%.1f".format(sizeInMb)} MB",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }

                            // Preview button
                            Button(shape = Shapes.small, modifier = Modifier.padding(6.dp), onClick = { onPreview() }) {
                                Icon(
                                    Icons.Default.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ver mapa", fontSize = 12.sp)
                            }
                        }
                    }
                    val percentage = if (progress.requiredResourceCount > 0) {
                        progress.completedResourceCount.toFloat() / progress.requiredResourceCount.toFloat()
                    } else 0f

                    LinearProgressIndicator(
                        progress = { percentage },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(percentage * 100).toInt()}%  •  ${progress.completedResourceCount}/${progress.requiredResourceCount} recursos",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                    }

/*                    Spacer(modifier = Modifier.height(8.dp))

                    // Pause / Resume
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (progress.status == DownloadStatus.ACTIVE) {
                            OutlinedButton(onClick = onPause) {
                                Icon(
                                    Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause")
                            }
                        } else {
                            OutlinedButton(onClick = onResume) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume")
                            }
                        }
                    }*/
                }


                else -> {}
            }
        }
    }
}
