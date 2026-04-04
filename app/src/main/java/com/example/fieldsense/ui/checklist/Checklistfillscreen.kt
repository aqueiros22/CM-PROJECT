package com.example.fieldsense.ui.checklist

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fieldsense.data.model.Answer
import com.example.fieldsense.data.model.QuestionType
import com.example.fieldsense.data.model.VisitChecklist
import com.example.fieldsense.ui.checklist.ChecklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistFillScreen(
    checklist: VisitChecklist,
    checklistViewModel: ChecklistViewModel,
    onBack: () -> Unit
) {
    // As answers já existem na BD (criadas vazias pelo createChecklistFromTemplate)
    // O user vai preencher os valores aqui
    val savedAnswers by checklistViewModel.getAnswersForChecklist(checklist.id).collectAsState()

    // Mapa local: answer.id -> valor atual (editável pelo user sem tocar na BD)
    val localValues = remember { mutableStateMapOf<Int, String>() }

    // Quando as answers carregarem da BD, inicializar o mapa local
    LaunchedEffect(savedAnswers) {
        savedAnswers.forEach { answer ->
            if (!localValues.containsKey(answer.id)) {
                localValues[answer.id] = answer.value
            }
        }
    }

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showSaveSuccess by rememberSaveable { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    val answeredCount by remember(localValues, savedAnswers) {
        derivedStateOf { localValues.count { it.value.isNotBlank() } }
    }
    val totalCount = savedAnswers.size

    val progress by remember(answeredCount, totalCount) {
        derivedStateOf {
            if (totalCount == 0) 0f else answeredCount.toFloat() / totalCount.toFloat()
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "progress"
    )

    fun hasUnsavedChanges(): Boolean =
        savedAnswers.any { answer -> localValues[answer.id] != answer.value }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = Color(0xF0E8F5E9),
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Sair sem guardar?", fontWeight = FontWeight.Bold) },
            text = { Text("As alterações não guardadas serão perdidas.") },
            confirmButton = {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Continuar") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            checklist.templateName,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            if (totalCount == 0) "A carregar..."
                            else "$answeredCount de $totalCount respondidas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges()) showExitDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Progresso",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Button(
                        onClick = {
                            isSaving = true
                            // Reconstruir answers com os valores preenchidos pelo user
                            val updatedAnswers = savedAnswers.map { answer ->
                                answer.copy(value = localValues[answer.id] ?: answer.value)
                            }
                            // insertChecklistWithAnswers apaga as antigas e insere as novas
                            checklistViewModel.insertChecklistWithAnswers(
                                checklist.copy(isSynced = false),
                                updatedAnswers
                            )
                            isSaving = false
                            showSaveSuccess = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving && totalCount > 0,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar Respostas", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->

        if (savedAnswers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Checklist,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Text(
                        "Nenhuma pergunta neste template.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Adiciona perguntas ao template primeiro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card de info da checklist
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = Color(0xF0E8F5E9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Criada em: ${checklist.date}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Uma card por pergunta
                itemsIndexed(savedAnswers) { index, answer ->
                    val currentValue = localValues[answer.id] ?: answer.value
                    val isAnswered = currentValue.isNotBlank()

                    QuestionCard(
                        index = index + 1,
                        answer = answer,
                        currentValue = currentValue,
                        isAnswered = isAnswered,
                        onValueChange = { newValue -> localValues[answer.id] = newValue }
                    )
                }
            }
        }
    }

    if (showSaveSuccess) {
        LaunchedEffect(showSaveSuccess) {
            kotlinx.coroutines.delay(2500)
            showSaveSuccess = false
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp),
                action = { TextButton(onClick = { showSaveSuccess = false }) { Text("OK") } },
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Text("Respostas guardadas com sucesso!")
                }
            }
        }
    }
}

@Composable
private fun QuestionCard(
    index: Int,
    answer: Answer,
    currentValue: String,
    isAnswered: Boolean,
    onValueChange: (String) -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isAnswered) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(300),
        label = "borderColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isAnswered) 1.5.dp else 0.dp,
            color = borderColor
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isAnswered) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAnswered) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                "$index",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    text = answer.questionText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (answer.questionType) {
                QuestionType.CHECKBOX -> {
                    CheckboxQuestion(value = currentValue, onValueChange = onValueChange)
                }
                QuestionType.TEXT -> {
                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Resposta") },
                        shape = MaterialTheme.shapes.medium,
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
                QuestionType.RATING -> {
                    RatingQuestion(value = currentValue, onValueChange = onValueChange)
                }
            }
        }
    }
}

@Composable
private fun CheckboxQuestion(value: String, onValueChange: (String) -> Unit) {
    // value: "true" | "false" | ""
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = value == "true",
            onClick = { onValueChange(if (value == "true") "" else "true") },
            label = { Text("Sim / Conforme") },
            leadingIcon = if (value == "true") {
                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF4CAF50),
                selectedLabelColor = Color.White,
                selectedLeadingIconColor = Color.White
            )
        )
        FilterChip(
            selected = value == "false",
            onClick = { onValueChange(if (value == "false") "" else "false") },
            label = { Text("Não / Não conforme") },
            leadingIcon = if (value == "false") {
                { Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
            } else null,
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
            )
        )
    }
}

@Composable
private fun RatingQuestion(value: String, onValueChange: (String) -> Unit) {
    // value: "1" a "5" ou ""
    val selectedRating = value.toIntOrNull() ?: 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..5).forEach { rating ->
            IconButton(
                onClick = { onValueChange(if (selectedRating == rating) "" else rating.toString()) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (rating <= selectedRating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$rating estrelas",
                    tint = if (rating <= selectedRating) Color(0xFFFFC107)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        if (selectedRating > 0) {
            Text(
                "($selectedRating/5)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
