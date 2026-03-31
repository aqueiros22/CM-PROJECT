package com.example.fieldsense.ui.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fieldsense.data.model.Visit


@Composable
fun VisitCard(visit: Visit, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = visit.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (visit.isSynced) Icons.Filled.Check else Icons.Filled.Build,
                        contentDescription = if (visit.isSynced) "Synced" else "Pending Sync",
                        modifier = Modifier.size(16.dp),
                        tint = if (visit.isSynced) Color(0xFF4CAF50) else Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Code: ${visit.code}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Location: ${visit.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = visit.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (visit.isSynced) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete Visit")
            }
        }
    }
}
@Composable
fun AddVisitDialog(
    initialLocation: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
){
    var code by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf(initialLocation) }

    LaunchedEffect(initialLocation) {
        location = initialLocation
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                "New Visit",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Site Name") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (location == "Fetching location..." || location == "Finding address...") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code, name, location) },
                enabled = code.isNotBlank() &&
                        name.isNotBlank() &&
                        location.isNotBlank() &&
                        location != "Fetching location..." &&
                        location != "Finding address...",
                shape = MaterialTheme.shapes.small
            ) {
                Text("Save Visit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
