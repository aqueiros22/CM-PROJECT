package com.example.fieldsense.ui.visits

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.data.model.Attachment
import com.example.fieldsense.data.model.Note
import com.example.fieldsense.data.model.Visit
import com.example.fieldsense.ui.attachments.AttachmentDetailScreen
import com.example.fieldsense.ui.attachments.AttachmentViewModel
import com.example.fieldsense.ui.notes.NoteDetailScreen
import com.example.fieldsense.ui.notes.NoteViewModel
import com.example.fieldsense.ui.utils.DeleteConfirmationDialog
import kotlinx.serialization.json.JsonObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitDetailScreen(
    visit: Visit,
    visitViewModel: VisitViewModel,
    noteViewModel: NoteViewModel,
    attachmentViewModel: AttachmentViewModel,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int) -> Unit = {}
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

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = "img_${System.currentTimeMillis()}.jpg"
            attachmentViewModel.insertAttachment(visit.id, fileName, it, "image")
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = context.contentResolver.query(
                it, null, null, null, null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "file_${System.currentTimeMillis()}"

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            visit.name,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Visit Details & Notes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val notesText = notes.joinToString("\n\n") { "${it.date}:\n${it.content}" }
                        val shareContent = "Visit: ${visit.name}\nLocation: ${visit.location}\nCode: ${visit.code}\n\nNotes:\n$notesText"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareContent)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Export Visit Data")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share All")
                    }
                    IconButton(onClick = { showEditVisitDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Visit")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = Color(0xF0E8F5E9),
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddNoteDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Note") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visit Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xF0E8F5E9)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(visit.date, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(visit.location, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.QrCode, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(visit.code, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                        }
                    }
                }
            }

            // Map Section
            item {
                VisitAreaMapSection(visit = visit, onEditArea = { onNavigateToDrawing(visit.id) })
            }

            // Attachments Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Attachments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = { imagePickerLauncher.launch("image/*") },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xF0E8F5E9)
                            )) {
                            Icon(Icons.Filled.Image, contentDescription = "Add Image", tint = Color.Black)
                        }
                        FilledTonalIconButton(onClick = { filePicker.launch("*/*") },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xF0E8F5E9)
                            )) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Add File", tint = Color.Black)
                        }
                    }
                }
            }

            if (attachments.isEmpty()) {
                item {
                    Text(
                        "No attachments yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
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

            // Notes Section
            item {
                Text(
                    "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (notes.isEmpty()) {
                item {
                    Text(
                        "No notes recorded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        onDelete = { noteViewModel.deleteNote(note) },
                        onClick = { selectedNoteId = note.id },


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
fun VisitAreaMapSection(visit: Visit, onEditArea: () -> Unit) {
    val context = LocalContext.current
    val points = remember(visit.area) {
        val list = mutableListOf<Position>()
        visit.area?.let { areaStr ->
            areaStr.split(";").forEach { pair ->
                val coords = pair.split(",")
                if (coords.size == 2) {
                    try {
                        list.add(Position(coords[1].toDouble(), coords[0].toDouble()))
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
        }
        list
    }

    // Extract center from visit.location
    val center = remember(visit.location) {
        try {
            val cleanText = visit.location.replace(" (Last known)", "").trim()
            val parts = cleanText.split(",")
            if (parts.size == 2) {
                Position(parts[1].trim().toDouble(), parts[0].trim().toDouble())
            } else {
                Position(-9.1399, 38.7169)
            }
        } catch (e: Exception) {
            Position(-9.1399, 38.7169)
        }
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = center,
            zoom = 15.0
        )
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Visit Area",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onEditArea) {
                Text(if (points.isEmpty()) "Define Area" else "Edit Area")
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    baseStyle = BaseStyle.Uri("https://api.maptiler.com/maps/hybrid-v4/style.json?key=${BuildConfig.MAPTILER_API_KEY}"),
                    cameraState = cameraState,
                    onMapClick = { _, _ -> ClickResult.Consume }
                ) {
                    // Visit location marker
                    val centerSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(
                            FeatureCollection(
                                Feature(
                                    geometry = Point(center),
                                    properties = JsonObject(content = emptyMap())
                                )
                            )
                        )
                    )
                    CircleLayer(
                        id = "visit-location-marker",
                        source = centerSource,
                        radius = const(6.dp),
                        color = const(MaterialTheme.colorScheme.primary)
                    )

                    // Polygon preview
                    if (points.size >= 2) {
                        val geoJson = buildString {
                            append("""{"type": "Feature", "geometry": {"type": "Polygon", "coordinates": [[""")
                            points.forEachIndexed { index, pos ->
                                if (index > 0) append(",")
                                append("[${pos.longitude}, ${pos.latitude}]")
                            }
                            append(",[${points.first().longitude}, ${points.first().latitude}]")
                            append("""]]}}""")
                        }

                        val polygonSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(geoJson))

                        FillLayer(
                            id = "area-fill-preview",
                            source = polygonSource,
                            color = const(Color(0xFF4CAF50)),
                            opacity = const(0.3f)
                        )
                        LineLayer(
                            id = "area-outline-preview",
                            source = polygonSource,
                            color = const(Color(0xFF4CAF50)),
                            width = const(2.dp)
                        )
                    }
                }
                
                if (points.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = CircleShape
                        ) {
                            Text(
                                "No area defined",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentCard(attachment: Attachment, onDelete: () -> Unit, onClick: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Delete Attachment",
            message = "Are you sure you want to delete this attachment?",
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (attachment.type == "image") Icons.Filled.Image else Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = attachment.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!attachment.isSynced) {
                Icon(
                    Icons.Filled.CloudUpload,
                    contentDescription = "Pending Sync",
                    modifier = Modifier.size(16.dp).padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun NoteCard(note: Note, onDelete: () -> Unit, onClick: () -> Unit) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Delete Note",
            message = "Are you sure you want to delete this note?",
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color(0xF0E8F5E9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!note.isSynced) {
                    Icon(
                        Icons.Filled.CloudUpload,
                        contentDescription = "Pending Sync",
                        modifier = Modifier.size(20.dp).padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    note.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, note.content)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp), Color.Black   )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

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
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Edit Field Visit", fontWeight = FontWeight.Bold) },
        containerColor = Color(0xF0E8F5E9),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Site Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(visit.copy(name = name, code = code, location = location, date = date))
                },
                enabled = name.isNotBlank() && code.isNotBlank() && location.isNotBlank() && date.isNotBlank()
            ) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Add New Note", fontWeight = FontWeight.Bold) },
        containerColor = Color(0xF0E8F5E9),
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Observation details...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to record note...")
                        }
                        speechRecognizerLauncher.launch(intent)
                    }) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = "Voice to Text", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content) },
                enabled = content.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) { Text("Record Note") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
