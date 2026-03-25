package com.example.fieldsense

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fieldsense.data.Attachment
import com.example.fieldsense.data.AttachmentViewModel
import com.example.fieldsense.data.Note
import com.example.fieldsense.data.NoteViewModel
import com.example.fieldsense.data.VisitViewModel
import com.example.fieldsense.data.Visit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    visit: Visit,
    visitViewModel: VisitViewModel,
    noteViewModel: NoteViewModel,
    attachmentViewModel: AttachmentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notes by noteViewModel.getNotesForVisit(visit.id).collectAsState()
    val attachments by attachmentViewModel.getAttachmentsForVisit(visit.id).collectAsState()

    var showAddNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showEditVisitDialog by rememberSaveable { mutableStateOf(false) }
    var selectedNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedNote = notes.find { it.id == selectedNoteId }


    var selectedAttachmentId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedAttachment = attachments.find { it.id == selectedAttachmentId }

    // Launcher para escolher imagem da galeria
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            attachmentViewModel.insertAttachment(visit.id, fileName, it, "image")
        }
    }

    // Launcher para escolher qualquer ficheiro
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "file_${System.currentTimeMillis()}"
            attachmentViewModel.insertAttachment(visit.id, fileName, it, "file")
        }
    }

    LaunchedEffect(Unit) {
        noteViewModel.syncPendingNotes()
        attachmentViewModel.syncPendingAttachments()
    }

    if (selectedNote != null) {
        NoteDetailScreen(
            note = selectedNote,
            noteViewModel = noteViewModel,
            onBack = { selectedNoteId = null }
        )
        return
    }

    if (selectedAttachment != null) {
        AttachmentDetailScreen(
            attachment = selectedAttachment,
            onBack = { selectedAttachmentId = null }
        )
        return
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(visit.name, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val notesText = notes.joinToString("\n\n") { "${it.date}:\n${it.content}" }
                        val shareContent = "Visita: ${visit.name}\nLocal: ${visit.location}\nCódigo: ${visit.code}\n\nNotas:\n$notesText"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareContent)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Exportar Notas")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Partilhar Tudo")
                    }
                    IconButton(onClick = { showEditVisitDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar Visita")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddNoteDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Nova Anotação")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Card de detalhes da visita
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Detalhes", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                        Text(visit.date, style = MaterialTheme.typography.bodyLarge)
                        Text(visit.location, style = MaterialTheme.typography.bodyLarge)
                        Text(visit.code, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Secção de anexos
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Anexos", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    Row {
                        // Botão para adicionar imagem
                        IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Icon(Icons.Filled.Image, contentDescription = "Adicionar Imagem",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                        // Botão para adicionar ficheiro
                        IconButton(onClick = { filePicker.launch("*/*") }) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Adicionar Ficheiro",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (attachments.isEmpty()) {
                item {
                    Text("Nenhum anexo ainda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(attachments) { attachment ->
                    AttachmentCard(
                        attachment = attachment,
                        onClick = { selectedAttachmentId = attachment.id },
                        onDelete = { attachmentViewModel.deleteAttachment(attachment) }
                    )
                }
            }

            // Secção de anotações
            item {
                Text("Anotações", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }

            if (notes.isEmpty()) {
                item {
                    Text("Nenhuma anotação ainda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        onDelete = { noteViewModel.deleteNote(note) },
                        onClick = { selectedNoteId = note.id }
                    )
                }
            }
        }

        if (showAddNoteDialog) {
            AddNoteDialog(
                onDismiss = { showAddNoteDialog = false },
                onConfirm = { content ->
                    noteViewModel.insertNote(visit.id, content)
                    showAddNoteDialog = false
                }
            )
        }

        if (showEditVisitDialog) {
            EditVisitDialog(
                visit = visit,
                onDismiss = { showEditVisitDialog = false },
                onConfirm = { updatedVisit ->
                    visitViewModel.updateVisit(updatedVisit)
                    showEditVisitDialog = false
                }
            )
        }
    }
}

@Composable
fun AttachmentCard(attachment: Attachment, onDelete: () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (attachment.type == "image") Icons.Filled.Image else Icons.Filled.AttachFile,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = attachment.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (attachment.isSynced) Icons.Filled.Check else Icons.Filled.Build,
                contentDescription = if (attachment.isSynced) "Synced" else "Pending",
                modifier = Modifier.size(16.dp),
                tint = if (attachment.isSynced) Color(0xFF4CAF50) else Color.Gray
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Apagar",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}

// Composables existentes sem alterações
@Composable
fun EditVisitDialog(
    visit: Visit,
    onDismiss: () -> Unit,
    onConfirm: (Visit) -> Unit
) {
    var code by rememberSaveable { mutableStateOf(visit.code) }
    var name by rememberSaveable { mutableStateOf(visit.name) }
    var location by rememberSaveable { mutableStateOf(visit.location) }
    var date by rememberSaveable { mutableStateOf(visit.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Visita") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Local") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Código") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Localização") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Data") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(visit.copy(name = name, code = code, location = location, date = date))
                },
                enabled = name.isNotBlank() && code.isNotBlank() && location.isNotBlank() && date.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun NoteCard(note: Note, onDelete: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (note.isSynced) Icons.Filled.Check else Icons.Filled.Build,
                contentDescription = if (note.isSynced) "Synced" else "Pending Sync",
                modifier = Modifier.size(16.dp),
                tint = if (note.isSynced) Color(0xFF4CAF50) else Color.Gray
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.content, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(note.date, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row {
                IconButton(onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, note.content)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Partilhar Nota", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Apagar",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
fun AddNoteDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var content by rememberSaveable { mutableStateOf("") }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                content = if (content.isBlank()) spokenText else "$content $spokenText"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Anotação") },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Anotação") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale para escrever a anotação...")
                        }
                        speechRecognizerLauncher.launch(intent)
                    }) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = "Voz para Texto")
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content) },
                enabled = content.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
