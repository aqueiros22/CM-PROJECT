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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Editar Template" else "Novo Template",
                        fontWeight = FontWeight.SemiBold
                    )
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
                        enabled = name.isNotBlank() &&
                                questions.isNotEmpty() &&
                                questions.all { it.text.isNotBlank() }
                    ) {
                        Text("Guardar", fontWeight = FontWeight.SemiBold)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Informação",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Template") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Perguntas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = {
                        questions = questions + Question(
                            templateId = template?.id ?: -1,
                            text = "",
                            type = QuestionType.CHECKBOX,
                            order = questions.size
                        )
                    }
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Adicionar Pergunta",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (questions.isEmpty()) {
                Text(
                    if (!questionsLoaded) "A carregar perguntas..."
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
                                questions = questions.toMutableList().also {
                                    it[index] = updated
                                }
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    shape = MaterialTheme.shapes.medium
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remover",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = MaterialTheme.shapes.medium
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    QuestionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (type) {
                                        QuestionType.CHECKBOX -> "Checkbox"
                                        QuestionType.TEXT -> "Texto livre"
                                        QuestionType.RATING -> "Avaliação (1-5)"
                                    }
                                )
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