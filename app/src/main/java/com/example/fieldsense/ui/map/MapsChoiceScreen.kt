package com.example.fieldsense.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.maplibre.compose.offline.OfflineManager
import androidx.compose.material3.TopAppBar
import com.example.fieldsense.ui.theme.FieldSenseGreen
import com.example.fieldsense.ui.theme.Shapes


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsChoiceScreen(
    packNotFound: Boolean,
    onBack: () -> Unit,
    onMapAttach: (String?) -> Unit,
    offlineManager: OfflineManager,

    ){
    val packsState = produceState<List<org.maplibre.compose.offline.OfflinePack>>(initialValue = emptyList()) {
        value = offlineManager.packs.toList()
    }
    var selectedPackName by remember { mutableStateOf<String?>(null) }
    var selectedPackId by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isInfoVisible by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Selecionar Mapa para Visita", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isInfoVisible = !isInfoVisible }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Instruções e Avisos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = if (isInfoVisible) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isInfoVisible) "Recolher" else "Expandir",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            AnimatedVisibility(
                visible = isInfoVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    if (packNotFound) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xA0E8F5E9)
                            ),
                            shape = Shapes.large,
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = FieldSenseGreen
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Nenhum mapa offline encontrado, escolha uma alternativa.",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xA0E8F5E9)
                        ),
                        shape = Shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)

                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = FieldSenseGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column() {
                                Text(
                                    text = "Esta visita não tem um mapa associado.",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Escolha um dos seus mapas transferidos para vincular a esta área.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }


            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (packsState.value.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhum mapa offline encontrado.", color = Color.Gray)
                        }
                    }
                }

                items(packsState.value) { pack ->
                    val packMetadataName = remember(pack) {
                        try {
                            val json = pack.metadata?.decodeToString() ?: ""
                            org.json.JSONObject(json).optString("name", "Mapa sem nome")
                        } catch (e: Exception) {
                            "Mapa Desconhecido "
                        }
                    }

                    val packMetadataId = remember(pack) {
                        try {
                            val json = pack.metadata?.decodeToString() ?: ""
                            org.json.JSONObject(json).optString("id", "")
                        } catch (e: Exception) {
                            ""
                        }
                    }

                    Card(
                        onClick = {
                            selectedPackName = packMetadataName
                            selectedPackId = packMetadataId
                            showConfirmDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xA0E8F5E9)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = Shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = packMetadataName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirmar Associação") },
                text = { Text("Deseja associar o mapa '$selectedPackName' a esta visita?") },
                confirmButton = {
                    Button(onClick = {
                        onMapAttach(selectedPackId)
                        showConfirmDialog = false
                    }) { Text("Confirmar") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }

}