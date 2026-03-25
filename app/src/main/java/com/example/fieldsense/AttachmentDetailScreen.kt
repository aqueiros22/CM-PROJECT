package com.example.fieldsense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.fieldsense.data.Attachment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentDetailScreen(
    attachment: Attachment,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(attachment.fileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (attachment.type == "image") {
                // Mostra a imagem — usa remoteUrl se sincronizado, localPath se não
                val imageSource = if (attachment.isSynced && attachment.remoteUrl.isNotEmpty())
                    attachment.remoteUrl
                else
                    attachment.localPath

                AsyncImage(
                    model = imageSource,
                    contentDescription = attachment.fileName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 500.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Para outros ficheiros mostra apenas info
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pré-visualização não disponível\npara este tipo de ficheiro",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info do ficheiro
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Detalhes", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Text("Nome: ${attachment.fileName}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Data: ${attachment.date}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text("Tipo: ${attachment.type}",
                        style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (attachment.isSynced) "✅ Sincronizado" else "Pendente",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}