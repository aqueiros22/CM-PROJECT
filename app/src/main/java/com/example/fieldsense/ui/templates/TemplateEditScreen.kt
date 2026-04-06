package com.example.fieldsense.ui.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fieldsense.data.model.Question
import com.example.fieldsense.data.model.QuestionType
import com.example.fieldsense.data.model.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    template: Template?,
    templateViewModel: TemplateViewModel,
    onBack: () -> Unit
) {
    val isEditing = template != null

    var name by rememberSaveable { mutableStateOf(template?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(template?.description ?: "") }
    var questionsLoaded by remember { mutableStateOf(!isEditing) }
    var questions by remember { mutableStateOf(emptyList<Question>()) }

    val existingQuestions by templateViewModel.getQuestionsForTemplate(
        template?.id ?: -1
    ).collectAsState(initial = emptyList())

    if (isEditing && !questionsLoaded && existingQuestions.isNotEmpty()) {
        questions = existingQuestions
        questionsLoaded = true
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val canSave = name.isNotBlank() && questions.isNotEmpty() && questions.all { it.text.isNotBlank() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            if (isEditing) "Editar Formulário" else "Novo Formulário",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            if (isEditing) "Modifica as perguntas" else "Define as perguntas",
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
                    TextButton(
                        onClick = {
                            if (isEditing) {
                                templateViewModel.updateTemplateWithQuestions(
                                    template!!.copy(name = name, description = description),
                                    questions
                                )
                            } else {
                                templateViewModel.insertTemplateWithQuestions(
                                    name, description, questions
                                )
                            }
                            onBack()
                        },
                        enabled = canSave
                    ) {
                        Text(
                            "Guardar",
                            fontWeight = FontWeight.SemiBold,
                            color = if (canSave) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color(0xF0E8F5E9)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Informação
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color(0xF0E8F5E9))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Informação",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Formulário") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Cabeçalho de perguntas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Perguntas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                FilledTonalIconButton(
                    onClick = {
                        questions = questions + Question(
                            templateId = template?.id ?: -1,
                            text = "",
                            type = QuestionType.CHECKBOX,
                            order = questions.size
                        )
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color(0xF0E8F5E9)
                    )
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar Pergunta", tint = Color.Black)
                }
            }

            if (questions.isEmpty()) {
                Text(
                    if (isEditing && !questionsLoaded) "A carregar perguntas..."
                    else "Nenhuma pergunta ainda. Clica '+' para adicionar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = questions,
                        key = { index, question -> "${question.id}_$index" }
                    ) { index, question ->
                        QuestionEditor(
                            question = question,
                            onUpdate = { updated ->
                                questions = questions.toMutableList().also { it[index] = updated }
                            },
                            onDelete = {
                                questions = questions
                                    .toMutableList()
                                    .also { it.removeAt(index) }
                                    .mapIndexed { i, q -> q.copy(order = i) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditor(
    question: Question,
    onUpdate: (Question) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = Color(0xF0E8F5E9))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = question.text,
                    onValueChange = { onUpdate(question.copy(text = it)) },
                    label = { Text("Pergunta") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                IconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remover",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when (question.type) {
                        QuestionType.CHECKBOX -> "Checkbox"
                        QuestionType.TEXT -> "Texto livre"
                        QuestionType.RATING -> "Avaliação (1-5)"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de resposta") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    QuestionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(when (type) {
                                    QuestionType.CHECKBOX -> "Checkbox"
                                    QuestionType.TEXT -> "Texto livre"
                                    QuestionType.RATING -> "Avaliação (1-5)"
                                })
                            },
                            onClick = {
                                onUpdate(question.copy(type = type))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}