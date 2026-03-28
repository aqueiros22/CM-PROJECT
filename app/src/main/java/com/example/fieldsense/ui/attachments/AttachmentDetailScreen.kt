package com.example.fieldsense.ui.attachments

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.fieldsense.data.model.Attachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

fun getExtension(fileName: String, mimeType: String): String {
    // Se já tem extensão, usa essa
    if (fileName.contains(".")) return fileName

    // Senão, adiciona com base no mimeType
    return when (mimeType) {
        "application/pdf"  -> "$fileName.pdf"
        "application/msword" -> "$fileName.doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "$fileName.docx"
        "text/plain" -> "$fileName.txt"
        "video/mp4"  -> "$fileName.mp4"
        "audio/mpeg" -> "$fileName.mp3"
        else -> fileName
    }
}
fun getMimeType(fileName: String): String {
    return when (fileName.substringAfterLast(".").lowercase()) {
        "pdf"  -> "application/pdf"
        "doc"  -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls"  -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        "txt"  -> "text/plain"
        "mp4"  -> "video/mp4"
        "mp3"  -> "audio/mpeg"
        else   -> "*/*"
    }
}

suspend fun downloadAndOpen(context: Context, attachment: Attachment) {
    withContext(Dispatchers.Main) {
        try {
            val correctedUrl = attachment.remoteUrl

            val mimeType = getMimeType(attachment.fileName)
            val fileNameWithExtension = getExtension(attachment.fileName, mimeType)

            val request = DownloadManager.Request(android.net.Uri.parse(correctedUrl))
                .setTitle(attachment.fileName)
                .setDescription("A descarregar...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileNameWithExtension
                )
                .setMimeType(getMimeType(attachment.fileName))

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(context, "Download iniciado. Verifica as notificações.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("Download", "Erro: ${e.message}", e)
            Toast.makeText(context, "Erro ao descarregar", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentDetailScreen(
    attachment: Attachment,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("📄", style = MaterialTheme.typography.displayLarge)
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            enabled = !isDownloading,
                            onClick = {
                                if (attachment.isSynced && attachment.remoteUrl.isNotEmpty()) {
                                    // Ficheiro sincronizado — download e abre
                                    coroutineScope.launch {
                                        isDownloading = true
                                        downloadAndOpen(context, attachment)
                                        isDownloading = false
                                    }
                                } else {
                                    // Ficheiro local — abre diretamente
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(
                                                attachment.localPath.toUri(),
                                                getMimeType(attachment.fileName)
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(intent, "Abrir com...")
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Não foi possível abrir o ficheiro",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("A descarregar...")
                            } else {
                                Icon(
                                    Icons.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Abrir com...")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = if (attachment.isSynced) "Sincronizado" else "Pendente",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}