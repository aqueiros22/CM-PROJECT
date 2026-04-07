package com.example.fieldsense.ui.visits

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fieldsense.BuildConfig
import com.example.fieldsense.R
import com.example.fieldsense.data.model.Area
import com.example.fieldsense.data.model.Attachment
import com.example.fieldsense.data.model.Note
import com.example.fieldsense.data.model.Visit
import com.example.fieldsense.data.model.VisitChecklist
import com.example.fieldsense.ui.areas.AreaViewModel
import com.example.fieldsense.ui.attachments.AttachmentDetailScreen
import com.example.fieldsense.ui.attachments.AttachmentViewModel
import com.example.fieldsense.ui.checklist.ChecklistFillScreen
import com.example.fieldsense.ui.checklist.ChecklistViewModel
import com.example.fieldsense.ui.notes.NoteDetailScreen
import com.example.fieldsense.ui.notes.NoteViewModel
import com.example.fieldsense.ui.templates.TemplateViewModel
import com.example.fieldsense.ui.templates.TemplateViewModelFactory
import com.example.fieldsense.ui.theme.FieldSenseGreen
import com.example.fieldsense.ui.utils.DeleteConfirmationDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    checklistViewModel: ChecklistViewModel,
    templateFactory: TemplateViewModelFactory,
    areaViewModel: AreaViewModel,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val notes by noteViewModel.getNotesForVisit(visit.id).collectAsState()
    val attachments by attachmentViewModel.getAttachmentsForVisit(visit.id).collectAsState()
    val areas by areaViewModel.getAreasForVisit(visit.id).collectAsState()

    var showAddNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showEditVisitDialog by rememberSaveable { mutableStateOf(false) }
    var selectedNoteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedNote = notes.find { it.id == selectedNoteId }

    var selectedAttachmentId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedAttachment = attachments.find { it.id == selectedAttachmentId }

    val checklists by checklistViewModel.getChecklistsForVisit(visit.id).collectAsState()
    var showChecklistPicker by rememberSaveable { mutableStateOf(false) }

    var selectedChecklistId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selectedChecklist = checklists.find { it.id == selectedChecklistId }

    val templateViewModel: TemplateViewModel = viewModel(factory = templateFactory)
    val templates by templateViewModel.templates.collectAsState()

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
    if (selectedChecklist != null) {
        ChecklistFillScreen(
            checklist = selectedChecklist,
            checklistViewModel = checklistViewModel,
            onBack = { selectedChecklistId = null }
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
                            "Detalhes da Visita e Notas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val notesText = notes.joinToString("\n\n") { "${it.date}:\n${it.content}" }
                        val shareContent = "Visita: ${visit.name}\nLocalização: ${visit.location}\nCódigo: ${visit.code}\n\nNotas:\n$notesText"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareContent)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Exportar Dados da Visita")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share))
                    }
                    IconButton(onClick = { showEditVisitDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_visit))
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
                text = { Text(stringResource(R.string.new_note)) },
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
                VisitAreaMapSection(
                    visit = visit,
                    areas = areas,
                    onEditArea = { onNavigateToDrawing(visit.id) }
                )
            }

            // Attachments Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.attachments),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(onClick = { imagePickerLauncher.launch("image/*") },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xF0E8F5E9)
                            )) {
                            Icon(Icons.Filled.Image, contentDescription = "Adicionar Imagem", tint = Color.Black)
                        }
                        FilledTonalIconButton(onClick = { filePicker.launch("*/*") },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = Color(0xF0E8F5E9)
                            )) {
                            Icon(Icons.Filled.AttachFile, contentDescription = "Adicionar Ficheiro", tint = Color.Black)
                        }
                    }
                }
            }

            if (attachments.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_attachments),
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Formulários",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    FilledTonalIconButton(
                        onClick = { showChecklistPicker = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xF0E8F5E9)
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Novo Formulário", tint = Color.Black)
                    }
                }
            }

            if (checklists.isEmpty()) {
                item {
                    Text(
                        "Nenhum formulário associado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                items(checklists) { checklist ->
                    ChecklistCard(
                        checklist = checklist,
                        onClick = { selectedChecklistId = checklist.id },
                        onDelete = { checklistViewModel.deleteChecklist(checklist) }
                    )
                }
            }

            // Notes Section
            item {
                Text(
                    stringResource(R.string.notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (notes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_notes),
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
        if (showChecklistPicker) {
            TemplatePickerDialog(
                templates = templates,
                onDismiss = { showChecklistPicker = false },
                onConfirm = { template ->
                    checklistViewModel.createChecklistFromTemplate(
                        visitId = visit.id,
                        template = template
                    )
                    showChecklistPicker = false
                }
            )
        }
    }
}

@Composable
fun VisitAreaMapSection(visit: Visit, areas: List<Area>, onEditArea: () -> Unit) {
    val context = LocalContext.current
    
    // Check if there is a map attached OR an address to show
    val hasMap = !visit.map.isNullOrBlank()
    val hasArea = areas.isNotEmpty()

    if (!hasMap && !hasArea) {
        // If no map is attached and no area is drawn, show only the "Define Area" button
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.visit_area),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = onEditArea,
                    colors = ButtonDefaults.buttonColors(containerColor = FieldSenseGreen)
                ) {
                    Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.define_area))
                }
            }
        }
        return
    }

    // Parse location (Address or Coordinates)
    var visitPosition by remember { mutableStateOf<Position?>(null) }
    LaunchedEffect(visit.location) {
        withContext(Dispatchers.IO) {
            try {
                val parts = visit.location.split(",")
                if (parts.size == 2 && parts[0].toDoubleOrNull() != null && parts[1].toDoubleOrNull() != null) {
                    visitPosition = Position(parts[1].trim().toDouble(), parts[0].trim().toDouble())
                } else {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(visit.location, 1)
                    if (!addresses.isNullOrEmpty()) {
                        visitPosition = Position(addresses[0].longitude, addresses[0].latitude)
                    }
                }
            } catch (e: Exception) { }
        }
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = visitPosition ?: Position(-9.1399, 38.7169),
            zoom = 15.0
        )
    )

    LaunchedEffect(visitPosition) {
        visitPosition?.let {
            cameraState.animateTo(CameraPosition(target = it, zoom = 15.0))
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.visit_area),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onEditArea) {
                Text(if (hasArea) "Editar Área" else "Definir Área")
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
                    // Draw existing areas
                    areas.forEach { area ->
                        key(area.id) {
                            val posList = area.getPositions()
                            if (posList.size >= 3) {
                                val source = rememberGeoJsonSource(
                                    data = GeoJsonData.JsonString(generatePolygonJson(posList))
                                )
                                FillLayer(
                                    id = "area-fill-${area.id}",
                                    source = source,
                                    color = const(FieldSenseGreen),
                                    opacity = const(0.3f)
                                )
                                LineLayer(
                                    id = "area-line-${area.id}",
                                    source = source,
                                    color = const(FieldSenseGreen),
                                    width = const(2.dp)
                                )
                            }
                        }
                    }

                    // Visit location marker
                    visitPosition?.let { pos ->
                        val centerSource = rememberGeoJsonSource(
                            data = GeoJsonData.Features(
                                FeatureCollection(
                                    listOf(Feature(geometry = Point(pos), properties = JsonObject(emptyMap())))
                                )
                            )
                        )
                        CircleLayer(
                            id = "visit-location-marker",
                            source = centerSource,
                            radius = const(8.dp),
                            color = const(Color.Red),
                            strokeColor = const(Color.White),
                            strokeWidth = const(2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper to generate GeoJSON Polygon string
 */
private fun generatePolygonJson(points: List<Position>): String {
    return buildString {
        append("""{"type": "Feature", "geometry": {"type": "Polygon", "coordinates": [[""")
        points.forEachIndexed { index, pos ->
            if (index > 0) append(",")
            append("[${pos.longitude}, ${pos.latitude}]")
        }
        // Close polygon
        append(",[${points.first().longitude}, ${points.first().latitude}]")
        append("""]]}}""")
    }
}

@Composable
fun AttachmentCard(attachment: Attachment, onDelete: () -> Unit, onClick: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Apagar Anexo",
            message = "Tem a certeza que deseja apagar este anexo?",
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
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (attachment.type == "image") Icons.Filled.Image else Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
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
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    contentDescription = stringResource(R.string.pending),
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
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(16.dp))
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
            title = "Apagar Nota",
            message = "Tem a certeza que deseja apagar esta nota?",
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
                        contentDescription = stringResource(R.string.pending),
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
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.share), modifier = Modifier.size(18.dp), Color.Black   )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(16.dp))
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
        title = { Text(stringResource(R.string.edit_visit), fontWeight = FontWeight.Bold) },
        containerColor = Color(0xF0E8F5E9),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.site_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text(stringResource(R.string.visit_code)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(stringResource(R.string.location)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Data") },
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
            ) { Text(stringResource(R.string.save_changes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
        title = { Text(stringResource(R.string.add_note_title), fontWeight = FontWeight.Bold) },
        containerColor = Color(0xF0E8F5E9),
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.note_details_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-PT")
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale para gravar nota...")
                        }
                        speechRecognizerLauncher.launch(intent)
                    }) {
                        Icon(imageVector = Icons.Filled.Mic, contentDescription = "Voz para Texto", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content) },
                enabled = content.isNotBlank(),
                shape = MaterialTheme.shapes.medium
            ) { Text(stringResource(R.string.record_note)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
fun ChecklistCard(
    checklist: VisitChecklist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Apagar Formulário",
            message = "Tem a certeza que deseja apagar \"${checklist.templateName}\"?",
            onConfirm = { onDelete(); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color(0xF0E8F5E9))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    checklist.templateName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    checklist.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Icon(Icons.Filled.Delete, contentDescription = "Apagar", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun TemplatePickerDialog(
    templates: List<com.example.fieldsense.data.model.Template>,
    onDismiss: () -> Unit,
    onConfirm: (com.example.fieldsense.data.model.Template) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escolher modelo de formulário", fontWeight = FontWeight.Bold) },
        text = {
            if (templates.isEmpty()) {
                Text("Nenhum modelo de formulário disponível. Crie um primeiro.")
            } else {
                LazyColumn {
                    items(templates) { template ->
                        Card(
                            onClick = { onConfirm(template) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(template.name, fontWeight = FontWeight.Medium)
                                if (template.description.isNotBlank()) {
                                    Text(
                                        template.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}